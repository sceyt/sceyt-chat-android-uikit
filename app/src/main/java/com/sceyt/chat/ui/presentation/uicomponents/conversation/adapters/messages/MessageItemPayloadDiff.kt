package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages

data class MessageItemPayloadDiff(
        val edited: Boolean,
        val statusChanged: Boolean,
        val avatarChanged: Boolean,
        val nameChanged: Boolean,
        val replayCountChanged: Boolean,
        val reactionsChanged: Boolean,
        val showAvatarAndNameChanged: Boolean,
        val filesChanged: Boolean,
) {
    fun hasDifference(): Boolean {
        return edited || statusChanged || avatarChanged || nameChanged || replayCountChanged
                || showAvatarAndNameChanged || reactionsChanged || filesChanged
    }

    companion object {
        val DEFAULT = MessageItemPayloadDiff(
            edited = true,
            statusChanged = true,
            avatarChanged = true,
            nameChanged = true,
            replayCountChanged = true,
            reactionsChanged = true,
            showAvatarAndNameChanged = true,
            filesChanged = true
        )
    }
}
