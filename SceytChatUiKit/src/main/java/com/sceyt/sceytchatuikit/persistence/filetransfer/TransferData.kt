package com.sceyt.sceytchatuikit.persistence.filetransfer

data class TransferData(
        val messageTid: Long,
        val attachmentTid: Long,
        val progressPercent: Float,
        val state: TransferState,
        var filePath: String?,
        val url: String?
){
    override fun toString(): String {
        return "messageTid $messageTid, attachmentTid $attachmentTid, progressPercent $progressPercent, state $state, filePath $filePath, url$url"
    }
}
