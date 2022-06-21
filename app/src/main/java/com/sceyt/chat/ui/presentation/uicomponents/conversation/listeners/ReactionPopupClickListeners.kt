package com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners

import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.ReactionItem

sealed interface ReactionPopupClickListeners {

    fun interface AddReaction : ReactionPopupClickListeners {
        fun onAddReaction(message: SceytMessage, key: String)
    }

    fun interface RemoveReaction : ReactionPopupClickListeners {
        fun onRemoveReaction(reactionItem: ReactionItem.Reaction)
    }

    fun interface DeleteReaction : ReactionPopupClickListeners {
        fun onDeleteReaction(reactionItem: ReactionItem.Reaction)
    }

    /** User this if you want to implement all callbacks */
    interface PopupClickListeners : AddReaction, RemoveReaction, DeleteReaction
}