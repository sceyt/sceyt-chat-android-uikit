package com.sceyt.chatuikit.data.models.messages

import android.os.Parcelable
import com.sceyt.chatuikit.persistence.extensions.equalsIgnoreNull
import com.sceyt.chatuikit.persistence.filetransfer.TransferData
import com.sceyt.chatuikit.persistence.filetransfer.TransferState
import kotlinx.parcelize.Parcelize

@Parcelize
data class SceytAttachment(
        val id: Long?,
        val messageId: Long,
        val messageTid: Long,
        val userId: String?,
        val name: String,
        val type: String,
        val metadata: String?,
        val fileSize: Long,
        val createdAt: Long,
        val url: String?,
        val filePath: String?,
        val transferState: TransferState?,
        val progressPercent: Float?,
        val originalFilePath: String?,
        val linkPreviewDetails: LinkPreviewDetails?
) : Parcelable, Cloneable {

    public override fun clone(): SceytAttachment {
        return SceytAttachment(
            id = id,
            messageId = messageId,
            messageTid = messageTid,
            userId = userId,
            name = name,
            type = type,
            createdAt = createdAt,
            metadata = metadata,
            fileSize = fileSize,
            url = url,
            filePath = filePath,
            transferState = transferState,
            progressPercent = progressPercent,
            originalFilePath = originalFilePath,
            linkPreviewDetails = linkPreviewDetails?.copy())
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

    fun getUpdatedWithTransferData(data: TransferData): SceytAttachment {
        return copy(
            transferState = data.state,
            progressPercent = data.progressPercent,
            url = data.url,
            filePath = data.filePath
        )
    }
}
