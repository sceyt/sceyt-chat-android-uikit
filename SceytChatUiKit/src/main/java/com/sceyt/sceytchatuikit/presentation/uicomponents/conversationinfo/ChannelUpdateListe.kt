package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo

import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel

interface ChannelUpdateListener {
    fun onChannelUpdated(channel: SceytChannel)
}