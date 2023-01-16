package com.sceyt.sceytchatuikit.data.repositories

import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.attachment.AttachmentListQuery
import com.sceyt.chat.models.user.User
import com.sceyt.chat.sceyt_callbacks.AttachmentsCallback
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.persistence.extensions.safeResume
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import kotlinx.coroutines.suspendCancellableCoroutine

class AttachmentsRepositoryImpl : AttachmentsRepository {

    private fun getQuery(conversationId: Long, types: List<String>) = AttachmentListQuery.Builder(conversationId)
        .setLimit(SceytKitConfig.ATTACHMENTS_LOAD_SIZE)
        .withTypes(types)
        .build()


    override suspend fun getPrevAttachments(conversationId: Long, lastAttachmentId: Long, types: List<String>): SceytResponse<Pair<List<Attachment>, Map<String, User>>> {
        return suspendCancellableCoroutine { continuation ->
            getQuery(conversationId, types).loadPrev(lastAttachmentId, object : AttachmentsCallback {
                override fun onResult(attachments: MutableList<Attachment>?, userMutableMap: MutableMap<String, User>?) {
                    continuation.safeResume(SceytResponse.Success(Pair(attachments
                            ?: listOf(), userMutableMap ?: mapOf())))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                }
            })
        }
    }

    override suspend fun getNextAttachments(conversationId: Long, lastAttachmentId: Long, types: List<String>): SceytResponse<Pair<List<Attachment>, Map<String, User>>> {
        return suspendCancellableCoroutine { continuation ->
            getQuery(conversationId, types).loadNext(lastAttachmentId, object : AttachmentsCallback {
                override fun onResult(attachments: MutableList<Attachment>, userMutableMap: MutableMap<String, User>) {
                    continuation.safeResume(SceytResponse.Success(Pair(attachments, userMutableMap)))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                }
            })
        }
    }

    override suspend fun getNearAttachments(conversationId: Long, attachmentId: Long, types: List<String>): SceytResponse<Pair<List<Attachment>, Map<String, User>>> {
        return suspendCancellableCoroutine { continuation ->
            getQuery(conversationId, types).loadNear(attachmentId, object : AttachmentsCallback {
                override fun onResult(attachments: MutableList<Attachment>, userMutableMap: MutableMap<String, User>) {
                    continuation.safeResume(SceytResponse.Success(Pair(attachments, userMutableMap)))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                }
            })
        }
    }
}