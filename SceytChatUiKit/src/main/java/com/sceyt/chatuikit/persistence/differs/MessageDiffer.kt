package com.sceyt.chatuikit.persistence.differs

import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.extensions.equalsIgnoreNull

data class MessageDiff(
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
    val selectionChanged: Boolean,
    val metadataChanged: Boolean,
    val pollChanged: Boolean,
) {
    fun hasDifference(): Boolean {
        return edited || bodyChanged || statusChanged || avatarChanged || nameChanged || replyCountChanged
                || replyContainerChanged || reactionsChanged || showAvatarAndNameChanged || filesChanged
                || selectionChanged || metadataChanged || pollChanged
    }

    companion object {
        val DEFAULT = MessageDiff(
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
            selectionChanged = true,
            metadataChanged = true,
            pollChanged = true
        )
        val DEFAULT_FALSE = MessageDiff(
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
            selectionChanged = false,
            metadataChanged = false,
            pollChanged = false
        )
    }

    override fun toString(): String {
        return "edited: $edited, bodyChanged: $bodyChanged, statusChanged: $statusChanged, avatarChanged: $avatarChanged, " +
                "nameChanged: $nameChanged, replyCountChanged: $replyCountChanged, reactionsChanged: $reactionsChanged, " +
                "showAvatarAndNameChanged: $showAvatarAndNameChanged, filesChanged: $filesChanged, selectionChanged: $selectionChanged, " +
                "metadataChanged: $metadataChanged, pollChanged: $pollChanged"
    }
}

fun SceytMessage.diff(other: SceytMessage): MessageDiff {
    return MessageDiff(
        edited = state != other.state,
        bodyChanged = body != other.body || bodyAttributes != other.bodyAttributes,
        statusChanged = !incoming && deliveryStatus != other.deliveryStatus,
        avatarChanged = user?.avatarURL.equalsIgnoreNull(other.user?.avatarURL).not(),
        nameChanged = user?.fullName.equalsIgnoreNull(other.user?.fullName).not(),
        replyCountChanged = replyCount != other.replyCount,
        replyContainerChanged = parentMessage != other.parentMessage || parentMessage?.user != other.parentMessage?.user
                || parentMessage?.state != other.parentMessage?.state || parentMessage?.body != other.parentMessage?.body,
        reactionsChanged = messageReactions?.equalsIgnoreNull(other.messageReactions)?.not()
            ?: other.reactionTotals.isNullOrEmpty().not(),
        showAvatarAndNameChanged = shouldShowAvatarAndName != other.shouldShowAvatarAndName
                || disabledShowAvatarAndName != other.disabledShowAvatarAndName,
        filesChanged = attachments?.size != other.attachments?.size,
        selectionChanged = isSelected != other.isSelected,
        metadataChanged = metadata != other.metadata,
        pollChanged = poll != other.poll
    )
}

fun SceytMessage.diffContent(other: SceytMessage): MessageDiff {
    return MessageDiff(
        edited = state != other.state,
        bodyChanged = body != other.body || bodyAttributes != other.bodyAttributes,
        statusChanged = !incoming && deliveryStatus != other.deliveryStatus,
        avatarChanged = user?.avatarURL.equalsIgnoreNull(other.user?.avatarURL).not(),
        nameChanged = user?.fullName.equalsIgnoreNull(other.user?.fullName).not(),
        replyCountChanged = replyCount != other.replyCount,
        replyContainerChanged = parentMessage != other.parentMessage || parentMessage?.user != other.parentMessage?.user
                || parentMessage?.state != other.parentMessage?.state || parentMessage?.body != other.parentMessage?.body,
        reactionsChanged = reactionTotals?.equalsIgnoreNull(other.reactionTotals)?.not()
            ?: other.reactionTotals.isNullOrEmpty().not(),
        showAvatarAndNameChanged = false,
        filesChanged = attachments?.size != other.attachments?.size,
        selectionChanged = isSelected != other.isSelected,
        metadataChanged = metadata != other.metadata,
        pollChanged = poll != other.poll
    )
}
