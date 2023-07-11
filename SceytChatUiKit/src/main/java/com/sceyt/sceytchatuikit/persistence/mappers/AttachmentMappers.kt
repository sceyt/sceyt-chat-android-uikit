package com.sceyt.sceytchatuikit.persistence.mappers

import android.graphics.Bitmap
import android.util.Size
import com.google.gson.Gson
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.extensions.decodeByteArrayToBitmap
import com.sceyt.sceytchatuikit.extensions.getMimeTypeTakeFirstPart
import com.sceyt.sceytchatuikit.extensions.toByteArraySafety
import com.sceyt.sceytchatuikit.logger.SceytLog
import com.sceyt.sceytchatuikit.persistence.constants.SceytConstants
import com.sceyt.sceytchatuikit.persistence.entity.messages.AttachmentDb
import com.sceyt.sceytchatuikit.persistence.entity.messages.AttachmentEntity
import com.sceyt.sceytchatuikit.persistence.entity.messages.AttachmentPayLoadEntity
import com.sceyt.sceytchatuikit.persistence.entity.messages.MessageEntity
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import com.sceyt.sceytchatuikit.presentation.customviews.voicerecorder.AudioMetadata
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.AttachmentDataFromJson
import org.json.JSONObject

fun SceytAttachment.toAttachmentDb(messageId: Long, messageTid: Long, channelId: Long) = AttachmentDb(
    AttachmentEntity(
        id = id,
        messageId = messageId,
        messageTid = messageTid,
        channelId = channelId,
        userId = userId,
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
            id = id,
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

fun AttachmentDb.toSdkAttachment(upload: Boolean): Attachment {
    with(attachmentEntity) {
        return Attachment.Builder(filePath ?: "", url ?: "", type)
            .setMetadata(metadata ?: "")
            .setName(name)
            .setUpload(upload)
            .build()
    }
}

fun AttachmentDb.toAttachmentPayLoad(messageStatus: MessageEntity): AttachmentPayLoadEntity {
    return with(attachmentEntity) {
        AttachmentPayLoadEntity(
            messageTid = messageTid,
            transferState = if (!messageStatus.incoming && messageStatus.deliveryStatus == DeliveryStatus.Pending
                    && messageStatus.forwardingDetailsDb == null)
                TransferState.PendingUpload else TransferState.PendingDownload,
            progressPercent = 0f,
            url = url,
            filePath = filePath)
    }
}

fun AttachmentPayLoadEntity.toTransferData(default: TransferState): TransferData {
    return TransferData(
        messageTid = messageTid,
        state = transferState ?: default,
        progressPercent = 0f,
        url = url,
        filePath = filePath
    )
}

fun SceytAttachment.toTransferData(): TransferData? {
    return TransferData(
        messageTid = messageTid,
        progressPercent = (progressPercent ?: 0).toFloat(),
        state = transferState ?: return null,
        filePath = filePath,
        url = url
    )
}

fun SceytAttachment.getInfoFromMetadata(): AttachmentDataFromJson {
    var size: Size? = null
    var duration: Long? = null
    var blurredThumbBitmap: Bitmap? = null
    var audioMetadata: AudioMetadata? = null

    try {
        val jsonObject = JSONObject(metadata ?: return AttachmentDataFromJson())
        when (type) {
            AttachmentTypeEnum.File.value(), AttachmentTypeEnum.Link.value() -> {
                return AttachmentDataFromJson()
            }

            AttachmentTypeEnum.Image.value(), AttachmentTypeEnum.Video.value() -> {
                blurredThumbBitmap = jsonObject.getFromJsonObject(SceytConstants.Thumb)?.toByteArraySafety()?.decodeByteArrayToBitmap()

                val width = jsonObject.getFromJsonObject(SceytConstants.Width)?.toIntOrNull()
                val height = jsonObject.getFromJsonObject(SceytConstants.Height)?.toIntOrNull()
                if (width != null && height != null)
                    size = Size(width, height)
            }

            AttachmentTypeEnum.Voice.value() -> audioMetadata = getMetadataFromAttachment()
        }

        if (type == AttachmentTypeEnum.Video.value() || type == AttachmentTypeEnum.Voice.value())
            duration = jsonObject.getFromJsonObject(SceytConstants.Duration)?.toLongOrNull()

    } catch (ex: Exception) {
        SceytLog.i(TAG, "Couldn't get data from attachment metadata with reason ${ex.message}")
    }

    return AttachmentDataFromJson(size, duration, blurredThumbBitmap, audioMetadata)
}

fun SceytAttachment.getMetadataFromAttachment(): AudioMetadata {
    return try {
        val result = Gson().fromJson(metadata, AudioMetadata::class.java)
        if (result.tmb == null) {
            // if thumb is null, should set it to an empty array
            result.copy(tmb = intArrayOf(0))
        } else result
    } catch (ex: Exception) {
        ex.printStackTrace()
        null
    } ?: AudioMetadata(intArrayOf(0), 0)
}

fun getAttachmentType(path: String?): AttachmentTypeEnum {
    return when (getMimeTypeTakeFirstPart(path)) {
        AttachmentTypeEnum.Image.value() -> AttachmentTypeEnum.Image
        AttachmentTypeEnum.Video.value() -> AttachmentTypeEnum.Video
        else -> AttachmentTypeEnum.File
    }
}

fun String?.getInfoFromMetadataByKey(key: String): String? {
    try {
        val jsonObject = JSONObject(this ?: return null)
        return jsonObject.getFromJsonObject(key)
    } catch (ex: Exception) {
        SceytLog.i("GetInfoFromMeta", "Couldn't get data from attachment metadata with reason ${ex.message}")
        return null
    }
}

private fun JSONObject.getFromJsonObject(name: String): String? {
    return try {
        getString(name)
    } catch (ex: Exception) {
        null
    }
}