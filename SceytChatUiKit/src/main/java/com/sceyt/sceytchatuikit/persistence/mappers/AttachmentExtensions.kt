package com.sceyt.sceytchatuikit.persistence.mappers

import android.content.ContentValues.TAG
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.util.Size
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.extensions.isNotNullOrBlank
import com.sceyt.sceytchatuikit.extensions.toBase64
import com.sceyt.sceytchatuikit.shared.utils.FileResizeUtil
import org.json.JSONObject

fun createMetadata(currentMetadata: String?, base64String: String?, size: Size?): String? {
    return try {
        val obj = if (currentMetadata.isNullOrBlank()) JSONObject()
        else JSONObject(currentMetadata.toString())
        obj.put("thumbnail", base64String)
        size?.let {
            obj.put("width", it.width)
            obj.put("height", it.height)
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
            obj.put("width", it.width)
            obj.put("height", it.height)
        }
        metadata = obj.toString()
    } catch (t: Throwable) {
        Log.e(TAG, "Could not parse malformed JSON: \"" + metadata.toString() + "\"")
    }
}

fun String?.getThumbByBytesAndSize(needThumb: Boolean): Pair<Size?, ByteArray?>? {
    var base64Thumb: ByteArray? = null
    var size: Size? = null
    try {
        val jsonObject = JSONObject(this ?: return null)
        if (needThumb) {
            val thumbnail = jsonObject.getString("thumbnail")
            base64Thumb = Base64.decode(thumbnail, Base64.NO_WRAP)
        }
        val width = jsonObject.getString("width").toIntOrNull()
        val height = jsonObject.getString("height").toIntOrNull()
        if (width != null && height != null)
            size = Size(width, height)
    } catch (ex: Exception) {
        Log.i("MetadataReader", "Couldn't get data from attachment metadata with reason ${ex.message}")
    }
    if (size == null && base64Thumb == null)
        return null

    return Pair(size, base64Thumb)
}

fun SceytAttachment.addBlurredBytesAndSizeToMetadata() {
    getBlurredBytesAndSizeToAsString(filePath, type)?.let {
        metadata = it
    }
}

fun getBlurredBytesAndSizeToAsString(filePath: String?, type: String): String? {
    return try {
        filePath?.let { path ->
            var size: Size? = null
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
                    FileResizeUtil.getVideoThumbByUrlAsByteArray(path, 10f)?.let { bytes ->
                        base64String = bytes.toBase64()
                    }
                }
                else -> return null
            }
            createMetadata(null, base64String, size)
        }
    } catch (ex: Exception) {
        Log.e(TAG, "Couldn't get an blurred image or sizes.")
        null
    }
}

fun SceytAttachment.existThumb(): Boolean {
    return try {
        val jsonObject = JSONObject(metadata ?: return false)
        jsonObject.getString("thumbnail").isNotNullOrBlank()
    } catch (ex: Exception) {
        Log.i("MetadataReader", "Couldn't get data from attachment metadata with reason ${ex.message}")
        false
    }
}

fun getMetadataFromThumb(filePath: String?, type: String): String? {
    return getBlurredBytesAndSizeToAsString(filePath, type)
}