package com.sceyt.chat.ui.presentation.common

import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.ChannelItemPayloadDiff

internal fun SceytChannel.diff(other: SceytChannel): ChannelItemPayloadDiff {
    return ChannelItemPayloadDiff(
        subjectChanged = channelSubject != other.channelSubject,
        avatarViewChanged = iconUrl != other.iconUrl,
        lastMessageChanged = lastMessage != other.lastMessage,
        lastMessageStatusChanged = lastMessage?.deliveryStatus != other.lastMessage?.deliveryStatus,
        unreadCountChanged = unreadCount != other.unreadCount,
        muteStateChanged = muted != other.muted
    )
}