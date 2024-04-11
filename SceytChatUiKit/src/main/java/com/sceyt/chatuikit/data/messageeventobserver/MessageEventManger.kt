package com.sceyt.chatuikit.data.messageeventobserver

import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytReaction

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
        fun onReactionAdded(message: SceytMessage, reaction: SceytReaction)
    }

    fun interface OnReactionDeleted : MessageEventManger {
        fun onReactionDeleted(message: SceytMessage, reaction: SceytReaction)
    }

    interface AllEventManagers : OnMessage, OnDirectMessage, OnMessageDeleted, OnMessageEdited,
            OnReactionAdded, OnReactionDeleted
}