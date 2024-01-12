package com.sceyt.sceytchatuikit.persistence

import com.sceyt.sceytchatuikit.data.models.LoadKeyData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentWithUserData
import com.sceyt.sceytchatuikit.data.models.messages.FileChecksumData
import com.sceyt.sceytchatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.persistence.entity.messages.AttachmentPayLoadDb
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import kotlinx.coroutines.flow.Flow

interface PersistenceAttachmentsMiddleWare {
    suspend fun getAllPayLoadsByMsgTid(tid: Long): List<AttachmentPayLoadDb>
    suspend fun getPrevAttachments(conversationId: Long, lastAttachmentId: Long, types: List<String>,
                                   offset: Int, ignoreDb: Boolean = false, loadKeyData: LoadKeyData = LoadKeyData()): Flow<PaginationResponse<AttachmentWithUserData>>

    suspend fun getNextAttachments(conversationId: Long, lastAttachmentId: Long, types: List<String>,
                                   offset: Int, ignoreDb: Boolean = false, loadKeyData: LoadKeyData = LoadKeyData()): Flow<PaginationResponse<AttachmentWithUserData>>

    suspend fun getNearAttachments(conversationId: Long, attachmentId: Long, types: List<String>,
                                   offset: Int, ignoreDb: Boolean = false, loadKeyData: LoadKeyData = LoadKeyData()): Flow<PaginationResponse<AttachmentWithUserData>>

    suspend fun updateAttachmentIdAndMessageId(message: SceytMessage)
    suspend fun updateTransferDataByMsgTid(data: TransferData)
    suspend fun updateAttachmentWithTransferData(data: TransferData)
    suspend fun updateAttachmentFilePathAndMetadata(messageTid: Long, newPath: String, fileSize: Long, metadata: String?)
    suspend fun getFileChecksumData(filePath: String?): FileChecksumData?
    suspend fun getLinkPreviewData(link: String?): SceytResponse<LinkPreviewDetails>
    suspend fun upsertLinkPreviewData(linkDetails: LinkPreviewDetails)
}