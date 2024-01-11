package com.sceyt.sceytchatuikit.data.channeleventobserver

import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel

data class ChannelEventData(
        val channel: SceytChannel?,
        val eventType: ChannelEventEnum,
        var channelId: Long? = channel?.id
)