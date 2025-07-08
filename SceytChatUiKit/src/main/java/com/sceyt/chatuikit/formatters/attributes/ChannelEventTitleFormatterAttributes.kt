package com.sceyt.chatuikit.formatters.attributes

import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.presentation.components.channel.header.helpers.ChannelEventData

data class ChannelEventTitleFormatterAttributes(
        val channel: SceytChannel,
        val users: List<ChannelEventData>,
)