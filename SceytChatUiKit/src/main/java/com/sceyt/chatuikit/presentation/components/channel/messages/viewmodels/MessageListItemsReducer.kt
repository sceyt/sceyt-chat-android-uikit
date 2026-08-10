package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
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
    fun replace(items: List<MessageListItem>): List<MessageListItem> {
        return canonicalize(
            messages = upsert(current = emptyList(), incoming = items.messages()),
            sourceItems = items,
            hasPrev = items.hasLoadingPrev(),
            hasNext = items.hasLoadingNext(),
        )
    }

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

    private fun MessageItem.merge(incoming: MessageItem): MessageItem {
        if (this === incoming) return this
        if (message.id > 0 && incoming.message.id == 0L) return this

        val updated = message.getUpdateMessage(incoming.message)
        val currentStatus = message.deliveryStatus
        val incomingStatus = incoming.message.deliveryStatus
        val deliveryStatus = currentStatus.advanceTo(incomingStatus)
        return copy(message = updated.copy(deliveryStatus = deliveryStatus))
    }

    private fun MessageItem.normalizeUpdate(candidate: MessageItem): MessageItem {
        if (message.id > 0 && candidate.message.id == 0L) return this
        val deliveryStatus = message.deliveryStatus.advanceTo(candidate.message.deliveryStatus)
        return if (deliveryStatus == candidate.message.deliveryStatus) candidate
        else candidate.copy(message = candidate.message.copy(deliveryStatus = deliveryStatus))
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
                first.createdAt != second.createdAt ||
                first.incoming != second.incoming ||
                first.type != second.type ||
                first.user?.id != second.user?.id ||
                first.isGroup != second.isGroup ||
                first.disabledShowAvatarAndName != second.disabledShowAvatarAndName ||
                first.shouldShowAvatarAndName != second.shouldShowAvatarAndName
    }

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
    private fun List<MessageListItem>.ifSameSnapshot(other: List<MessageListItem>) =
        if (hasSameItemInstances(this, other)) this else other

    private data class UnreadBoundary(
        val separator: UnreadMessagesSeparatorItem,
        val targetTid: Long,
    )
}
