package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages

data class MessageItemPayloadDiff(
        val edited: Boolean,
        val bodyChanged: Boolean,
        val statusChanged: Boolean,
        val avatarChanged: Boolean,
        val nameChanged: Boolean,
        val replayCountChanged: Boolean,
        val replayContainerChanged: Boolean,
        val reactionsChanged: Boolean,
        val showAvatarAndNameChanged: Boolean,
        val filesChanged: Boolean
) {
    fun hasDifference(): Boolean {
        return edited || bodyChanged || statusChanged || avatarChanged || nameChanged || replayCountChanged
                || replayContainerChanged || showAvatarAndNameChanged /*|| reactionsChanged || filesChanged*/
    }

    companion object {
        val DEFAULT = MessageItemPayloadDiff(
            edited = true,
            bodyChanged = true,
            statusChanged = true,
            avatarChanged = true,
            nameChanged = true,
            replayCountChanged = true,
            replayContainerChanged = true,
            reactionsChanged = true,
            showAvatarAndNameChanged = true,
            filesChanged = true
        )
    }

    override fun toString(): String {
        return "edited: $edited, bodyChanged: $bodyChanged, statusChanged: $statusChanged, avatarChanged: $avatarChanged, " +
                "nameChanged: $nameChanged, replayCountChanged: $replayCountChanged, reactionsChanged: $reactionsChanged, " +
                "showAvatarAndNameChanged: $showAvatarAndNameChanged, filesChanged: $filesChanged"
    }
}
