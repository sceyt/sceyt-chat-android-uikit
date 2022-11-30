package com.sceyt.sceytchatuikit.data.models.channels

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

fun stringToEnum(type: String): ChannelTypeEnum {
    return when (type) {
        "direct" -> ChannelTypeEnum.Direct
        "private" -> ChannelTypeEnum.Private
        "public" -> ChannelTypeEnum.Public
        else -> throw Exception("Unknown channel type $type")
    }
}

fun ChannelTypeEnum?.isGroup() = this == ChannelTypeEnum.Private || this == ChannelTypeEnum.Public

