package com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners

import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem

class MessageEventListenersImpl : MessageEventListeners.EventListeners {
    private var addReactionEvent: MessageEventListeners.AddReaction? = null
    private var removeReactionEvent: MessageEventListeners.RemoveReaction? = null
    private var deleteReactionEvent: MessageEventListeners.DeleteReaction? = null


    override fun onAddReactionEvent(item: MessageListItem.MessageItem) {
        addReactionEvent?.onAddReactionEvent(item)
    }

    override fun onRemoveReactionEvent(item: MessageListItem.MessageItem) {
        removeReactionEvent?.onRemoveReactionEvent(item)
    }

    override fun onDeleteReactionEvent(item: MessageListItem.MessageItem) {
        deleteReactionEvent?.onDeleteReactionEvent(item)
    }

    fun setListener(listener: MessageEventListeners) {
        when (listener) {
            is MessageEventListeners.EventListeners -> {
                addReactionEvent = listener
                removeReactionEvent = listener
                deleteReactionEvent = listener
            }
            is MessageEventListeners.AddReaction -> {
                addReactionEvent = listener
            }
            is MessageEventListeners.RemoveReaction -> {
                removeReactionEvent = listener
            }
            is MessageEventListeners.DeleteReaction -> {
                deleteReactionEvent = listener
            }
        }
    }

}