package com.sceyt.sceytchatuikit.data.models.channels

enum class ChannelTypeEnum {
    Direct, Private, Public;

    fun getString() = when (this) {
        Direct -> "direct"
        Private -> "private"
        Public -> "public"
    }
}

fun stringToEnum(type: String): ChannelTypeEnum {
    return when (type) {
        ChannelTypeEnum.Direct.getString() -> ChannelTypeEnum.Direct
        ChannelTypeEnum.Private.getString() -> ChannelTypeEnum.Private
        ChannelTypeEnum.Public.getString() -> ChannelTypeEnum.Public
        else -> throw Exception("Unknown channel type $type")
    }
}