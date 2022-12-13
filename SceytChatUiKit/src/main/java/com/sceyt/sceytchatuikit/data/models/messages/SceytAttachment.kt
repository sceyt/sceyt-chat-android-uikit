package com.sceyt.sceytchatuikit.data.models.messages

import android.os.Parcelable
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class SceytAttachment(
        val tid: Long,
        val messageTid: Long,
        val name: String,
        val type: String,
        val metadata: String?,
        var fileSize: Long,
        var url: String?,
        var filePath: String?
) : Parcelable, Cloneable {

    public override fun clone(): SceytAttachment {
        return SceytAttachment(tid, messageTid, name, type, metadata, fileSize, url, filePath)
    }

    @IgnoredOnParcel
    var fileTransferData: TransferData? = null

    @IgnoredOnParcel
    private var uploadProgressListener: ((TransferData) -> Unit)? = null


    internal fun setListener(listener: (TransferData) -> Unit) {
        uploadProgressListener = null
        uploadProgressListener = listener
        fileTransferData?.let { uploadProgressListener?.invoke(it) }
    }

    internal fun removeListener() {
        uploadProgressListener = null
    }

    fun update(data: TransferData) {
        fileTransferData = data
        uploadProgressListener?.invoke(data)
    }
}
