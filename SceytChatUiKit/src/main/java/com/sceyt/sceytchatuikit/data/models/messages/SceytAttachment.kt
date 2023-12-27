package com.sceyt.sceytchatuikit.data.models.messages

import android.os.Parcelable
import com.sceyt.sceytchatuikit.persistence.extensions.equalsIgnoreNull
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
        var progressPercent: Float?,
        var originalFilePath: String?,
        var linkPreviewDetails: LinkPreviewDetails?
) : Parcelable, Cloneable {

    public override fun clone(): SceytAttachment {
        return SceytAttachment(id, messageId, messageTid, userId, name, type, metadata, fileSize,
            createdAt, url, filePath, transferState, progressPercent, originalFilePath, linkPreviewDetails)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is SceytAttachment) return false
        return id == other.id && messageId == other.messageId && messageTid == other.messageTid
                && userId == other.userId && name.equalsIgnoreNull(other.name) && type == other.type
                && metadata.equalsIgnoreNull(other.metadata) && fileSize == other.fileSize
                && createdAt == other.createdAt && url.equalsIgnoreNull(other.url) && filePath.equalsIgnoreNull(other.filePath)
                && transferState == other.transferState && progressPercent == other.progressPercent
                && originalFilePath.equalsIgnoreNull(other.originalFilePath)
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    fun updateWithTransferData(data: TransferData) {
        transferState = data.state
        progressPercent = data.progressPercent
        url = data.url
        filePath = data.filePath
    }
}
