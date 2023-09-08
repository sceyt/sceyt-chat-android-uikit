package com.sceyt.sceytchatuikit.persistence.filetransfer

data class TransferData(
        val messageTid: Long,
        val progressPercent: Float,
        var state: TransferState,
        var filePath: String?,
        val url: String?,
        val thumbData: ThumbData? = null
) {
    var fileLoadedSize: String? = null
    var fileTotalSize: String? = null

    override fun toString(): String {
        return "messageTid $messageTid, progressPercent $progressPercent, state $state, filePath $filePath," +
                " url$url thumbData $thumbData fileLoadedSize $fileLoadedSize fileTotalSize $fileTotalSize"
    }

    fun isCalculatedLoadedSize() = !fileLoadedSize.isNullOrBlank() && !fileTotalSize.isNullOrBlank()

    fun isTransferring() = state == TransferState.Downloading || state == TransferState.Uploading || state == TransferState.Preparing

    companion object {

        fun TransferData.withPrettySizes(fileSize: Long): TransferData {
            FileTransferHelper.getFilePrettySizes(fileSize, progressPercent).run {
                fileLoadedSize = first
                fileTotalSize = second
            }
            return this
        }
    }
}
