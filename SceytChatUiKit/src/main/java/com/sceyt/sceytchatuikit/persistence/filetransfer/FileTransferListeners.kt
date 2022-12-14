package com.sceyt.sceytchatuikit.persistence.filetransfer

import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment


sealed interface FileTransferListeners {

    fun interface UploadListener : FileTransferListeners {
        fun upload(attachment: SceytAttachment,
                   transferTask: TransferTask)
    }

    fun interface DownloadListener : FileTransferListeners {
        fun download(
                attachment: SceytAttachment,
                transferTask: TransferTask)
    }

    fun interface OnPause : FileTransferListeners {
        fun pause(messageTid: Long,
                  attachment: SceytAttachment,
                  state: TransferState)
    }


    fun interface OnResume : FileTransferListeners {
        fun resume(messageTid: Long,
                   attachment: SceytAttachment,
                   state: TransferState)
    }

    /** Use this if you want to implement all callbacks */
    interface Listeners : UploadListener, DownloadListener, OnPause, OnResume
}