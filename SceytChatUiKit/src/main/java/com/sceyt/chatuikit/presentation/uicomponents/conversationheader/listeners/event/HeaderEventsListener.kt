package com.sceyt.chatuikit.presentation.uicomponents.conversationheader.listeners.event

import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.data.channeleventobserver.ChannelTypingEventData

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