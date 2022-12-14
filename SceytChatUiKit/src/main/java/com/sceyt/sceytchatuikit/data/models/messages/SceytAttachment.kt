package com.sceyt.sceytchatuikit.data.models.messages

import android.os.Parcelable
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
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
        var filePath: String?,
        var transferState: TransferState?,
        var progressPercent: Float?
) : Parcelable, Cloneable {

    public override fun clone(): SceytAttachment {
        return SceytAttachment(tid, messageTid, name, type, metadata, fileSize, url,
            filePath, transferState, progressPercent)
    }

    /*@IgnoredOnParcel
    var fileTransferData: TransferData? = transferState?.let {
        TransferData(messageTid, tid, progressPercent
                ?: 0f, it, filePath, url)
    }*/
}
