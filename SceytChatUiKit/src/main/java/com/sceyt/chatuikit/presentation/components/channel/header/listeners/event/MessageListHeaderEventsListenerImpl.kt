package com.sceyt.chatuikit.presentation.components.channel.header.listeners.event

import com.sceyt.chat.models.ConnectionState
import com.sceyt.chatuikit.data.managers.channel.event.ChannelMemberActivityEvent
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.presentation.components.channel.header.MessagesListHeaderView
import com.sceyt.chatuikit.presentation.components.channel.header.listeners.event.MessageListHeaderEventsListener.ConnectionStateListener
import com.sceyt.chatuikit.presentation.components.channel.header.listeners.event.MessageListHeaderEventsListener.MemberActivityListener
import com.sceyt.chatuikit.presentation.components.channel.header.listeners.event.MessageListHeaderEventsListener.PresenceUpdateListener

open class MessageListHeaderEventsListenerImpl : MessageListHeaderEventsListener.EventListeners,
    ConnectionStateListener {
    @Suppress("unused")
    constructor()

    internal constructor(view: MessagesListHeaderView) {
        defaultListeners = view
    }

    private var defaultListeners: MessageListHeaderEventsListener.EventListeners? = null
    private var memberActivityListener: MemberActivityListener? = null
    private var presenceUpdateListener: PresenceUpdateListener? = null
    private var connectionStateListener: ConnectionStateListener? = null

    override fun onActivityEvent(event: ChannelMemberActivityEvent) {
        defaultListeners?.onActivityEvent(event)
        memberActivityListener?.onActivityEvent(event)
    }

    override fun onPresenceUpdateEvent(channel: SceytChannel) {
        defaultListeners?.onPresenceUpdateEvent(channel)
        presenceUpdateListener?.onPresenceUpdateEvent(channel)
    }

    override fun onConnectionStateChanged(state: ConnectionState, channel: SceytChannel) {
        defaultListeners?.onConnectionStateChanged(state, channel)
        connectionStateListener?.onConnectionStateChanged(state, channel)
    }

    fun setListener(listener: MessageListHeaderEventsListener) {
        when (listener) {
            is MessageListHeaderEventsListener.EventListeners -> {
                memberActivityListener = listener
                presenceUpdateListener = listener
                connectionStateListener = listener
            }

            is MemberActivityListener -> {
                memberActivityListener = listener
            }

            is PresenceUpdateListener -> {
                presenceUpdateListener = listener
            }

            is ConnectionStateListener -> {
                connectionStateListener = listener
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
