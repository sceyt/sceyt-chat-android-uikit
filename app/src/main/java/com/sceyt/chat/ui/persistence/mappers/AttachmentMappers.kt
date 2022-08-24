package com.sceyt.chat.ui.persistence.mappers

import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.ui.data.models.messages.SceytAttachment
import com.sceyt.chat.ui.persistence.entity.messages.AttachmentEntity

fun Attachment.toAttachmentEntity(messageId: Long) = AttachmentEntity(
    messageId = messageId,
    tid = tid,
    name = name,
    type = type,
    metadata = metadata,
    fileSize = uploadedFileSize,
    url = url
)


fun SceytAttachment.toAttachmentEntity(messageId: Long) = AttachmentEntity(
    messageId = messageId,
    tid = tid,
    name = name,
    type = type,
    metadata = metadata,
    fileSize = fileSize,
    url = url
)


fun AttachmentEntity.toAttachment() = SceytAttachment(
    tid = tid,
    name = name,
    type = type,
    metadata = metadata,
    fileSize = fileSize,
    url = url
)