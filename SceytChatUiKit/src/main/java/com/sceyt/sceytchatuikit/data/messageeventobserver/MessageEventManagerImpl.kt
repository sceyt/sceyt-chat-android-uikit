package com.sceyt.sceytchatuikit.data.messageeventobserver

import com.sceyt.chat.models.message.Reaction
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage

open class MessageEventManagerImpl : MessageEventManger.AllEventManagers {
    private var defaultListeners: MessageEventManger.AllEventManagers? = null

    constructor()

    internal constructor(defaultListener: MessageEventManger.AllEventManagers) : this() {
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

    override fun onReactionAdded(message: SceytMessage, reaction: Reaction) {
        defaultListeners?.onReactionAdded(message, reaction)
    }

    override fun onReactionDeleted(message: SceytMessage, reaction: Reaction) {
        defaultListeners?.onReactionDeleted(message, reaction)
    }

    internal fun setDefaultListeners(listener: MessageEventManger.AllEventManagers) {
        defaultListeners = listener
    }
}