package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners

import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.reactions.ReactionItem

sealed interface ReactionPopupClickListeners {

    fun interface AddReaction : ReactionPopupClickListeners {
        fun onAddReaction(message: SceytMessage, key: String)
    }

    fun interface RemoveReaction : ReactionPopupClickListeners {
        fun onRemoveReaction(reactionItem: ReactionItem.Reaction)
    }

    /** Use this if you want to implement all callbacks */
    interface PopupClickListeners : AddReaction, RemoveReaction
}