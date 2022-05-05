package com.sceyt.chat.ui.presentation.uicomponents.channels.listeners

import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.ChannelListItem


sealed interface ChannelListeners {

    fun interface ChannelClickListener : ChannelListeners {
        fun onChannelClick(item: ChannelListItem.ChannelItem)
    }

    fun interface ChannelLongClickListener : ChannelListeners {
        fun onChannelLongClick(item: ChannelListItem.ChannelItem)
    }

    fun interface AvatarClickListener : ChannelListeners {
        fun onAvatarClick(item: ChannelListItem.ChannelItem)
    }

    /** User this if you want to implement all callbacks */
    interface Listeners :
            ChannelClickListener,
            ChannelLongClickListener,
            AvatarClickListener,
            ChannelListeners
}
