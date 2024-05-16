package com.sceyt.chatuikit.presentation.uicomponents.conversationheader.clicklisteners

import android.view.View

sealed interface HeaderClickListeners {

    fun interface AvatarClickListener : HeaderClickListeners {
        fun onAvatarClick(view: View)
    }

    fun interface ToolbarClickListener : HeaderClickListeners {
        fun onToolbarClick(view: View)
    }

    fun interface BackClickListener : HeaderClickListeners {
        fun onBackClick(view: View)
    }

    /** Use this if you want to implement all callbacks */
    interface ClickListeners : AvatarClickListener, ToolbarClickListener, BackClickListener
}