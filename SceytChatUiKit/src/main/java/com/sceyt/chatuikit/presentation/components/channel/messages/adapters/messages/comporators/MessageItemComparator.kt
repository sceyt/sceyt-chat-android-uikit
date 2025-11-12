package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.comporators

import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem

class MessageItemComparator : Comparator<MessageListItem> {

    private val messageComparator = MessageComparator()

    override fun compare(next: MessageListItem, prev: MessageListItem): Int {
        // If both are MessageItems, delegate to MessageComparator to handle pending messages
        return if (next is MessageListItem.MessageItem && prev is MessageListItem.MessageItem) {
            messageComparator.compare(next.message, prev.message)
        } else {
            // For other item types (separators, loading items), sort by creation time
            next.getMessageCreatedAt().compareTo(prev.getMessageCreatedAt())
        }
    }
}