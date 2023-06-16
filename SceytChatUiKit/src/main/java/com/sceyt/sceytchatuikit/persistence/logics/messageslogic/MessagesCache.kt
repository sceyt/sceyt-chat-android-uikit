package com.sceyt.sceytchatuikit.persistence.logics.messageslogic

import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.extensions.removeAllIf
import com.sceyt.sceytchatuikit.persistence.entity.messages.AttachmentPayLoadEntity
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.Downloaded
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.Downloading
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.ErrorDownload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.ErrorUpload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.FilePathChanged
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.PauseDownload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.PauseUpload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.PendingDownload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.PendingUpload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.ThumbLoaded
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.Uploaded
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.Uploading
import com.sceyt.sceytchatuikit.presentation.common.diffContent
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.comporators.MessageComparator
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class MessagesCache {
    private var cachedMessages = hashMapOf<Long, HashMap<Long, SceytMessage>>()
    private val lock = Any()

    companion object {
        private val messageUpdatedFlow_ = MutableSharedFlow<Pair<Long, List<SceytMessage>>>(
            extraBufferCapacity = 30,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val messageUpdatedFlow: SharedFlow<Pair<Long, List<SceytMessage>>> = messageUpdatedFlow_

        private val messagesClearedFlow_ = MutableSharedFlow<Pair<Long, Long>>(
            extraBufferCapacity = 30,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val messagesClearedFlow: SharedFlow<Pair<Long, Long>> = messagesClearedFlow_
    }


    /** Added messages like upsert, and check is differences between messages*/
    fun addAll(channelId: Long, list: List<SceytMessage>, checkDifference: Boolean): Boolean {
        synchronized(lock) {
            return if (checkDifference)
                putAndCheckHasDiff(channelId, true, *list.toTypedArray())
            else {
                putAllMessages(channelId, list)
                false
            }
        }
    }

    fun add(channelId: Long, message: SceytMessage) {
        synchronized(lock) {
            val exist = getMessageByTid(channelId, message.id) != null
            val payLoad = if (exist)
                getPayLoads(channelId, message) else null
            putMessage(channelId, message)
            if (exist)
                emitMessageUpdated(channelId, payLoad?.toList(), message)
        }
    }

    fun get(channelId: Long, tid: Long): SceytMessage? {
        synchronized(lock) {
            return getMessageByTid(channelId, tid)
        }
    }

    fun clear() {
        synchronized(lock) {
            cachedMessages.clear()
        }
    }

    fun clearAllExceptPending(channelId: Long) {
        synchronized(lock) {
            getMessagesMap(channelId)?.values
                ?.filter { it.deliveryStatus != DeliveryStatus.Pending }
                ?.map { it.tid }?.forEach {
                    cachedMessages.remove(it)
                    deleteMessage(channelId, it)
                }
        }
    }

    fun getSorted(channelId: Long): List<SceytMessage> {
        synchronized(lock) {
            return getMessagesMap(channelId)?.values?.sortedWith(MessageComparator())?.map { it.clone() }
                    ?: emptyList()
        }
    }

    fun messageUpdated(channelId: Long, vararg message: SceytMessage) {
        synchronized(lock) {
            val payLoad = getPayLoads(channelId, *message)
            message.forEach {
                updateMessage(channelId, it)
            }
            emitMessageUpdated(channelId, payLoad, *message)
        }
    }

    fun updateMessagesStatus(channelId: Long, status: DeliveryStatus, vararg tIds: Long) {
        synchronized(lock) {
            val updatesMessages = mutableListOf<SceytMessage>()
            tIds.forEach {
                getMessageByTid(channelId, it)?.let { message ->
                    message.deliveryStatus = status
                    updatesMessages.add(message)
                }
            }
            val payLoad = getPayLoads(channelId, *updatesMessages.toTypedArray())
            emitMessageUpdated(channelId, payLoad, *updatesMessages.toTypedArray())
        }
    }

    fun deleteMessage(channelId: Long, tid: Long) {
        synchronized(lock) {
            cachedMessages[channelId]?.remove(tid)
        }
    }

    fun deleteAllMessagesLowerThenDate(channelId: Long, messagesDeletionDate: Long) {
        synchronized(lock) {
            if (getMessagesMap(channelId)?.removeAllIf { it.createdAt <= messagesDeletionDate && it.deliveryStatus != DeliveryStatus.Pending } == true) {
                messagesClearedFlow_.tryEmit(Pair(channelId, messagesDeletionDate))
            }
        }
    }

    fun upsertMessages(channelId: Long, vararg message: SceytMessage) {
        synchronized(lock) {
            message.forEach {
                val payLoad = getPayLoads(channelId, it)
                if (putAndCheckHasDiff(channelId, false, it))
                    emitMessageUpdated(channelId, payLoad, it)
            }
        }
    }

    fun upsertNotifyUpdateAnyway(channelId: Long, vararg message: SceytMessage) {
        synchronized(lock) {
            message.forEach {
                val payLoad = getPayLoads(channelId, it)
                updateMessage(channelId, it)
                emitMessageUpdated(channelId, payLoad, it)
            }
        }
    }

    private fun emitMessageUpdated(channelId: Long, payLoads: List<AttachmentPayLoadEntity>?, vararg message: SceytMessage) {
        setPayloads(payLoads, message.toList())
        messageUpdatedFlow_.tryEmit(Pair(channelId, message.map { it.clone() }))
    }

    private fun getMessagesMap(channelId: Long): HashMap<Long, SceytMessage>? {
        return cachedMessages[channelId]
    }

    private fun putAllMessages(channelId: Long, list: List<SceytMessage>) {
        cachedMessages[channelId]?.let {
            it.putAll(list.associateBy { message -> message.tid })
        } ?: run {
            cachedMessages[channelId] = HashMap(list.associateBy { message -> message.tid })
        }
    }

    private fun putMessage(channelId: Long, message: SceytMessage) {
        cachedMessages[channelId]?.let {
            it[message.tid] = message
        } ?: run {
            cachedMessages[channelId] = hashMapOf(message.tid to message)
        }
    }

    private fun updateMessage(channelId: Long, message: SceytMessage) {
        cachedMessages[channelId]?.let {
            it[message.tid] = message
        }
    }

    private fun getMessageByTid(channelId: Long, tid: Long): SceytMessage? {
        return cachedMessages[channelId]?.let {
            it[tid]
        }
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

    private fun getPayLoads(channelId: Long, vararg messages: SceytMessage): List<AttachmentPayLoadEntity>? {
        var payloads: List<AttachmentPayLoadEntity>? = null
        messages.forEach {
            payloads = getMessageByTid(channelId, it.tid)?.attachments?.map { attachment ->
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

    private fun putAndCheckHasDiff(channelId: Long, includeNotExistToDiff: Boolean, vararg messages: SceytMessage): Boolean {
        var detectedDiff = false
        messages.forEach {
            if (!detectedDiff) {
                val old = getMessageByTid(channelId, it.tid)
                detectedDiff = old?.diffContent(it)?.hasDifference() ?: includeNotExistToDiff
            }
            updateMessage(channelId, it)
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

        cachedMessages.values.forEach { messageHashMap ->
            messageHashMap[updateDate.messageTid]?.let { message ->
                message.attachments?.forEach { attachment ->
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
    }

    fun updateAttachmentFilePathAndMeta(messageTid: Long, path: String?, metadata: String?) {
        cachedMessages.values.forEach { messageHashMap ->
            messageHashMap[messageTid]?.let { message ->
                message.attachments?.forEach { attachment ->
                    attachment.filePath = path
                    attachment.metadata = metadata
                }
            }
        }
    }
}