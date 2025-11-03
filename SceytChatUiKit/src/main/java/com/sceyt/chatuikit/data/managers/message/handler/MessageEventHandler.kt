package com.sceyt.chatuikit.data.managers.message.handler

import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytReaction
import com.sceyt.chatuikit.data.models.messages.Vote

sealed interface MessageEventHandler {

    fun interface OnMessage : MessageEventHandler {
        fun onMessage(channel: SceytChannel, message: SceytMessage)
    }

    fun interface OnDirectMessage : MessageEventHandler {
        fun onDirectMessage(message: SceytMessage)
    }

    fun interface OnMessageDeleted : MessageEventHandler {
        fun onMessageDeleted(message: SceytMessage)
    }

    fun interface OnMessageEdited : MessageEventHandler {
        fun onMessageEdited(message: SceytMessage)
    }

    fun interface OnReactionAdded : MessageEventHandler {
        fun onReactionAdded(message: SceytMessage, reaction: SceytReaction)
    }

    fun interface OnReactionDeleted : MessageEventHandler {
        fun onReactionDeleted(message: SceytMessage, reaction: SceytReaction)
    }

    fun interface OnVoteAdded : MessageEventHandler {
        fun onVoteAdded(message: SceytMessage, votes: List<Vote>)
    }

    fun interface OnVoteDeleted : MessageEventHandler {
        fun onVoteDeleted(message: SceytMessage, votes: List<Vote>)
    }

    fun interface OnVoteRetracted : MessageEventHandler {
        fun onVoteRetracted(message: SceytMessage, votes: List<Vote>)
    }

    fun interface OnPollClosed : MessageEventHandler {
        fun onPollClosed(message: SceytMessage)
    }

    interface AllEventManagers : OnMessage, OnDirectMessage, OnMessageDeleted, OnMessageEdited,
        OnReactionAdded, OnReactionDeleted, OnVoteAdded, OnVoteDeleted,
        OnVoteRetracted, OnPollClosed
}