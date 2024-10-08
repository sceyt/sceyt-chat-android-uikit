package com.sceyt.chatuikit.presentation.components.channel_info

import com.sceyt.chatuikit.data.models.channels.SceytChannel

interface ChannelUpdateListener {
    fun onChannelUpdated(channel: SceytChannel)
}