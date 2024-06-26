package com.sceyt.chatuikit.presentation.uicomponents.conversation.listeners

import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.presentation.uicomponents.conversation.MessagesListView
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.reactions.ReactionItem

class ReactionPopupClickListenersImpl(view: MessagesListView) : ReactionPopupClickListeners.PopupClickListeners {
    private var defaultListeners: ReactionPopupClickListeners.PopupClickListeners = view
    private var addReaction: ReactionPopupClickListeners.AddReaction? = null
    private var removeReactionListener: ReactionPopupClickListeners.RemoveReaction? = null

    override fun onAddReaction(message: SceytMessage, key: String) {
        defaultListeners.onAddReaction(message, key)
        addReaction?.onAddReaction(message, key)
    }

    override fun onRemoveReaction(reactionItem: ReactionItem.Reaction) {
        defaultListeners.onRemoveReaction(reactionItem)
        removeReactionListener?.onRemoveReaction(reactionItem)
    }


    fun setListener(listener: ReactionPopupClickListeners) {
        when (listener) {
            is ReactionPopupClickListeners.PopupClickListeners -> {
                addReaction = listener
                removeReactionListener = listener
            }
            is ReactionPopupClickListeners.AddReaction -> {
                addReaction = listener
            }
            is ReactionPopupClickListeners.RemoveReaction -> {
                removeReactionListener = listener
            }
        }
    }
}