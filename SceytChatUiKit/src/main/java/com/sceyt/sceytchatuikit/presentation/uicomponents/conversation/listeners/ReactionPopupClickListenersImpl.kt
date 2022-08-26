package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners

import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.MessagesListView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.reactions.ReactionItem

class ReactionPopupClickListenersImpl(view: MessagesListView) : ReactionPopupClickListeners.PopupClickListeners {
    private var defaultListeners: ReactionPopupClickListeners.PopupClickListeners = view
    private var addReaction: ReactionPopupClickListeners.AddReaction? = null
    private var removeReactionListener: ReactionPopupClickListeners.RemoveReaction? = null
    private var deleteReactionListener: ReactionPopupClickListeners.DeleteReaction? = null

    override fun onAddReaction(message: SceytMessage, key: String) {
        defaultListeners.onAddReaction(message, key)
        addReaction?.onAddReaction(message, key)
    }

    override fun onRemoveReaction(reactionItem: ReactionItem.Reaction) {
        defaultListeners.onRemoveReaction(reactionItem)
        removeReactionListener?.onRemoveReaction(reactionItem)
    }

    override fun onDeleteReaction(reactionItem: ReactionItem.Reaction) {
        defaultListeners.onDeleteReaction(reactionItem)
        deleteReactionListener?.onDeleteReaction(reactionItem)
    }

    fun setListener(listener: ReactionPopupClickListeners) {
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