package com.sceyt.chatuikit.data.managers.message.handler

import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytReaction
import com.sceyt.chatuikit.data.models.messages.Vote

open class MessageEventHandlerImpl : MessageEventHandler.AllEventManagers {
    private var defaultListeners: MessageEventHandler.AllEventManagers? = null

    constructor()

    internal constructor(defaultListener: MessageEventHandler.AllEventManagers) : this() {
        defaultListeners = defaultListener
    }

    override fun onMessage(channel: SceytChannel, message: SceytMessage) {
        defaultListeners?.onMessage(channel, message)
    }

    override fun onDirectMessage(message: SceytMessage) {
        defaultListeners?.onDirectMessage(message)
    }

    override fun onMessageDeleted(message: SceytMessage) {
        defaultListeners?.onMessageDeleted(message)
    }

    override fun onMessageEdited(message: SceytMessage) {
        defaultListeners?.onMessageEdited(message)
    }

    override fun onReactionAdded(message: SceytMessage, reaction: SceytReaction) {
        defaultListeners?.onReactionAdded(message, reaction)
    }

    override fun onReactionDeleted(message: SceytMessage, reaction: SceytReaction) {
        defaultListeners?.onReactionDeleted(message, reaction)
    }

    override fun onVoteChanged(
        message: SceytMessage,
        addedVotes: List<Vote>,
        removedVoted: List<Vote>
    ) {
        defaultListeners?.onVoteChanged(message, addedVotes, removedVoted)
    }

    override fun onVoteRetracted(message: SceytMessage, retractedVotes: List<Vote>) {
        defaultListeners?.onVoteRetracted(message, retractedVotes)
    }

    override fun onPollClosed(message: SceytMessage) {
        defaultListeners?.onPollClosed(message)
    }

    internal fun setDefaultListeners(listener: MessageEventHandler.AllEventManagers) {
        defaultListeners = listener
    }
}