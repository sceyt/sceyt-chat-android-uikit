package com.sceyt.chatuikit.presentation.components.channel.header.listeners.click

import android.view.View
import com.sceyt.chatuikit.presentation.components.channel.header.MessagesListHeaderView

open class HeaderClickListenersImpl(view: MessagesListHeaderView) : HeaderClickListeners.ClickListeners {
    private var defaultListeners: HeaderClickListeners.ClickListeners = view
    private var avatarClickListener: HeaderClickListeners.AvatarClickListener? = null
    private var toolbarClickListener: HeaderClickListeners.ToolbarClickListener? = null
    private var backClickListener: HeaderClickListeners.BackClickListener? = null

    override fun onAvatarClick(view: View) {
        defaultListeners.onAvatarClick(view)
        avatarClickListener?.onAvatarClick(view)
    }

    override fun onToolbarClick(view: View) {
        defaultListeners.onToolbarClick(view)
        toolbarClickListener?.onToolbarClick(view)
    }

    override fun onBackClick(view: View) {
        defaultListeners.onBackClick(view)
        backClickListener?.onBackClick(view)
    }

    fun setListener(listener: HeaderClickListeners) {
        when (listener) {
            is HeaderClickListeners.ClickListeners -> {
                avatarClickListener = listener
                toolbarClickListener = listener
                backClickListener = listener
            }
            is HeaderClickListeners.AvatarClickListener -> {
                avatarClickListener = listener
            }
            is HeaderClickListeners.ToolbarClickListener -> {
                toolbarClickListener = listener
            }
            is HeaderClickListeners.BackClickListener -> {
                backClickListener = listener
            }
        }
    }
}