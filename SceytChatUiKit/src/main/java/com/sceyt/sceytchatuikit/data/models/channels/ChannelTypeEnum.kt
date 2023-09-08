package com.sceyt.sceytchatuikit.data.models.channels

enum class ChannelTypeEnum {
    Direct, Private, Public, Group, Broadcast;

    fun getString() = when (this) {
        Direct -> "direct"
        Private -> "private"
        Public -> "public"
        Group -> "group"
        Broadcast -> "broadcast"
    }
}

fun stringToEnum(type: String): ChannelTypeEnum {
    return when (type) {
        ChannelTypeEnum.Direct.getString() -> ChannelTypeEnum.Direct
        ChannelTypeEnum.Private.getString() -> ChannelTypeEnum.Private
        ChannelTypeEnum.Public.getString() -> ChannelTypeEnum.Public
        ChannelTypeEnum.Group.getString() -> ChannelTypeEnum.Group
        ChannelTypeEnum.Broadcast.getString() -> ChannelTypeEnum.Broadcast
        else -> ChannelTypeEnum.Private
    }
}