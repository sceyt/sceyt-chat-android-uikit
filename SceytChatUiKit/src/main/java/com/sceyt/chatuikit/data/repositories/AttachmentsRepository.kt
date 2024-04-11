package com.sceyt.chatuikit.data.repositories

import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.link.LinkDetails
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.data.models.SceytResponse

interface AttachmentsRepository {
    suspend fun getPrevAttachments(conversationId: Long, lastAttachmentId: Long, types: List<String>): SceytResponse<Pair<List<Attachment>, Map<String, User>>>
    suspend fun getNextAttachments(conversationId: Long, lastAttachmentId: Long, types: List<String>): SceytResponse<Pair<List<Attachment>, Map<String, User>>>
    suspend fun getNearAttachments(conversationId: Long, attachmentId: Long, types: List<String>): SceytResponse<Pair<List<Attachment>, Map<String, User>>>
    suspend fun getLinkPreviewData(link: String): SceytResponse<LinkDetails>
}