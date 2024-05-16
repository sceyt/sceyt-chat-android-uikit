package com.sceyt.chatuikit.data.models.messages

import com.sceyt.chatuikit.persistence.filetransfer.TransferState

data class AttachmentPayLoadData(
        val messageTid: Long,
        val transferState: TransferState?,
        val progressPercent: Float?,
        val url: String?,
        val filePath: String?
)