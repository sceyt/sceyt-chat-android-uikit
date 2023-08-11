package com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter

data class ChannelItemPayloadDiff(
        val subjectChanged: Boolean,
        val avatarViewChanged: Boolean,
        var lastMessageChanged: Boolean,
        val lastMessageStatusChanged: Boolean,
        val unreadCountChanged: Boolean,
        val muteStateChanged: Boolean,
        val onlineStateChanged: Boolean,
        val markedUsUnreadChanged: Boolean,
        val lastReadMsdChanged: Boolean,
        val peerBlockedChanged: Boolean,
        val typingStateChanged: Boolean,
        val membersChanged: Boolean,
        val metadataUpdated: Boolean
) {
    fun hasDifference(): Boolean {
        return subjectChanged || avatarViewChanged || lastMessageChanged || lastMessageStatusChanged ||
                unreadCountChanged || muteStateChanged || onlineStateChanged || markedUsUnreadChanged ||
                lastReadMsdChanged || peerBlockedChanged || typingStateChanged || membersChanged || metadataUpdated
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
            membersChanged = true,
            metadataUpdated = true
        )

        val DEFAULT_FALSE = ChannelItemPayloadDiff(
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
            metadataUpdated = false
        )
    }
}
