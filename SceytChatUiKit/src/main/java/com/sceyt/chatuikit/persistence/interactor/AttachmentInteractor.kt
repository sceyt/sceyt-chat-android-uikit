package com.sceyt.chatuikit.persistence.interactor

import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.AttachmentWithUserData
import com.sceyt.chatuikit.data.models.messages.FileChecksumData
import com.sceyt.chatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import kotlinx.coroutines.flow.Flow

interface AttachmentInteractor {
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