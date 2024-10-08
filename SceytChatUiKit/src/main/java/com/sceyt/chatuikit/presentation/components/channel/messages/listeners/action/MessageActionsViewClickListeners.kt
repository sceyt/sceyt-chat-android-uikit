package com.sceyt.chatuikit.presentation.components.channel.messages.listeners.action

import com.sceyt.chatuikit.data.models.messages.SceytMessage

sealed interface MessageActionsViewClickListeners {

    fun interface CopyMessage : MessageActionsViewClickListeners {
        fun onCopyMessagesClick(vararg messages: SceytMessage)
    }

    fun interface DeleteMessage : MessageActionsViewClickListeners {
        fun onDeleteMessageClick(vararg messages: SceytMessage, requireForMe: Boolean, actionFinish: () -> Unit)
    }

    fun interface EditMessage : MessageActionsViewClickListeners {
        fun onEditMessageClick(message: SceytMessage)
    }

    fun interface MessageInfo : MessageActionsViewClickListeners {
        fun onMessageInfoClick(message: SceytMessage)
    }

    fun interface ForwardMessage : MessageActionsViewClickListeners {
        fun onForwardMessageClick(vararg messages: SceytMessage)
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
    interface ActionsViewClickListeners : CopyMessage, DeleteMessage, EditMessage, MessageInfo,
            ForwardMessage, ReactMessage, ReplyMessage, ReplyInThreadMessage
}