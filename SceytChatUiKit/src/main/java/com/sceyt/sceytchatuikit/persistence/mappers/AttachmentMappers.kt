package com.sceyt.sceytchatuikit.persistence.mappers

import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.persistence.entity.messages.AttachmentDb
import com.sceyt.sceytchatuikit.persistence.entity.messages.AttachmentEntity
import com.sceyt.sceytchatuikit.persistence.entity.messages.AttachmentPayLoadEntity
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState

fun SceytAttachment.toAttachmentDb(messageId: Long, messageTid: Long) = AttachmentDb(
    AttachmentEntity(messageId = messageId,
        messageTid = messageTid,
        tid = tid,
        name = name,
        type = type,
        metadata = metadata,
        fileSize = fileSize,
        url = url,
        filePath = filePath), null
)


fun AttachmentDb.toAttachment(): SceytAttachment {
    with(attachmentEntity) {
        return SceytAttachment(
            tid = tid,
            messageTid = messageTid,
            name = name,
            type = type,
            metadata = metadata,
            fileSize = fileSize,
            url = payLoad?.url ?: url,
            filePath = payLoad?.filePath ?: filePath,
            transferState = payLoad?.transferState,
            progressPercent = payLoad?.progressPercent)
    }
}


fun AttachmentDb.toSdkAttachment(upload: Boolean = true): Attachment {
    with(attachmentEntity) {
        return Attachment.Builder(filePath, url, type)
            .setMetadata(metadata)
            .setName(name)
            .withTid(tid)
            .setUpload(upload)
            .build()
    }
}

fun AttachmentDb.toAttachmentPayLoad(messageStatus: DeliveryStatus): AttachmentPayLoadEntity {
    return with(attachmentEntity) {
        AttachmentPayLoadEntity(
            messageTid = messageTid,
            transferState = if (messageStatus == DeliveryStatus.Pending)
                TransferState.PendingUpload else TransferState.PendingDownload,
            progressPercent = 0f,
            url = url,
            filePath = filePath)
    }
}

fun AttachmentPayLoadEntity.toTransferData(attachmentTid: Long, default: TransferState): TransferData {
    return TransferData(
        messageTid = messageTid,
        attachmentTid = attachmentTid,
        state = transferState ?: default,
        progressPercent = 0f,
        url = url,
        filePath = filePath
    )
}