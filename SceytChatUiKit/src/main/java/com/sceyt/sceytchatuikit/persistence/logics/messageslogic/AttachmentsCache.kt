package com.sceyt.sceytchatuikit.persistence.logics.messageslogic

import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.presentation.common.diffBetweenServerData
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class AttachmentsCache {
    private var cachedAttachments = hashMapOf<Long, SceytAttachment>()
    private val lock = Any()

    companion object {
        private val attachmentUpdatedFlow_ = MutableSharedFlow<List<SceytAttachment>>(
            extraBufferCapacity = 30,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val attachmentUpdatedFlow: SharedFlow<List<SceytAttachment>> = attachmentUpdatedFlow_
    }

    /** Added attachments like upsert, and check is differences between attachments*/
    fun addAll(list: List<SceytAttachment>, checkDifference: Boolean): Boolean {
        synchronized(lock) {
            return if (checkDifference)
                putAndCheckHasDiff(true, *list.toTypedArray())
            else {
                cachedAttachments.putAll(list.associateBy { it.messageTid })
                false
            }
        }
    }

    fun add(attachment: SceytAttachment) {
        synchronized(lock) {
            val exist = cachedAttachments[attachment.messageTid] != null
            cachedAttachments[attachment.messageTid] = attachment
            if (exist)
                emitAttachmentUpdated(attachment)
        }
    }

    fun get(messageTid: Long): SceytAttachment? {
        synchronized(lock) {
            return cachedAttachments[messageTid]
        }
    }

    fun clear() {
        synchronized(lock) {
            cachedAttachments.clear()
        }
    }

    fun getSorted(desc: Boolean = true): List<SceytAttachment> {
        synchronized(lock) {
            val data = if (desc)
                cachedAttachments.values.sortedByDescending { it.createdAt }
            else cachedAttachments.values.sortedBy { it.createdAt }
            return data.map { it.clone() }
        }
    }

    fun messageUpdated(vararg attachments: SceytAttachment) {
        synchronized(lock) {
            attachments.forEach {
                cachedAttachments[it.messageTid] = it
            }
            emitAttachmentUpdated(*attachments)
        }
    }


    fun deleteAttachment(messageTid: Long) {
        synchronized(lock) {
            cachedAttachments.remove(messageTid)
        }
    }

    fun upsertAttachments(vararg attachments: SceytAttachment) {
        synchronized(lock) {
            attachments.forEach {
                if (putAndCheckHasDiff(false, it))
                    emitAttachmentUpdated(it)
            }
        }
    }

    private fun emitAttachmentUpdated(vararg message: SceytAttachment) {
        attachmentUpdatedFlow_.tryEmit(message.map { it.clone() })
    }

    private fun putAndCheckHasDiff(includeNotExistToDiff: Boolean, vararg messages: SceytAttachment): Boolean {
        var detectedDiff = false
        messages.forEach {
            if (!detectedDiff) {
                val old = cachedAttachments[it.messageTid]
                detectedDiff = old?.diffBetweenServerData(it)?.hasDifference() ?: includeNotExistToDiff
            }
            cachedAttachments[it.messageTid] = it
        }
        return detectedDiff
    }
}
