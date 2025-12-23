package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages

import com.sceyt.chatuikit.data.models.messages.SceytMessage

sealed class MessageListItem {
    data class MessageItem(val message: SceytMessage) : MessageListItem()
    data class DateSeparatorItem(
        val createdAt: Long,
        val messageTid: Long,
        val messageId: Long
    ) : MessageListItem()

    data class UnreadMessagesSeparatorItem(val createdAt: Long, val msgId: Long) : MessageListItem()
    data object LoadingPrevItem : MessageListItem()
    data object LoadingNextItem : MessageListItem()

    fun getMessageCreatedAt(): Long {
        return when (this) {
            is MessageItem -> message.createdAt
            is DateSeparatorItem -> createdAt
            is UnreadMessagesSeparatorItem -> createdAt
            is LoadingPrevItem -> 0
            is LoadingNextItem -> Long.MAX_VALUE
        }
    }

    fun getMessageCreatedAtForDateHeader(): Long? {
        return when (this) {
            is MessageItem -> message.createdAt
            is DateSeparatorItem -> createdAt
            is UnreadMessagesSeparatorItem -> createdAt
            is LoadingPrevItem -> null
            is LoadingNextItem -> null
        }
    }

    fun getItemId(): Long {
        return when (this) {
            is MessageItem -> message.tid
            is DateSeparatorItem -> hashCode().toLong()
            is UnreadMessagesSeparatorItem -> hashCode().toLong()
            is LoadingPrevItem -> hashCode().toLong()
            is LoadingNextItem -> hashCode().toLong()
        }
    }

    fun getMessageId(): Long? {
        return when (this) {
            is MessageItem -> message.id
            is DateSeparatorItem -> messageId
            is UnreadMessagesSeparatorItem -> msgId
            is LoadingPrevItem -> null
            is LoadingNextItem -> null
        }
    }

    var highlight = false
}
