package com.sceyt.chat.ui.presentation.common

import com.sceyt.chat.models.member.Member
import com.sceyt.chat.ui.data.models.channels.ChannelTypeEnum
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.models.channels.SceytDirectChannel
import com.sceyt.chat.ui.data.toPublicChannel
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.ChannelItemPayloadDiff

internal fun SceytChannel.diff(other: SceytChannel): ChannelItemPayloadDiff {
    return ChannelItemPayloadDiff(
        subjectChanged = channelSubject != other.channelSubject,
        avatarViewChanged = iconUrl != other.iconUrl,
        lastMessageChanged = lastMessage != other.lastMessage,
        lastMessageStatusChanged = lastMessage?.deliveryStatus != other.lastMessage?.deliveryStatus,
        unreadCountChanged = unreadCount != other.unreadCount,
        muteStateChanged = muted != other.muted,
        onlineStateChanged = channelType == ChannelTypeEnum.Direct
                && (this as? SceytDirectChannel)?.peer?.presence?.state != (other as? SceytDirectChannel)?.peer?.presence?.state
    )
}

fun SceytChannel.checkIsMemberInPublicChannel(): Boolean {
    return channelType == ChannelTypeEnum.Public && toPublicChannel().myRole() != Member.MemberType.MemberTypeNone
}