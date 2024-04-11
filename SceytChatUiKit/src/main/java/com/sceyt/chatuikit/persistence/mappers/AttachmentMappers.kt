package com.sceyt.chatuikit.persistence.mappers

import android.graphics.Bitmap
import android.util.Size
import com.google.gson.Gson
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.FileChecksumData
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.extensions.decodeByteArrayToBitmap
import com.sceyt.chatuikit.extensions.getMimeTypeTakeFirstPart
import com.sceyt.chatuikit.extensions.toByteArraySafety
import com.sceyt.chatuikit.data.constants.SceytConstants
import com.sceyt.chatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.chatuikit.persistence.entity.FileChecksumEntity
import com.sceyt.chatuikit.persistence.entity.messages.AttachmentDb
import com.sceyt.chatuikit.persistence.entity.messages.AttachmentEntity
import com.sceyt.chatuikit.persistence.entity.messages.AttachmentPayLoadEntity
import com.sceyt.chatuikit.persistence.entity.messages.MessageEntity
import com.sceyt.chatuikit.persistence.filetransfer.TransferData
import com.sceyt.chatuikit.persistence.filetransfer.TransferData.Companion.withPrettySizes
import com.sceyt.chatuikit.persistence.filetransfer.TransferState
import com.sceyt.chatuikit.presentation.customviews.voicerecorder.AudioMetadata
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.files.AttachmentDataFromJson
import com.sceyt.chatuikit.shared.utils.BitmapUtil
import com.sceyt.chatuikit.shared.utils.ThumbHash
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
        filePath = filePath,
        originalFilePath = originalFilePath), null, linkPreviewDetails?.toLinkDetailsEntity())

fun AttachmentDb.toAttachment(): SceytAttachment {
    with(attachmentEntity) {
        val isLink = type == AttachmentTypeEnum.Link.value()
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
            url = if (isLink) url else payLoad?.url ?: url,
            filePath = if (isLink) null else payLoad?.filePath ?: filePath,
            transferState = if (isLink) TransferState.PendingDownload else payLoad?.transferState,
            progressPercent = if (isLink) 0f else payLoad?.progressPercent,
            originalFilePath = if (isLink) null else originalFilePath,
            linkPreviewDetails = linkDetails?.toLinkPreviewDetails(isHiddenLinkDetails()))
    }
}

fun AttachmentDb.toSdkAttachment(upload: Boolean): Attachment {
    with(attachmentEntity) {
        val isLink = type == AttachmentTypeEnum.Link.value()
        return Attachment.Builder(if (isLink) "" else filePath ?: "", url ?: "", type)
            .setMetadata(metadata ?: "")
            .setName(name)
            .setUpload(upload)
            .build()
    }
}

fun AttachmentDb.toAttachmentPayLoad(messageStatus: MessageEntity): AttachmentPayLoadEntity {
    return with(attachmentEntity) {
        val isLink = type == AttachmentTypeEnum.Link.value()
        AttachmentPayLoadEntity(
            messageTid = messageTid,
            transferState = if (!messageStatus.incoming && messageStatus.deliveryStatus == DeliveryStatus.Pending
                    && messageStatus.forwardingDetailsDb == null && !isLink)
                TransferState.PendingUpload else TransferState.PendingDownload,
            progressPercent = 0f,
            url = url,
            filePath = if (isLink) null else filePath)
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
    ).withPrettySizes(fileSize)
}

fun SceytAttachment.toTransferData(transferState: TransferState,
                                   progress: Float = progressPercent ?: 0f): TransferData {
    return TransferData(
        messageTid = messageTid,
        progressPercent = progress,
        state = transferState,
        filePath = filePath,
        url = url
    )
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

fun FileChecksumEntity.toFileChecksumData() = FileChecksumData(
    checksum, resizedFilePath, url, metadata, fileSize
)

fun SceytAttachment.getInfoFromMetadata(): AttachmentDataFromJson {
    var size: Size? = null
    var duration: Long? = null
    var blurredThumbBitmap: Bitmap? = null
    var audioMetadata: AudioMetadata? = null

    try {
        val jsonObject = JSONObject(metadata ?: return AttachmentDataFromJson())
        when (type) {
            AttachmentTypeEnum.File.value() -> {
                return AttachmentDataFromJson()
            }

            AttachmentTypeEnum.Image.value(), AttachmentTypeEnum.Video.value(), AttachmentTypeEnum.Link.value() -> {
                blurredThumbBitmap = getThumbFromMetadata(metadata)

                val width = jsonObject.getFromJsonObject(SceytConstants.Width)?.toIntOrNull()
                val height = jsonObject.getFromJsonObject(SceytConstants.Height)?.toIntOrNull()
                if (width != null && height != null)
                    size = Size(width, height)
            }

            AttachmentTypeEnum.Voice.value() -> audioMetadata = getMetadataFromAttachment()
        }

        if (type == AttachmentTypeEnum.Video.value() || type == AttachmentTypeEnum.Voice.value())
            duration = jsonObject.getFromJsonObject(SceytConstants.Duration)?.toLongOrNull()

    } catch (_: Exception) {
    }

    return AttachmentDataFromJson(size, duration, blurredThumbBitmap, audioMetadata)
}

fun getThumbFromMetadata(metadata: String?): Bitmap? {
    metadata ?: return null
    var blurredThumbBitmap: Bitmap? = null
    val bytes = metadata.getInfoFromMetadataByKey(SceytConstants.Thumb)?.toByteArraySafety()
    try {
        val image = ThumbHash.thumbHashToRGBA(bytes)
        blurredThumbBitmap = BitmapUtil.bitmapFromRgba(image.width, image.height, image.rgba)
    } catch (_: Exception) {
    }

    if (blurredThumbBitmap == null && bytes != null)
        blurredThumbBitmap = bytes.decodeByteArrayToBitmap()

    return blurredThumbBitmap
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