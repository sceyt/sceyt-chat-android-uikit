package com.sceyt.chatuikit.presentation.components.channel_info.groups.adapter.listeners

import android.view.View
import com.sceyt.chatuikit.data.models.channels.SceytChannel

sealed interface CommonGroupClickListeners {

    fun interface ChannelClickListener : CommonGroupClickListeners {
        fun onChannelClick(view: View, channel: SceytChannel)
    }

    fun interface ChannelLongClickListener : CommonGroupClickListeners {
        fun onChannelLongClick(view: View, channel: SceytChannel)
    }

    fun interface AvatarClickListener : CommonGroupClickListeners {
        fun onAvatarClick(view: View, channel: SceytChannel)
    }

    interface ClickListeners : ChannelClickListener, ChannelLongClickListener, AvatarClickListener
}