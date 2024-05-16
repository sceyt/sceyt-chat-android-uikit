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
    return isDirect() && getPeer()?.user?.activityState == UserState.Deleted
}

fun SceytChannel.getDefaultAvatar(): Int {
    val theme = SceytChatUIKit.theme
    return if (isDirect()) {
        when {
            isPeerDeleted() -> theme.deletedUserAvatar
            isSelf() -> theme.notesAvatar
            else -> theme.userDefaultAvatar
        }
    } else 0
}

fun SceytChannel.isPeerBlocked(): Boolean {
    return isDirect() && getPeer()?.user?.blocked == true
}

fun SceytChannel.getChannelType(): ChannelTypeEnum {
    return stringToEnum(type)
}

fun SceytChannel.getPeer(): SceytMember? {
    return members?.firstOrNull { it.id != SceytChatUIKit.chatUIFacade.myId } ?: run {
        if (isSelf()) members?.firstOrNull() else null
    }
}

fun ChannelTypeEnum?.isGroup() = this != ChannelTypeEnum.Direct

fun SceytChannel.isDirect() = type == ChannelTypeEnum.Direct.getString()

fun SceytChannel.isPrivate() = type == ChannelTypeEnum.Private.getString() || type == ChannelTypeEnum.Group.getString()

fun SceytChannel.isPublic() = type == ChannelTypeEnum.Public.getString() || type == ChannelTypeEnum.Broadcast.getString()

fun SceytChannel.isSelf(): Boolean {
    val isSelf = try {
        Gson().fromJson(metadata, SelfChannelMetadata::class.java).isSelf?.toBoolean() ?: false
    } catch (e: Exception) {
        false
    }
    return type == ChannelTypeEnum.Direct.getString() && isSelf
}

