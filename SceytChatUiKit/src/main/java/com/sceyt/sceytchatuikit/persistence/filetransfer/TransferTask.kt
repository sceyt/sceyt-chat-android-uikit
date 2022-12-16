package com.sceyt.sceytchatuikit.persistence.filetransfer

data class TransferTask(
        val messageTid: Long,
        val state: TransferState?,
        val progressCallback: ProgressUpdateCallback,
        val resultCallback: TransferResultCallback)