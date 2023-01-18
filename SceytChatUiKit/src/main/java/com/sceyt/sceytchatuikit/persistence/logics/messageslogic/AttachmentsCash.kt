package com.sceyt.sceytchatuikit.persistence.logics.messageslogic

import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.presentation.common.diff
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class AttachmentsCash {
    private var cashedAttachments = hashMapOf<Long, SceytAttachment>()
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
                cashedAttachments.putAll(list.associateBy { it.messageTid })
                false
            }
        }
    }

    fun add(attachment: SceytAttachment) {
        synchronized(lock) {
            val exist = cashedAttachments[attachment.tid] != null
            cashedAttachments[attachment.tid] = attachment
            if (exist)
                emitAttachmentUpdated(attachment)
        }
    }

    fun get(messageTid: Long): SceytAttachment? {
        synchronized(lock) {
            return cashedAttachments[messageTid]
        }
    }

    fun clear() {
        synchronized(lock) {
            cashedAttachments.clear()
        }
    }

    fun getSorted(desc: Boolean = true): List<SceytAttachment> {
        synchronized(lock) {
            val data = if (desc)
                cashedAttachments.values.sortedByDescending { it.createdAt }
            else cashedAttachments.values.sortedBy { it.createdAt }
            return data.map { it.clone() }
        }
    }

    fun messageUpdated(vararg attachments: SceytAttachment) {
        synchronized(lock) {
            attachments.forEach {
                cashedAttachments[it.messageTid] = it
            }
            emitAttachmentUpdated(*attachments)
        }
    }


    fun deleteAttachment(messageTid: Long) {
        synchronized(lock) {
            cashedAttachments.remove(messageTid)
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
                val old = cashedAttachments[it.messageTid]
                detectedDiff = old?.diff(it)?.hasDifference() ?: includeNotExistToDiff
            }
            cashedAttachments[it.messageTid] = it
        }
        return detectedDiff
    }
}
