package com.sceyt.sceytchatuikit.persistence.logics.messageslogic

import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.extensions.removeAllIf
import com.sceyt.sceytchatuikit.persistence.entity.messages.AttachmentPayLoadEntity
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.*
import com.sceyt.sceytchatuikit.presentation.common.diffContent
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.comporators.MessageComparator
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class MessagesCache {
    private var cachedMessages = hashMapOf<Long, SceytMessage>()
    private val lock = Any()

    companion object {
        private val messageUpdatedFlow_ = MutableSharedFlow<List<SceytMessage>>(
            extraBufferCapacity = 30,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val messageUpdatedFlow: SharedFlow<List<SceytMessage>> = messageUpdatedFlow_

        private val messagesClearedFlow_ = MutableSharedFlow<Pair<Long, Long>>(
            extraBufferCapacity = 30,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val messagesClearedFlow: SharedFlow<Pair<Long, Long>> = messagesClearedFlow_
    }

    /** Added messages like upsert, and check is differences between messages*/
    fun addAll(list: List<SceytMessage>, checkDifference: Boolean): Boolean {
        synchronized(lock) {
            return if (checkDifference)
                putAndCheckHasDiff(true, *list.toTypedArray())
            else {
                cachedMessages.putAll(list.associateBy { it.tid })
                false
            }
        }
    }

    fun add(message: SceytMessage) {
        synchronized(lock) {
            val exist = cachedMessages[message.tid] != null
            val payLoad = if (exist)
                getPayLoads(message) else null
            cachedMessages[message.tid] = message
            if (exist)
                emitMessageUpdated(payLoad?.toList(), message)
        }
    }

    fun get(tid: Long): SceytMessage? {
        synchronized(lock) {
            return cachedMessages[tid]
        }
    }

    fun clear() {
        synchronized(lock) {
            cachedMessages.clear()
        }
    }

    fun clearAllExceptPending() {
        synchronized(lock) {
            cachedMessages.values
                .filter { it.deliveryStatus != DeliveryStatus.Pending }
                .map { it.tid }.forEach {
                    cachedMessages.remove(it)
                }
        }
    }

    fun getSorted(): List<SceytMessage> {
        synchronized(lock) {
            return cachedMessages.values.sortedWith(MessageComparator()).map { it.clone() }
        }
    }

    fun messageUpdated(vararg message: SceytMessage) {
        synchronized(lock) {
            val payLoad = getPayLoads(*message)
            message.forEach {
                cachedMessages[it.tid] = it
            }
            emitMessageUpdated(payLoad, *message)
        }
    }

    fun updateMessagesStatus(status: DeliveryStatus, vararg tIds: Long) {
        synchronized(lock) {
            val updatesMessages = mutableListOf<SceytMessage>()
            tIds.forEach {
                cachedMessages[it]?.let { message ->
                    message.deliveryStatus = status
                    updatesMessages.add(message)
                }
            }
            val payLoad = getPayLoads(*updatesMessages.toTypedArray())
            emitMessageUpdated(payLoad, *updatesMessages.toTypedArray())
        }
    }

    fun deleteMessage(tid: Long) {
        synchronized(lock) {
            cachedMessages.remove(tid)
        }
    }

    fun deleteAllMessagesLowerThenDate(channelId: Long, messagesDeletionDate: Long) {
        synchronized(lock) {
            if (cachedMessages.removeAllIf { it.createdAt <= messagesDeletionDate && it.deliveryStatus != DeliveryStatus.Pending }) {
                messagesClearedFlow_.tryEmit(Pair(channelId, messagesDeletionDate))
            }
        }
    }

    fun upsertMessages(vararg message: SceytMessage) {
        synchronized(lock) {
            message.forEach {
                val payLoad = getPayLoads(it)
                if (putAndCheckHasDiff(false, it))
                    emitMessageUpdated(payLoad, it)
            }
        }
    }

    fun upsertNotifyUpdateAnyway(vararg message: SceytMessage) {
        synchronized(lock) {
            message.forEach {
                val payLoad = getPayLoads(it)
                cachedMessages[it.tid] = it
                emitMessageUpdated(payLoad, it)
            }
        }
    }

    private fun emitMessageUpdated(payLoads: List<AttachmentPayLoadEntity>?, vararg message: SceytMessage) {
        setPayloads(payLoads, message.toList())
        messageUpdatedFlow_.tryEmit(message.map { it.clone() })
    }

    private fun setPayloads(payloads: List<AttachmentPayLoadEntity>?, messages: List<SceytMessage>) {
        payloads ?: return
        messages.forEach {
            payloads.find { payLoad -> payLoad.messageTid == it.tid }?.let { entity ->
                it.attachments?.forEach { attachment ->
                    attachment.transferState = entity.transferState
                    attachment.progressPercent = entity.progressPercent
                    attachment.filePath = entity.filePath
                    attachment.url = entity.url
                }
            }
        }
    }

    private fun getPayLoads(vararg messages: SceytMessage): List<AttachmentPayLoadEntity>? {
        var payloads: List<AttachmentPayLoadEntity>? = null
        messages.forEach {
            payloads = cachedMessages[it.tid]?.attachments?.map { attachment ->
                AttachmentPayLoadEntity(
                    messageTid = it.tid,
                    transferState = attachment.transferState,
                    progressPercent = attachment.progressPercent,
                    url = attachment.url,
                    filePath = attachment.filePath
                )
            }
        }
        return payloads
    }

    private fun putAndCheckHasDiff(includeNotExistToDiff: Boolean, vararg messages: SceytMessage): Boolean {
        var detectedDiff = false
        messages.forEach {
            if (!detectedDiff) {
                val old = cachedMessages[it.tid]
                detectedDiff = old?.diffContent(it)?.hasDifference() ?: includeNotExistToDiff
            }
            cachedMessages[it.tid] = it
        }
        return detectedDiff
    }

    fun updateAttachmentTransferData(updateDate: TransferData) {
        fun update(attachment: SceytAttachment) {
            attachment.transferState = updateDate.state
            attachment.progressPercent = updateDate.progressPercent
            attachment.filePath = updateDate.filePath
            attachment.url = updateDate.url
        }
        cachedMessages[updateDate.messageTid]?.let {
            it.attachments?.forEach { attachment ->
                when (updateDate.state) {
                    PendingUpload, Uploading, Uploaded, ErrorUpload, PauseUpload -> {
                        if (attachment.messageTid == updateDate.messageTid)
                            update(attachment)
                    }
                    Downloading, Downloaded, PendingDownload, ErrorDownload, PauseDownload -> {
                        if (attachment.url == updateDate.url)
                            update(attachment)
                    }
                    FilePathChanged, ThumbLoaded -> return
                }
            }
        }
    }

    fun updateAttachmentFilePathAndMeta(messageTid: Long, path: String?, metadata: String?) {
        cachedMessages[messageTid]?.let {
            it.attachments?.forEach { attachment ->
                attachment.filePath = path
                attachment.metadata = metadata
            }
        }
    }

    //TODO: above methods will be removed soon
    fun updateAttachmentFilePath(messageTid: Long, path: String?) {
        cachedMessages[messageTid]?.let {
            it.attachments?.forEach { attachment ->
                attachment.filePath = path
            }
        }
    }
}