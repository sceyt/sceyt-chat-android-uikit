package com.sceyt.sceytchatuikit.persistence.filetransfer

data class TransferData(
        val messageTid: Long,
        val progressPercent: Float,
        var state: TransferState,
        var filePath: String?,
        val url: String?,
        val thumbData: ThumbData? = null,
        var fileLoadedSize: String = "",
        var fileTotalSize: String = "",
) {
    override fun toString(): String {
        return "messageTid $messageTid, progressPercent $progressPercent, state $state, filePath $filePath," +
                " url$url thumbData $thumbData fileLoadedSize $fileLoadedSize fileTotalSize $fileTotalSize"
    }

    fun isCalculatedLoadedSize() = fileLoadedSize.isNotBlank() && fileTotalSize.isNotBlank()

    fun isTransferring() = state == TransferState.Downloading || state == TransferState.Uploading
}
