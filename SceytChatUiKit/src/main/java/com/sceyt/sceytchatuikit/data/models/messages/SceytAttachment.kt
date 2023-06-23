package com.sceyt.sceytchatuikit.data.models.messages

import android.os.Parcelable
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import kotlinx.parcelize.Parcelize

@Parcelize
data class SceytAttachment(
        val id: Long?,
        val messageId: Long,
        val messageTid: Long,
        var userId: String?,
        val name: String,
        val type: String,
        var metadata: String?,
        var fileSize: Long,
        val createdAt: Long,
        var url: String?,
        var filePath: String?,
        var transferState: TransferState?,
        var progressPercent: Float?

) : Parcelable, Cloneable {

    public override fun clone(): SceytAttachment {
        return SceytAttachment(id, messageId, messageTid, userId, name, type, metadata, fileSize,
            createdAt, url, filePath, transferState, progressPercent)
    }

    fun updateWithTransferData(data: TransferData) {
        transferState = data.state
        progressPercent = data.progressPercent
        url = data.url
        filePath = data.filePath
    }
}
