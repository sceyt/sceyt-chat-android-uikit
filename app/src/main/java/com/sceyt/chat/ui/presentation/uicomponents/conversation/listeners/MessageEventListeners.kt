package com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners

import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem

sealed interface MessageEventListeners {

    fun interface AddReaction : MessageEventListeners {
        fun onAddReactionEvent(item: MessageListItem.MessageItem)
    }

    fun interface RemoveReaction : MessageEventListeners {
        fun onRemoveReactionEvent(item: MessageListItem.MessageItem)
    }

    fun interface DeleteReaction : MessageEventListeners {
        fun onDeleteReactionEvent(item: MessageListItem.MessageItem)
    }

    /** User this if you want to implement all callbacks */
    interface EventListeners :
            AddReaction,
            RemoveReaction,
            DeleteReaction
}