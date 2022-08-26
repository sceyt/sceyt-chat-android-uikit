package com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter

data class ChannelItemPayloadDiff(
        val subjectChanged: Boolean,
        val avatarViewChanged: Boolean,
        val lastMessageChanged: Boolean,
        val lastMessageStatusChanged: Boolean,
        val unreadCountChanged: Boolean,
        val muteStateChanged: Boolean,
        val onlineStateChanged: Boolean
) {
    fun hasDifference(): Boolean {
        return subjectChanged || avatarViewChanged || lastMessageChanged || unreadCountChanged || muteStateChanged || onlineStateChanged
    }

    companion object {
        val DEFAULT = ChannelItemPayloadDiff(
            subjectChanged = true,
            avatarViewChanged = true,
            lastMessageChanged = true,
            lastMessageStatusChanged = true,
            unreadCountChanged = true,
            muteStateChanged = true,
            onlineStateChanged = true
        )
    }
}
