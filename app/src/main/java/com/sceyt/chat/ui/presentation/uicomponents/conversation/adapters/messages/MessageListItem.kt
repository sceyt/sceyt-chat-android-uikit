package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages

import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.shared.helpers.LinkPreviewHelper

sealed class MessageListItem {
    data class MessageItem(val message: SceytMessage) : MessageListItem() {
        var linkPreviewData: LinkPreviewHelper.PreviewMetaData? = null
    }

    data class DateSeparatorItem(val createdAt: Long, val msgId: Long) : MessageListItem()
    object LoadingMoreItem : MessageListItem()

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
            other is LoadingMoreItem && this is LoadingMoreItem -> true
            else -> false
        }
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
