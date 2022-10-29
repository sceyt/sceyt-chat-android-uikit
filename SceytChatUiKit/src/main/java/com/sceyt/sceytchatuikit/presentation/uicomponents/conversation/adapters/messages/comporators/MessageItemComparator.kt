package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.comporators

import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem

class MessageItemComparator : Comparator<MessageListItem> {

    override fun compare(next: MessageListItem, prev: MessageListItem): Int {
        return when {
            (next is MessageListItem.MessageItem) && next.message.deliveryStatus == DeliveryStatus.Pending
                    && (prev is MessageListItem.MessageItem) && prev.message.deliveryStatus == DeliveryStatus.Pending ->
                next.getMessageCreatedAt().compareTo(prev.getMessageCreatedAt())

            (next is MessageListItem.MessageItem) && next.message.deliveryStatus == DeliveryStatus.Pending -> 1
            (next is MessageListItem.LoadingMoreItem) -> 0

            (prev is MessageListItem.MessageItem) && prev.message.deliveryStatus == DeliveryStatus.Pending -> -1
            (prev is MessageListItem.LoadingMoreItem) -> 0

            else -> {
                next.getMessageCreatedAt().compareTo(prev.getMessageCreatedAt())
            }
        }
    }
}