package com.sceyt.sceytchatuikit.persistence.extensions

import android.content.Context
import android.util.Log
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.shared.utils.FileResizeUtil
import com.sceyt.sceytchatuikit.shared.utils.TranscodeResultEnum
import com.sceyt.sceytchatuikit.shared.utils.VideoTranscodeHelper
import java.io.File

fun Attachment.resizeImage(context: Context): Attachment {
    var resizedAttachment = this
    try {
        val resizedImageFile = FileResizeUtil.resizeAndCompressImage(context,
            url, System.currentTimeMillis().toString(), reqSize = 600)
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

fun SceytAttachment.resizeImage(context: Context): SceytAttachment {
    try {
        filePath?.let {
            val resizedImageFile = FileResizeUtil.resizeAndCompressImage(context,
                it, System.currentTimeMillis().toString(), reqSize = 600)
            filePath = resizedImageFile.path
        }
    } catch (ex: Exception) {
        Log.e("ImageResize", ex.message.toString())
    }
    return this
}

suspend fun Attachment.transcodeVideo(context: Context): Attachment {
    var transcodeAttachment = this
    val dest = File(context.cacheDir.toString() + System.currentTimeMillis().toString())
    val result = VideoTranscodeHelper.transcodeAsResult(context, destination = dest, uri = url)
    if (result.resultType == TranscodeResultEnum.Success) {
        transcodeAttachment = Attachment.Builder(dest.path, url, type)
            .withTid(tid)
            .setName(name)
            .setMetadata(metadata)
            .setUpload(true)
            .build()
    }

    return transcodeAttachment
}

suspend fun SceytAttachment.transcodeVideo(context: Context): SceytAttachment {
    val dest = File(context.cacheDir.toString() + System.currentTimeMillis().toString())
    filePath?.let {
        val result = VideoTranscodeHelper.transcodeAsResult(context, destination = dest, uri = it)
        if (result.resultType == TranscodeResultEnum.Success) {
            filePath = dest.path
        }
    }
    return this
}