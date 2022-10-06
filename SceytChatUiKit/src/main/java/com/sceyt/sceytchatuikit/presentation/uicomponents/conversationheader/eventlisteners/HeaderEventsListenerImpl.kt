package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.eventlisteners

import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelTypingEventData
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.ConversationHeaderView

open class HeaderEventsListenerImpl(view: ConversationHeaderView) : HeaderEventsListener.EventListeners {
    private var defaultListeners: HeaderEventsListener.EventListeners = view
    private var typingListener: HeaderEventsListener.TypingListener? = null
    private var presenceUpdateListener: HeaderEventsListener.PresenceUpdateListener? = null

    override fun onTypingEvent(data: ChannelTypingEventData) {
        defaultListeners.onTypingEvent(data)
        typingListener?.onTypingEvent(data)
    }

    override fun onPresenceUpdateEvent(users: List<User>) {
        defaultListeners.onPresenceUpdateEvent(users)
        presenceUpdateListener?.onPresenceUpdateEvent(users)
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