package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chatuikit.data.managers.connection.ConnectionEventManager
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageDb
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.logicimpl.message.PendingSendRegistry
import com.sceyt.chatuikit.persistence.mappers.isTransferTrackedType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class SendPendingMessagesUseCase(
    private val messageDao: MessageDao,
    private val registry: PendingSendRegistry,
    private val isConnected: () -> Boolean = { ConnectionEventManager.isConnected },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend operator fun invoke(
        channelId: Long,
        skipPausedUploads: Boolean,
        sender: PendingMessageSender
    ): Boolean = withContext(ioDispatcher) {
        if (!isConnected()) return@withContext false

        val pendingMessages = messageDao.getPendingMessages(channelId)
        if (pendingMessages.isEmpty()) return@withContext false

        pendingMessages.forEach { snapshot ->
            val tid = snapshot.messageEntity.tid
            if (!registry.tryAcquire(tid)) return@forEach
            try {
                val messageDb = messageDao.getPendingMessageByTid(tid) ?: return@forEach
                if (skipPausedUploads && isUploadPaused(messageDb)) return@forEach
                sender.send(messageDb, isUploadedForward(messageDb))
            } finally {
                registry.release(tid)
            }
        }
        true
    }

    private fun isUploadPaused(messageDb: MessageDb): Boolean {
        val attachments = messageDb.attachments
        if (attachments.isNullOrEmpty()) return false
        return attachments.all { it.payLoad?.transferState == TransferState.PauseUpload }
    }

    private fun isUploadedForward(messageDb: MessageDb): Boolean {
        val isForwarded = messageDb.messageEntity.forwardingDetailsDb != null
        val areTrackedAttachmentsUploaded = messageDb.attachments?.all { attachment ->
            val isTracked = isTransferTrackedType(attachment.attachmentEntity.type)
            val isUploaded = attachment.payLoad?.transferState == TransferState.Uploaded
            !isTracked || isUploaded
        } == true
        return isForwarded && areTrackedAttachmentsUploaded
    }
}
