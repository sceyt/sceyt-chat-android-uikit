package com.sceyt.chatuikit.persistence.mappers

import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import android.util.Size
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chatuikit.data.constants.SceytConstants
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.extensions.getBooleanOrNull
import com.sceyt.chatuikit.extensions.getStringOrNull
import com.sceyt.chatuikit.extensions.isNotNullOrBlank
import com.sceyt.chatuikit.extensions.toBase64
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.database.entity.messages.AttachmentEntity
import com.sceyt.chatuikit.shared.utils.BitmapUtil
import com.sceyt.chatuikit.shared.utils.FileResizeUtil
import com.sceyt.chatuikit.shared.utils.ThumbHash
import org.json.JSONObject
import java.util.concurrent.TimeUnit

fun createMetadata(currentMetadata: String?, data: Map<String, Any>): String? {
    return try {
        val obj = if (currentMetadata.isNullOrBlank()) JSONObject()
        else JSONObject(currentMetadata.toString())
        data.forEach {
            obj.put(it.key, it.value)
        }
        obj.toString()
    } catch (t: Throwable) {
        SceytLog.e(TAG, "Could not parse malformed JSON: \"$currentMetadata\", error: ${t.message}")
        null
    }
}

fun LinkPreviewDetails.toMetadata(): String? {
    val data = hashMapOf<String, Any>()
    data[SceytConstants.Thumb] = thumb ?: ""
    if (imageWidth != null && imageHeight != null) {
        data[SceytConstants.Width] = imageWidth
        data[SceytConstants.Height] = imageHeight
    }

    data[SceytConstants.Description] = description ?: ""
    data[SceytConstants.ImageUrl] = imageUrl ?: ""
    data[SceytConstants.ThumbnailUrl] = faviconUrl ?: ""
    data[SceytConstants.HideLinkDetails] = hideDetails
    return createMetadata(null, data)
}

fun SceytAttachment.getUpsertSizeMetadata(size: Size?): String? {
    return try {
        val obj = if (metadata.isNullOrBlank()) JSONObject()
        else JSONObject(metadata.toString())
        size?.let {
            obj.put(SceytConstants.Width, it.width)
            obj.put(SceytConstants.Height, it.height)
        }
        return obj.toString()
    } catch (t: Throwable) {
        Log.e(TAG, "Could not parse malformed JSON: \"$metadata\", error: ${t.message}")
        null
    }
}

fun SceytAttachment.addAttachmentMetadata(context: Context): String {
    return getBlurredBytesAndSizeAsString(context, metadata, filePath, type) ?: run {
        metadata ?: ""
    }
}

fun getBlurredBytesAndSizeAsString(context: Context, metadata: String?, filePath: String?, type: String): String? {
    return try {
        filePath?.let { path ->
            val size: Size?
            var durationMilliSec: Long? = null
            var base64String: String? = null
            when (type) {
                AttachmentTypeEnum.Image.value -> {
                    size = FileResizeUtil.getImageSizeOriented(path)
                    FileResizeUtil.resizeAndCompressBitmapWithFilePath(path, 100)?.let { bm ->
                        val bytes = ThumbHash.rgbaToThumbHash(bm.width, bm.height, BitmapUtil.bitmapToRgba(bm))
                        base64String = bytes.toBase64()
                    }
                }

                AttachmentTypeEnum.Video.value -> {
                    size = FileResizeUtil.getVideoSizeOriented(path)
                    durationMilliSec = FileResizeUtil.getVideoDuration(context, filePath)
                    FileResizeUtil.getVideoThumbByUrlAsByteArray(path, 100f)?.let { bm ->
                        val bytes = ThumbHash.rgbaToThumbHash(bm.width, bm.height, BitmapUtil.bitmapToRgba(bm))
                        base64String = bytes.toBase64()
                    }
                }

                else -> return null
            }

            val data = hashMapOf<String, Any>()
            data[SceytConstants.Thumb] = base64String ?: ""
            size?.let {
                data[SceytConstants.Width] = it.width
                data[SceytConstants.Height] = it.height
            }
            durationMilliSec?.let {
                val durSec = TimeUnit.MILLISECONDS.toSeconds(it)
                data[SceytConstants.Duration] = durSec
            }
            createMetadata(metadata, data)
        }
    } catch (ex: Exception) {
        Log.e(TAG, "Couldn't get an blurred image or sizes. Error: ${ex.message}")
        null
    }
}

fun SceytAttachment.existThumb(): Boolean {
    return try {
        val jsonObject = JSONObject(metadata ?: return false)
        jsonObject.getString(SceytConstants.Thumb).isNotNullOrBlank()
    } catch (_: Exception) {
        false
    }
}


fun SceytAttachment.getLinkPreviewDetails(): LinkPreviewDetails? {
    if (type != AttachmentTypeEnum.Link.value) return null
    if (url.isNullOrBlank()) return null
    try {
        val jsonObject = JSONObject(metadata ?: return null)
        val thumb = jsonObject.getStringOrNull(SceytConstants.Thumb)
        val width = jsonObject.getStringOrNull(SceytConstants.Width)
        val height = jsonObject.getStringOrNull(SceytConstants.Height)
        val description = jsonObject.getStringOrNull(SceytConstants.Description)
        val imageUrl = jsonObject.getStringOrNull(SceytConstants.ImageUrl)
        val thumbnailUrl = jsonObject.getStringOrNull(SceytConstants.ThumbnailUrl)
        val hideLinkDetails = jsonObject.getBooleanOrNull(SceytConstants.HideLinkDetails)
        return LinkPreviewDetails(
            link = url.toString(),
            url = url,
            title = name,
            description = description,
            siteName = "",
            faviconUrl = thumbnailUrl,
            imageUrl = imageUrl,
            imageWidth = width?.toIntOrNull(),
            imageHeight = height?.toIntOrNull(),
            thumb = thumb,
            hideDetails = hideLinkDetails == true)
    } catch (_: Exception) {
        return null
    }
}

fun Attachment.getLinkPreviewDetails(): LinkPreviewDetails? {
    if (type != AttachmentTypeEnum.Link.value) return null
    if (url.isNullOrBlank()) return null
    try {
        val jsonObject = JSONObject(metadata ?: return null)
        val thumb = jsonObject.getStringOrNull(SceytConstants.Thumb)
        val width = jsonObject.getStringOrNull(SceytConstants.Width)
        val height = jsonObject.getStringOrNull(SceytConstants.Height)
        val description = jsonObject.getStringOrNull(SceytConstants.Description)
        val imageUrl = jsonObject.getStringOrNull(SceytConstants.ImageUrl)
        val thumbnailUrl = jsonObject.getStringOrNull(SceytConstants.ThumbnailUrl)
        val hideLinkDetails = jsonObject.getBooleanOrNull(SceytConstants.HideLinkDetails)
        return LinkPreviewDetails(
            link = url,
            url = url,
            title = name,
            description = description,
            siteName = "",
            faviconUrl = thumbnailUrl,
            imageUrl = imageUrl,
            imageWidth = width?.toIntOrNull(),
            imageHeight = height?.toIntOrNull(),
            thumb = thumb,
            hideDetails = hideLinkDetails == true)
    } catch (_: Exception) {
        return null
    }
}

fun SceytAttachment.isLink(): Boolean {
    return type == AttachmentTypeEnum.Link.value
}

fun SceytAttachment.isHiddenLinkDetails(): Boolean {
    return isHiddenLinkDetails(metadata, type)
}

fun Attachment.isHiddenLinkDetails(): Boolean {
    return isHiddenLinkDetails(metadata, type)
}

internal fun AttachmentEntity.isHiddenLinkDetails(): Boolean {
    return isHiddenLinkDetails(metadata, type)
}

private fun isHiddenLinkDetails(metadata: String?, type: String): Boolean {
    if (type != AttachmentTypeEnum.Link.value) return false
    try {
        val jsonObject = JSONObject(metadata ?: return false)
        return jsonObject.getBooleanOrNull(SceytConstants.HideLinkDetails) == true
    } catch (_: Exception) {
        return false
    }
}