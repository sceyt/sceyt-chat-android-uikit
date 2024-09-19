package com.sceyt.chatuikit.data.models.channels

import com.sceyt.chatuikit.SceytChatUIKit

enum class ChannelTypeEnum {
    Direct, Group, Public;

    fun getString() = when (this) {
        Direct -> SceytChatUIKit.config.channelTypesConfig.directChannel
        Group -> SceytChatUIKit.config.channelTypesConfig.groupChannel
        Public -> SceytChatUIKit.config.channelTypesConfig.broadcastChannel
    }
}

fun stringToEnum(type: String): ChannelTypeEnum {
    return when (type) {
        ChannelTypeEnum.Direct.getString() -> ChannelTypeEnum.Direct
        ChannelTypeEnum.Public.getString() -> ChannelTypeEnum.Public
        ChannelTypeEnum.Group.getString() -> ChannelTypeEnum.Group
        else -> ChannelTypeEnum.Group
    }
}