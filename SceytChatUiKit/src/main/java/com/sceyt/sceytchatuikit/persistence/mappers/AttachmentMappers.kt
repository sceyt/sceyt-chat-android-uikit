package com.sceyt.sceytchatuikit.persistence.mappers

import com.google.gson.Gson
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentMetadata
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.persistence.entity.messages.AttachmentEntity

fun Attachment.toAttachmentEntity(messageId: Long, messageTid: Long) = AttachmentEntity(
    messageId = messageId,
    messageTid = messageTid,
    tid = tid,
    name = name,
    type = type,
    metadata = metadata,
    fileSize = uploadedFileSize,
    url = url,
    filePath = filePath
)


fun SceytAttachment.toAttachmentEntity(messageId: Long, messageTid: Long) = AttachmentEntity(
    messageId = messageId,
    messageTid = messageTid,
    tid = tid,
    name = name,
    type = type,
    metadata = metadata,
    fileSize = fileSize,
    url = url,
    filePath = filePath
)


fun AttachmentEntity.toAttachment() = SceytAttachment(
    tid = tid,
    messageTid = messageTid,
    name = name,
    type = type,
    metadata = metadata,
    fileSize = fileSize,
    url = url,
    filePath = filePath
)


fun AttachmentEntity.toSdkAttachment(upload: Boolean = true): Attachment {
    val data = Gson().fromJson(metadata, AttachmentMetadata::class.java)
    return Attachment.Builder(data.localPath, url, type)
        .setMetadata(metadata)
        .setName(name)
        .withTid(tid)
        .setUpload(upload)
        .build()
}
