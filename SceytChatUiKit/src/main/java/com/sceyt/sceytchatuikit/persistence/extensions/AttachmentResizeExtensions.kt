package com.sceyt.sceytchatuikit.persistence.extensions

import android.app.Application
import android.util.Log
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.sceytchatuikit.shared.utils.FileResizeUtil
import com.sceyt.sceytchatuikit.shared.utils.TranscodeResultEnum
import com.sceyt.sceytchatuikit.shared.utils.VideoTranscodeHelper
import java.io.File

fun Attachment.resizeImage(application: Application): Attachment {
    var resizedAttachment = this
    try {
        val resizedImageFile = FileResizeUtil.resizeAndCompressImage(application,
            url, System.currentTimeMillis().toString(), reqSize = 600)
        resizedAttachment = Attachment.Builder(resizedImageFile.path, type)
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

suspend fun Attachment.transcodeVideo(application: Application): Attachment {
    var transcodeAttachment = this
    val dest = File(application.cacheDir.toString() + System.currentTimeMillis().toString())
    val result = VideoTranscodeHelper.transcodeAsResult(application, destination = dest, uri = url)
    if (result.resultType == TranscodeResultEnum.Success) {
        transcodeAttachment = Attachment.Builder(dest.path, type)
            .withTid(tid)
            .setName(name)
            .setMetadata(metadata)
            .setUpload(true)
            .build()
    }

    return transcodeAttachment
}