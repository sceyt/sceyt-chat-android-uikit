package com.sceyt.chatuikit.persistence.mappers

import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.message.BodyAttribute
import com.sceyt.chatuikit.data.models.channels.DraftAttachment
import com.sceyt.chatuikit.data.models.channels.DraftMessage
import com.sceyt.chatuikit.data.models.channels.DraftVoiceAttachment
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.media.audio.AudioRecordData
import com.sceyt.chatuikit.persistence.database.entity.messages.DraftAttachmentEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.DraftMessageDb
import com.sceyt.chatuikit.persistence.database.entity.messages.DraftMessageEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.DraftVoiceAttachmentEntity
import com.sceyt.chatuikit.persistence.file_transfer.TransferState

internal fun DraftMessageDb.toDraftMessage() = DraftMessage(
    channelId = draftMessageEntity.chatId,
    body = draftMessageEntity.message,
    createdAt = draftMessageEntity.createdAt,
    mentionUsers = mentionUsers?.map { it.toSceytUser() },
    replyOrEditMessage = replyOrEditMessage?.toSceytMessage(),
    isReply = draftMessageEntity.isReplyMessage ?: false,
    bodyAttributes = draftMessageEntity.styleRanges,
    attachments = attachments?.map { it.toDraftAttachment() },
    voiceAttachment = voiceAttachment?.toDraftVoiceAttachment()
)

internal fun DraftAttachmentEntity.toDraftAttachment() = DraftAttachment(
    channelId = chatId,
    filePath = filePath,
    type = type
)

internal fun DraftVoiceAttachmentEntity.toDraftVoiceAttachment() = DraftVoiceAttachment(
    channelId = chatId,
    filePath = filePath,
    duration = duration,
    amplitudes = amplitudes
)

internal fun DraftMessage.toDraftMessageEntity(
        bodyAttributes: List<BodyAttribute>?,
) = DraftMessageEntity(
    chatId = channelId,
    message = body,
    createdAt = createdAt,
    replyOrEditMessageId = replyOrEditMessage?.id,
    isReplyMessage = isReply,
    styleRanges = bodyAttributes
)

internal fun DraftAttachment.toDraftAttachmentEntity() = DraftAttachmentEntity(
    chatId = channelId,
    filePath = filePath,
    type = type
)

internal fun DraftVoiceAttachment.toDraftVoiceAttachmentEntity() = DraftVoiceAttachmentEntity(
    chatId = channelId,
    filePath = filePath,
    duration = duration,
    amplitudes = amplitudes
)

internal fun AudioRecordData.toVoiceAttachmentData(channelId: Long) = DraftVoiceAttachment(
    channelId = channelId,
    filePath = file.path,
    duration = duration,
    amplitudes = amplitudes
)

internal fun DraftAttachment.toSceytAttachment() = Attachment
    .Builder(
        /* filePath = */ filePath,
        /* url = */ "",
        /* type = */ type.value,
    )
    .build()
    .toSceytAttachment(0L, TransferState.PendingUpload)

internal fun DraftVoiceAttachment.toSceytAttachment() = Attachment
    .Builder(
        /* filePath = */ filePath,
        /* url = */ "",
        /* type = */ AttachmentTypeEnum.Voice.value,
    )
    .build()
    .toSceytAttachment(0L, TransferState.PendingUpload)