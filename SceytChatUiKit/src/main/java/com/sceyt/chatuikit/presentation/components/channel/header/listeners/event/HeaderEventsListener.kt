package com.sceyt.chatuikit.presentation.components.channel.header.listeners.event

import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.data.managers.channel.event.ChannelTypingEventData

sealed interface HeaderEventsListener {

    fun interface TypingListener : HeaderEventsListener {
        fun onTypingEvent(data: ChannelTypingEventData)
    }

    fun interface PresenceUpdateListener : HeaderEventsListener {
        fun onPresenceUpdateEvent(user: User)
    }

    /** Use this if you want to implement all callbacks */
    interface EventListeners : TypingListener, PresenceUpdateListener
}