package com.sceyt.sceytchatuikit.data.channeleventobserver

import com.sceyt.chat.models.channel.Channel

data class ChannelUnreadCountUpdatedEventData(
        val channel: Channel?,
        var totalUnreadChannelCount: Long,
        val totalUnreadMessageCount: Long
)