package com.sceyt.sceytchatuikit.presentation.common

import com.sceyt.chat.models.role.Role
import com.sceyt.chat.models.user.UserActivityState
import com.sceyt.sceytchatuikit.SceytKitClient.myId
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.data.models.channels.stringToEnum
import com.sceyt.sceytchatuikit.persistence.extensions.equalsIgnoreNull
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelItemPayloadDiff

fun SceytChannel.diff(other: SceytChannel): ChannelItemPayloadDiff {
    val firstMember = getFirstMember()
    val otherFirstMember = other.getFirstMember()
    val lastMessageChanged = lastMessage != other.lastMessage || lastMessage?.body.equalsIgnoreNull(other.lastMessage?.body).not()
            || lastMessage?.state != other.lastMessage?.state
    val userReactionsChanged = newReactions?.maxOfOrNull { it.id } != other.newReactions?.maxOfOrNull { it.id }
    val lastDraftMessageChanged = draftMessage != other.draftMessage
    val membersCountChanged = memberCount != other.memberCount
    val peerBlockedChanged = isDirect() && firstMember?.user?.blocked != otherFirstMember?.user?.blocked

    return ChannelItemPayloadDiff(
        subjectChanged = channelSubject.equalsIgnoreNull(other.channelSubject).not(),
        avatarViewChanged = iconUrl.equalsIgnoreNull(other.iconUrl).not(),
        lastMessageChanged = lastMessageChanged || userReactionsChanged || lastDraftMessageChanged,
        lastMessageStatusChanged = lastMessage?.deliveryStatus != other.lastMessage?.deliveryStatus,
        unreadCountChanged = newMessageCount != other.newMessageCount,
        muteStateChanged = muted != other.muted,
        onlineStateChanged = isDirect() && firstMember?.user?.presence?.state != otherFirstMember?.user?.presence?.state,
        markedUsUnreadChanged = unread != other.unread,
        lastReadMsdChanged = lastDisplayedMessageId != other.lastDisplayedMessageId,
        peerBlockedChanged = peerBlockedChanged,
        typingStateChanged = typingData != other.typingData,
        membersChanged = membersCountChanged || members != other.members,
        metadataUpdated = metadata != other.metadata)
}

fun SceytChannel.checkIsMemberInChannel(): Boolean {
    return if (isGroup) {
        members?.find { it.id == myId } != null
    } else true
}

fun SceytChannel.getMyRole(): Role? {
    return if (isGroup) {
        members?.find { it.id == myId }?.role
    } else null
}

fun SceytChannel.isPeerDeleted(): Boolean {
    return isDirect() && getFirstMember()?.user?.activityState == UserActivityState.Deleted
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

fun ChannelTypeEnum?.isGroup() = this == ChannelTypeEnum.Private || this == ChannelTypeEnum.Public

fun SceytChannel.isDirect() = type == ChannelTypeEnum.Direct.getString()

fun SceytChannel.isPrivate() = type == ChannelTypeEnum.Private.getString()

fun SceytChannel.isPublic() = type == ChannelTypeEnum.Public.getString()

