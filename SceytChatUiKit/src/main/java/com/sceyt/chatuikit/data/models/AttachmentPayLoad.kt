package com.sceyt.chatuikit.data.models

import com.sceyt.chatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.chatuikit.persistence.file_transfer.TransferState

data class AttachmentPayLoad(
        val messageTid: Long,
        val transferState: TransferState,
        val progressPercent: Float?,
        val url: String?,
        val filePath: String?,
        val linkPreviewDetails: LinkPreviewDetails?
)