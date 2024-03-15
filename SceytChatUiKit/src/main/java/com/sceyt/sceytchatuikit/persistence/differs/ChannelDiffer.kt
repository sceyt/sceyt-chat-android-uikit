package com.sceyt.sceytchatuikit.persistence.differs

import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.persistence.extensions.equalsIgnoreNull
import com.sceyt.sceytchatuikit.presentation.common.getPeer
import com.sceyt.sceytchatuikit.presentation.common.isDirect

data class ChannelDiff(
        val subjectChanged: Boolean,
        val avatarViewChanged: Boolean,
        val lastMessageChanged: Boolean,
        val lastMessageStatusChanged: Boolean,
        val unreadCountChanged: Boolean,
        val muteStateChanged: Boolean,
        val onlineStateChanged: Boolean,
        val markedUsUnreadChanged: Boolean,
        val lastReadMsdChanged: Boolean,
        val peerBlockedChanged: Boolean,
        val typingStateChanged: Boolean,
        val membersChanged: Boolean,
        val metadataUpdated: Boolean,
        val urlUpdated: Boolean,
        val pinStateChanged: Boolean
) {
    fun hasDifference(): Boolean {
        return subjectChanged || avatarViewChanged || lastMessageChanged || lastMessageStatusChanged ||
                unreadCountChanged || muteStateChanged || onlineStateChanged || markedUsUnreadChanged ||
                lastReadMsdChanged || peerBlockedChanged || typingStateChanged || membersChanged ||
                metadataUpdated || urlUpdated || pinStateChanged
    }

    companion object {
        val DEFAULT = ChannelDiff(
            subjectChanged = true,
            avatarViewChanged = true,
            lastMessageChanged = true,
            lastMessageStatusChanged = true,
            unreadCountChanged = true,
            muteStateChanged = true,
            onlineStateChanged = true,
            markedUsUnreadChanged = true,
            lastReadMsdChanged = true,
            peerBlockedChanged = true,
            typingStateChanged = true,
            membersChanged = true,
            metadataUpdated = true,
            urlUpdated = true,
            pinStateChanged = true
        )

        val DEFAULT_FALSE = ChannelDiff(
            subjectChanged = false,
            avatarViewChanged = false,
            lastMessageChanged = false,
            lastMessageStatusChanged = false,
            unreadCountChanged = false,
            muteStateChanged = false,
            onlineStateChanged = false,
            markedUsUnreadChanged = false,
            lastReadMsdChanged = false,
            peerBlockedChanged = false,
            typingStateChanged = false,
            membersChanged = false,
            metadataUpdated = false,
            urlUpdated = false,
            pinStateChanged = false
        )
    }
}

fun SceytChannel.diff(other: SceytChannel): ChannelDiff {
    val firstMember = getPeer()
    val otherFirstMember = other.getPeer()
    val lastMessageChanged = lastMessage != other.lastMessage || lastMessage?.body.equalsIgnoreNull(other.lastMessage?.body).not()
            || lastMessage?.state != other.lastMessage?.state || lastMessage?.bodyAttributes.equalsIgnoreNull(lastMessage?.bodyAttributes).not()
    val pendingReactionChanged = pendingReactions != other.pendingReactions
    val userReactionsChanged = pendingReactionChanged || newReactions?.maxOfOrNull { it.id } != other.newReactions?.maxOfOrNull { it.id }
    val lastDraftMessageChanged = draftMessage != other.draftMessage
    val membersCountChanged = memberCount != other.memberCount && userRole != other.userRole
    val peerBlockedChanged = isDirect() && firstMember?.user?.blocked != otherFirstMember?.user?.blocked

    return ChannelDiff(
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
        metadataUpdated = metadata != other.metadata,
        urlUpdated = uri != other.uri,
        pinStateChanged = pinnedAt != other.pinnedAt)
}

