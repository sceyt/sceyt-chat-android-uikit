package com.sceyt.chatuikit.persistence.workers

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequest
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sceyt.chat.models.message.Message
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.constants.SceytConstants
import com.sceyt.chatuikit.data.constants.SceytConstants.SCEYT_WORKER_TAG
import com.sceyt.chatuikit.data.managers.connection.ConnectionEventManager
import com.sceyt.chatuikit.data.models.fold
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.extensions.hasPermissions
import com.sceyt.chatuikit.extensions.isNotNullOrBlank
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.notifications.service.FileTransferNotificationData
import com.sceyt.chatuikit.persistence.database.dao.FileChecksumDao
import com.sceyt.chatuikit.persistence.database.entity.FileChecksumEntity
import com.sceyt.chatuikit.persistence.extensions.safeResume
import com.sceyt.chatuikit.persistence.extensions.withMetadata
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
import com.sceyt.chatuikit.persistence.workers.UploadAndSendAttachmentWorkManager.FILE_TRANSFER_NOTIFICATION_ID
import com.sceyt.chatuikit.persistence.workers.UploadAndSendAttachmentWorkManager.IS_SHARING
import com.sceyt.chatuikit.persistence.workers.UploadAndSendAttachmentWorkManager.MESSAGE_TID
import com.sceyt.chatuikit.persistence.workers.UploadAndSendAttachmentWorkManager.RESUME_PAUSED_UPLOAD
import com.sceyt.chatuikit.presentation.extensions.isNotPending
import com.sceyt.chatuikit.shared.utils.FileChecksumCalculator
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.seconds
import kotlin.Result as KtResult

object UploadAndSendAttachmentWorkManager {

    internal const val MESSAGE_TID = "MESSAGE_TID"
    internal const val IS_SHARING = "IS_SHARING"
    internal const val RESUME_PAUSED_UPLOAD = "RESUME_PAUSED_UPLOAD"
    const val FILE_TRANSFER_NOTIFICATION_ID = 1223344

    fun schedule(
        context: Context,
        messageTid: Long,
        channelId: Long?,
        workPolicy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP,
        isSharing: Boolean = false,
        resumePausedUpload: Boolean = false,
    ): Operation {
        val dataBuilder = Data.Builder()
        dataBuilder.putLong(MESSAGE_TID, messageTid)
        dataBuilder.putBoolean(IS_SHARING, isSharing)
        dataBuilder.putBoolean(RESUME_PAUSED_UPLOAD, resumePausedUpload)

        val workRequest = OneTimeWorkRequest.Builder(UploadAndSendAttachmentWorker::class.java)
            .addTag(messageTid.toString())
            .apply { channelId?.let { addTag(channelId.toString()) } }
            .addTag(SCEYT_WORKER_TAG)
            .setInputData(dataBuilder.build())
            .build()

        return WorkManager.getInstance(context)
            .beginUniqueWork(messageTid.toString(), workPolicy, workRequest)
            .enqueue()
    }

    fun cancelWorksByTag(context: Context, tag: String): Operation {
        return WorkManager.getInstance(context).cancelAllWorkByTag(tag)
    }
}

class UploadAndSendAttachmentWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), SceytKoinComponent {
    private val fileTransferService: FileTransferService by inject()
    private val attachmentLogic: PersistenceAttachmentLogic by inject()
    private val messageLogic: PersistenceMessagesLogic by inject()
    private val channelLogic: PersistenceChannelsLogic by inject()
    private val fileChecksumDao: FileChecksumDao by inject()
    private val previewTransfer: AttachmentPreviewTransfer by inject()

    // A retried worker must respect a user pause made after the explicit resume request.
    private val resumePausedUpload: Boolean
        get() = inputData.getBoolean(RESUME_PAUSED_UPLOAD, false) && runAttemptCount == 0

    private suspend fun uploadAttachmentsBeforeSend(
        message: SceytMessage,
        isSharing: Boolean,
        resumePausedUpload: Boolean,
    ): KtResult<List<SceytAttachment>> {
        val payloads = attachmentLogic.getAllPayLoadsByMsgTid(message.tid)
        val attachments = message.attachments?.toMutableList()
            ?: return KtResult.failure(Exception("Attachments not found"))

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
            if (!shouldStartUpload(attachment.transferState, resumePausedUpload)) {
                return KtResult.failure(Exception("Attachment upload is paused"))
            }

            val transferData = TransferData(
                messageTid = message.tid,
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

            val result = suspendCancellableCoroutine { continuation ->
                uploadFile(attachment, continuation, isSharing)
            }
            return result.map { uploadedAttachment ->
                // Encrypted URLs require matching keys and are excluded from checksum URL reuse.
                if (checksum != null && !SceytChatUIKit.fileTransfer.encryptionEnabled)
                    fileChecksumDao.updateUrl(checksum, uploadedAttachment.url)

                attachments[index] = uploadedAttachment
                attachments
            }
        }
        return KtResult.failure(Exception("Could not find any attachment to upload"))
    }

    private suspend fun uploadPreviewIfNeeded(
        message: SceytMessage,
        attachments: MutableList<SceytAttachment>,
        resumePausedUpload: Boolean,
    ): KtResult<Unit> {
        val video = attachments.find { it.type == AttachmentTypeEnum.Video.value }
            ?: return KtResult.success(Unit)

        if (message.isForwarded || attachments.any { it.isPreview() })
            return KtResult.success(Unit)

        if (!shouldStartUpload(video.transferState, resumePausedUpload))
            return KtResult.success(Unit)

        return when (val result = previewTransfer.upload(video)) {
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

    private fun List<SceytAttachment>.withPreviewLink(): List<SceytAttachment> {
        val previewUrl =
            firstOrNull { it.isPreview() }?.url?.takeIf { it.isNotBlank() } ?: return this
        val link = mapOf(SceytConstants.Preview to previewUrl)
        return map {
            if (it.type == AttachmentTypeEnum.Video.value) it.copy(
                metadata = it.metadata.withMetadata(
                    link
                )
            )
            else it
        }
    }

    private fun uploadFile(
        attachment: SceytAttachment,
        continuation: CancellableContinuation<KtResult<SceytAttachment>>,
        isSharing: Boolean
    ) {
        // Cancellation can happen before the worker reaches task registration.
        if (!continuation.isActive) return

        val existingTask = fileTransferService.findTransferTask(attachment)
        val completionKey = this.toString()
        val configureTask: TransferTask.() -> Unit = {
            isSharedUpload = isSharedUpload || isSharing
            if (state == TransferState.PauseUpload && resumePausedUpload) {
                state = TransferState.WaitingToUpload
            }
            addOnCompletionListener(completionKey, continuation::safeResume)
            continuation.invokeOnCancellation {
                removeOnCompletionListener(completionKey)

                // A worker that joined an existing task only removes its own listener.
                if (existingTask != null) return@invokeOnCancellation

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

    override suspend fun doWork(): Result {
        val data = inputData
        val messageTid = data.getLong(MESSAGE_TID, 0)
        val isSharing = data.getBoolean(IS_SHARING, false)
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

        val attachments = result.getOrThrow().withPreviewLink()
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

    private fun finishWorkWithError(error: String): Result {
        // UIKit controls retries through pending messages, so errors also finish this work successfully.
        val result = Result.success()
        SceytLog.e(TAG, "SendAttachmentWorker worker finished with error: $error")
        SceytChatUIKit.notifications.fileTransferServiceNotification.notificationHandler
            .serviceWorkerFinished(result = result)
        return result
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
        setForeground(foregroundInfo)
    }

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
