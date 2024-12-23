package com.sceyt.chatuikit.presentation.components.channel.header.listeners.click

import android.view.View

sealed interface MessageListHeaderClickListeners {

    fun interface AvatarClickListener : MessageListHeaderClickListeners {
        fun onAvatarClick(view: View)
    }

    fun interface ToolbarClickListener : MessageListHeaderClickListeners {
        fun onToolbarClick(view: View)
    }

    fun interface BackClickListener : MessageListHeaderClickListeners {
        fun onBackClick(view: View)
    }

    /** Use this if you want to implement all callbacks */
    interface ClickListeners : AvatarClickListener, ToolbarClickListener, BackClickListener
}

internal fun MessageListHeaderClickListeners.setListener(listener: MessageListHeaderClickListeners) {
    (this as? MessageListHeaderClickListenersImpl)?.setListener(listener)
}