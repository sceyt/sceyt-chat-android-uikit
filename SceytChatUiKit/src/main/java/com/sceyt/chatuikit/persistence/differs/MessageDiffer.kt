package com.sceyt.chatuikit.persistence.differs

import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytPollDetails
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.persistence.extensions.equalsIgnoreNull
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.FileListItem
import com.sceyt.chatuikit.presentation.extensions.isSelfDestructed

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
    val isSelfDestructedChanged: Boolean
) {
    fun hasDifference(): Boolean {
        return edited || bodyChanged || statusChanged || avatarChanged || nameChanged || replyCountChanged
                || replyContainerChanged || reactionsChanged || showAvatarAndNameChanged || filesChanged
                || selectionChanged || metadataChanged || pollChanged || isSelfDestructedChanged
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
            pollChanged = true,
            isSelfDestructedChanged = true
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
            pollChanged = false,
            isSelfDestructedChanged = false
        )
    }

    override fun toString(): String {
        return "edited: $edited, bodyChanged: $bodyChanged, statusChanged: $statusChanged, avatarChanged: $avatarChanged, " +
                "nameChanged: $nameChanged, replyCountChanged: $replyCountChanged, reactionsChanged: $reactionsChanged, " +
                "showAvatarAndNameChanged: $showAvatarAndNameChanged, filesChanged: $filesChanged, selectionChanged: $selectionChanged, " +
                "metadataChanged: $metadataChanged, pollChanged: $pollChanged viewOnceStateChanged: $isSelfDestructedChanged"
    }
}

fun SceytMessage.diff(other: SceytMessage): MessageDiff {
    return MessageDiff(
        edited = state != other.state,
        bodyChanged = body != other.body || bodyAttributes != other.bodyAttributes ||
                isBodyExpanded != other.isBodyExpanded,
        statusChanged = !incoming && deliveryStatus != other.deliveryStatus,
        avatarChanged = user?.avatarURL.equalsIgnoreNull(other.user?.avatarURL).not(),
        nameChanged = user?.fullName.equalsIgnoreNull(other.user?.fullName).not(),
        replyCountChanged = replyCount != other.replyCount,
        replyContainerChanged = hasReplyContentDifference(other),
        reactionsChanged = messageReactions?.equalsIgnoreNull(other.messageReactions)?.not()
            ?: other.reactionTotals.isNullOrEmpty().not(),
        showAvatarAndNameChanged = shouldShowAvatarAndName != other.shouldShowAvatarAndName
                || disabledShowAvatarAndName != other.disabledShowAvatarAndName,
        filesChanged = hasFileContentDifference(other),
        selectionChanged = isSelected != other.isSelected,
        metadataChanged = metadata != other.metadata,
        pollChanged = poll?.pollChanged(other.poll) ?: (other.poll != null),
        isSelfDestructedChanged = isSelfDestructedChanged(other)
    )
}

fun SceytMessage.diffContent(other: SceytMessage): MessageDiff {
    return MessageDiff(
        edited = state != other.state,
        bodyChanged = body != other.body || bodyAttributes != other.bodyAttributes ||
                isBodyExpanded != other.isBodyExpanded,
        statusChanged = !incoming && deliveryStatus != other.deliveryStatus,
        avatarChanged = user?.avatarURL.equalsIgnoreNull(other.user?.avatarURL).not(),
        nameChanged = user?.fullName.equalsIgnoreNull(other.user?.fullName).not(),
        replyCountChanged = replyCount != other.replyCount,
        replyContainerChanged = hasReplyContentDifference(other),
        reactionsChanged = reactionTotals?.equalsIgnoreNull(other.reactionTotals)?.not()
            ?: other.reactionTotals.isNullOrEmpty().not(),
        showAvatarAndNameChanged = false,
        filesChanged = hasFileContentDifference(other),
        selectionChanged = isSelected != other.isSelected,
        metadataChanged = metadata != other.metadata,
        pollChanged = poll?.pollChanged(other.poll) ?: (other.poll != null),
        isSelfDestructedChanged = isSelfDestructedChanged(other)
    )
}

fun SceytMessage.isSelfDestructedChanged(other: SceytMessage): Boolean {
    if (!viewOnce || !other.viewOnce)
        return false

    return isSelfDestructed() != other.isSelfDestructed()
}

private fun SceytMessage.hasFileContentDifference(other: SceytMessage): Boolean {
    return attachments.hasAttachmentContentDifference(other.attachments) ||
            files.hasRenderStateDifference(other.files)
}

private fun List<FileListItem>?.hasRenderStateDifference(other: List<FileListItem>?): Boolean {
    if (this === other) return false
    val first = orEmpty()
    val second = other.orEmpty()
    if (first.size != second.size) return true

    return first.indices.any {
        first[it].attachment.hasContentDifferenceIgnoringProgress(second[it].attachment) ||
                first[it].thumbPath != second[it].thumbPath
    }
}

private fun SceytMessage.hasReplyContentDifference(other: SceytMessage): Boolean {
    return parentMessage?.id != other.parentMessage?.id ||
            parentMessage?.tid != other.parentMessage?.tid ||
            parentMessage?.user != other.parentMessage?.user ||
            parentMessage?.state != other.parentMessage?.state ||
            parentMessage?.body != other.parentMessage?.body ||
            parentMessage?.attachments.hasAttachmentContentDifference(
                other.parentMessage?.attachments
            )
}

private fun List<SceytAttachment>?.hasAttachmentContentDifference(
    other: List<SceytAttachment>?,
): Boolean {
    if (this === other) return false
    if (this == null || other == null || size != other.size) return true
    return indices.any { get(it).hasContentDifferenceIgnoringProgress(other[it]) }
}

private fun SceytAttachment.hasContentDifferenceIgnoringProgress(
    other: SceytAttachment,
): Boolean {
    if (linkPreviewDetails != other.linkPreviewDetails) return true
    if (this == other) return false
    if (progressPercent == other.progressPercent) return true

    return copy(progressPercent = other.progressPercent) != other
}

fun SceytPollDetails.pollChanged(other: SceytPollDetails?): Boolean {
    val otherPoll = other ?: return true
    if (id != otherPoll.id) return true
    if (messageTid != otherPoll.messageTid) return true
    if (!name.equalsIgnoreNull(otherPoll.name)) return true
    if (!description.equalsIgnoreNull(otherPoll.description)) return true
    if (anonymous != otherPoll.anonymous) return true
    if (allowMultipleVotes != otherPoll.allowMultipleVotes) return true
    if (allowVoteRetract != otherPoll.allowVoteRetract) return true
    if (votesPerOption.size != otherPoll.votesPerOption.size) return true
    if (options.size != otherPoll.options.size) return true
    if (votes != otherPoll.votes) return true
    if (ownVotes != otherPoll.ownVotes) return true
    if (!pendingVotes.equalsIgnoreNull(otherPoll.pendingVotes)) return true
    if (createdAt != otherPoll.createdAt) return true
    if (updatedAt != otherPoll.updatedAt) return true
    if (closedAt != otherPoll.closedAt) return true
    if (closed != otherPoll.closed) return true

    return false
}
