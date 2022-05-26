package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages

import com.sceyt.chat.ui.data.models.messages.SceytMessage

sealed class MessageListItem {
    data class MessageItem(val message: SceytMessage) : MessageListItem()
    object LoadingMoreItem : MessageListItem()

    override fun equals(other: Any?): Boolean {
        return when {
            other == null -> false
            other !is MessageListItem -> false
            other is MessageItem && this is MessageItem -> {
                other.message == message
            }
            other is LoadingMoreItem && this is LoadingMoreItem -> true
            else -> false
        }
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
