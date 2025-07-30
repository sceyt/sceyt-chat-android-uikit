package com.sceyt.chatuikit.formatters.attributes

import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.styles.channel.ChannelItemStyle

data class ChannelItemSubtitleFormatterAttributes(
        val channel: SceytChannel,
        val channelItemStyle: ChannelItemStyle,
)