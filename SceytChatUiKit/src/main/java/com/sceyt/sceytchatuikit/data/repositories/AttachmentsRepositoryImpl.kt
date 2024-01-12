package com.sceyt.sceytchatuikit.data.repositories

import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.attachment.AttachmentListQuery
import com.sceyt.chat.models.link.LinkDetails
import com.sceyt.chat.models.link.LoadLinkDetailsRequest
import com.sceyt.chat.models.user.User
import com.sceyt.chat.sceyt_callbacks.AttachmentsCallback
import com.sceyt.chat.sceyt_callbacks.LinkDetailsCallback
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.logger.SceytLog
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
                    SceytLog.e(TAG, "getPrevAttachments error: ${e?.message}")
                }
            })
        }
    }

    override suspend fun getNextAttachments(conversationId: Long, lastAttachmentId: Long, types: List<String>): SceytResponse<Pair<List<Attachment>, Map<String, User>>> {
        return suspendCancellableCoroutine { continuation ->
            getQuery(conversationId, types).loadNext(lastAttachmentId, object : AttachmentsCallback {
                override fun onResult(attachments: MutableList<Attachment>?, userMutableMap: MutableMap<String, User>?) {
                    continuation.safeResume(SceytResponse.Success(Pair(attachments
                            ?: listOf(), userMutableMap ?: mapOf())))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "getNextAttachments error: ${e?.message}")
                }
            })
        }
    }

    override suspend fun getNearAttachments(conversationId: Long, attachmentId: Long, types: List<String>): SceytResponse<Pair<List<Attachment>, Map<String, User>>> {
        return suspendCancellableCoroutine { continuation ->
            getQuery(conversationId, types).loadNear(attachmentId, object : AttachmentsCallback {
                override fun onResult(attachments: MutableList<Attachment>?, userMutableMap: MutableMap<String, User>?) {
                    continuation.safeResume(SceytResponse.Success(Pair(attachments
                            ?: listOf(), userMutableMap ?: mapOf())))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "getNearAttachments error: ${e?.message}")
                }
            })
        }
    }

    override suspend fun getLinkPreviewData(link: String): SceytResponse<LinkDetails> {
        return suspendCancellableCoroutine { continuation ->
            LoadLinkDetailsRequest(link).execute(object : LinkDetailsCallback {
                override fun onResult(linkDetails: LinkDetails?) {
                    continuation.safeResume(SceytResponse.Success(linkDetails))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "getLinkPreviewData error: ${e?.message}, for link: $link")
                }
            })
        }
    }
}