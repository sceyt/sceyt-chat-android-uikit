package com.sceyt.chatuikit.presentation.uicomponents.conversationinfo

import com.sceyt.chatuikit.data.models.channels.SceytChannel

interface ChannelUpdateListener {
    fun onChannelUpdated(channel: SceytChannel)
}