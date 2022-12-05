package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages

import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.shared.helpers.LinkPreviewHelper

sealed class MessageListItem {
    data class MessageItem(val message: SceytMessage) : MessageListItem() {
        var linkPreviewData: LinkPreviewHelper.PreviewMetaData? = null
    }

    data class DateSeparatorItem(val createdAt: Long, val msgId: Long) : MessageListItem()
    data class UnreadMessagesSeparatorItem(val createdAt: Long, val msgId: Long) : MessageListItem()
    object LoadingPrevItem : MessageListItem()
    object LoadingNextItem : MessageListItem()

    override fun equals(other: Any?): Boolean {
        return when {
            other == null -> false
            other !is MessageListItem -> false
            other is MessageItem && this is MessageItem -> {
                other.message == message
            }
            other is DateSeparatorItem && this is DateSeparatorItem -> {
                other.createdAt == createdAt && other.msgId == msgId
            }
            other is UnreadMessagesSeparatorItem && this is UnreadMessagesSeparatorItem -> {
                other.msgId == msgId
            }
            other is LoadingPrevItem && this is LoadingPrevItem -> true
            other is LoadingNextItem && this is LoadingNextItem -> true
            else -> false
        }
    }

    fun getMessageCreatedAt(): Long {
        return when (this) {
            is MessageItem -> message.createdAt
            is DateSeparatorItem -> createdAt
            is UnreadMessagesSeparatorItem -> createdAt
            is LoadingPrevItem -> 0
            is LoadingNextItem -> Long.MAX_VALUE
        }
    }

    var highlighted = false

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
