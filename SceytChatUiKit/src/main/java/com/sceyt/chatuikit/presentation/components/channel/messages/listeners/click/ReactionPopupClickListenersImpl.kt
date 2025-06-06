package com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click

import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytReaction
import com.sceyt.chatuikit.presentation.components.channel.messages.MessagesListView
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.reactions.ReactionItem

class ReactionPopupClickListenersImpl : ReactionPopupClickListeners.PopupClickListeners {
    @Suppress("unused")
    constructor()

    internal constructor(view: MessagesListView) {
        defaultListeners = view
    }

    private var defaultListeners: ReactionPopupClickListeners.PopupClickListeners? = null
    private var addReaction: ReactionPopupClickListeners.AddReaction? = null
    private var removeReactionListener: ReactionPopupClickListeners.RemoveReaction? = null
    private var reactionClickListener: ReactionPopupClickListeners.ReactionClick? = null

    override fun onAddReaction(message: SceytMessage, key: String) {
        defaultListeners?.onAddReaction(message, key)
        addReaction?.onAddReaction(message, key)
    }

    override fun onRemoveReaction(message: SceytMessage, reactionItem: ReactionItem.Reaction) {
        defaultListeners?.onRemoveReaction(message, reactionItem)
        removeReactionListener?.onRemoveReaction(message, reactionItem)
    }

    override fun onReactionClick(message: SceytMessage, reaction: SceytReaction) {
        defaultListeners?.onReactionClick(message, reaction)
        reactionClickListener?.onReactionClick(message, reaction)
    }

    fun setListener(listener: ReactionPopupClickListeners) {
        when (listener) {
            is ReactionPopupClickListeners.PopupClickListeners -> {
                addReaction = listener
                removeReactionListener = listener
                reactionClickListener = listener
            }

            is ReactionPopupClickListeners.AddReaction -> {
                addReaction = listener
            }

            is ReactionPopupClickListeners.RemoveReaction -> {
                removeReactionListener = listener
            }

            is ReactionPopupClickListeners.ReactionClick -> {
                reactionClickListener = listener
            }
        }
    }

    internal fun withDefaultListeners(
            listener: ReactionPopupClickListeners.PopupClickListeners
    ): ReactionPopupClickListenersImpl {
        defaultListeners = listener
        return this
    }
}