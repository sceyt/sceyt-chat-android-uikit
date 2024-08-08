package com.sceyt.chatuikit.persistence.filetransfer

data class TransferData(
        val messageTid: Long,
        val progressPercent: Float,
        var state: TransferState,
        var filePath: String?,
        val url: String?,
        val thumbData: ThumbData? = null,
        val fileLoadedSize: String? = null,
        val fileTotalSize: String? = null
) {
    fun isCalculatedLoadedSize() = !fileLoadedSize.isNullOrBlank() && !fileTotalSize.isNullOrBlank()

    fun isTransferring() = state == TransferState.Downloading || state == TransferState.Uploading || state == TransferState.Preparing

    companion object {

        fun TransferData.withPrettySizes(fileSize: Long): TransferData {
            FileTransferHelper.getFilePrettySizes(fileSize, progressPercent).run {
                return copy(
                    fileLoadedSize = first,
                    fileTotalSize = second)
            }
        }
    }
}
