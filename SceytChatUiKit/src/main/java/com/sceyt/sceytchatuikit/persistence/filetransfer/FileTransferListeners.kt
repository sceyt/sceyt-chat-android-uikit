package com.sceyt.sceytchatuikit.persistence.filetransfer

import com.sceyt.sceytchatuikit.data.models.messages.AttachmentTypeEnum


sealed interface FileTransferListeners {

    fun interface UploadListener : FileTransferListeners {
        fun upload(messageTid: Long,
                   attachmentTid: Long, path: String,
                   type: AttachmentTypeEnum,
                   progressCallback: ProgressUpdateCallback,
                   resultCallback: UploadResult)
    }

    fun interface DownloadListener : FileTransferListeners {
        fun download(tid: Long, path: String)
    }

    /** Use this if you want to implement all callbacks */
    interface Listeners : UploadListener, DownloadListener
}