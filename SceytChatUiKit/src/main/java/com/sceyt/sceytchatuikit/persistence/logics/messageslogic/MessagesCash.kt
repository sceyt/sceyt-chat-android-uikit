package com.sceyt.sceytchatuikit.persistence.logics.messageslogic

import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.persistence.entity.messages.AttachmentPayLoadEntity
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.*
import com.sceyt.sceytchatuikit.presentation.common.diffContent
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.comporators.MessageComparator
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class MessagesCash {
    private var cashedMessages = hashMapOf<Long, SceytMessage>()
    private val lock = Any()

    companion object {
        private val messageUpdatedFlow_ = MutableSharedFlow<List<SceytMessage>>(
            extraBufferCapacity = 30,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val messageUpdatedFlow: SharedFlow<List<SceytMessage>> = messageUpdatedFlow_
    }

    /** Added messages like upsert, and check is differences between messages*/
    fun addAll(list: List<SceytMessage>, checkDifference: Boolean): Boolean {
        synchronized(lock) {
            return if (checkDifference)
                putAndCheckHasDiff(true, *list.toTypedArray())
            else {
                cashedMessages.putAll(list.associateBy { it.tid })
                false
            }
        }
    }

    fun add(message: SceytMessage) {
        synchronized(lock) {
            val payLoad = getPayLoads(message)
            cashedMessages[message.tid] = message
            emitMessageUpdated(payLoad?.toList(), message)
        }
    }

    fun get(tid: Long): SceytMessage? {
        synchronized(lock) {
            return cashedMessages[tid]
        }
    }

    fun clear() {
        synchronized(lock) {
            cashedMessages.clear()
        }
    }

    fun getSorted(): List<SceytMessage> {
        synchronized(lock) {
            return cashedMessages.values.sortedWith(MessageComparator()).map { it.clone() }
        }
    }

    fun messageUpdated(vararg message: SceytMessage) {
        synchronized(lock) {
            val payLoad = getPayLoads(*message)
            message.forEach {
                cashedMessages[it.tid] = it
            }
            emitMessageUpdated(payLoad, *message)
        }
    }

    fun updateMessagesStatus(status: DeliveryStatus, vararg tIds: Long) {
        synchronized(lock) {
            val updatesMessages = mutableListOf<SceytMessage>()
            tIds.forEach {
                cashedMessages[it]?.let { message ->
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
            cashedMessages.remove(tid)
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
            payloads = cashedMessages[it.tid]?.attachments?.map { attachment ->
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
                val old = cashedMessages[it.tid]
                detectedDiff = old?.diffContent(it)?.hasDifference() ?: includeNotExistToDiff
            }
            cashedMessages[it.tid] = it
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
        cashedMessages[updateDate.messageTid]?.let {
            it.attachments?.forEach { attachment ->
                when (updateDate.state) {
                    PendingUpload, Uploading, Uploaded, ErrorUpload, PauseUpload -> {
                        if (attachment.tid == updateDate.attachmentTid)
                            update(attachment)

                    }
                    Downloading, Downloaded, PendingDownload, ErrorDownload, PauseDownload -> {
                        if (attachment.url == updateDate.url)
                            update(attachment)
                    }
                    FilePathChanged -> return
                }
            }
        }
    }

    fun updateAttachmentFilePathAndMeta(messageTid: Long, path: String?, metadata: String?) {
        cashedMessages[messageTid]?.let {
            it.attachments?.forEach { attachment ->
                attachment.filePath = path
                attachment.metadata = metadata
            }
        }
    }
}