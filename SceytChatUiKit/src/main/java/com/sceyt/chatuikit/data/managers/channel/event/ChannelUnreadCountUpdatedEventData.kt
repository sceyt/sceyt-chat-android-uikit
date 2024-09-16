package com.sceyt.chatuikit.data.managers.channel.event

import com.sceyt.chat.models.channel.Channel

data class ChannelUnreadCountUpdatedEventData(
        val channel: Channel?,
        var totalUnreadChannelCount: Long,
        val totalUnreadMessageCount: Long
)