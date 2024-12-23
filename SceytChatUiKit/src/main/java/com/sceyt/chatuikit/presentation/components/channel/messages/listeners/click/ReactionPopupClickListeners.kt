package com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click

import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.reactions.ReactionItem

sealed interface ReactionPopupClickListeners {

    fun interface AddReaction : ReactionPopupClickListeners {
        fun onAddReaction(message: SceytMessage, key: String)
    }

    fun interface RemoveReaction : ReactionPopupClickListeners {
        fun onRemoveReaction(message: SceytMessage, reactionItem: ReactionItem.Reaction)
    }

    /** Use this if you want to implement all callbacks */
    interface PopupClickListeners : AddReaction, RemoveReaction
}

internal fun ReactionPopupClickListeners.setListener(listener: ReactionPopupClickListeners) {
    (this as? ReactionPopupClickListenersImpl)?.setListener(listener)
}