package com.sceyt.sceytchatuikit.persistence.filetransfer

data class TransferData(
        val messageTid: Long,
        val progressPercent: Float,
        var state: TransferState,
        var filePath: String?,
        val url: String?,
        val thumbData: ThumbData? = null,
) {
    override fun toString(): String {
        return "messageTid $messageTid, progressPercent $progressPercent, state $state, filePath $filePath, url$url thumbData $thumbData"
    }

    fun isTransferring() = state == TransferState.Downloading || state == TransferState.Uploading
}
