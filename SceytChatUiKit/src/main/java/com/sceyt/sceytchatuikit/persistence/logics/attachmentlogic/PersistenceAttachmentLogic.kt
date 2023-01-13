package com.sceyt.sceytchatuikit.persistence.logics.attachmentlogic

import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentWithUserData
import com.sceyt.sceytchatuikit.persistence.entity.messages.AttachmentPayLoadEntity
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import kotlinx.coroutines.flow.Flow

interface PersistenceAttachmentLogic {
    suspend fun getAllPayLoadsByMsgTid(tid: Long): List<AttachmentPayLoadEntity>
    suspend fun getPrevAttachments(conversationId: Long, lastAttachmentId: Long, types: List<String>): Flow<PaginationResponse<AttachmentWithUserData>>
    suspend fun getNextAttachments(conversationId: Long, lastAttachmentId: Long, types: List<String>): Flow<PaginationResponse<AttachmentWithUserData>>
    suspend fun getNearAttachments(conversationId: Long, attachmentId: Long, types: List<String>): Flow<PaginationResponse<AttachmentWithUserData>>
    fun updateTransferDataByMsgTid(data: TransferData)
    fun updateAttachmentWithTransferData(data: TransferData)
    fun updateAttachmentFilePathAndMetadata(messageTid: Long, newPath: String, fileSize: Long, metadata: String?)
}