package com.sceyt.chatuikit.presentation.components.channel_info.groups.adapter.listeners

import android.view.View
import com.sceyt.chatuikit.data.models.channels.SceytChannel

open class CommonGroupClickListenersImpl : CommonGroupClickListeners.ClickListeners {
    private var clickListener: CommonGroupClickListeners.ClickListener? = null
    private var avatarClickListener: CommonGroupClickListeners.AvatarClickListener? = null

    override fun onGroupClick(view: View, channel: SceytChannel) {
        clickListener?.onGroupClick(view, channel)
    }

    override fun onAvatarClick(view: View, channel: SceytChannel) {
        avatarClickListener?.onAvatarClick(view, channel)
    }

    fun setListener(listener: CommonGroupClickListeners) {
        when (listener) {
            is CommonGroupClickListeners.ClickListener -> {
                clickListener = listener
            }

            is CommonGroupClickListeners.AvatarClickListener -> {
                avatarClickListener = listener
            }
        }
    }
}