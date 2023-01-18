package com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter

data class ChannelItemPayloadDiff(
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
        val membersChanged: Boolean
) {
    fun hasDifference(): Boolean {
        return subjectChanged || avatarViewChanged || lastMessageChanged || lastMessageStatusChanged ||
                unreadCountChanged || muteStateChanged || onlineStateChanged || markedUsUnreadChanged ||
                lastReadMsdChanged || peerBlockedChanged || typingStateChanged || membersChanged
    }

    companion object {
        val DEFAULT = ChannelItemPayloadDiff(
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
            membersChanged = true
        )
    }
}
