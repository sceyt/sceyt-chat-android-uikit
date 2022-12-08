package com.sceyt.sceytchatuikit.persistence.filetransfer

import com.sceyt.sceytchatuikit.data.models.messages.AttachmentTypeEnum

abstract class FileTransferListenersImpl : FileTransferListeners.Listeners {
    private var uploadListener: FileTransferListeners.UploadListener? = null
    private var downloadListener: FileTransferListeners.DownloadListener? = null

    override fun upload(messageTid: Long,
                        attachmentTid: Long, path: String,
                        type: AttachmentTypeEnum,
                        progressCallback: ProgressUpdateCallback,
                        resultCallback: UploadResult) {
        uploadListener?.upload(messageTid, attachmentTid, path, type, progressCallback, resultCallback)
    }

    override fun download(tid: Long, path: String) {
        downloadListener?.download(tid, path)
    }

    fun setListener(listener: FileTransferListeners) {
        when (listener) {
            is FileTransferListeners.Listeners -> {
                uploadListener = listener
                downloadListener = listener
            }
            is FileTransferListeners.DownloadListener -> {
                downloadListener = listener
            }
            is FileTransferListeners.UploadListener -> {
                uploadListener = listener
            }
        }
    }
}