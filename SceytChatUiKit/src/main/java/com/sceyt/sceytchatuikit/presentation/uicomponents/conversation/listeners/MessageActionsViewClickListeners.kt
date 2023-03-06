package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners

import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage

sealed interface MessageActionsViewClickListeners {

    fun interface CopyMessage : MessageActionsViewClickListeners {
        fun onCopyMessageClick(message: SceytMessage)
    }

    fun interface DeleteMessage : MessageActionsViewClickListeners {
        fun onDeleteMessageClick(message: SceytMessage, onlyForMe: Boolean)
    }

    fun interface EditMessage : MessageActionsViewClickListeners {
        fun onEditMessageClick(message: SceytMessage)
    }

    fun interface ForwardMessage : MessageActionsViewClickListeners {
        fun onForwardMessageClick(message: SceytMessage)
    }

    fun interface ReactMessage : MessageActionsViewClickListeners {
        fun onReactMessageClick(message: SceytMessage)
    }

    fun interface ReplyMessage : MessageActionsViewClickListeners {
        fun onReplyMessageClick(message: SceytMessage)
    }

    fun interface ReplyInThreadMessage : MessageActionsViewClickListeners {
        fun onReplyMessageInThreadClick(message: SceytMessage)
    }

    /** Use this if you want to implement all callbacks */
    interface ActionsViewClickListeners : CopyMessage, DeleteMessage, EditMessage, ForwardMessage,
            ReactMessage, ReplyMessage, ReplyInThreadMessage
}