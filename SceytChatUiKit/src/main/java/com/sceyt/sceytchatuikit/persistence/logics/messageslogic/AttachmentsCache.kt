package com.sceyt.sceytchatuikit.persistence.logics.messageslogic

import com.sceyt.sceytchatuikit.data.models.messages.AttachmentPayLoadData
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import com.sceyt.sceytchatuikit.presentation.common.diffBetweenServerData
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class AttachmentsCache {
    @Volatile
    private var cachedAttachments = hashMapOf<String, HashMap<Long, SceytAttachment>>()
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
                list.groupBy { it.type }.forEach {
                    val map = cachedAttachments[it.key] ?: hashMapOf()
                    map.putAll(it.value.associateBy { attachment -> attachment.messageTid })
                    cachedAttachments[it.key] = map
                }
                false
            }
        }
    }

    fun add(attachment: SceytAttachment) {
        synchronized(lock) {
            val exist = cachedAttachments[attachment.type]?.get(attachment.messageTid) != null
            putToCache(attachment)
            if (exist)
                emitAttachmentUpdated(attachment)
        }
    }

    fun get(type: String, messageTid: Long): SceytAttachment? {
        synchronized(lock) {
            return cachedAttachments[type]?.get(messageTid)?.clone()
        }
    }

    fun clear(types: List<String>) {
        synchronized(lock) {
            types.forEach { type ->
                cachedAttachments.remove(type)
            }
        }
    }

    fun getSorted(types: List<String>, desc: Boolean = true): List<SceytAttachment> {
        synchronized(lock) {
            val filteredAttachments = cachedAttachments
                .filterKeys { it in types }
                .flatMap { it.value.values }

            val sortedAttachments = if (desc) {
                filteredAttachments.sortedByDescending { it.id }
            } else {
                filteredAttachments.sortedBy { it.id }
            }

            return sortedAttachments.map { it.clone() }
        }
    }

    fun deleteAttachment(messageTid: Long) {
        synchronized(lock) {
            cachedAttachments.values.forEach {
                it.remove(messageTid)
            }
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

            cachedAttachments.values.forEach {
                it[updateDate.messageTid]?.let { attachment ->
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
    }

    fun updateAttachmentLinkDetails(data: LinkPreviewDetails) {
        synchronized(lock) {
            cachedAttachments[AttachmentTypeEnum.Link.value()]?.values?.forEach { attachment ->
                if (attachment.url == data.link)
                    attachment.linkPreviewDetails = data
            }
        }
    }

    fun updateLinkDetailsSize(link: String, width: Int, height: Int) {
        synchronized(lock) {
            cachedAttachments[AttachmentTypeEnum.Link.value()]?.values?.forEach { attachment ->
                if (attachment.url == link) {
                    attachment.linkPreviewDetails?.imageWidth = width
                    attachment.linkPreviewDetails?.imageHeight = height
                }
            }
        }
    }

    fun updateThumb(link: String, thumb: String) {
        synchronized(lock) {
            cachedAttachments[AttachmentTypeEnum.Link.value()]?.values?.forEach { attachment ->
                if (attachment.url == link)
                    attachment.linkPreviewDetails?.thumb = thumb
            }
        }
    }

    /** Set attachment data from cash, which saved in local db.*/
    private fun initMessagePayLoads(attachmentToUpdate: SceytAttachment) {
        setPayloads(attachmentToUpdate)
    }

    private fun setPayloads(attachmentToUpdate: SceytAttachment) {
        val cashedAttachment = get(attachmentToUpdate.type, attachmentToUpdate.messageTid) ?: return
        val attachmentPayLoadData = getAttachmentPayLoads(cashedAttachment)
        attachmentToUpdate.linkPreviewDetails = cashedAttachment.linkPreviewDetails
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

    private fun putAndCheckHasDiff(includeNotExistToDiff: Boolean, vararg attachments: SceytAttachment): Boolean {
        var detectedDiff = false
        attachments.forEach {
            initMessagePayLoads(it)
            if (!detectedDiff) {
                val old = cachedAttachments[it.type]?.get(it.messageTid)
                detectedDiff = old?.diffBetweenServerData(it)?.hasDifference()
                        ?: includeNotExistToDiff
            }
            cachedAttachments[it.type]?.let { map ->
                map[it.messageTid] = it
            } ?: run {
                cachedAttachments[it.type] = hashMapOf(it.messageTid to it)
            }
        }
        return detectedDiff
    }

    private fun putToCache(attachment: SceytAttachment) {
        val map = cachedAttachments[attachment.type] ?: hashMapOf()
        map[attachment.messageTid] = attachment
        initMessagePayLoads(attachment)
        cachedAttachments[attachment.type] = map
    }
}
