package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.listeners

import android.view.View
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.ConversationHeaderView

open class HeaderClickListenersImpl(view: ConversationHeaderView) : HeaderClickListeners.ClickListeners {
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