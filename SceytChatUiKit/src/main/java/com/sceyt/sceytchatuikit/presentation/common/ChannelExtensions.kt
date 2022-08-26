package com.sceyt.sceytchatuikit.presentation.common

import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytDirectChannel
import com.sceyt.sceytchatuikit.data.toGroupChannel
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelItemPayloadDiff

internal fun SceytChannel.diff(other: SceytChannel): ChannelItemPayloadDiff {
    return ChannelItemPayloadDiff(
        subjectChanged = channelSubject != other.channelSubject,
        avatarViewChanged = iconUrl != other.iconUrl,
        lastMessageChanged = lastMessage != other.lastMessage,
        lastMessageStatusChanged = lastMessage?.deliveryStatus != other.lastMessage?.deliveryStatus,
        unreadCountChanged = unreadCount != other.unreadCount,
        muteStateChanged = muted != other.muted,
        onlineStateChanged = channelType == ChannelTypeEnum.Direct
                && (this as? SceytDirectChannel)?.peer?.user?.presence?.state != (other as? SceytDirectChannel)?.peer?.user?.presence?.state
    )
}

fun SceytChannel.checkIsMemberInChannel(myId: String?): Boolean {
    return if (isGroup) {
        toGroupChannel().members.find { it.id == myId } != null
    } else true
}