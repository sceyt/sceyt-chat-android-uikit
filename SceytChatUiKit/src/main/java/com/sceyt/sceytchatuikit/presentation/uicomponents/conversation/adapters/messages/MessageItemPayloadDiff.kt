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
        val filesChanged: Boolean,
        val selectionChanged: Boolean
) {
    fun hasDifference(): Boolean {
        return edited || bodyChanged || statusChanged || avatarChanged || nameChanged || replyCountChanged
                || replyContainerChanged || reactionsChanged || showAvatarAndNameChanged || filesChanged || selectionChanged
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
            filesChanged = true,
            selectionChanged = true
        )
        val DEFAULT_FALSE = MessageItemPayloadDiff(
            edited = false,
            bodyChanged = false,
            statusChanged = false,
            avatarChanged = false,
            nameChanged = false,
            replyCountChanged = false,
            replyContainerChanged = false,
            reactionsChanged = false,
            showAvatarAndNameChanged = false,
            filesChanged = false,
            selectionChanged = false
        )
    }

    override fun toString(): String {
        return "edited: $edited, bodyChanged: $bodyChanged, statusChanged: $statusChanged, avatarChanged: $avatarChanged, " +
                "nameChanged: $nameChanged, replyCountChanged: $replyCountChanged, reactionsChanged: $reactionsChanged, " +
                "showAvatarAndNameChanged: $showAvatarAndNameChanged, filesChanged: $filesChanged, selectionChanged: $selectionChanged"
    }
}
