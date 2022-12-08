package com.sceyt.sceytchatuikit.data.models.messages

import android.os.Parcelable
import com.sceyt.sceytchatuikit.persistence.filetransfer.ProgressState
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
        val fileSize: Long,
        val url: String
) : Parcelable, Cloneable {

    public override fun clone(): SceytAttachment {
        return SceytAttachment(tid, messageTid, name, type, metadata, fileSize, url)
    }

    @IgnoredOnParcel
    var transferData: TransferData = TransferData(messageTid, tid, 0.02f, ProgressState.Pending,url)
}
