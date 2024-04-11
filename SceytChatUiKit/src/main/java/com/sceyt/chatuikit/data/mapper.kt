package com.sceyt.chatuikit.data

import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.user.Presence
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.data.models.channels.DraftMessage
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.entity.messages.DraftMessageDb
import com.sceyt.chatuikit.persistence.entity.messages.DraftMessageEntity
import com.sceyt.chatuikit.persistence.filetransfer.TransferState
import com.sceyt.chatuikit.persistence.mappers.getLinkPreviewDetails
import com.sceyt.chatuikit.persistence.mappers.toSceytMessage
import com.sceyt.chatuikit.persistence.mappers.toUser
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem


fun Member.toSceytMember() = SceytMember(
    role = role,
    user = this
)

fun SceytMember.toMember(): Member {
    return Member(role, user)
}

fun Attachment.toSceytAttachment(messageTid: Long, transferState: TransferState,
                                 progress: Float = 0f, linkPreviewDetails: LinkPreviewDetails? = null) = SceytAttachment(
    id = id,
    messageTid = messageTid,
    messageId = messageId,
    userId = userId,
    name = name,
    type = type,
    metadata = metadata,
    fileSize = uploadedFileSize,
    createdAt = createdAt,
    url = url,
    filePath = filePath,
    transferState = transferState,
    progressPercent = progress,
    originalFilePath = filePath,
    linkPreviewDetails = linkPreviewDetails ?: getLinkPreviewDetails()
)


fun SceytAttachment.toAttachment(): Attachment = Attachment(
    id ?: 0,
    messageId,
    name,
    type,
    metadata ?: "",
    fileSize,
    url ?: "",
    createdAt,
    userId,
    0,
    filePath ?: "",
    false,
)


fun SceytAttachment.toFileListItem(message: SceytMessage): FileListItem {
    return when (type) {
        AttachmentTypeEnum.Image.value() -> FileListItem.Image(this, message)
        AttachmentTypeEnum.Video.value() -> FileListItem.Video(this, message)
        AttachmentTypeEnum.Voice.value() -> FileListItem.Voice(this, message)
        else -> FileListItem.File(this, message)
    }
}

fun Presence.hasDiff(other: Presence): Boolean {
    return state != other.state || status != other.status || lastActiveAt != other.lastActiveAt
}

fun DraftMessageDb.toDraftMessage() = DraftMessage(
    chatId = draftMessageEntity.chatId,
    message = draftMessageEntity.message,
    createdAt = draftMessageEntity.createdAt,
    mentionUsers = mentionUsers?.map { it.toUser() },
    replyOrEditMessage = replyOrEditMessage?.toSceytMessage(),
    isReply = draftMessageEntity.isReplyMessage ?: false,
    bodyAttributes = draftMessageEntity.styleRanges
)

fun DraftMessageEntity.toDraftMessage(mentionUsers: List<User>?, replyMessage: SceytMessage?) = DraftMessage(
    chatId = chatId,
    message = message,
    createdAt = createdAt,
    mentionUsers = mentionUsers,
    replyOrEditMessage = replyMessage,
    isReply = isReplyMessage ?: false,
    bodyAttributes = styleRanges
)

fun User.copy() = User(
    id, firstName, lastName, avatarURL, metadata, presence?.copy(), activityState, blocked
)

fun Presence.copy() = Presence(
    state, status, lastActiveAt
)