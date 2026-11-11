package com.sceyt.chatuikit.persistence.workers

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkInfo
import androidx.work.WorkerParameters
import androidx.work.await
import com.sceyt.chat.models.message.Message
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.managers.connection.ConnectionEventManager
import com.sceyt.chatuikit.data.models.fold
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.extensions.hasConnectedNetwork
import com.sceyt.chatuikit.extensions.hasPermissions
import com.sceyt.chatuikit.extensions.isNotNullOrBlank
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.notifications.service.FileTransferNotificationData
import com.sceyt.chatuikit.persistence.database.dao.FileChecksumDao
import com.sceyt.chatuikit.persistence.database.entity.FileChecksumEntity
import com.sceyt.chatuikit.persistence.extensions.safeResume
import com.sceyt.chatuikit.persistence.extensions.toMetadataJson
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferHelper
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.file_transfer.TransferTask
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceChannelsLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceMessagesLogic
import com.sceyt.chatuikit.persistence.logicimpl.AttachmentPreviewTransfer
import com.sceyt.chatuikit.persistence.logicimpl.PreviewUploadResult
import com.sceyt.chatuikit.persistence.mappers.isPreview
import com.sceyt.chatuikit.persistence.mappers.isTransferTracked
import com.sceyt.chatuikit.persistence.mappers.toMessage
import com.sceyt.chatuikit.persistence.mappers.toTransferData
import com.sceyt.chatuikit.persistence.mappers.withPreviewLink
import com.sceyt.chatuikit.persistence.workers.UploadAndSendAttachmentWorkManager.FILE_TRANSFER_NOTIFICATION_ID
import com.sceyt.chatuikit.persistence.workers.UploadAndSendAttachmentWorkManager.IS_SHARING
import com.sceyt.chatuikit.persistence.workers.UploadAndSendAttachmentWorkManager.MESSAGE_TID
import com.sceyt.chatuikit.persistence.workers.UploadAndSendAttachmentWorkManager.RESUME_PAUSED_UPLOAD
import com.sceyt.chatuikit.presentation.extensions.isNotPending
import com.sceyt.chatuikit.shared.utils.FileChecksumCalculator
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.seconds
import kotlin.Result as KtResult

class UploadAndSendAttachmentWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), SceytKoinComponent {
    private val uploadLock: AttachmentUploadLock by inject()
    private val fileTransferService: FileTransferService by inject()
    private val attachmentLogic: PersistenceAttachmentLogic by inject()
    private val messageLogic: PersistenceMessagesLogic by inject()
    private val channelLogic: PersistenceChannelsLogic by inject()
    private val fileChecksumDao: FileChecksumDao by inject()
    private val previewTransfer: AttachmentPreviewTransfer by inject()

    // A retried worker must respect a user pause made after the explicit resume request.
    private val resumePausedUpload: Boolean
        get() = inputData.getBoolean(RESUME_PAUSED_UPLOAD, false) && runAttemptCount == 0

    override suspend fun doWork(): Result {
        val messageTid = inputData.getLong(MESSAGE_TID, 0)
        val isSharing =
            inputData.getBoolean(IS_SHARING, false) || fileTransferService.getTasks().values.any {
                it.messageTid == messageTid && it.isSharedUpload
            }

        return if (isSharing) {
            uploadAndSend(true)
        } else {
            uploadLock.withLock {
                uploadAndSend(false)
            }
        }
    }

    private suspend fun uploadAttachmentsBeforeSend(
        message: SceytMessage,
        isSharing: Boolean,
        resumePausedUpload: Boolean,
    ): KtResult<List<SceytAttachment>> {
        val payloads = attachmentLogic.getAllPayLoadsByMsgTid(message.tid)
        val attachments = message.attachments?.toMutableList()
            ?: return KtResult.failure(Exception("Attachments not found"))

        // Apply the explicit resume before preparation can expose the pause control again.
        if (resumePausedUpload) {
            val attachment = attachments.firstOrNull { it.isTransferTracked() }
            val task = attachment?.let(fileTransferService::findTransferTask)
            if (task?.state == TransferState.PauseUpload)
                task.state = TransferState.WaitingToUpload
        }

        uploadPreviewIfNeeded(message, attachments, resumePausedUpload).onFailure {
            return KtResult.failure(it)
        }

        for ((index, attachment) in attachments.withIndex()) {
            // Link previews already contain their remote reference.
            if (!attachment.isTransferTracked())
                continue

            val payload = payloads.find { it.messageTid == attachment.messageTid }
            // A stored upload state or URL lets a resumed worker reuse the previous upload.
            if (payload != null && (payload.transferState == TransferState.Uploaded || payload.url.isNotNullOrBlank())) {
                val transferData = payload.toTransferData(TransferState.Uploaded, 100f)
                attachmentLogic.updateAttachmentWithTransferData(transferData)
                attachments[index] = attachment.copy(url = payload.url)
                return KtResult.success(attachments)
            }

            val filePath = attachment.originalFilePath ?: attachment.filePath
            if (filePath.isNullOrEmpty()) {
                SceytLog.i(TAG, "Skip uploading a file path is null or empty")
                continue
            }

            val checksum = FileChecksumCalculator.calculateFileChecksum(filePath)
            if (checksum != null) {
                val checksumEntity = FileChecksumEntity(
                    checksum = checksum,
                    resizedFilePath = null,
                    url = null,
                    metadata = attachment.metadata,
                    fileSize = attachment.fileSize
                )
                fileChecksumDao.insert(checksumEntity)
            }

            // Automatic worker runs leave paused uploads waiting for an explicit resume.
            val isPaused =
                fileTransferService.findTransferTask(attachment)?.state == TransferState.PauseUpload
            if (isPaused || !shouldStartUpload(attachment.transferState, resumePausedUpload)) {
                return KtResult.failure(Exception("Attachment upload is paused"))
            }

            markWaitingToUpload(message.tid, attachment)

            val uploadAttachment = if (attachment.transferState == TransferState.PauseUpload)
                attachment.copy(transferState = TransferState.WaitingToUpload)
            else attachment
            val result = uploadFile(uploadAttachment, isSharing)
            return result.map { uploaded ->
                checksum?.let { saveUploadedChecksum(it, uploaded) }

                attachments[index] = uploaded.attachment
                attachments
            }
        }
        return KtResult.failure(Exception("Could not find any attachment to upload"))
    }

    private suspend fun saveUploadedChecksum(checksum: Long, uploaded: UploadedAttachment) {
        val encrypted = SceytChatUIKit.fileTransfer.encryptionEnabled
        val encryptionMetadata = uploaded.encryptionMetadata.orEmpty()
        if (encrypted && encryptionMetadata.isEmpty())
            return

        val savedMetadata = if (encrypted) encryptionMetadata.toMetadataJson() else null
        fileChecksumDao.updateUpload(checksum, uploaded.attachment.url, savedMetadata)
    }

    private suspend fun uploadPreviewIfNeeded(
        message: SceytMessage,
        attachments: MutableList<SceytAttachment>,
        resumePausedUpload: Boolean,
    ): KtResult<Unit> {
        val video = attachments.find { it.type == AttachmentTypeEnum.Video.value }
            ?: return KtResult.success(Unit)

        if (message.viewOnce || message.isForwarded || attachments.any { it.isPreview() })
            return KtResult.success(Unit)

        if (!shouldStartUpload(video.transferState, resumePausedUpload))
            return KtResult.success(Unit)

        if (video.transferState != TransferState.Uploaded)
            markWaitingToUpload(message.tid, video)

        val result = previewTransfer.upload(video)
        if (fileTransferService.findTransferTask(video)?.state == TransferState.PauseUpload)
            return KtResult.failure(Exception("Attachment upload is paused"))

        return when (result) {
            is PreviewUploadResult.Uploaded -> {
                attachments.add(result.preview)
                KtResult.success(Unit)
            }

            is PreviewUploadResult.Unavailable -> KtResult.success(Unit)
            is PreviewUploadResult.Failed -> {
                val transferData = TransferData(
                    messageTid = message.tid,
                    progressPercent = 0f,
                    state = TransferState.ErrorUpload,
                    filePath = video.filePath,
                    url = video.url
                )
                FileTransferHelper.emitAttachmentTransferUpdate(transferData, video.fileSize)
                attachmentLogic.updateAttachmentWithTransferData(transferData)
                KtResult.failure(result.error)
            }
        }
    }

    private suspend fun markWaitingToUpload(messageTid: Long, attachment: SceytAttachment) {
        val transferData = TransferData(
            messageTid = messageTid,
            progressPercent = attachment.progressPercent ?: 0f,
            state = TransferState.WaitingToUpload,
            filePath = attachment.filePath,
            url = attachment.url
        )

        FileTransferHelper.emitAttachmentTransferUpdate(
            transferData = transferData,
            fileSize = attachment.fileSize
        )
        attachmentLogic.updateAttachmentWithTransferData(transferData)
    }

    private suspend fun uploadFile(
        attachment: SceytAttachment,
        isSharing: Boolean
    ): KtResult<UploadedAttachment> = suspendCancellableCoroutine { continuation ->
        // Cancellation can happen before the worker reaches task registration.
        if (!continuation.isActive) return@suspendCancellableCoroutine

        val existingTask = fileTransferService.findTransferTask(attachment)
        val completionKey = this.toString()
        val configureTask: TransferTask.() -> Unit = {
            isSharedUpload = isSharedUpload || isSharing
            addOnCompletionListener(completionKey) { result ->
                continuation.safeResume(result.map { UploadedAttachment(it, encryptionMetadata) })
            }
            continuation.invokeOnCancellation {
                removeOnCompletionListener(completionKey)

                // A worker that joined an existing task only removes its own listener.
                if (existingTask != null) return@invokeOnCancellation
                if (isStoppedBySystem()) {
                    SceytLog.i(TAG, "Worker stopped by the system, keeping upload for message tid: ${attachment.messageTid}")
                    return@invokeOnCancellation
                }

                // A newer worker may already have replaced this task.
                if (fileTransferService.findTransferTask(attachment) === this) {
                    fileTransferService.cancelUpload(attachment)
                }
            }
        }

        // A resumed worker rejoins the upload retained by the coordinator.
        fileTransferService.upload(
            attachment = attachment,
            shared = isSharing || existingTask?.isSharedUpload == true,
            configureTask = configureTask,
        )
    }

    private fun isStoppedBySystem(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
            return false

        return when (stopReason) {
            WorkInfo.STOP_REASON_NOT_STOPPED, WorkInfo.STOP_REASON_CANCELLED_BY_APP -> false
            else -> true
        }
    }

    private suspend fun uploadAndSend(isSharing: Boolean): Result {
        val messageTid = inputData.getLong(MESSAGE_TID, 0)
        val message = messageLogic.getMessageFromDbByTid(messageTid)
            ?: return finishWorkWithError("Message not found: $messageTid")

        // A queued worker can run after the message has already been sent.
        if (message.isNotPending())
            return finishWorkWithSuccess()

        if (applicationContext.hasPermissions(android.Manifest.permission.FOREGROUND_SERVICE))
            startForeground(message.channelId, message)

        val result = uploadAttachmentsBeforeSend(
            message = message,
            isSharing = isSharing,
            resumePausedUpload = resumePausedUpload,
        )

        // Sending requires a completed upload and a worker that is still allowed to run.
        if (result.isFailure || isStopped) {
            return finishWorkWithError("Could not upload attachments, message tid: $messageTid")
        }

        val attachments = result.getOrThrow().withPreviewLink(viewOnce = message.viewOnce)
        val messageToSend = message.copy(attachments = attachments).toMessage()
        if (ConnectionEventManager.isConnected) {
            // The current connection can send the message immediately.
            return sendMessage(message.channelId, messageToSend)
        }

        SceytLog.i(
            TAG,
            "SceytChat is not connected. Connecting to send message tid: $messageTid"
        )
        val connectionResult = SceytChatUIKit.chatConnectionProvider
            ?.connect(20.seconds.inWholeMilliseconds)
            ?: KtResult.failure(IllegalStateException("ChatConnectionProvider is not configured"))

        if (connectionResult.isFailure) {
            return finishWorkWithError(
                "Could not connect to send message tid: $messageTid. " +
                        connectionResult.exceptionOrNull()?.message
            )
        }

        return sendMessage(message.channelId, messageToSend)
    }

    private suspend fun sendMessage(channelId: Long, message: Message): Result {
        val response = messageLogic.sendMessageWithUploadedAttachments(channelId, message)
        return response.fold(
            onSuccess = { sentMessage ->
                if (sentMessage != null)
                    messageLogic.attachmentSuccessfullySent(sentMessage)
                finishWorkWithSuccess()
            },
            onError = {
                val errorMessage = "Could not send message: ${response.message}"
                SceytLog.e(TAG, errorMessage)
                finishWorkWithError(errorMessage)
            },
        )
    }

    private suspend fun finishWorkWithError(error: String): Result {
        // UIKit controls retries through pending messages, so errors also finish this work successfully.
        val result = Result.success()
        SceytLog.e(TAG, "SendAttachmentWorker worker finished with error: $error")
        scheduleRetryIfOffline()
        SceytChatUIKit.notifications.fileTransferServiceNotification.notificationHandler
            .serviceWorkerFinished(result = result)
        return result
    }

    private suspend fun scheduleRetryIfOffline() {
        if (applicationContext.hasConnectedNetwork())
            return

        val messageTid = inputData.getLong(MESSAGE_TID, 0)
        SceytLog.i(TAG, "Network is unavailable, queued pending attachment retry for message tid: $messageTid")
        PendingAttachmentRetryWorker.schedule(applicationContext).await()
    }

    private fun finishWorkWithSuccess(): Result {
        val result = Result.success()
        SceytChatUIKit.notifications.fileTransferServiceNotification.notificationHandler
            .serviceWorkerFinished(result = result)
        return result
    }

    private suspend fun startForeground(channelId: Long, message: SceytMessage) {
        val notification = createNotification(channelId, message) ?: return
        val foregroundInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ForegroundInfo(
                FILE_TRANSFER_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else ForegroundInfo(FILE_TRANSFER_NOTIFICATION_ID, notification)

        try {
            setForeground(foregroundInfo)
        } catch (error: IllegalStateException) {
            if (!error.isForegroundStartNotAllowed())
                throw error
            SceytLog.i(
                TAG,
                "Foreground is not allowed in the background, message tid: ${message.tid}"
            )
        }
    }

    private fun IllegalStateException.isForegroundStartNotAllowed(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && this is ForegroundServiceStartNotAllowedException

    private suspend fun createNotification(channelId: Long, message: SceytMessage): Notification? {
        val serviceNotification = SceytChatUIKit.notifications.fileTransferServiceNotification

        // The channel can disappear while this worker is queued.
        val channel = channelLogic.getChannelFromDb(channelId) ?: return null
        return serviceNotification.notificationBuilder.buildNotification(
            context = applicationContext,
            data = FileTransferNotificationData(channel = channel, message = message),
            notificationId = FILE_TRANSFER_NOTIFICATION_ID
        )
    }

    companion object {
        private const val TAG = "UploadAndSendAttachmentWorker"
    }
}

internal fun shouldStartUpload(
    state: TransferState?,
    resumePausedUpload: Boolean,
): Boolean = state != TransferState.PauseUpload || resumePausedUpload

private class UploadedAttachment(
    val attachment: SceytAttachment,
    val encryptionMetadata: Map<String, String>?,
)
