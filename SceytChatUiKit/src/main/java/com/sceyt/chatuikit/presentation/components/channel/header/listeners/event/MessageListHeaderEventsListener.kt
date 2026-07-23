package com.sceyt.chatuikit.presentation.components.channel.header.listeners.event

import com.sceyt.chatuikit.data.managers.channel.event.ChannelMemberActivityEvent
import com.sceyt.chatuikit.data.models.channels.SceytChannel

sealed interface MessageListHeaderEventsListener {

    fun interface MemberActivityListener : MessageListHeaderEventsListener {
        fun onActivityEvent(event: ChannelMemberActivityEvent)
    }

    fun interface PresenceUpdateListener : MessageListHeaderEventsListener {
        fun onPresenceUpdateEvent(channel: SceytChannel)
    }

    /** Use this if you want to implement all callbacks */
    interface EventListeners : MemberActivityListener, PresenceUpdateListener
}

internal fun MessageListHeaderEventsListener.setListener(listener: MessageListHeaderEventsListener) {
    (this as? MessageListHeaderEventsListenerImpl)?.setListener(listener)
}