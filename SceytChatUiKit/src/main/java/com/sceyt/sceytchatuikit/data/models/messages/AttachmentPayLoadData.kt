package com.sceyt.sceytchatuikit.data.models.messages

import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState

data class AttachmentPayLoadData(
        val messageTid: Long,
        val transferState: TransferState?,
        val progressPercent: Float?,
        val url: String?,
        val filePath: String?
)