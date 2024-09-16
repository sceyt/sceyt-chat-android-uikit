package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.comporators

import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem

class MessageItemComparator : Comparator<MessageListItem> {

    override fun compare(next: MessageListItem, prev: MessageListItem): Int {
        return next.getMessageCreatedAt().compareTo(prev.getMessageCreatedAt())
    }
}