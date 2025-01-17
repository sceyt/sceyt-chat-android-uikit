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
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.managers.connection.ConnectionEventManager
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.extensions.hasPermissions
import com.sceyt.chatuikit.extensions.isNotNullOrBlank
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.notifications.FileTransferNotificationData
import com.sceyt.chatuikit.persistence.database.dao.FileChecksumDao
import com.sceyt.chatuikit.persistence.database.entity.FileChecksumEntity
import com.sceyt.chatuikit.persistence.extensions.safeResume
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferHelper
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceChannelsLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceMessagesLogic
import com.sceyt.chatuikit.persistence.mappers.toMessage
import com.sceyt.chatuikit.persistence.mappers.toTransferData
import com.sceyt.chatuikit.persistence.workers.SendAttachmentWorkManager.IS_SHARING
import com.sceyt.chatuikit.persistence.workers.SendAttachmentWorkManager.MESSAGE_TID
import com.sceyt.chatuikit.persistence.workers.SendAttachmentWorkManager.NOTIFICATION_ID
import com.sceyt.chatuikit.shared.utils.FileChecksumCalculator
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.inject

object SendAttachmentWorkManager : SceytKoinComponent {

    internal const val MESSAGE_TID = "MESSAGE_TID"
    internal const val IS_SHARING = "IS_SHARING"
    internal const val NOTIFICATION_ID = 1223344

    fun schedule(
            context: Context,
            messageTid: Long,
            channelId: Long?,
            workPolicy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP,
            isSharing: Boolean = false,
    ): Operation {
        val dataBuilder = Data.Builder()
        dataBuilder.putLong(MESSAGE_TID, messageTid)
        dataBuilder.putBoolean(IS_SHARING, isSharing)

        val myWorkRequest = OneTimeWorkRequest.Builder(SendAttachmentWorker::class.java)
            .addTag(messageTid.toString())
            .apply { channelId?.let { addTag(channelId.toString()) } }
            .setInputData(dataBuilder.build())
            .build()

        return WorkManager.getInstance(context).beginUniqueWork(messageTid.toString(), workPolicy, myWorkRequest)
            .enqueue()
    }

    fun cancelWorksByTag(context: Context, tag: String) {
        WorkManager.getInstance(context).cancelAllWorkByTag(tag)
    }
}

class SendAttachmentWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams), SceytKoinComponent {
    private val fileTransferService: FileTransferService by inject()
    private val attachmentLogic: PersistenceAttachmentLogic by inject()
    private val messageLogic: PersistenceMessagesLogic by inject()
    private val channelLogic: PersistenceChannelsLogic by inject()
    private val fileChecksumDao: FileChecksumDao by inject()

    private suspend fun checkToUploadAttachmentsBeforeSend(tmpMessage: SceytMessage, isSharing: Boolean): kotlin.Result<List<SceytAttachment>> {
        val payloads = attachmentLogic.getAllPayLoadsByMsgTid(tmpMessage.tid)
        val attachments = tmpMessage.attachments?.toMutableList()
                ?: return kotlin.Result.failure(Exception("Attachments not found"))

        for ((index, attachment) in attachments.withIndex()) {
            if (attachment.type == AttachmentTypeEnum.Link.value)
                continue

            val payload = payloads.find { it.payLoadEntity.messageTid == attachment.messageTid }?.payLoadEntity
            if (payload != null && (payload.transferState == TransferState.Uploaded || payload.url.isNotNullOrBlank())) {
                val transferData = payload.toTransferData(TransferState.Uploaded, 100f)
                attachmentLogic.updateAttachmentWithTransferData(transferData)
                attachments[index] = attachment.copy(url = payload.url)
                return kotlin.Result.success(attachments)
            } else {
                val filePath = attachment.originalFilePath ?: attachment.filePath
                if (filePath.isNullOrEmpty()) {
                    SceytLog.i(TAG, "Skip uploading a file path is null or empty")
                    continue
                }

                val checksum = FileChecksumCalculator.calculateFileChecksum(filePath)

                if (checksum != null) {
                    val checksumEntity = FileChecksumEntity(checksum, null, null, attachment.metadata, attachment.fileSize)
                    fileChecksumDao.insert(checksumEntity)
                }

                val (success, url) = suspendCancellableCoroutine { continuation ->
                    if (attachment.transferState != TransferState.PauseUpload) {

                        val transferData = TransferData(tmpMessage.tid, attachment.progressPercent
                                ?: 0f, TransferState.WaitingToUpload,
                            attachment.filePath, attachment.url)

                        FileTransferHelper.emitAttachmentTransferUpdate(transferData, attachment.fileSize)

                        runBlocking {
                            attachmentLogic.updateAttachmentWithTransferData(transferData)
                        }

                        uploadFile(attachment, continuation, isSharing)
                    }
                }

                if (success && url.isNotNullOrBlank()) {
                    if (checksum != null)
                        fileChecksumDao.updateUrl(checksum, url)
                    attachments[index] = attachment.copy(url = url)
                    return kotlin.Result.success(attachments)
                } else
                    return kotlin.Result.failure(Exception("Could not upload file"))
            }
        }
        return kotlin.Result.failure(Exception("Could not find any attachment to upload"))
    }

    private fun uploadFile(attachment: SceytAttachment, continuation: CancellableContinuation<Pair<Boolean, String?>>, isSharing: Boolean) {
        if (isSharing) {
            fileTransferService.uploadSharedFile(attachment, FileTransferHelper.createTransferTask(attachment).also { task ->
                task.addOnCompletionListener(this.toString(), listener = { success: Boolean, url: String? ->
                    continuation.safeResume(Pair(success, url))
                })
            })
        } else {
            fileTransferService.upload(attachment, FileTransferHelper.createTransferTask(attachment).also { task ->
                task.addOnCompletionListener(this.toString(), listener = { success: Boolean, url: String? ->
                    continuation.safeResume(Pair(success, url))
                })
            })
        }
    }

    override suspend fun doWork(): Result {
        val data = inputData
        val messageTid = data.getLong(MESSAGE_TID, 0)
        val isSharing = data.getBoolean(IS_SHARING, false)
        val tmpMessage = messageLogic.getMessageDbByTid(messageTid)
                ?: return Result.failure()

        if (tmpMessage.deliveryStatus != DeliveryStatus.Pending)
            return Result.success()

        if (applicationContext.hasPermissions(android.Manifest.permission.FOREGROUND_SERVICE))
            startForeground(tmpMessage.channelId, tmpMessage)

        val result = checkToUploadAttachmentsBeforeSend(tmpMessage, isSharing)
        return if (result.isSuccess && !isStopped) {
            val messageToSend = tmpMessage.copy(attachments = result.getOrThrow()).toMessage()

            ConnectionEventManager.awaitToConnectSceyt()
            val response = messageLogic.sendMessageWithUploadedAttachments(tmpMessage.channelId, messageToSend)
            if (response is SceytResponse.Success) {
                response.data?.let {
                    messageLogic.attachmentSuccessfullySent(it)
                }
                Result.success()
            } else Result.retry()

        } else Result.failure()
    }

    private suspend fun startForeground(channelId: Long, message: SceytMessage) {
        val notification = creteNotification(channelId, message) ?: return
        val foregroundInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else ForegroundInfo(NOTIFICATION_ID, notification)
        setForeground(foregroundInfo)
    }

    private suspend fun creteNotification(channelId: Long, message: SceytMessage): Notification? {
        val serviceNotification = SceytChatUIKit.notifications.fileTransferServiceNotification
        val channel = channelLogic.getChannelFromDb(channelId) ?: return null
        return serviceNotification.notificationBuilder.buildNotification(
            context = applicationContext,
            data = FileTransferNotificationData(channel = channel, message = message)
        )
    }
}