package com.sceyt.chatuikit.presentation.components.channel.header.listeners.event

import com.sceyt.chatuikit.data.managers.channel.event.ChannelTypingEventData
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.presentation.components.channel.header.MessagesListHeaderView

open class MessageListHeaderEventsListenerImpl : MessageListHeaderEventsListener.EventListeners {
    @Suppress("unused")
    constructor()

    internal constructor(view: MessagesListHeaderView) {
        defaultListeners = view
    }

    private var defaultListeners: MessageListHeaderEventsListener.EventListeners? = null
    private var typingListener: MessageListHeaderEventsListener.TypingListener? = null
    private var presenceUpdateListener: MessageListHeaderEventsListener.PresenceUpdateListener? = null

    override fun onTypingEvent(data: ChannelTypingEventData) {
        defaultListeners?.onTypingEvent(data)
        typingListener?.onTypingEvent(data)
    }

    override fun onPresenceUpdateEvent(user: SceytUser) {
        defaultListeners?.onPresenceUpdateEvent(user)
        presenceUpdateListener?.onPresenceUpdateEvent(user)
    }

    fun setListener(listener: MessageListHeaderEventsListener) {
        when (listener) {
            is MessageListHeaderEventsListener.EventListeners -> {
                typingListener = listener
                presenceUpdateListener = listener
            }

            is MessageListHeaderEventsListener.TypingListener -> {
                typingListener = listener
            }

            is MessageListHeaderEventsListener.PresenceUpdateListener -> {
                presenceUpdateListener = listener
            }
        }
    }

    internal fun withDefaultListeners(
            listeners: MessageListHeaderEventsListener.EventListeners
    ): MessageListHeaderEventsListenerImpl {
        defaultListeners = listeners
        return this
    }
}