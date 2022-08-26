package com.sceyt.sceytchatuikit.data.channeleventobserver

import com.sceyt.chat.models.channel.Channel

data class ChannelEventData(
        val channel: Channel?,
        val eventType: ChannelEventEnum,
        var channelId: Long? = channel?.id
)