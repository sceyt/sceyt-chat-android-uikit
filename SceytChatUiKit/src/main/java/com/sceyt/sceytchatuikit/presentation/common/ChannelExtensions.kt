package com.sceyt.sceytchatuikit.presentation.common

import com.sceyt.chat.models.user.UserState
import com.sceyt.sceytchatuikit.SceytKitClient.myId
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.data.models.channels.stringToEnum
import com.sceyt.sceytchatuikit.sceytstyles.UserStyle

fun SceytChannel.checkIsMemberInChannel(): Boolean {
    return if (isGroup) {
        !userRole.isNullOrEmpty()
    } else true
}

fun SceytChannel.isPeerDeleted(): Boolean {
    return isDirect() && getFirstMember()?.user?.activityState == UserState.Deleted
}

fun SceytChannel.getDefaultAvatar(): Int {
    return if (isDirect()) {
        if (isPeerDeleted()) UserStyle.deletedUserAvatar
        else UserStyle.userDefaultAvatar
    } else 0
}

fun SceytChannel.isPeerBlocked(): Boolean {
    return isDirect() && getFirstMember()?.user?.blocked == true
}

fun SceytChannel.getChannelType(): ChannelTypeEnum {
    return stringToEnum(type)
}

fun SceytChannel.getFirstMember(): SceytMember? {
    return members?.firstOrNull { it.id != myId }
}

fun ChannelTypeEnum?.isGroup() = this != ChannelTypeEnum.Direct

fun SceytChannel.isDirect() = type == ChannelTypeEnum.Direct.getString()

fun SceytChannel.isPrivate() = type == ChannelTypeEnum.Private.getString() || type == ChannelTypeEnum.Group.getString()

fun SceytChannel.isPublic() = type == ChannelTypeEnum.Public.getString() || type == ChannelTypeEnum.Broadcast.getString()

