package com.sceyt.sceytchatuikit.presentation.common

import com.sceyt.chat.models.role.Role
import com.sceyt.chat.models.user.UserActivityStatus
import com.sceyt.sceytchatuikit.SceytKitClient.myId
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytDirectChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytGroupChannel
import com.sceyt.sceytchatuikit.data.toGroupChannel
import com.sceyt.sceytchatuikit.persistence.extensions.equalsIgnoreNull
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelItemPayloadDiff

fun SceytChannel.diff(other: SceytChannel): ChannelItemPayloadDiff {
    val lastMessageChanged = lastMessage != other.lastMessage || lastMessage?.body.equalsIgnoreNull(other.lastMessage?.body).not()
    val peerBlockedChanged = channelType == ChannelTypeEnum.Direct
            && (this as? SceytDirectChannel)?.peer?.user?.blocked != (other as? SceytDirectChannel)?.peer?.user?.blocked
    return ChannelItemPayloadDiff(
        subjectChanged = channelSubject.equalsIgnoreNull(other.channelSubject).not(),
        avatarViewChanged = iconUrl.equalsIgnoreNull(other.iconUrl).not(),
        lastMessageChanged = lastMessageChanged,
        lastMessageStatusChanged = lastMessage?.deliveryStatus != other.lastMessage?.deliveryStatus || lastMessageChanged,
        unreadCountChanged = unreadMessageCount != other.unreadMessageCount,
        muteStateChanged = muted != other.muted,
        onlineStateChanged = channelType == ChannelTypeEnum.Direct
                && (this as? SceytDirectChannel)?.peer?.user?.presence?.state != (other as? SceytDirectChannel)?.peer?.user?.presence?.state,
        markedUsUnreadChanged = markedUsUnread != other.markedUsUnread,
        lastReadMsdChanged = lastReadMessageId != other.lastReadMessageId,
        peerBlockedChanged = peerBlockedChanged,
        typingStateChanged = typingData != other.typingData,
        membersChanged = (this as? SceytGroupChannel)?.memberCount != (other as? SceytGroupChannel)?.memberCount,
        metadataUpdated = metadata != other.metadata)
}

fun SceytChannel.checkIsMemberInChannel(): Boolean {
    return if (isGroup) {
        toGroupChannel().members.find { it.id == myId } != null
    } else true
}

fun SceytChannel.getMyRole(): Role? {
    return if (isGroup) {
        toGroupChannel().members.find { it.id == myId }?.role
    } else null
}

fun SceytChannel.isPeerDeleted(): Boolean {
    return (this is SceytDirectChannel) && peer?.user?.activityState == UserActivityStatus.Deleted
}