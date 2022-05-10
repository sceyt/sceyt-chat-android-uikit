package com.sceyt.chat.ui.data.models.channels

import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.channel.DirectChannel
import com.sceyt.chat.models.channel.PrivateChannel

enum class ChannelTypeEnum {
    Direct, Private, Group
}

fun getChannelType(channel: Channel): ChannelTypeEnum {
    return when (channel) {
        is DirectChannel -> ChannelTypeEnum.Direct
        is PrivateChannel -> ChannelTypeEnum.Private
        else -> ChannelTypeEnum.Group
    }
}
