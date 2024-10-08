package com.sceyt.chatuikit.data.models.channels

import com.sceyt.chatuikit.SceytChatUIKit

enum class ChannelTypeEnum {
    Direct,
    Group,
    Public;

    val value: String
        get() = when (this) {
            Direct -> SceytChatUIKit.config.channelTypesConfig.direct
            Group -> SceytChatUIKit.config.channelTypesConfig.group
            Public -> SceytChatUIKit.config.channelTypesConfig.broadcast
        }
}

fun stringToEnum(type: String): ChannelTypeEnum {
    return when (type) {
        ChannelTypeEnum.Direct.value -> ChannelTypeEnum.Direct
        ChannelTypeEnum.Public.value -> ChannelTypeEnum.Public
        ChannelTypeEnum.Group.value -> ChannelTypeEnum.Group
        else -> ChannelTypeEnum.Group
    }
}