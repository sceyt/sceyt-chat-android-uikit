package com.sceyt.chatuikit.presentation.components.channel.header.listeners.click

import android.view.View
import com.sceyt.chatuikit.presentation.components.channel.header.MessagesListHeaderView

open class MessageListHeaderClickListenersImpl : MessageListHeaderClickListeners.ClickListeners {
    @Suppress("unused")
    constructor()

    internal constructor(view: MessagesListHeaderView) {
        defaultListeners = view
    }

    private var defaultListeners: MessageListHeaderClickListeners.ClickListeners? = null
    private var avatarClickListener: MessageListHeaderClickListeners.AvatarClickListener? = null
    private var toolbarClickListener: MessageListHeaderClickListeners.ToolbarClickListener? = null
    private var backClickListener: MessageListHeaderClickListeners.BackClickListener? = null

    override fun onAvatarClick(view: View) {
        defaultListeners?.onAvatarClick(view)
        avatarClickListener?.onAvatarClick(view)
    }

    override fun onToolbarClick(view: View) {
        defaultListeners?.onToolbarClick(view)
        toolbarClickListener?.onToolbarClick(view)
    }

    override fun onBackClick(view: View) {
        defaultListeners?.onBackClick(view)
        backClickListener?.onBackClick(view)
    }

    fun setListener(listener: MessageListHeaderClickListeners) {
        when (listener) {
            is MessageListHeaderClickListeners.ClickListeners -> {
                avatarClickListener = listener
                toolbarClickListener = listener
                backClickListener = listener
            }

            is MessageListHeaderClickListeners.AvatarClickListener -> {
                avatarClickListener = listener
            }

            is MessageListHeaderClickListeners.ToolbarClickListener -> {
                toolbarClickListener = listener
            }

            is MessageListHeaderClickListeners.BackClickListener -> {
                backClickListener = listener
            }
        }
    }

    internal fun withDefaultListeners(
            listener: MessageListHeaderClickListeners.ClickListeners
    ): MessageListHeaderClickListenersImpl {
        defaultListeners = listener
        return this
    }
}