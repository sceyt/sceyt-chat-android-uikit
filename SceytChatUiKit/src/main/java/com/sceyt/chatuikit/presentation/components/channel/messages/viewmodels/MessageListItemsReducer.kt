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
}
