package com.sceyt.chat.ui.data.models.channels

import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.channel.DirectChannel
import com.sceyt.chat.models.channel.PrivateChannel
import com.sceyt.chat.models.channel.PublicChannel

enum class ChannelTypeEnum {
    Direct, Private, Public
}

fun getChannelType(channel: Channel): ChannelTypeEnum {
    return when (channel) {
        is DirectChannel -> ChannelTypeEnum.Direct
        is PrivateChannel -> ChannelTypeEnum.Private
        is PublicChannel -> ChannelTypeEnum.Public
        else -> throw Exception("Not supported channel type")
    }
}

fun ChannelTypeEnum?.isGroup() = this == ChannelTypeEnum.Private || this == ChannelTypeEnum.Public

