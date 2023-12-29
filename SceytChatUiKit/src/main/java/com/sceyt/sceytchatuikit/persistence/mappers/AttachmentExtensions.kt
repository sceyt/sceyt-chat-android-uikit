package com.sceyt.sceytchatuikit.persistence.mappers

import android.content.ContentValues.TAG
import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.Size
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.extensions.getStringOrNull
import com.sceyt.sceytchatuikit.extensions.isNotNullOrBlank
import com.sceyt.sceytchatuikit.extensions.toBase64
import com.sceyt.sceytchatuikit.logger.SceytLog
import com.sceyt.sceytchatuikit.persistence.constants.SceytConstants
import com.sceyt.sceytchatuikit.shared.utils.BitmapUtil
import com.sceyt.sceytchatuikit.shared.utils.FileResizeUtil
import com.sceyt.sceytchatuikit.shared.utils.ThumbHash
import org.json.JSONObject
import java.util.concurrent.TimeUnit

fun createMetadata(currentMetadata: String?, base64String: String?, size: Size?,
                   duration: Long?, description: String?, imageUrl: String?,
                   thumbnailUrl: String?): String? {
    return try {
        val obj = if (currentMetadata.isNullOrBlank()) JSONObject()
        else JSONObject(currentMetadata.toString())
        obj.put(SceytConstants.Thumb, base64String)
        size?.let {
            obj.put(SceytConstants.Width, it.width)
            obj.put(SceytConstants.Height, it.height)
        }
        duration?.let {
            val durSec = TimeUnit.MILLISECONDS.toSeconds(it)
            obj.put(SceytConstants.Duration, durSec)
        }
        description?.let {
            obj.put(SceytConstants.Description, it)
        }
        imageUrl?.let {
            obj.put(SceytConstants.ImageUrl, it)
        }
        thumbnailUrl?.let {
            obj.put(SceytConstants.ThumbnailUrl, it)
        }
        obj.toString()
    } catch (t: Throwable) {
        SceytLog.e(TAG, "Could not parse malformed JSON: \"" + currentMetadata.toString() + "\"")
        null
    }
}

fun LinkPreviewDetails.toMetadata(): String? {
    val size = if (imageWidth != null && imageHeight != null) Size(imageWidth!!, imageHeight!!) else null
    return createMetadata(null, null, size, null, description, imageUrl, faviconUrl)
}

fun SceytAttachment.upsertSizeMetadata(size: Size?) {
    try {
        val obj = if (metadata.isNullOrBlank()) JSONObject()
        else JSONObject(metadata.toString())
        size?.let {
            obj.put(SceytConstants.Width, it.width)
            obj.put(SceytConstants.Height, it.height)
        }
        metadata = obj.toString()
    } catch (t: Throwable) {
        Log.e(TAG, "Could not parse malformed JSON: \"" + metadata.toString() + "\"")
    }
}

fun SceytAttachment.addAttachmentMetadata(context: Context) {
    getBlurredBytesAndSizeAsString(context, metadata, filePath, type)?.let {
        metadata = it
    } ?: run { metadata = metadata ?: "" }
}

fun getBlurredBytesAndSizeAsString(context: Context, metadata: String?, filePath: String?, type: String): String? {
    return try {
        filePath?.let { path ->
            val size: Size?
            var durationMilliSec: Long? = null
            var base64String: String? = null
            when (type) {
                AttachmentTypeEnum.Image.value() -> {
                    size = FileResizeUtil.getImageSizeOriented(Uri.parse(path))
                    FileResizeUtil.resizeAndCompressBitmapWithFilePath(path, 100)?.let { bm ->
                        val bytes = ThumbHash.rgbaToThumbHash(bm.width, bm.height, BitmapUtil.bitmapToRgba(bm))
                        base64String = bytes.toBase64()
                    }
                }

                AttachmentTypeEnum.Video.value() -> {
                    size = FileResizeUtil.getVideoSizeOriented(path)
                    durationMilliSec = FileResizeUtil.getVideoDuration(context, filePath)
                    FileResizeUtil.getVideoThumbByUrlAsByteArray(path, 100f)?.let { bm ->
                        val bytes = ThumbHash.rgbaToThumbHash(bm.width, bm.height, BitmapUtil.bitmapToRgba(bm))
                        base64String = bytes.toBase64()
                    }
                }

                else -> return null
            }
            createMetadata(metadata, base64String, size, durationMilliSec, null, null, null)
        }
    } catch (ex: Exception) {
        Log.e(TAG, "Couldn't get an blurred image or sizes.")
        null
    }
}

fun getDimensions(type: String, path: String): Size? {
    return when (type) {
        AttachmentTypeEnum.Image.value() -> {
            FileResizeUtil.getImageDimensionsSize(Uri.parse(path))
        }

        AttachmentTypeEnum.Video.value() -> {
            FileResizeUtil.getVideoSize(path)
        }

        else -> return null
    }
}

fun SceytAttachment.existThumb(): Boolean {
    return try {
        val jsonObject = JSONObject(metadata ?: return false)
        jsonObject.getString(SceytConstants.Thumb).isNotNullOrBlank()
    } catch (ex: Exception) {
        false
    }
}


fun SceytAttachment.getLinkPreviewDetails(): LinkPreviewDetails? {
    try {
        val jsonObject = JSONObject(metadata ?: return null)
        val thumb = jsonObject.getStringOrNull(SceytConstants.Thumb)
        val width = jsonObject.getStringOrNull(SceytConstants.Width)
        val height = jsonObject.getStringOrNull(SceytConstants.Height)
        val description = jsonObject.getStringOrNull(SceytConstants.Description)
        val imageUrl = jsonObject.getStringOrNull(SceytConstants.ImageUrl)
        val thumbnailUrl = jsonObject.getStringOrNull(SceytConstants.ThumbnailUrl)
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
            thumb = thumb)
    } catch (ex: Exception) {
        return null
    }
}

fun SceytAttachment.isLink(): Boolean {
    return type == AttachmentTypeEnum.Link.value()
}