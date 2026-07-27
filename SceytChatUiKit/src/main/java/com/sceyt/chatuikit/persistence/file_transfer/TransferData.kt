package com.sceyt.chatuikit.persistence.file_transfer

import android.os.Parcelable
import com.sceyt.chatuikit.extensions.toPrettySize
import kotlinx.parcelize.Parcelize

@Parcelize
data class TransferData(
        val messageTid: Long,
        val progressPercent: Float,
        var state: TransferState,
        var filePath: String?,
        val url: String?,
        val thumbData: ThumbData? = null,
        val fileLoadedSize: String? = null,
        val fileTotalSize: String? = null
) : Parcelable {

    override fun toString(): String {
        return "progressPercent: $progressPercent, state: $state, filePath: $filePath, url: $url messageTid: $messageTid"
    }

    fun isCalculatedLoadedSize() = !fileLoadedSize.isNullOrBlank() && !fileTotalSize.isNullOrBlank()

    fun isTransferring() = state == TransferState.Downloading || state == TransferState.Uploading || state == TransferState.Preparing

    companion object {

        fun TransferData.withPrettySizes(fileSize: Long): TransferData {
            getFilePrettySizes(fileSize, progressPercent).run {
                return copy(
                    fileLoadedSize = first,
                    fileTotalSize = second)
            }
        }

        fun getFilePrettySizes(fileSize: Long, progressPercent: Float): Pair<String, String> {
            val format = if (fileSize > 99f) "%.2f" else "%.1f"
            val fileTotalSize = fileSize.toPrettySize()
            val fileLoadedSize = (fileSize * progressPercent / 100).toPrettySize(format)
            return Pair(fileLoadedSize, fileTotalSize)
        }
    }
}
