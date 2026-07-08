package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem.MessageItem
import com.sceyt.chatuikit.shared.utils.DateTimeUtil

internal class MessageListItemsReducer {

    data class AppendResult(
        val resultItems: List<MessageListItem>,
        val insertedItems: List<MessageListItem>,
        val changed: Boolean,
    )

    data class DeleteResult(
        val resultItems: List<MessageListItem>,
        val changed: Boolean,
        // False when a day header had to be reassigned to a surviving same-day message: the adapter's
        // incremental delete-by-tid can't express that, so the caller must emit a full Replace instead.
        val canUseIncrementalDelete: Boolean,
    )

    /**
     * Removes message items by tid. A [MessageListItem.DateSeparatorItem] owned by a deleted message is
     * reassigned to the next surviving same-day message (so the day header survives), or dropped when no
     * same-day message remains.
     */
    fun deleteByTids(
        items: List<MessageListItem>,
        tIds: Set<Long>,
        enableDateSeparator: Boolean,
    ): DeleteResult {
        var reassignedSeparator = false
        val result = ArrayList<MessageListItem>(items.size)
        items.forEachIndexed { index, item ->
            when (item) {
                is MessageItem if item.message.tid in tIds -> Unit // drop deleted message
                is MessageListItem.DateSeparatorItem if item.messageTid in tIds -> {
                    val survivor = items
                        .subList(index + 1, items.size)
                        .firstOrNull { it is MessageItem && it.message.tid !in tIds } as? MessageItem
                    if (survivor != null &&
                        DateTimeUtil.isSameDay(item.createdAt, survivor.message.createdAt)
                    ) {
                        result.add(
                            item.copy(
                                messageTid = survivor.message.tid,
                                messageId = survivor.message.id
                            )
                        )
                        reassignedSeparator = true
                    } // else: no same-day survivor, drop the header
                }

                else -> result.add(item)
            }
        }
        val normalized = normalize(result, enableDateSeparator)
        return DeleteResult(
            resultItems = normalized,
            changed = normalized != items,
            canUseIncrementalDelete = !reassignedSeparator && normalized == result
        )
    }

    fun normalize(
        items: List<MessageListItem>,
        enableDateSeparator: Boolean,
    ): List<MessageListItem> {
        if (!enableDateSeparator) return items

        var lastDateSeparator: MessageListItem.DateSeparatorItem? = null
        var changed = false
        val result = ArrayList<MessageListItem>(items.size)
        items.forEach { item ->
            if (item is MessageListItem.DateSeparatorItem) {
                val isDuplicate = lastDateSeparator?.let {
                    DateTimeUtil.isSameDay(it.createdAt, item.createdAt)
                } == true

                if (isDuplicate) {
                    changed = true
                } else {
                    result.add(item)
                    lastDateSeparator = item
                }
            } else {
                result.add(item)
            }
        }
        return if (changed) result else items
    }

    fun mergePrevPage(
        current: List<MessageListItem>,
        newItems: List<MessageListItem>,
        enableDateSeparator: Boolean,
    ): List<MessageListItem> {
        val withoutLoading = current.filterNot { it is MessageListItem.LoadingPrevItem }
        if (newItems.isEmpty()) return withoutLoading

        val result = (newItems + withoutLoading).toMutableList()
        val firstOldItem = withoutLoading.firstOrNull { it is MessageItem } as? MessageItem
        val dateItem = withoutLoading.firstOrNull { item ->
            item is MessageListItem.DateSeparatorItem && item.messageTid == firstOldItem?.message?.tid
        }
        val newLastItem = newItems.lastOrNull()

        if (newLastItem is MessageItem && firstOldItem != null) {
            if (firstOldItem.message.isGroup) {
                val firstOldIndex = result.indexOf(firstOldItem)
                if (firstOldIndex != -1) {
                    val oldMessage = firstOldItem.message
                    result[firstOldIndex] = firstOldItem.copy(
                        message = oldMessage.copy(
                            shouldShowAvatarAndName = oldMessage.incoming &&
                                    oldMessage.user?.id != newLastItem.message.user?.id
                        )
                    )
                }
            }

            val needShowDate = !DateTimeUtil.isSameDay(
                epochOne = firstOldItem.message.createdAt,
                epochTwo = newLastItem.message.createdAt
            )
            if (!needShowDate) {
                val dateIndex = result.indexOf(dateItem)
                if (dateIndex != -1)
                    result.removeAt(dateIndex)
            }
        }
        return normalize(result, enableDateSeparator)
    }

    fun appendNextPage(
        current: List<MessageListItem>,
        newItems: List<MessageListItem>,
    ): AppendResult {
        val withoutLoading = current.filterNot { it is MessageListItem.LoadingNextItem }
        if (newItems.isEmpty()) {
            return AppendResult(
                resultItems = withoutLoading,
                insertedItems = emptyList(),
                changed = withoutLoading != current
            )
        }

        return AppendResult(
            resultItems = withoutLoading + newItems,
            insertedItems = newItems,
            changed = true
        )
    }

    fun appendRealtime(
        current: List<MessageListItem>,
        newItems: List<MessageListItem>,
    ): AppendResult {
        val withoutLoading = current.filterNot { it is MessageListItem.LoadingNextItem }
        val existingItems = withoutLoading.toSet()
        val insertedItems = LinkedHashSet<MessageListItem>()
        newItems.forEach { item ->
            if (item !in existingItems)
                insertedItems.add(item)
        }

        if (insertedItems.isEmpty()) {
            return AppendResult(
                resultItems = current,
                insertedItems = emptyList(),
                changed = false
            )
        }

        return AppendResult(
            resultItems = withoutLoading + insertedItems,
            insertedItems = insertedItems.toList(),
            changed = true
        )
    }

    fun mergeAroundCenter(
        current: List<MessageListItem>,
        newItems: List<MessageListItem>,
        centerMessageId: Long,
        enableDateSeparator: Boolean,
    ): List<MessageListItem>? {
        if (newItems.isEmpty()) return null
        val hasCenter = current.any { item ->
            item is MessageItem && item.message.id == centerMessageId
        }
        if (!hasCenter) return null

        val sorted = (current + newItems).sortedBy { item -> item.getMessageCreatedAt() }

        val seenTids = HashSet<Long>()
        val deduped = sorted.filter { item ->
            if (item is MessageItem) seenTids.add(item.message.tid) else true
        }
        return normalize(deduped, enableDateSeparator)
    }
}
