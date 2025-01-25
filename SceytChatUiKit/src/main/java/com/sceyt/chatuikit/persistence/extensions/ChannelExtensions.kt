package com.sceyt.chatuikit.persistence.extensions

import com.google.gson.Gson
import com.sceyt.chat.models.user.UserState
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.channels.SelfChannelMetadata
import com.sceyt.chatuikit.data.models.channels.stringToEnum
import com.sceyt.chatuikit.extensions.toBoolean

fun SceytChannel.checkIsMemberInChannel(): Boolean {
    return if (isGroup) {
        !userRole.isNullOrEmpty()
    } else true
}

fun SceytChannel.isPeerDeleted(): Boolean {
    return isDirect() && getPeer()?.user?.state == UserState.Deleted
}

fun SceytChannel.isPeerBlocked(): Boolean {
    return isDirect() && getPeer()?.user?.blocked == true
}

fun SceytChannel.getChannelType(): ChannelTypeEnum {
    return stringToEnum(type)
}

fun SceytChannel.getPeer(): SceytMember? {
    return members?.firstOrNull { it.id != SceytChatUIKit.chatUIFacade.myId } ?: run {
        if (isSelf) members?.firstOrNull() else null
    }
}

fun ChannelTypeEnum?.isGroup() = this != ChannelTypeEnum.Direct

fun SceytChannel.isDirect() = type == ChannelTypeEnum.Direct.value

fun SceytChannel.isPublic() = type == ChannelTypeEnum.Public.value

fun SceytChannel.isGroup() = type != ChannelTypeEnum.Direct.value

internal fun SceytChannel.isSelf(): Boolean {
    val isSelf by lazy {
        getSelfChannelMetadata(metadata)?.isSelf?.toBoolean() ?: false
    }
    return type == ChannelTypeEnum.Direct.value && isSelf
}

internal fun getSelfChannelMetadata(metadata: String?): SelfChannelMetadata? {
    return try {
        Gson().fromJson(metadata, SelfChannelMetadata::class.java)
    } catch (e: Exception) {
        null
    }
}