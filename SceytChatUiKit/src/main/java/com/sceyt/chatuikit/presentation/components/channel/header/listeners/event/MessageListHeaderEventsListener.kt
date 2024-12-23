package com.sceyt.chatuikit.presentation.components.channel.header.listeners.event

import com.sceyt.chatuikit.data.managers.channel.event.ChannelTypingEventData
import com.sceyt.chatuikit.data.models.messages.SceytUser

sealed interface MessageListHeaderEventsListener {

    fun interface TypingListener : MessageListHeaderEventsListener {
        fun onTypingEvent(data: ChannelTypingEventData)
    }

    fun interface PresenceUpdateListener : MessageListHeaderEventsListener {
        fun onPresenceUpdateEvent(user: SceytUser)
    }

    /** Use this if you want to implement all callbacks */
    interface EventListeners : TypingListener, PresenceUpdateListener
}

internal fun MessageListHeaderEventsListener.setListener(listener: MessageListHeaderEventsListener) {
    (this as? MessageListHeaderEventsListenerImpl)?.setListener(listener)
}