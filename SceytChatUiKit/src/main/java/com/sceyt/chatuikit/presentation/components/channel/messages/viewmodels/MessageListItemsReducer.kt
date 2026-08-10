package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytMessageType
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem.DateSeparatorItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem.MessageItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem.UnreadMessagesSeparatorItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.comporators.MessageItemComparator
import com.sceyt.chatuikit.presentation.extensions.getUpdateMessage
import com.sceyt.chatuikit.shared.utils.DateTimeUtil

/** Pure list transformations. The ViewModel remains responsible for mapping domain messages. */
internal class MessageListItemsReducer(
    private val enableDateSeparator: Boolean,
) {
    /** Rebuilds a canonical snapshot while preserving the supplied message order. */
    fun replace(items: List<MessageListItem>): List<MessageListItem> {
        return canonicalize(
            messages = reconcileWindow(current = emptyList(), incoming = items.messages()),
            sourceItems = items,
            hasPrev = items.hasLoadingPrev(),
            hasNext = items.hasLoadingNext(),
        )
    }

    /** Replaces the visible window while reconciling overlapping rows with current state. */
    fun replaceWindow(
        current: List<MessageListItem>,
        incoming: List<MessageListItem>,
    ): List<MessageListItem> {
        val result = canonicalize(
            messages = reconcileWindow(current.messages(), incoming.messages()),
            sourceItems = incoming,
            hasPrev = incoming.hasLoadingPrev(),
            hasNext = incoming.hasLoadingNext(),
        )
        return current.ifSameSnapshot(result)
    }

    /** Upserts an older page before current messages and adopts its previous-edge loader. */
    fun prependPage(
        current: List<MessageListItem>,
        incoming: List<MessageListItem>,
    ): List<MessageListItem> {
        val result = canonicalize(
            messages = upsert(current.messages(), incoming.messages(), prepend = true),
            sourceItems = incoming + current,
            hasPrev = incoming.hasLoadingPrev(),
            hasNext = current.hasLoadingNext(),
        )
        return current.ifSameSnapshot(result)
    }

    /** Upserts a newer page after current messages and adopts its next-edge loader. */
    fun appendPage(
        current: List<MessageListItem>,
        incoming: List<MessageListItem>,
    ): List<MessageListItem> {
        val result = canonicalize(
            messages = upsert(current.messages(), incoming.messages()),
            sourceItems = current + incoming,
            hasPrev = current.hasLoadingPrev(),
            hasNext = incoming.hasLoadingNext(),
        )
        return current.ifSameSnapshot(result)
    }

    /** Reconciles live messages, ignoring unseen tids while a newer-message gap exists. */
    fun appendRealtime(
        current: List<MessageListItem>,
        incoming: List<MessageListItem>,
    ): List<MessageListItem> {
        val currentMessages = current.messages()
        val realtimeMessages = if (current.hasLoadingNext()) {
            val currentTids = currentMessages.mapTo(HashSet(currentMessages.size)) { it.message.tid }
            incoming.messages().filter { it.message.tid in currentTids }
        } else incoming.messages()
        if (realtimeMessages.isEmpty()) return current

        val result = canonicalize(
            messages = upsert(currentMessages, realtimeMessages),
            sourceItems = if (current.hasLoadingNext()) current else current + incoming,
            hasPrev = current.hasLoadingPrev(),
            hasNext = current.hasLoadingNext(),
        )
        return current.ifSameSnapshot(result)
    }

    /** Removes the previous-edge loader if it is present. */
    fun hideLoadingPrev(current: List<MessageListItem>): List<MessageListItem> {
        if (!current.hasLoadingPrev()) return current
        return current.filterNot { it is MessageListItem.LoadingPrevItem }
    }

    /** Removes the next-edge loader if it is present. */
    fun hideLoadingNext(current: List<MessageListItem>): List<MessageListItem> {
        if (!current.hasLoadingNext()) return current
        return current.filterNot { it is MessageListItem.LoadingNextItem }
    }

    /** Removes the unread boundary and repairs the adjacent group/avatar boundary. */
    fun removeUnreadSeparator(current: List<MessageListItem>): List<MessageListItem> {
        if (current.none { it is UnreadMessagesSeparatorItem }) return current
        val source = current.filterNot { it is UnreadMessagesSeparatorItem }
        val result = canonicalize(
            messages = source.messages(),
            sourceItems = source,
            hasPrev = current.hasLoadingPrev(),
            hasNext = current.hasLoadingNext(),
        )
        return current.ifSameSnapshot(result)
    }

    /** Updates one message by tid; returning the original item produces a no-op. */
    fun updateByTid(
        current: List<MessageListItem>,
        tid: Long,
        update: (MessageItem) -> MessageItem,
    ): List<MessageListItem> {
        val index = current.indexOfFirst { it is MessageItem && it.message.tid == tid }
        if (index == -1) return current

        val oldItem = current[index] as MessageItem
        val candidate = update(oldItem)
        require(candidate.message.tid == tid) { "A message update cannot change its tid" }
        if (candidate === oldItem) return current

        val updatedItem = oldItem.normalizeUpdate(candidate)
        if (updatedItem === oldItem) return current

        val source = current.mapIndexed { currentIndex, item ->
            if (currentIndex == index) updatedItem else item
        }
        if (!oldItem.affectsDerivedItemsComparedWith(updatedItem))
            return source

        val result = canonicalize(
            messages = source.messages(),
            sourceItems = source,
            hasPrev = current.hasLoadingPrev(),
            hasNext = current.hasLoadingNext(),
        )
        return current.ifSameSnapshot(result)
    }

    /** Removes matching messages and rebuilds their derived separators and group boundaries. */
    fun deleteByTids(
        current: List<MessageListItem>,
        tids: Set<Long>,
    ): List<MessageListItem> {
        if (tids.isEmpty() || current.none { it is MessageItem && it.message.tid in tids })
            return current

        val result = canonicalize(
            messages = current.messages().filterNot { it.message.tid in tids },
            sourceItems = current,
            hasPrev = current.hasLoadingPrev(),
            hasNext = current.hasLoadingNext(),
        )
        return current.ifSameSnapshot(result)
    }

    /** Deletes messages at or before [createdAt], retaining local pending messages. */
    fun deleteAtOrBeforePreservingPending(
        current: List<MessageListItem>,
        createdAt: Long,
    ): List<MessageListItem> {
        val source = current.filterNot { item ->
            item.getMessageCreatedAt() <= createdAt &&
                    (item !is MessageItem ||
                            item.message.deliveryStatus != MessageDeliveryStatus.Pending)
        }
        if (source.size == current.size) return current

        val result = canonicalize(
            messages = source.messages(),
            sourceItems = source,
            hasPrev = source.hasLoadingPrev(),
            hasNext = source.hasLoadingNext(),
        )
        return current.ifSameSnapshot(result)
    }

    /** Clears selection on every visible message in one list mutation. */
    fun clearSelection(current: List<MessageListItem>): List<MessageListItem> {
        if (current.none { it is MessageItem && it.message.isSelected }) return current
        return current.map { item ->
            if (item is MessageItem && item.message.isSelected)
                item.copy(message = item.message.copy(isSelected = false))
            else item
        }
    }

    /** Reconciles direct row updates and edit-like updates that also affect reply previews. */
    fun reconcileMessages(
        current: List<MessageListItem>,
        rowOnlyUpdates: List<MessageItem>,
        rowAndReplyUpdates: List<MessageItem>,
    ): List<MessageListItem> {
        if (rowOnlyUpdates.isEmpty() && rowAndReplyUpdates.isEmpty()) return current

        val rowUpdatesByTid = LinkedHashMap<Long, MessageItem>()
        rowOnlyUpdates.forEach { item ->
            val existing = rowUpdatesByTid[item.message.tid]
            rowUpdatesByTid[item.message.tid] = existing?.merge(item) ?: item
        }
        val withRepliesByTid = LinkedHashMap<Long, MessageItem>()
        rowAndReplyUpdates.forEach { item ->
            val existing = withRepliesByTid[item.message.tid]
            withRepliesByTid[item.message.tid] = existing?.merge(item) ?: item
        }
        val withRepliesById = withRepliesByTid.values
            .asSequence()
            .filter { it.message.id > 0 }
            .associateBy { it.message.id }

        var changed = false
        var requiresCanonicalization = false
        val updated = current.mapNotNull { listItem ->
            if (listItem !is MessageItem) return@mapNotNull listItem

            val message = listItem.message
            val localDelete = withRepliesByTid[message.tid]
                ?.takeIf { message.id == 0L && it.isPendingDelete() }
            if (localDelete != null) {
                changed = true
                requiresCanonicalization = true
                return@mapNotNull null
            }

            val mainUpdate = withRepliesById[message.id].takeIf { message.id > 0 }
                ?: rowUpdatesByTid[message.tid]
            var result = mainUpdate?.let { listItem.merge(it) } ?: listItem
            if (result !== listItem && listItem.affectsDerivedItemsComparedWith(result))
                requiresCanonicalization = true

            val parent = result.message.parentMessage
            val parentUpdate = parent?.let { parentMessage ->
                withRepliesById[parentMessage.id].takeIf { parentMessage.id > 0 }
                    ?: withRepliesByTid[parentMessage.tid]
                        ?.takeUnless { it.isPendingDelete() }
            }
            if (parent != null && parentUpdate != null) {
                val updatedParent = parent.merge(parentUpdate.message)
                if (updatedParent !== parent)
                    result = result.copy(message = result.message.copy(parentMessage = updatedParent))
            }

            if (result !== listItem) changed = true
            result
        }
        if (!changed) return current
        if (!requiresCanonicalization) return updated

        val result = canonicalize(
            messages = upsert(current = emptyList(), incoming = updated.messages()),
            sourceItems = updated,
            hasPrev = current.hasLoadingPrev(),
            hasNext = current.hasLoadingNext(),
        )
        return current.ifSameSnapshot(result)
    }

    /** Merges an anchored centered window and orders it with the message comparator. */
    fun mergeAroundCenter(
        current: List<MessageListItem>,
        incoming: List<MessageListItem>,
        centerMessageId: Long,
    ): List<MessageListItem> {
        val currentMessages = current.messages()
        if (centerMessageId == 0L || currentMessages.none { it.message.id == centerMessageId })
            return current

        val messages = upsert(currentMessages, incoming.messages())
            .sortedWith(MessageItemComparator())
        val result = canonicalize(
            messages = messages,
            sourceItems = current + incoming,
            hasPrev = current.hasLoadingPrev(),
            hasNext = current.hasLoadingNext(),
        )
        return current.ifSameSnapshot(result)
    }

    /** Message equality is tid-only, so content changes are detected by MessageItem identity. */
    internal fun hasSameItemInstances(
        first: List<MessageListItem>,
        second: List<MessageListItem>,
    ): Boolean {
        if (first.size != second.size) return false
        return first.indices.all { index ->
            val firstItem = first[index]
            val secondItem = second[index]
            if (firstItem is MessageItem && secondItem is MessageItem)
                firstItem === secondItem
            else firstItem == secondItem
        }
    }

    /** Deduplicates by tid, reconciles overlaps in place, and preserves new-item order. */
    private fun upsert(
        current: List<MessageItem>,
        incoming: List<MessageItem>,
        prepend: Boolean = false,
    ): List<MessageItem> {
        val currentByTid = LinkedHashMap<Long, MessageItem>()
        current.forEach { item ->
            val existing = currentByTid[item.message.tid]
            currentByTid[item.message.tid] = existing?.merge(item) ?: item
        }

        val incomingByTid = LinkedHashMap<Long, MessageItem>()
        incoming.forEach { item ->
            val existing = incomingByTid[item.message.tid] ?: currentByTid[item.message.tid]
            incomingByTid[item.message.tid] = existing?.merge(item) ?: item
        }

        val tids = LinkedHashSet<Long>(currentByTid.size + incomingByTid.size)
        if (prepend) incomingByTid.keys.filterTo(tids) { it !in currentByTid }
        tids.addAll(currentByTid.keys)
        if (!prepend) tids.addAll(incomingByTid.keys)
        return tids.map { tid -> incomingByTid[tid] ?: requireNotNull(currentByTid[tid]) }
    }

    /** Keeps only the incoming window while merging overlaps with the current snapshot. */
    private fun reconcileWindow(
        current: List<MessageItem>,
        incoming: List<MessageItem>,
    ): List<MessageItem> {
        val currentByTid = LinkedHashMap<Long, MessageItem>()
        current.forEach { item ->
            val existing = currentByTid[item.message.tid]
            currentByTid[item.message.tid] = existing?.merge(item) ?: item
        }

        val result = LinkedHashMap<Long, MessageItem>()
        incoming.forEach { item ->
            val existing = result[item.message.tid] ?: currentByTid[item.message.tid]
            result[item.message.tid] = existing?.merge(item) ?: item
        }
        return result.values.toList()
    }

    private fun MessageItem.merge(incoming: MessageItem): MessageItem {
        if (this === incoming) return this
        val updated = message.merge(incoming.message)
        return if (updated === message) this else copy(message = updated)
    }

    private fun SceytMessage.merge(incoming: SceytMessage): SceytMessage {
        if (this === incoming) return this
        if (id > 0 && incoming.id == 0L) return this

        val updated = getUpdateMessage(incoming)
        return updated.copy(deliveryStatus = deliveryStatus.advanceTo(incoming.deliveryStatus))
    }

    private fun MessageItem.normalizeUpdate(candidate: MessageItem): MessageItem {
        if (message.id > 0 && candidate.message.id == 0L) return this
        val deliveryStatus = message.deliveryStatus.advanceTo(candidate.message.deliveryStatus)
        return if (deliveryStatus == candidate.message.deliveryStatus) candidate
        else candidate.copy(message = candidate.message.copy(deliveryStatus = deliveryStatus))
    }

    private fun MessageItem.isPendingDelete(): Boolean {
        return message.id == 0L &&
                message.deliveryStatus == MessageDeliveryStatus.Pending &&
                message.state == MessageState.Deleted
    }

    private fun MessageDeliveryStatus.advanceTo(incoming: MessageDeliveryStatus): MessageDeliveryStatus {
        if (this == MessageDeliveryStatus.Failed) return incoming
        if (incoming == MessageDeliveryStatus.Failed)
            return if (this == MessageDeliveryStatus.Pending) incoming else this
        return maxOf(this, incoming)
    }

    private fun MessageItem.affectsDerivedItemsComparedWith(other: MessageItem): Boolean {
        val first = message
        val second = other.message
        return first.id != second.id ||
                first.tid != second.tid ||
                first.createdAt != second.createdAt ||
                first.incoming != second.incoming ||
                first.type != second.type ||
                first.user?.id != second.user?.id ||
                first.isGroup != second.isGroup ||
                first.disabledShowAvatarAndName != second.disabledShowAvatarAndName ||
                first.shouldShowAvatarAndName != second.shouldShowAvatarAndName
    }

    /** Derives loaders, separators, and group boundaries without sorting messages. */
    private fun canonicalize(
        messages: List<MessageItem>,
        sourceItems: List<MessageListItem>,
        hasPrev: Boolean,
        hasNext: Boolean,
    ): List<MessageListItem> {
        val unread = sourceItems.unreadBoundary(messages)
        val normalizedMessages = messages.normalizeGroupAvatars(unread?.targetTid)
        var previousMessageAt: Long? = null
        return buildList {
            if (hasPrev) add(MessageListItem.LoadingPrevItem)
            normalizedMessages.forEach { item ->
                if (enableDateSeparator && previousMessageAt?.let {
                        DateTimeUtil.isSameDay(it, item.message.createdAt)
                    } != true
                ) {
                    add(
                        DateSeparatorItem(
                            createdAt = item.message.createdAt,
                            messageTid = item.message.tid,
                            messageId = item.message.id,
                        )
                    )
                }
                if (unread?.targetTid == item.message.tid)
                    add(unread.separator.copy(createdAt = item.message.createdAt))
                add(item)
                previousMessageAt = item.message.createdAt
            }
            if (hasNext) add(MessageListItem.LoadingNextItem)
        }
    }

    private fun List<MessageItem>.normalizeGroupAvatars(unreadTargetTid: Long?): List<MessageItem> {
        var previous: MessageItem? = null
        return map { item ->
            val message = item.message
            val previousMessage = previous?.message
            val shouldShow = message.incoming && message.isGroup &&
                    !message.disabledShowAvatarAndName &&
                    (message.tid == unreadTargetTid ||
                            previousMessage == null ||
                            previousMessage.user?.id != message.user?.id ||
                            !DateTimeUtil.isSameDay(previousMessage.createdAt, message.createdAt) ||
                            previousMessage.type == SceytMessageType.System.value)
            val normalized = if (message.shouldShowAvatarAndName == shouldShow) item
            else item.copy(message = message.copy(shouldShowAvatarAndName = shouldShow))
            previous = normalized
            normalized
        }
    }

    private fun List<MessageListItem>.unreadBoundary(
        messages: List<MessageItem>,
    ): UnreadBoundary? {
        val separatorIndex = indexOfFirst { it is UnreadMessagesSeparatorItem }
        if (separatorIndex == -1) return null
        val separator = get(separatorIndex) as UnreadMessagesSeparatorItem
        val survivingTids = messages.mapTo(HashSet(messages.size)) { it.message.tid }
        val target = asSequence()
            .drop(separatorIndex + 1)
            .filterIsInstance<MessageItem>()
            .firstOrNull { it.message.tid in survivingTids }
            ?: return null
        return UnreadBoundary(separator = separator, targetTid = target.message.tid)
    }

    private fun List<MessageListItem>.messages() = filterIsInstance<MessageItem>()
    private fun List<MessageListItem>.hasLoadingPrev() = any { it is MessageListItem.LoadingPrevItem }
    private fun List<MessageListItem>.hasLoadingNext() = any { it is MessageListItem.LoadingNextItem }
    /** Returns this list when [other] has the same item sequence and message-row instances. */
    private fun List<MessageListItem>.ifSameSnapshot(other: List<MessageListItem>) =
        if (hasSameItemInstances(this, other)) this else other

    private data class UnreadBoundary(
        val separator: UnreadMessagesSeparatorItem,
        val targetTid: Long,
    )
}
