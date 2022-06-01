package com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners

import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.presentation.uicomponents.conversation.MessagesListView
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.ReactionItem

class ReactionPopupClickListenersImpl(view: MessagesListView) : ReactionPopupClickListeners.PopupClickListeners {
    private var defaultListeners: ReactionPopupClickListeners.PopupClickListeners = view
    private var addReaction: ReactionPopupClickListeners.AddReaction? = null
    private var removeReactionListener: ReactionPopupClickListeners.RemoveReaction? = null
    private var deleteReactionListener: ReactionPopupClickListeners.DeleteReaction? = null

    override fun onAddReaction(message: SceytMessage, score: String) {
        defaultListeners.onAddReaction(message, score)
        addReaction?.onAddReaction(message, score)
    }

    override fun onRemoveReaction(reactionItem: ReactionItem.Reaction) {
        defaultListeners.onRemoveReaction(reactionItem)
        removeReactionListener?.onRemoveReaction(reactionItem)
    }

    override fun onDeleteReaction(reactionItem: ReactionItem.Reaction) {
        defaultListeners.onDeleteReaction(reactionItem)
        deleteReactionListener?.onDeleteReaction(reactionItem)
    }

    fun setListener(listener: MessagePopupClickListeners) {
        when (listener) {
            is ReactionPopupClickListeners.PopupClickListeners -> {
                addReaction = listener
                removeReactionListener = listener
                deleteReactionListener = listener
            }
            is ReactionPopupClickListeners.AddReaction -> {
                addReaction = listener
            }
            is ReactionPopupClickListeners.RemoveReaction -> {
                removeReactionListener = listener
            }
            is ReactionPopupClickListeners.DeleteReaction -> {
                deleteReactionListener = listener
            }
        }
    }
}