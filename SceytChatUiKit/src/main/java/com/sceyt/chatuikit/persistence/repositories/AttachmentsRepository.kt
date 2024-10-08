package com.sceyt.chatuikit.persistence.repositories

import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.link.LinkDetails
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytUser

interface AttachmentsRepository {
    suspend fun getPrevAttachments(
            conversationId: Long, lastAttachmentId: Long, types: List<String>
    ): SceytResponse<Pair<List<Attachment>, Map<String, SceytUser>>>

    suspend fun getNextAttachments(
            conversationId: Long, lastAttachmentId: Long, types: List<String>
    ): SceytResponse<Pair<List<Attachment>, Map<String, SceytUser>>>

    suspend fun getNearAttachments(
            conversationId: Long, attachmentId: Long, types: List<String>
    ): SceytResponse<Pair<List<Attachment>, Map<String, SceytUser>>>

    suspend fun getLinkPreviewData(link: String): SceytResponse<LinkDetails>
}