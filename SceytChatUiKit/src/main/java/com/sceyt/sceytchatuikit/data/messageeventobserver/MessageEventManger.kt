package com.sceyt.sceytchatuikit.data.messageeventobserver

import com.sceyt.chat.models.message.Reaction
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage

sealed interface MessageEventManger {

    fun interface OnMessage : MessageEventManger {
        fun onMessage(channel: SceytChannel, message: SceytMessage)
    }

    fun interface OnDirectMessage : MessageEventManger {
        fun onDirectMessage(message: SceytMessage)
    }

    fun interface OnMessageDeleted : MessageEventManger {
        fun onMessageDeleted(message: SceytMessage)
    }

    fun interface OnMessageEdited : MessageEventManger {
        fun onMessageEdited(message: SceytMessage)
    }

    fun interface OnReactionAdded : MessageEventManger {
        fun onReactionAdded(message: SceytMessage, reaction: Reaction)
    }

    fun interface OnReactionDeleted : MessageEventManger {
        fun onReactionDeleted(message: SceytMessage, reaction: Reaction)
    }

    interface AllEventManagers : OnMessage, OnDirectMessage, OnMessageDeleted, OnMessageEdited,
            OnReactionAdded, OnReactionDeleted
}