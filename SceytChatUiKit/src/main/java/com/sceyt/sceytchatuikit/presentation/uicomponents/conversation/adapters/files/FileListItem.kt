package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import android.util.Size
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.extensions.decodeByteArrayToBitmap
import org.json.JSONObject

sealed class FileListItem {
    lateinit var file: SceytAttachment
    lateinit var sceytMessage: SceytMessage
    var size: Size? = null
    var thumb: Bitmap? = null

    private constructor()

    constructor(file: SceytAttachment, sceytMessage: SceytMessage) : this() {
        this.file = file
        this.sceytMessage = sceytMessage
        file.metadata.getThumbByBytesAndSize()
    }

    data class File(val attachment: SceytAttachment,
                    val message: SceytMessage) : FileListItem(attachment, message)

    data class Image(val attachment: SceytAttachment,
                     val message: SceytMessage) : FileListItem(attachment, message)

    data class Video(val attachment: SceytAttachment,
                     val message: SceytMessage) : FileListItem(attachment, message)

    object LoadingMoreItem : FileListItem()


    private fun String?.getThumbByBytesAndSize() {
        var base64Thumb: ByteArray? = null
        var size: Size? = null
        try {
            val jsonObject = JSONObject(this ?: return)
            val thumbnail = jsonObject.getString("thumbnail")
            base64Thumb = Base64.decode(thumbnail, Base64.DEFAULT)
            val width = jsonObject.getString("width").toIntOrNull()
            val height = jsonObject.getString("height").toIntOrNull()
            if (width != null && height != null)
                size = Size(width, height)
        } catch (ex: Exception) {
            Log.i("MetadataReader", "Couldn't get data from attachment metadata with reason ${ex.message}")
        }
        if (size == null && base64Thumb == null)
            return

        this@FileListItem.size = size
        this@FileListItem.thumb = base64Thumb?.decodeByteArrayToBitmap()
    }


    /*private var uploadProgressListener: ((FileLoadData) -> Unit)? = null
    private var downloadProgressListener: ((FileLoadData, java.io.File?) -> Unit)? = null

    internal fun setUploadListener(listener: ((FileLoadData) -> Unit)? = null) {
        uploadProgressListener = listener
        uploadProgressListener?.invoke(fileLoadData.apply { loading = true })
      *//*  file.setUploaderProgress(object : ProgressCallback {
            override fun onResult(progress: Float) {
                val intValuePercent = progress * 100
                if (intValuePercent < 100) {
                    fileLoadData.update(progress = max(1f, intValuePercent), loading = true, success = false)
                    uploadProgressListener?.invoke(fileLoadData)
                }
            }

            override fun onError(p0: SceytException?) {
                fileLoadData.update(progress = null, loading = false, success = false)
                uploadProgressListener?.invoke(fileLoadData)
            }
        })*//*

        *//*file.setUploaderCompletion(object : ActionCallback {
            override fun onSuccess() {
                fileLoadData.update(progress = 100f, loading = false, success = true)
                uploadProgressListener?.invoke(fileLoadData)
            }

            override fun onError(p0: SceytException?) {
                fileLoadData.update(progress = null, loading = false, success = false)
                uploadProgressListener?.invoke(fileLoadData)
            }
        })*//*
    }

    internal fun setDownloadProgressListener(listener: ((FileLoadData, java.io.File?) -> Unit)? = null) {
        downloadProgressListener = null
        downloadProgressListener = listener
    }

    internal fun updateDownloadState(progress: Float?, loading: Boolean) {
        val progressValue = if (progress != null) max(1f, progress) else null
        fileLoadData.update(progressValue, loading = loading)
        downloadProgressListener?.invoke(fileLoadData, null)
    }

    internal fun downloadFinish(result: java.io.File?, success: Boolean) {
        val progress = if (success) 100f else 0f
        fileLoadData.update(progress, loading = false, success = success)
        downloadProgressListener?.invoke(fileLoadData, result)
    }*/


}

