package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages

import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage

sealed class MessageListItem {
    data class MessageItem(val message: SceytMessage) : MessageListItem()
    data class DateSeparatorItem(val createdAt: Long, val msgTid: Long) : MessageListItem()
    data class UnreadMessagesSeparatorItem(val createdAt: Long, val msgId: Long) : MessageListItem()
    object LoadingPrevItem : MessageListItem()
    object LoadingNextItem : MessageListItem()

    fun getMessageCreatedAt(): Long {
        return when (this) {
            is MessageItem -> message.createdAt
            is DateSeparatorItem -> createdAt
            is UnreadMessagesSeparatorItem -> createdAt
            is LoadingPrevItem -> 0
            is LoadingNextItem -> Long.MAX_VALUE
        }
    }

    fun getItemId(): Long {
        return when (this) {
            is MessageItem -> message.id
            is DateSeparatorItem -> hashCode().toLong()
            is UnreadMessagesSeparatorItem -> hashCode().toLong()
            is LoadingPrevItem -> hashCode().toLong()
            is LoadingNextItem -> hashCode().toLong()
        }
    }

    var highlighted = false
}
