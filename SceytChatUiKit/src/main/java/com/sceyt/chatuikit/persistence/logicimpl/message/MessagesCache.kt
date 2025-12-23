package com.sceyt.chatuikit.persistence.logicimpl.message

import com.sceyt.chatuikit.data.models.messages.AttachmentPayLoadData
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.extensions.isNotNullOrBlank
import com.sceyt.chatuikit.extensions.removeAllIf
import com.sceyt.chatuikit.extensions.removeAllIfCollect
import com.sceyt.chatuikit.persistence.differs.diffContent
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Downloaded
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Downloading
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ErrorDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ErrorUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.FilePathChanged
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PauseDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PauseUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PendingDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PendingUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Preparing
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ThumbLoaded
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Uploaded
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Uploading
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.WaitingToUpload
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.comporators.MessageComparator
import com.sceyt.chatuikit.presentation.extensions.isNotPending
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

typealias ChannelId = Long
typealias MessageTid = Long
typealias MessageDeletionDate = Long

class MessagesCache {
    private var cachedMessages = hashMapOf<Long, HashMap<Long, SceytMessage>>()
    private val mutex = Mutex()

    companion object {
        private val messageUpdatedFlow_ = MutableSharedFlow<Pair<ChannelId, List<SceytMessage>>>(
            extraBufferCapacity = 30,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val messageUpdatedFlow = messageUpdatedFlow_.asSharedFlow()

        private val messagesClearedFlow_ = MutableSharedFlow<Pair<ChannelId, MessageDeletionDate>>(
            extraBufferCapacity = 30,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val messagesClearedFlow = messagesClearedFlow_.asSharedFlow()

        private val messagesHardDeletedFlow_ = MutableSharedFlow<Pair<ChannelId, List<MessageTid>>>(
            extraBufferCapacity = 30,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val messagesHardDeletedFlow = messagesHardDeletedFlow_.asSharedFlow()
    }


    /** Upsert messages to cash.
     * @param checkDifference if true check differences and add payloads, otherwise only put to cash.
     * @param checkDiffAndNotifyUpdate if true then will emit message update event, when detected
     * message change. */
    suspend fun addAll(
        channelId: Long, list: List<SceytMessage>,
        checkDifference: Boolean,
        checkDiffAndNotifyUpdate: Boolean,
    ): Boolean = mutex.withLock {
        return@withLock if (checkDifference)
            putAndCheckHasDiff(channelId, true, checkDiffAndNotifyUpdate, *list.toTypedArray())
        else {
            putAllMessages(channelId, list)
            false
        }
    }

    suspend fun updateAllSyncedMessagesAndGetMissing(
        channelId: Long,
        messages: List<SceytMessage>
    ): List<SceytMessage> = mutex.withLock {
        val missingMessages = mutableListOf<SceytMessage>()
        messages.forEach {
            val updatedMessage = initMessagePayLoads(channelId, it)
            val old = getMessageByTid(channelId, it.tid)
            val hasDiff = old?.diffContent(updatedMessage)?.hasDifference() ?: false
            if (hasDiff)
                emitMessageUpdated(channelId, updatedMessage)

            updateMessage(channelId, updatedMessage, false)

            if (old == null)
                missingMessages.add(updatedMessage)
        }

        return@withLock missingMessages
    }

    suspend fun add(channelId: Long, message: SceytMessage) = mutex.withLock {
        putAndCheckHasDiff(channelId, false, true, message)
    }

    suspend fun get(channelId: Long, tid: Long): SceytMessage? = mutex.withLock {
        return@withLock getMessageByTid(channelId, tid)
    }

    suspend fun clear(channelId: Long) = mutex.withLock {
        cachedMessages[channelId]?.clear()
    }

    suspend fun clearAllExceptPending(channelId: Long) = mutex.withLock {
        getMessagesMap(channelId)?.values
            ?.filter { it.isNotPending() }
            ?.map { it.tid }?.forEach {
                cachedMessages[channelId]?.remove(it)
            }
    }

    suspend fun getSorted(channelId: Long): List<SceytMessage> = mutex.withLock {
        return@withLock getMessagesMap(channelId)?.values?.sortedWith(MessageComparator())
            ?: emptyList()
    }

    suspend fun messageUpdated(channelId: Long, vararg message: SceytMessage) = mutex.withLock {
        val messages = message.map {
            updateMessage(channelId, it, true)
        }
        emitMessageUpdated(channelId, *messages.toTypedArray())
    }

    suspend fun updateMessagesStatus(
        channelId: Long,
        status: MessageDeliveryStatus,
        vararg tIds: Long
    ) = mutex.withLock {
        val updatesMessages = mutableListOf<SceytMessage>()
        tIds.forEach {
            getMessageByTid(channelId, it)?.let { message ->
                if (message.deliveryStatus < status) {
                    updatesMessages.add(message.copy(deliveryStatus = status))
                }
            }
        }
        emitMessageUpdated(channelId, *updatesMessages.toTypedArray())
    }

    suspend fun hardDeleteMessage(channelId: Long, message: SceytMessage) = mutex.withLock {
        cachedMessages[channelId]?.remove(message.tid)
        messagesHardDeletedFlow_.tryEmit(channelId to listOf(message.tid))
    }

    suspend fun hardDeleteMessage(channelId: Long, vararg tIds: Long) = mutex.withLock {
        tIds.forEach { tid ->
            cachedMessages[channelId]?.remove(tid)
        }
        messagesHardDeletedFlow_.tryEmit(channelId to tIds.toList())
    }

    internal suspend fun deleteAllMessagesWhere(
        predicate: (SceytMessage) -> Boolean
    ) = mutex.withLock {
        cachedMessages.forEach { (_, map) ->
            map.removeAllIf(predicate)
        }
    }

    suspend fun forceDeleteAllMessagesWhere(predicate: (SceytMessage) -> Boolean) = mutex.withLock {
        cachedMessages.forEach { (channelId, map) ->
            val removedMessages = map.removeAllIfCollect(predicate)
            if (removedMessages.isNotEmpty()) {
                val tIds = removedMessages.map { it.tid }
                messagesHardDeletedFlow_.tryEmit(channelId to tIds)
            }
        }
    }

    suspend fun deleteAllClearedMessages(
        channelId: Long,
        messagesDeletionDate: Long
    ) = mutex.withLock {
        if (getMessagesMap(channelId)?.removeAllIf { it.createdAt <= messagesDeletionDate && it.isNotPending() } == true) {
            messagesClearedFlow_.tryEmit(Pair(channelId, messagesDeletionDate))
        }
    }

    suspend fun upsertMessages(channelId: Long, vararg message: SceytMessage) = mutex.withLock {
        message.forEach {
            putAndCheckHasDiff(channelId, false, true, it)
        }
    }

    suspend fun upsertNotifyUpdateAnyway(
        channelId: Long,
        vararg message: SceytMessage
    ) = mutex.withLock {
        message.forEach {
            emitMessageUpdated(channelId, updateMessage(channelId, it, true))
        }
    }

    private fun emitMessageUpdated(channelId: Long, vararg message: SceytMessage) {
        messageUpdatedFlow_.tryEmit(Pair(channelId, message.toList()))
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

    private fun updateMessage(
        channelId: Long,
        message: SceytMessage,
        initPayloads: Boolean
    ): SceytMessage {
        var updatedMessage: SceytMessage = message
        cachedMessages[channelId]?.let {
            if (initPayloads)
                updatedMessage = initMessagePayLoads(channelId, message)
            it[message.tid] = updatedMessage
        } ?: run {
            cachedMessages[channelId] = hashMapOf(message.tid to message)
        }
        return updatedMessage
    }

    private fun getMessageByTid(channelId: Long, tid: Long): SceytMessage? {
        return cachedMessages[channelId]?.get(tid)
    }

    /** Set message attachments and pending reactions from cash, which saved in local db.*/
    private fun initMessagePayLoads(channelId: Long, messageToUpdate: SceytMessage): SceytMessage {
        return setPayloads(channelId, messageToUpdate)
    }

    private fun setPayloads(channelId: Long, messageToUpdate: SceytMessage): SceytMessage {
        fun setPayloadsImpl(message: SceytMessage): SceytMessage {
            val cashedMessage = getMessageByTid(channelId, message.tid)
            val attachmentPayLoadData = getAttachmentPayLoads(cashedMessage)
            val attachmentsLinkDetails = getAttachmentLinkDetails(cashedMessage)
            val attachments = getUpdatedAttachmentsWithPayLoads(
                payloadData = attachmentPayLoadData,
                attachmentsLinkDetails = attachmentsLinkDetails,
                message = message
            )
            return message.copy(attachments = attachments?.toList())
        }

        var updatedMessage = setPayloadsImpl(messageToUpdate)
        // Set payloads for parent message
        messageToUpdate.parentMessage?.let {
            if (it.id != 0L) {
                updatedMessage = updatedMessage.copy(parentMessage = setPayloadsImpl(it))
            }
        }

        val pendingReactions = updatedMessage.pendingReactions?.toMutableSet() ?: mutableSetOf()
        val needToAddReactions = messageToUpdate.pendingReactions?.toSet() ?: emptySet()
        pendingReactions.removeAll(needToAddReactions)
        pendingReactions.addAll(needToAddReactions)
        return updatedMessage.copy(pendingReactions = pendingReactions.toList())
    }

    private fun getUpdatedAttachmentsWithPayLoads(
        payloadData: List<AttachmentPayLoadData>?,
        attachmentsLinkDetails: List<LinkPreviewDetails>?,
        message: SceytMessage,
    ): List<SceytAttachment>? {
        val updateAttachments = message.attachments?.toMutableList() ?: return null
        val updateLinkDetails = attachmentsLinkDetails?.run { ArrayList(this) }
        payloadData?.filter { payLoad -> payLoad.messageTid == message.tid }?.let { data ->
            message.attachments.forEachIndexed { index, attachment ->
                if (attachment.type == AttachmentTypeEnum.Link.value) {
                    updateLinkDetails?.find { it.url == attachment.url }?.let {
                        updateAttachments[index] = attachment.copy(linkPreviewDetails = it)
                        updateLinkDetails.remove(it)
                    }
                    return@forEachIndexed
                }
                val predicate: (AttachmentPayLoadData) -> Boolean =
                    if (attachment.url.isNotNullOrBlank()) {
                        { data.any { it.url == attachment.url } }
                    } else {
                        { data.any { it.filePath == attachment.filePath } }
                    }
                data.find(predicate)?.let {
                    updateAttachments[index] = attachment.copy(
                        transferState = it.transferState,
                        progressPercent = it.progressPercent,
                        filePath = it.filePath,
                        url = it.url
                    )
                }
            }
        }
        updateLinkDetails?.forEach { linkDetails ->
            updateAttachments.indexOfFirst { it.url == linkDetails.link }.takeIf { it != -1 }?.let {
                val item = updateAttachments[it]
                updateAttachments[it] = item.copy(linkPreviewDetails = linkDetails)
            }
        }
        return updateAttachments
    }

    private fun getAttachmentPayLoads(cashedMessage: SceytMessage?): List<AttachmentPayLoadData>? {
        val payloads = cashedMessage?.attachments?.filter {
            it.type != AttachmentTypeEnum.Link.value
        }?.map { attachment ->
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
        val payloads = cashedMessage?.attachments?.filter {
            it.type == AttachmentTypeEnum.Link.value
        }?.mapNotNull { attachment ->
            attachment.linkPreviewDetails
        }
        return payloads
    }

    private fun putAndCheckHasDiff(
        channelId: Long,
        includeNotExistToDiff: Boolean,
        checkDiffAndNotifyUpdate: Boolean,
        vararg messages: SceytMessage,
    ): Boolean {
        var detectedDiff = false
        messages.forEach {
            val updateMessage = initMessagePayLoads(channelId, it)
            if (!detectedDiff || checkDiffAndNotifyUpdate) {
                val old = getMessageByTid(channelId, updateMessage.tid)
                val hasDiff = old?.diffContent(updateMessage)?.hasDifference()
                    ?: includeNotExistToDiff
                if (!detectedDiff)
                    detectedDiff = hasDiff

                if (checkDiffAndNotifyUpdate && hasDiff)
                    emitMessageUpdated(channelId, updateMessage)
            }
            updateMessage(channelId, updateMessage, false)
        }
        return detectedDiff
    }

    private fun updateAllAttachments(
        predicate: (SceytAttachment) -> Boolean,
        updater: SceytAttachment.() -> SceytAttachment,
    ) {
        cachedMessages.values.forEach { messageHashMap ->
            for ((key, value) in messageHashMap.entries) {
                val newAttachments = value.attachments?.toMutableList()?.apply {
                    forEachIndexed { index, attachment ->
                        if (predicate(attachment))
                            set(index, attachment.updater())
                    }
                }
                messageHashMap[key] = value.copy(attachments = newAttachments)
            }
        }
    }

    suspend fun updateAttachmentTransferData(updateDate: TransferData) = mutex.withLock {
        fun update(attachment: SceytAttachment): SceytAttachment {
            return attachment.copy(
                transferState = updateDate.state,
                progressPercent = updateDate.progressPercent,
                filePath = updateDate.filePath,
                url = updateDate.url
            )
        }

        cachedMessages.values.forEach { messageHashMap ->
            messageHashMap[updateDate.messageTid]?.let { message ->
                val attachments = message.attachments?.toMutableList() ?: return@let
                attachments.forEachIndexed att@{ index, attachment ->
                    if (attachment.type == AttachmentTypeEnum.Link.value)
                        return@att

                    when (updateDate.state) {
                        PendingUpload, Uploading, Uploaded, ErrorUpload, PauseUpload, Preparing, WaitingToUpload -> {
                            if (attachment.filePath == updateDate.filePath) {
                                attachments[index] = update(attachment)
                                messageHashMap[updateDate.messageTid] =
                                    message.copy(attachments = attachments)
                            }
                        }

                        Downloading, Downloaded, PendingDownload, ErrorDownload, PauseDownload -> {
                            if (attachment.url == updateDate.url) {
                                attachments[index] = update(attachment)
                                messageHashMap[updateDate.messageTid] =
                                    message.copy(attachments = attachments)
                            }
                        }

                        FilePathChanged, ThumbLoaded -> return@withLock
                    }
                }
            }
        }
    }

    suspend fun updateAttachmentFilePathAndMeta(
        messageTid: Long,
        path: String?,
        metadata: String?
    ) = mutex.withLock {
        cachedMessages.values.forEach { messageHashMap ->
            messageHashMap[messageTid]?.let { message ->
                val attachments = message.attachments?.toMutableList() ?: return@let
                attachments.forEachIndexed att@{ index, attachment ->
                    if (attachment.type == AttachmentTypeEnum.Link.value)
                        return@att
                    attachments[index] = attachment.copy(
                        filePath = path,
                        metadata = metadata
                    )
                    messageHashMap[messageTid] = message.copy(attachments = attachments)
                }
            }
        }
    }

    suspend fun updateAttachmentLinkDetails(data: LinkPreviewDetails) = mutex.withLock {
        updateAllAttachments(predicate = { it.url == data.link }, updater = {
            copy(linkPreviewDetails = data)
        })
    }

    suspend fun updateLinkDetailsSize(link: String, width: Int, height: Int) = mutex.withLock {
        updateAllAttachments(predicate = { it.url == link }, updater = {
            copy(
                linkPreviewDetails = linkPreviewDetails?.copy(
                    imageWidth = width,
                    imageHeight = height
                )
            )
        })
    }

    suspend fun updateThumb(link: String, thumb: String) = mutex.withLock {
        updateAllAttachments(predicate = { it.url == link }, updater = {
            copy(linkPreviewDetails = linkPreviewDetails?.copy(thumb = thumb))
        })
    }

    suspend fun moveMessagesToNewChannel(
        pendingChannelId: Long,
        newChannelId: Long
    ) = mutex.withLock {
        cachedMessages[newChannelId] = cachedMessages[pendingChannelId] ?: return@withLock
        cachedMessages.remove(pendingChannelId)
    }

    internal suspend fun deletePendingReaction(
        channelId: Long,
        tid: Long,
        key: String
    ): SceytMessage? = mutex.withLock {
        return@withLock cachedMessages[channelId]?.get(tid)?.let {
            val newReactions = it.pendingReactions?.toMutableSet()?.apply {
                find { data -> data.key == key }?.let { reactionData ->
                    remove(reactionData)
                }
            }
            val message = it.copy(pendingReactions = newReactions?.toList())
            cachedMessages[channelId]?.put(tid, message)
        }
    }
}