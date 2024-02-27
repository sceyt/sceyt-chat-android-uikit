package com.sceyt.sceytchatuikit.persistence.logics.messageslogic

import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentPayLoadData
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.extensions.isNotNullOrBlank
import com.sceyt.sceytchatuikit.extensions.removeAllIf
import com.sceyt.sceytchatuikit.persistence.differs.diffContent
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
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.Preparing
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.ThumbLoaded
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.Uploaded
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.Uploading
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.WaitingToUpload
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.comporators.MessageComparator
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class MessagesCache {
    @Volatile
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


    /** Upsert messages to cash.
     * @param checkDifference if true check differences and add payloads, otherwise only put to cash.
     * @param checkDiffAndNotifyUpdate if true then will emit message update event, when detected
     * message change. */
    fun addAll(channelId: Long, list: List<SceytMessage>,
               checkDifference: Boolean,
               checkDiffAndNotifyUpdate: Boolean): Boolean {

        synchronized(lock) {
            return if (checkDifference)
                putAndCheckHasDiff(channelId, true, checkDiffAndNotifyUpdate, *list.toTypedArray())
            else {
                putAllMessages(channelId, list)
                false
            }
        }
    }

    fun updateAllSyncedMessagesAndGetMissing(channelId: Long, messages: List<SceytMessage>): List<SceytMessage> {
        synchronized(lock) {
            val missingMessages = mutableListOf<SceytMessage>()
            messages.forEach {
                initMessagePayLoads(channelId, it)
                val old = getMessageByTid(channelId, it.tid)
                val hasDiff = old?.diffContent(it)?.hasDifference() ?: false
                if (hasDiff)
                    emitMessageUpdated(channelId, it)

                updateMessage(channelId, it, false)

                if (old == null)
                    missingMessages.add(it)
            }

            return missingMessages
        }
    }

    fun add(channelId: Long, message: SceytMessage) {
        synchronized(lock) {
            putAndCheckHasDiff(channelId, false, true, message)
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
            message.forEach {
                updateMessage(channelId, it, true)
            }
            emitMessageUpdated(channelId, *message)
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
            emitMessageUpdated(channelId, *updatesMessages.toTypedArray())
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
                putAndCheckHasDiff(channelId, false, true, it)
            }
        }
    }

    fun upsertNotifyUpdateAnyway(channelId: Long, vararg message: SceytMessage) {
        synchronized(lock) {
            message.forEach {
                updateMessage(channelId, it, true)
                emitMessageUpdated(channelId, it)
            }
        }
    }

    private fun emitMessageUpdated(channelId: Long, vararg message: SceytMessage) {
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

    private fun updateMessage(channelId: Long, message: SceytMessage, initPayloads: Boolean) {
        cachedMessages[channelId]?.let {
            if (initPayloads)
                initMessagePayLoads(channelId, message)
            it[message.tid] = message
        } ?: run {
            cachedMessages[channelId] = hashMapOf(message.tid to message)
        }
    }

    private fun getMessageByTid(channelId: Long, tid: Long): SceytMessage? {
        return cachedMessages[channelId]?.get(tid)
    }

    /** Set message attachments and pending reactions from cash, which saved in local db.*/
    private fun initMessagePayLoads(channelId: Long, messageToUpdate: SceytMessage) {
        setPayloads(channelId, messageToUpdate)
    }

    private fun setPayloads(channelId: Long, messageToUpdate: SceytMessage) {
        fun setPayloadsImpl(message: SceytMessage): SceytMessage? {
            val cashedMessage = getMessageByTid(channelId, message.tid)
            val attachmentPayLoadData = getAttachmentPayLoads(cashedMessage)
            val attachmentsLinkDetails = getAttachmentLinkDetails(cashedMessage)
            updateAttachmentsPayLoads(attachmentPayLoadData, attachmentsLinkDetails, message)
            return cashedMessage
        }

        val cashedMessage = setPayloadsImpl(messageToUpdate)
        // Set payloads for parent message
        messageToUpdate.parentMessage?.let {
            if (it.id != 0L)
                setPayloadsImpl(it)
        }

        val pendingReactions = cashedMessage?.pendingReactions?.toMutableSet() ?: mutableSetOf()
        val needToAddReactions = messageToUpdate.pendingReactions?.toSet() ?: emptySet()
        pendingReactions.removeAll(needToAddReactions)
        pendingReactions.addAll(needToAddReactions)
        messageToUpdate.pendingReactions = pendingReactions.toList()
    }

    private fun updateAttachmentsPayLoads(payloadData: List<AttachmentPayLoadData>?,
                                          attachmentsLinkDetails: List<LinkPreviewDetails>?,
                                          message: SceytMessage) {
        val updateLinkDetails = attachmentsLinkDetails?.run { ArrayList(this) }
        payloadData?.filter { payLoad -> payLoad.messageTid == message.tid }?.let { data ->
            message.attachments?.forEach { attachment ->
                if (attachment.type == AttachmentTypeEnum.Link.value()) {
                    updateLinkDetails?.find { it.url == attachment.url }?.let {
                        attachment.linkPreviewDetails = it
                        updateLinkDetails.remove(it)
                    }
                    return@forEach
                }
                val predicate: (AttachmentPayLoadData) -> Boolean = if (attachment.url.isNotNullOrBlank()) {
                    { data.any { it.url == attachment.url } }
                } else {
                    { data.any { it.filePath == attachment.filePath } }
                }
                data.find(predicate)?.let {
                    attachment.transferState = it.transferState
                    attachment.progressPercent = it.progressPercent
                    attachment.filePath = it.filePath
                    attachment.url = it.url
                }
            }
        }
        updateLinkDetails?.forEach { linkDetails ->
            message.attachments?.find { it.url == linkDetails.link }?.let {
                it.linkPreviewDetails = linkDetails
            }
        }
    }

    private fun getAttachmentPayLoads(cashedMessage: SceytMessage?): List<AttachmentPayLoadData>? {
        val payloads = cashedMessage?.attachments?.filter { it.type != AttachmentTypeEnum.Link.value() }?.map { attachment ->
            AttachmentPayLoadData(
                messageTid = cashedMessage.tid,
                transferState = attachment.transferState,
                progressPercent = attachment.progressPercent,
                url = attachment.url,
                filePath = attachment.filePath
            )
        }
        return payloads
    }

    private fun getAttachmentLinkDetails(cashedMessage: SceytMessage?): List<LinkPreviewDetails>? {
        val payloads = cashedMessage?.attachments?.filter { it.type == AttachmentTypeEnum.Link.value() }?.mapNotNull { attachment ->
            attachment.linkPreviewDetails
        }
        return payloads
    }

    private fun putAndCheckHasDiff(channelId: Long,
                                   includeNotExistToDiff: Boolean,
                                   checkDiffAndNotifyUpdate: Boolean,
                                   vararg messages: SceytMessage): Boolean {
        var detectedDiff = false
        messages.forEach {
            initMessagePayLoads(channelId, it)
            if (!detectedDiff || checkDiffAndNotifyUpdate) {
                val old = getMessageByTid(channelId, it.tid)
                val hasDiff = old?.diffContent(it)?.hasDifference() ?: includeNotExistToDiff
                if (!detectedDiff)
                    detectedDiff = hasDiff
                if (checkDiffAndNotifyUpdate && hasDiff)
                    emitMessageUpdated(channelId, it)
            }
            updateMessage(channelId, it, false)
        }
        return detectedDiff
    }

    private fun getAllAttachmentsWithLink(link: String): List<SceytAttachment> {
        return cachedMessages.values.flatMap { messageHashMap ->
            messageHashMap.values.flatMap {
                it.attachments?.filter { attachment -> attachment.url == link } ?: emptyList()
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

            cachedMessages.values.forEach { messageHashMap ->
                messageHashMap[updateDate.messageTid]?.let { message ->
                    message.attachments?.forEach att@{ attachment ->
                        if (attachment.type == AttachmentTypeEnum.Link.value())
                            return@att

                        when (updateDate.state) {
                            PendingUpload, Uploading, Uploaded, ErrorUpload, PauseUpload, Preparing, WaitingToUpload -> {
                                if (attachment.filePath == updateDate.filePath)
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
    }

    fun updateAttachmentFilePathAndMeta(messageTid: Long, path: String?, metadata: String?) {
        synchronized(lock) {
            cachedMessages.values.forEach { messageHashMap ->
                messageHashMap[messageTid]?.let { message ->
                    message.attachments?.forEach att@{ attachment ->
                        if (attachment.type == AttachmentTypeEnum.Link.value())
                            return@att
                        attachment.filePath = path
                        attachment.metadata = metadata
                    }
                }
            }
        }
    }

    fun updateAttachmentLinkDetails(data: LinkPreviewDetails) {
        synchronized(lock) {
            getAllAttachmentsWithLink(data.link).forEach { attachment ->
                attachment.linkPreviewDetails = data
            }
        }
    }

    fun updateLinkDetailsSize(link: String, width: Int, height: Int) {
        synchronized(lock) {
            getAllAttachmentsWithLink(link).forEach { attachment ->
                attachment.linkPreviewDetails?.imageWidth = width
                attachment.linkPreviewDetails?.imageHeight = height
            }
        }
    }

    fun updateThumb(link: String, thumb: String) {
        synchronized(lock) {
            getAllAttachmentsWithLink(link).forEach { attachment ->
                attachment.linkPreviewDetails?.thumb = thumb
            }
        }
    }

    fun moveMessagesToNewChannel(pendingChannelId: Long, newChannelId: Long) {
        synchronized(lock) {
            cachedMessages[newChannelId] = cachedMessages[pendingChannelId] ?: return
            cachedMessages.remove(pendingChannelId)
        }
    }

    internal fun deletePendingReaction(channelId: Long, tid: Long, key: String): SceytMessage? {
        synchronized(lock) {
            return cachedMessages[channelId]?.get(tid)?.let {
                val newReactions = it.pendingReactions?.toMutableSet()?.apply {
                    find { data -> data.key == key }?.let { reactionData ->
                        remove(reactionData)
                    }
                }
                it.pendingReactions = newReactions?.toList()
                it
            }
        }
    }
}