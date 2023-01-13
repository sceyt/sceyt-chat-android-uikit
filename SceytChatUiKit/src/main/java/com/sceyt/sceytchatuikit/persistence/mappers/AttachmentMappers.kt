package com.sceyt.sceytchatuikit.persistence.mappers

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import android.util.Size
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.extensions.decodeByteArrayToBitmap
import com.sceyt.sceytchatuikit.persistence.constants.SceytConstants
import com.sceyt.sceytchatuikit.persistence.entity.messages.AttachmentDb
import com.sceyt.sceytchatuikit.persistence.entity.messages.AttachmentEntity
import com.sceyt.sceytchatuikit.persistence.entity.messages.AttachmentPayLoadEntity
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import org.json.JSONObject

fun SceytAttachment.toAttachmentDb(messageId: Long, messageTid: Long) = AttachmentDb(
    AttachmentEntity(
        id = id,
        messageId = messageId,
        messageTid = messageTid,
        userId = userId,
        tid = tid,
        name = name,
        type = type,
        createdAt = createdAt,
        metadata = metadata,
        fileSize = fileSize,
        url = url,
        filePath = filePath), null
)


fun AttachmentDb.toAttachment(): SceytAttachment {
    with(attachmentEntity) {
        return SceytAttachment(
            id = primaryKey.toLong(),
            tid = tid,
            messageId = messageId,
            messageTid = messageTid,
            userId = userId,
            name = name,
            type = type,
            metadata = metadata,
            fileSize = fileSize,
            createdAt = createdAt,
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

fun SceytAttachment.toTransferData(): TransferData? {
    return TransferData(
        messageTid = messageTid,
        attachmentTid = tid,
        progressPercent = (progressPercent ?: 0).toFloat(),
        state = transferState ?: return null,
        filePath = filePath,
        url = url
    )
}

fun SceytAttachment.getInfoFromMetadata(callback: (size: Size?, blurredThumb: Bitmap?, duration: Long?) -> Unit) {
    metadata?.getInfoFromMetadata(callback)
}

private fun String?.getInfoFromMetadata(callback: (size: Size?, blurredThumb: Bitmap?, videoDuration: Long?) -> Unit) {
    var base64Thumb: ByteArray? = null
    var size: Size? = null
    var duration: Long? = null
    try {
        val jsonObject = JSONObject(this ?: return)
        jsonObject.getFromJsonObject(SceytConstants.Thumb)?.let {
            base64Thumb = Base64.decode(it, Base64.NO_WRAP)
        }
        val width = jsonObject.getFromJsonObject(SceytConstants.Width)?.toIntOrNull()
        val height = jsonObject.getFromJsonObject(SceytConstants.Height)?.toIntOrNull()
        duration = jsonObject.getFromJsonObject(SceytConstants.Duration)?.toLongOrNull()
        if (width != null && height != null)
            size = Size(width, height)
    } catch (ex: Exception) {
        Log.i(this?.TAG, "Couldn't get data from attachment metadata with reason ${ex.message}")
    }

    callback(size, base64Thumb?.decodeByteArrayToBitmap(), duration)
}

private fun String?.getInfoFromMetadata(keys: List<String>, callback: (size: Size?, blurredThumb: Bitmap?, videoDuration: Long?) -> Unit) {
    var base64Thumb: ByteArray? = null
    var size: Size? = null
    var duration: Long? = null
    try {
        val jsonObject = JSONObject(this ?: return)
        jsonObject.getFromJsonObject(SceytConstants.Thumb)?.let {
            base64Thumb = Base64.decode(it, Base64.NO_WRAP)
        }
        val width = jsonObject.getFromJsonObject(SceytConstants.Width)?.toIntOrNull()
        val height = jsonObject.getFromJsonObject(SceytConstants.Height)?.toIntOrNull()
        duration = jsonObject.getFromJsonObject(SceytConstants.Duration)?.toLongOrNull()
        if (width != null && height != null)
            size = Size(width, height)
    } catch (ex: Exception) {
        Log.i(this?.TAG, "Couldn't get data from attachment metadata with reason ${ex.message}")
    }

    callback(size, base64Thumb?.decodeByteArrayToBitmap(), duration)
}

private fun JSONObject.getFromJsonObject(name: String): String? {
    return try {
        getString(name)
    } catch (ex: Exception) {
        null
    }
}