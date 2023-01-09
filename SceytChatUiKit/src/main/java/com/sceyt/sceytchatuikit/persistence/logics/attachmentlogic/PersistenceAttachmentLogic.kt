package com.sceyt.sceytchatuikit.persistence.logics.attachmentlogic

import com.sceyt.sceytchatuikit.persistence.entity.messages.AttachmentPayLoadEntity
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData

interface PersistenceAttachmentLogic {
    suspend fun getAllPayLoadsByMsgTid(tid: Long): List<AttachmentPayLoadEntity>
    fun updateTransferDataByMsgTid(data: TransferData)
    fun updateAttachmentWithTransferData(data: TransferData)
    fun updateAttachmentFilePathAndMetadata(messageTid: Long, newPath: String, fileSize: Long, metadata: String?)
}