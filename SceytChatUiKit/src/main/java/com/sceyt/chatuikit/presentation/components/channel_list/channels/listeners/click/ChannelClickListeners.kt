package com.sceyt.chatuikit.presentation.components.channel_list.channels.listeners.click

import android.view.View
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItem


sealed interface ChannelClickListeners {

    fun interface ChannelClickListener : ChannelClickListeners {
        fun onChannelClick(item: ChannelListItem.ChannelItem)
    }

    fun interface ChannelLongClickListener : ChannelClickListeners {
        fun onChannelLongClick(view: View, item: ChannelListItem.ChannelItem)
    }

    fun interface AvatarClickListener : ChannelClickListeners {
        fun onAvatarClick(item: ChannelListItem.ChannelItem)
    }

    /** Use this if you want to implement all callbacks */
    interface ClickListeners :
            ChannelClickListener,
            ChannelLongClickListener,
            AvatarClickListener,
            ChannelClickListeners
}

internal fun ChannelClickListeners.setListener(listener: ChannelClickListeners) {
    (this as? ChannelClickListenersImpl)?.setListener(listener)
}