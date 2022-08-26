package com.sceyt.sceytchatuikit.presentation.uicomponents.channels.listeners

import android.view.View
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelListItem


sealed interface ChannelClickListeners {

    fun interface ChannelClickClickListener : ChannelClickListeners {
        fun onChannelClick(item: ChannelListItem.ChannelItem)
    }

    fun interface ChannelClickLongClickListener : ChannelClickListeners {
        fun onChannelLongClick(view: View, item: ChannelListItem.ChannelItem)
    }

    fun interface AvatarClickListener : ChannelClickListeners {
        fun onAvatarClick(item: ChannelListItem.ChannelItem)
    }

    /** User this if you want to implement all callbacks */
    interface ClickListeners :
            ChannelClickClickListener,
            ChannelClickLongClickListener,
            AvatarClickListener,
            ChannelClickListeners
}
