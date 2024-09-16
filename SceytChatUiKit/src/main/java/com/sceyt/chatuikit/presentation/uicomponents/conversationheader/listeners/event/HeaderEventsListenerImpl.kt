package com.sceyt.chatuikit.presentation.uicomponents.conversationheader.listeners.event

import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.data.channeleventobserver.ChannelTypingEventData
import com.sceyt.chatuikit.presentation.uicomponents.conversationheader.MessagesListHeaderView

open class HeaderEventsListenerImpl(view: MessagesListHeaderView) : HeaderEventsListener.EventListeners {
    private var defaultListeners: HeaderEventsListener.EventListeners = view
    private var typingListener: HeaderEventsListener.TypingListener? = null
    private var presenceUpdateListener: HeaderEventsListener.PresenceUpdateListener? = null

    override fun onTypingEvent(data: ChannelTypingEventData) {
        defaultListeners.onTypingEvent(data)
        typingListener?.onTypingEvent(data)
    }

    override fun onPresenceUpdateEvent(user: User) {
        defaultListeners.onPresenceUpdateEvent(user)
        presenceUpdateListener?.onPresenceUpdateEvent(user)
    }

    fun setListener(listener: HeaderEventsListener) {
        when (listener) {
            is HeaderEventsListener.EventListeners -> {
                typingListener = listener
                presenceUpdateListener = listener
            }
            is HeaderEventsListener.TypingListener -> {
                typingListener = listener
            }
            is HeaderEventsListener.PresenceUpdateListener -> {
                presenceUpdateListener = listener
            }
        }
    }
}