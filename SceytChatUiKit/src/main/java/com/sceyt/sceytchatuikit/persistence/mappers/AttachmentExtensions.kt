package com.sceyt.sceytchatuikit.persistence.mappers

import android.app.Application
import android.content.ContentValues.TAG
import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.Size
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.MessageTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.extensions.*
import com.sceyt.sceytchatuikit.persistence.constants.SceytConstants
import com.sceyt.sceytchatuikit.shared.utils.FileResizeUtil
import org.json.JSONObject
import java.util.concurrent.TimeUnit

fun getMessageTypeFromAttachments(body: String?, vararg attachments: Attachment): String {
    if (attachments.isNotEmpty() && attachments.size == 1) {
        return if (attachments[0].type.isEqualsVideoOrImage())
            MessageTypeEnum.Media.value()
        else MessageTypeEnum.File.value()
    }
    return if (body.isLink()) MessageTypeEnum.Link.value() else MessageTypeEnum.Text.value()
}

fun createMetadata(currentMetadata: String?, base64String: String?, size: Size?, duration: Long?): String? {
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
        obj.toString()
    } catch (t: Throwable) {
        Log.e(TAG, "Could not parse malformed JSON: \"" + currentMetadata.toString() + "\"")
        null
    }
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

fun SceytAttachment.addAttachmentMetadata(application: Application) {
    getBlurredBytesAndSizeToAsString(application, filePath, type)?.let {
        metadata = it
    } ?: run { metadata = "" }
}

fun getBlurredBytesAndSizeToAsString(context: Context, filePath: String?, type: String): String? {
    return try {
        filePath?.let { path ->
            val size: Size?
            var durationMilliSec: Long? = null
            var base64String: String? = null
            when (type) {
                AttachmentTypeEnum.Image.value() -> {
                    size = FileResizeUtil.getImageSize(Uri.parse(path))
                    FileResizeUtil.getImageThumbByUrlAsByteArray(path, 10f)?.let { bytes ->
                        base64String = bytes.toBase64()
                    }
                }
                AttachmentTypeEnum.Video.value() -> {
                    size = FileResizeUtil.getVideoSize(path)
                    durationMilliSec = FileResizeUtil.getVideoDuration(context, filePath)
                    FileResizeUtil.getVideoThumbByUrlAsByteArray(path, 10f)?.let { bytes ->
                        base64String = bytes.toBase64()
                    }
                }
                else -> return null
            }
            createMetadata(null, base64String, size, durationMilliSec)
        }
    } catch (ex: Exception) {
        Log.e(TAG, "Couldn't get an blurred image or sizes.")
        null
    }
}

fun getDimensions(type: String, path: String): Size? {
    return when (type) {
        AttachmentTypeEnum.Image.value() -> {
            FileResizeUtil.getImageSize(Uri.parse(path))
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
        Log.i("MetadataReader", "Couldn't get data from attachment metadata with reason ${ex.message}")
        false
    }
}

fun getMetadataFromThumb(context: Context, filePath: String?, type: String): String? {
    return getBlurredBytesAndSizeToAsString(context, filePath, type)
}