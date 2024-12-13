package com.sceyt.chatuikit.data.managers.channel.event

import com.sceyt.chatuikit.data.models.channels.SceytChannel

data class ChannelEventData(
        val channel: SceytChannel?,
        val eventType: ChannelEventEnum,
        val channelId: Long? = channel?.id
)