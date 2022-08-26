package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners

import android.view.View
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage

sealed interface MessagePopupClickListeners {

    fun interface CopyMessage : MessagePopupClickListeners {
        fun onCopyMessageClick(message: SceytMessage)
    }

    fun interface DeleteMessage : MessagePopupClickListeners {
        fun onDeleteMessageClick(message: SceytMessage)
    }

    fun interface EditMessage : MessagePopupClickListeners {
        fun onEditMessageClick(message: SceytMessage)
    }

    fun interface ReactMessage : MessagePopupClickListeners {
        fun onReactMessageClick(view: View, message: SceytMessage)
    }

    fun interface ReplayMessage : MessagePopupClickListeners {
        fun onReplayMessageClick(message: SceytMessage)
    }

    fun interface ReplayInThreadMessage : MessagePopupClickListeners {
        fun onReplayMessageInThreadClick(message: SceytMessage)
    }

    /** User this if you want to implement all callbacks */
    interface PopupClickListeners : CopyMessage, DeleteMessage, EditMessage, ReactMessage,
            ReplayMessage, ReplayInThreadMessage
}