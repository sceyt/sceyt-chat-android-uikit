package com.sceyt.chat.ui.data.channeleventobserverservice

import com.sceyt.chat.models.channel.Channel

data class ChannelEventData(
        val channel: Channel?,
        val eventType: ChannelEventEnum,
        var channelId: Long? = channel?.id
)