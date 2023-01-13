package com.sceyt.sceytchatuikit.persistence.extensions

import android.content.Context
import android.util.Log
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.sceytchatuikit.extensions.getMimeTypeTakeExtension
import com.sceyt.sceytchatuikit.shared.utils.FileResizeUtil
import com.sceyt.sceytchatuikit.shared.utils.TranscodeResultEnum.*
import com.sceyt.sceytchatuikit.shared.utils.VideoTranscodeHelper
import java.io.File
import java.util.*

fun Attachment.resizeImage(context: Context): Attachment {
    var resizedAttachment = this
    try {
        val resizedImageFile = FileResizeUtil.resizeAndCompressImage(context, url, reqSize = 600)
        resizedAttachment = Attachment.Builder(resizedImageFile.path, url, type)
            .withTid(tid)
            .setName(name)
            .setMetadata(metadata)
            .setUpload(true)
            .build()
    } catch (ex: Exception) {
        Log.e("ImageResize", ex.message.toString())
    }
    return resizedAttachment
}

fun resizeImage(context: Context, path: String?, reqSize: Int = 600): Result<String> {
    return try {
        path?.let {
            val resizedImageFile = FileResizeUtil.resizeAndCompressImage(context, it, reqSize)
            Result.success(resizedImageFile.path)
        } ?: Result.failure(Exception("Wrong file path"))
    } catch (ex: Exception) {
        Log.e("ImageResize", ex.message.toString())
        Result.failure(ex)
    }
}

suspend fun Attachment.transcodeVideo(context: Context): Attachment {
    var transcodeAttachment = this
    val dest = File(context.cacheDir.toString() + System.currentTimeMillis().toString())
    val result = VideoTranscodeHelper.transcodeAsResult(context, destination = dest, uri = url)
    if (result.resultType == Success) {
        transcodeAttachment = Attachment.Builder(dest.path, url, type)
            .withTid(tid)
            .setName(name)
            .setMetadata(metadata)
            .setUpload(true)
            .build()
    }

    return transcodeAttachment
}

fun transcodeVideo(context: Context, path: String?, callback: (Result<String>) -> Unit) {
    path?.let {
        val dest = File("${context.cacheDir}/" + UUID.randomUUID() + getMimeTypeTakeExtension(path))
        VideoTranscodeHelper.transcodeAsResultWithCallback(context, destination = dest, uri = it) { data ->
            when (data.resultType) {
                Cancelled -> callback(Result.failure(Exception("Canceled")))
                Failure -> callback(Result.failure(Exception(data.errorMessage)))
                Success -> callback(Result.success(dest.path))
                else -> {}
            }
        }
    } ?: run { callback(Result.failure(Exception("Wrong file path"))) }
}