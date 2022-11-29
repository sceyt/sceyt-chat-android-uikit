package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages

data class MessageItemPayloadDiff(
        val edited: Boolean,
        val bodyChanged: Boolean,
        val statusChanged: Boolean,
        val avatarChanged: Boolean,
        val nameChanged: Boolean,
        val replyCountChanged: Boolean,
        val replyContainerChanged: Boolean,
        val reactionsChanged: Boolean,
        val showAvatarAndNameChanged: Boolean,
        val filesChanged: Boolean
) {
    fun hasDifference(): Boolean {
        return edited || bodyChanged || statusChanged || avatarChanged || nameChanged || replyCountChanged
                || replyContainerChanged || reactionsChanged || showAvatarAndNameChanged || filesChanged
    }

    companion object {
        val DEFAULT = MessageItemPayloadDiff(
            edited = true,
            bodyChanged = true,
            statusChanged = true,
            avatarChanged = true,
            nameChanged = true,
            replyCountChanged = true,
            replyContainerChanged = true,
            reactionsChanged = true,
            showAvatarAndNameChanged = true,
            filesChanged = true
        )
    }

    override fun toString(): String {
        return "edited: $edited, bodyChanged: $bodyChanged, statusChanged: $statusChanged, avatarChanged: $avatarChanged, " +
                "nameChanged: $nameChanged, replyCountChanged: $replyCountChanged, reactionsChanged: $reactionsChanged, " +
                "showAvatarAndNameChanged: $showAvatarAndNameChanged, filesChanged: $filesChanged"
    }
}
