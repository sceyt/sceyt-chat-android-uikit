package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files

import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData

sealed class FileListItem() {
    lateinit var file: SceytAttachment
    lateinit var sceytMessage: SceytMessage

    constructor(file: SceytAttachment, sceytMessage: SceytMessage) : this() {
        this.file = file
        this.sceytMessage = sceytMessage
    }

    data class File(val attachment: SceytAttachment,
                    val message: SceytMessage) : FileListItem(attachment, message)

    data class Image(val attachment: SceytAttachment,
                     val message: SceytMessage) : FileListItem(attachment, message)

    data class Video(val attachment: SceytAttachment,
                     val message: SceytMessage) : FileListItem(attachment, message)

    object LoadingMoreItem : FileListItem()


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

