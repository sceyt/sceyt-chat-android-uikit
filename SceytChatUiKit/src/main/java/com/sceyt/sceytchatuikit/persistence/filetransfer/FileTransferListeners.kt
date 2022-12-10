package com.sceyt.sceytchatuikit.persistence.filetransfer

import com.sceyt.chat.models.attachment.Attachment


sealed interface FileTransferListeners {

    fun interface UploadListener : FileTransferListeners {
        fun upload(messageTid: Long,
                   attachment: Attachment,
                   progressCallback: ProgressUpdateCallback,
                   resultCallback: TransferResult)
    }

    fun interface DownloadListener : FileTransferListeners {
        fun download(messageTid: Long,
                     attachment: Attachment,
                     progressCallback: ProgressUpdateCallback,
                     resultCallback: TransferResult)
    }

    /** Use this if you want to implement all callbacks */
    interface Listeners : UploadListener, DownloadListener
}