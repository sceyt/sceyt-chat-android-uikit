package com.sceyt.chatuikit.presentation.components.channel.header.listeners.event

import com.sceyt.chatuikit.data.managers.channel.event.ChannelMemberActivityEvent
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.presentation.components.channel.header.MessagesListHeaderView

open class MessageListHeaderEventsListenerImpl : MessageListHeaderEventsListener.EventListeners {
    @Suppress("unused")
    constructor()

    internal constructor(view: MessagesListHeaderView) {
        defaultListeners = view
    }

    private var defaultListeners: MessageListHeaderEventsListener.EventListeners? = null
    private var memberActivityListener: MessageListHeaderEventsListener.MemberActivityListener? = null
    private var presenceUpdateListener: MessageListHeaderEventsListener.PresenceUpdateListener? = null

    override fun onActivityEvent(event: ChannelMemberActivityEvent) {
        defaultListeners?.onActivityEvent(event)
        memberActivityListener?.onActivityEvent(event)
    }

    override fun onPresenceUpdateEvent(user: SceytUser) {
        defaultListeners?.onPresenceUpdateEvent(user)
        presenceUpdateListener?.onPresenceUpdateEvent(user)
    }

    fun setListener(listener: MessageListHeaderEventsListener) {
        when (listener) {
            is MessageListHeaderEventsListener.EventListeners -> {
                memberActivityListener = listener
                presenceUpdateListener = listener
            }

            is MessageListHeaderEventsListener.MemberActivityListener -> {
                memberActivityListener = listener
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