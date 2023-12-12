package com.sceyt.sceytchatuikit.persistence.logics.messageslogic

import com.sceyt.sceytchatuikit.data.models.messages.AttachmentPayLoadData
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
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
            initMessagePayLoads(attachment)
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
                cachedAttachments.values.sortedByDescending { it.id }
            else cachedAttachments.values.sortedBy { it.id }
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

    fun updateAttachmentTransferData(updateDate: TransferData) {
        synchronized(lock) {
            fun update(attachment: SceytAttachment) {
                attachment.transferState = updateDate.state
                attachment.progressPercent = updateDate.progressPercent
                attachment.filePath = updateDate.filePath
                attachment.url = updateDate.url
            }

            cachedAttachments[updateDate.messageTid]?.let { attachment ->
                if (attachment.type == AttachmentTypeEnum.Link.value())
                    return

                when (updateDate.state) {
                    TransferState.PendingUpload, TransferState.Uploading, TransferState.Uploaded, TransferState.ErrorUpload, TransferState.PauseUpload, TransferState.Preparing, TransferState.WaitingToUpload -> {
                        if (attachment.filePath == updateDate.filePath)
                            update(attachment)
                    }

                    TransferState.Downloading, TransferState.Downloaded, TransferState.PendingDownload, TransferState.ErrorDownload, TransferState.PauseDownload -> {
                        if (attachment.url == updateDate.url)
                            update(attachment)
                    }

                    TransferState.FilePathChanged, TransferState.ThumbLoaded -> return
                }
            }
        }
    }

    /** Set attachment data from cash, which saved in local db.*/
    private fun initMessagePayLoads(attachmentToUpdate: SceytAttachment) {
        setPayloads(attachmentToUpdate)
    }

    private fun setPayloads(attachmentToUpdate: SceytAttachment) {
        val cashedMessage = get(attachmentToUpdate.messageTid) ?: return
        val attachmentPayLoadData = getAttachmentPayLoads(cashedMessage)
        updateAttachmentsPayLoads(attachmentPayLoadData, attachmentToUpdate)
    }

    private fun getAttachmentPayLoads(cashedAttachment: SceytAttachment?): AttachmentPayLoadData? {
        cashedAttachment ?: return null
        return AttachmentPayLoadData(
            messageTid = cashedAttachment.messageTid,
            transferState = cashedAttachment.transferState,
            progressPercent = cashedAttachment.progressPercent,
            url = cashedAttachment.url,
            filePath = cashedAttachment.filePath
        )
    }

    private fun updateAttachmentsPayLoads(data: AttachmentPayLoadData?, attachment: SceytAttachment) {
        if (attachment.type == AttachmentTypeEnum.Link.value() || data == null) return
        attachment.transferState = data.transferState
        attachment.progressPercent = data.progressPercent
        attachment.filePath = data.filePath
        attachment.url = data.url
    }

    private fun emitAttachmentUpdated(vararg message: SceytAttachment) {
        attachmentUpdatedFlow_.tryEmit(message.map { it.clone() })
    }

    private fun putAndCheckHasDiff(includeNotExistToDiff: Boolean, vararg messages: SceytAttachment): Boolean {
        var detectedDiff = false
        messages.forEach {
            initMessagePayLoads(it)
            if (!detectedDiff) {
                val old = cachedAttachments[it.messageTid]
                detectedDiff = old?.diffBetweenServerData(it)?.hasDifference()
                        ?: includeNotExistToDiff
            }
            cachedAttachments[it.messageTid] = it
        }
        return detectedDiff
    }
}
