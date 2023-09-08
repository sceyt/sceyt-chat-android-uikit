package com.sceyt.sceytchatuikit.persistence.extensions

import android.content.Context
import android.util.Log
import com.abedelazizshe.lightcompressorlibrary.VideoQuality
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.sceytchatuikit.extensions.getMimeTypeTakeExtension
import com.sceyt.sceytchatuikit.shared.utils.FileResizeUtil
import com.sceyt.sceytchatuikit.shared.mediaencoder.TranscodeResultEnum.Cancelled
import com.sceyt.sceytchatuikit.shared.mediaencoder.TranscodeResultEnum.Failure
import com.sceyt.sceytchatuikit.shared.mediaencoder.TranscodeResultEnum.Progress
import com.sceyt.sceytchatuikit.shared.mediaencoder.TranscodeResultEnum.Start
import com.sceyt.sceytchatuikit.shared.mediaencoder.TranscodeResultEnum.Success
import com.sceyt.sceytchatuikit.shared.mediaencoder.VideoTranscodeData
import com.sceyt.sceytchatuikit.shared.mediaencoder.VideoTranscodeHelper
import java.io.File
import java.util.UUID


fun resizeImage(context: Context, path: String?, reqSize: Int = 600): Result<String> {
    return try {
        path?.let {
            val resizedImageFile = FileResizeUtil.resizeAndCompressImage(context, it, reqSize)
            if (resizedImageFile == null) {
                Result.failure(Exception("Could not resize image"))
            } else Result.success(resizedImageFile.path)
        } ?: Result.failure(Exception("Wrong file path"))
    } catch (ex: Exception) {
        Log.e("ImageResize", ex.message.toString())
        Result.failure(ex)
    }
}

suspend fun Attachment.transcodeVideo(context: Context, quality: VideoQuality): Attachment {
    var transcodeAttachment = this
    val dest = File(context.cacheDir.toString() + UUID.randomUUID())
    val result = VideoTranscodeHelper.transcodeAsResult(destination = dest, path = url, quality)
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

fun transcodeVideo(context: Context, path: String?, quality: VideoQuality = VideoQuality.MEDIUM,
                   progressCallback: ((VideoTranscodeData) -> Unit)? = null,
                   callback: (Result<String>) -> Unit) {
    path?.let {
        val dest = File("${context.filesDir}/" + UUID.randomUUID() + getMimeTypeTakeExtension(path))
        VideoTranscodeHelper.transcodeAsResultWithCallback(destination = dest, path = it, quality) { data ->
            when (data.resultType) {
                Cancelled -> callback(Result.failure(Exception("Canceled")))
                Failure -> callback(Result.failure(Exception(data.errorMessage)))
                Success -> callback(Result.success(dest.path))
                Progress -> progressCallback?.invoke(data)
                Start -> progressCallback?.invoke(data)
            }
        }
    } ?: run { callback(Result.failure(Exception("Wrong file path"))) }
}