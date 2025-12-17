package com.sceyt.chatuikit.persistence.logicimpl.attachment

import com.sceyt.chatuikit.data.models.messages.AttachmentPayLoadData
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.persistence.differs.diffBetweenServerData
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AttachmentsCache {
    private var cachedAttachments = hashMapOf<String, HashMap<Long, SceytAttachment>>()
    private val mutex = Mutex()

    companion object {
        private val attachmentUpdatedFlow_ = MutableSharedFlow<List<SceytAttachment>>(
            extraBufferCapacity = 30,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val attachmentUpdatedFlow: SharedFlow<List<SceytAttachment>> = attachmentUpdatedFlow_
    }

    /** Added attachments like upsert, and check is differences between attachments*/
    suspend fun addAll(list: List<SceytAttachment>, checkDifference: Boolean): Boolean {
        mutex.withLock {
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

    suspend fun add(attachment: SceytAttachment) {
        mutex.withLock {
            val exist = cachedAttachments[attachment.type]?.get(attachment.messageTid) != null
            putToCache(attachment)
            if (exist)
                emitAttachmentUpdated(attachment)
        }
    }

    suspend fun get(type: String, messageTid: Long): SceytAttachment? {
        mutex.withLock {
            return getImpl(type, messageTid)
        }
    }

    suspend fun clear(types: List<String>) {
        mutex.withLock {
            types.forEach { type ->
                cachedAttachments.remove(type)
            }
        }
    }

    suspend fun getSorted(types: List<String>, desc: Boolean = true): List<SceytAttachment> {
        mutex.withLock {
            val filteredAttachments = cachedAttachments
                .filterKeys { it in types }
                .flatMap { it.value.values }

            val sortedAttachments = if (desc) {
                filteredAttachments.sortedByDescending { it.id }
            } else {
                filteredAttachments.sortedBy { it.id }
            }

            return sortedAttachments
        }
    }

    suspend fun deleteAttachment(messageTid: Long) {
        mutex.withLock {
            cachedAttachments.values.forEach {
                it.remove(messageTid)
            }
        }
    }

    suspend fun upsertAttachments(vararg attachments: SceytAttachment) {
        mutex.withLock {
            attachments.forEach {
                if (putAndCheckHasDiff(false, it))
                    emitAttachmentUpdated(it)
            }
        }
    }

    suspend fun updateAttachmentTransferData(updateDate: TransferData) {
        mutex.withLock {
            fun update(attachment: SceytAttachment): SceytAttachment {
                return attachment.copy(
                    transferState = updateDate.state,
                    progressPercent = updateDate.progressPercent,
                    filePath = updateDate.filePath,
                    url = updateDate.url
                )
            }

            cachedAttachments.values.forEach {
                it[updateDate.messageTid]?.let { attachment ->
                    if (attachment.type == AttachmentTypeEnum.Link.value)
                        return@withLock

                    when (updateDate.state) {
                        TransferState.PendingUpload, TransferState.Uploading, TransferState.Uploaded,
                        TransferState.ErrorUpload, TransferState.PauseUpload, TransferState.Preparing,
                        TransferState.WaitingToUpload ->
                            if (attachment.filePath == updateDate.filePath) {
                                it[updateDate.messageTid] = update(attachment)
                            }

                        TransferState.Downloading, TransferState.Downloaded, TransferState.PendingDownload,
                        TransferState.ErrorDownload, TransferState.PauseDownload -> {
                            if (attachment.url == updateDate.url)
                                it[updateDate.messageTid] = update(attachment)
                        }

                        TransferState.FilePathChanged, TransferState.ThumbLoaded -> return@withLock
                    }
                }
            }
        }
    }

    suspend fun updateAttachmentLinkDetails(data: LinkPreviewDetails) {
        mutex.withLock {
            val map = cachedAttachments[AttachmentTypeEnum.Link.value] ?: return@withLock
            map.entries.forEach { (key, attachment) ->
                if (attachment.url == data.link)
                    map[key] = attachment.copy(linkPreviewDetails = data)
            }
        }
    }

    suspend fun updateLinkDetailsSize(link: String, width: Int, height: Int) {
        mutex.withLock {
            cachedAttachments[AttachmentTypeEnum.Link.value]?.entries?.forEach { entry ->
                val (_, attachment) = entry
                if (attachment.url == link) {
                    val linkPreviewDetails = attachment.linkPreviewDetails?.copy(
                        imageWidth = width,
                        imageHeight = height
                    )
                    entry.setValue(attachment.copy(linkPreviewDetails = linkPreviewDetails))
                }
            }
        }
    }

    suspend fun updateThumb(link: String, thumb: String) {
        mutex.withLock {
            cachedAttachments[AttachmentTypeEnum.Link.value]?.entries?.forEach { entry ->
                val (_, attachment) = entry
                if (attachment.url == link) {
                    val linkPreviewDetails = attachment.linkPreviewDetails?.copy(thumb = thumb)
                    entry.setValue(attachment.copy(linkPreviewDetails = linkPreviewDetails))
                }
            }
        }
    }

    private fun getImpl(type: String, messageTid: Long): SceytAttachment? {
        return cachedAttachments[type]?.get(messageTid)
    }

    /** Set attachment data from cash, which saved in local db.*/
    private fun initMessagePayLoads(attachmentToUpdate: SceytAttachment): SceytAttachment {
        return setPayloads(attachmentToUpdate)
    }

    private fun setPayloads(attachmentToUpdate: SceytAttachment): SceytAttachment {
        val cashedAttachment = getImpl(attachmentToUpdate.type, attachmentToUpdate.messageTid)
                ?: return attachmentToUpdate
        val attachmentPayLoadData = getAttachmentPayLoads(cashedAttachment)
        val linkPreviewDetails = cashedAttachment.linkPreviewDetails
        return updateAttachmentsPayLoads(attachmentPayLoadData, attachmentToUpdate, linkPreviewDetails)
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

    private fun updateAttachmentsPayLoads(data: AttachmentPayLoadData?,
                                          attachment: SceytAttachment,
                                          linkPreviewDetails: LinkPreviewDetails?): SceytAttachment {
        if (attachment.type == AttachmentTypeEnum.Link.value || data == null) return attachment
        return attachment.copy(
            transferState = data.transferState,
            progressPercent = data.progressPercent,
            filePath = data.filePath,
            url = data.url,
            linkPreviewDetails = linkPreviewDetails
        )
    }

    private fun emitAttachmentUpdated(vararg message: SceytAttachment) {
        attachmentUpdatedFlow_.tryEmit(message.toList())
    }

    private fun putAndCheckHasDiff(includeNotExistToDiff: Boolean, vararg attachments: SceytAttachment): Boolean {
        var detectedDiff = false
        attachments.forEach {
            val attachment = initMessagePayLoads(it)
            if (!detectedDiff) {
                val old = cachedAttachments[attachment.type]?.get(attachment.messageTid)
                detectedDiff = old?.diffBetweenServerData(attachment)?.hasDifference()
                        ?: includeNotExistToDiff
            }
            cachedAttachments[attachment.type]?.let { map ->
                map[attachment.messageTid] = attachment
            } ?: run {
                cachedAttachments[attachment.type] = hashMapOf(attachment.messageTid to attachment)
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
