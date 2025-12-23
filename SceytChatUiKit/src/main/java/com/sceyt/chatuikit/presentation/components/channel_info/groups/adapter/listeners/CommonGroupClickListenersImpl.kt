package com.sceyt.chatuikit.presentation.components.channel_info.groups.adapter.listeners

import android.view.View
import com.sceyt.chatuikit.data.models.channels.SceytChannel

open class CommonGroupClickListenersImpl : CommonGroupClickListeners.ClickListeners {
    private var channelClickListener: CommonGroupClickListeners.ChannelClickListener? = null
    private var channelLongClickListener: CommonGroupClickListeners.ChannelLongClickListener? = null
    private var avatarClickListener: CommonGroupClickListeners.AvatarClickListener? = null

    override fun onChannelClick(view: View, channel: SceytChannel) {
        channelClickListener?.onChannelClick(view, channel)
    }

    override fun onChannelLongClick(view: View, channel: SceytChannel) {
        channelLongClickListener?.onChannelLongClick(view, channel)
    }

    override fun onAvatarClick(view: View, channel: SceytChannel) {
        avatarClickListener?.onAvatarClick(view, channel)
    }

    fun setListener(listener: CommonGroupClickListeners) {
        when (listener) {
            is CommonGroupClickListeners.ClickListeners -> {
                channelClickListener = listener
                channelLongClickListener = listener
                avatarClickListener = listener
            }

            is CommonGroupClickListeners.ChannelClickListener -> {
                channelClickListener = listener
            }

            is CommonGroupClickListeners.ChannelLongClickListener -> {
                channelLongClickListener = listener
            }

            is CommonGroupClickListeners.AvatarClickListener -> {
                avatarClickListener = listener
            }
        }
    }
}