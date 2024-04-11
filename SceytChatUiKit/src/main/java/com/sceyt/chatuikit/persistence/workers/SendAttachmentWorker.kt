package com.sceyt.chatuikit.persistence.workers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequest
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytKitClient
import com.sceyt.chatuikit.data.connectionobserver.ConnectionEventsObserver
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.extensions.initPendingIntent
import com.sceyt.chatuikit.extensions.isNotNullOrBlank
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.dao.FileChecksumDao
import com.sceyt.chatuikit.persistence.entity.FileChecksumEntity
import com.sceyt.chatuikit.persistence.extensions.safeResume
import com.sceyt.chatuikit.persistence.filetransfer.FileTransferHelper
import com.sceyt.chatuikit.persistence.filetransfer.FileTransferService
import com.sceyt.chatuikit.persistence.filetransfer.TransferData
import com.sceyt.chatuikit.persistence.filetransfer.TransferData.Companion.withPrettySizes
import com.sceyt.chatuikit.persistence.filetransfer.TransferState
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceMessagesLogic
import com.sceyt.chatuikit.persistence.mappers.toMessage
import com.sceyt.chatuikit.persistence.mappers.toTransferData
import com.sceyt.chatuikit.persistence.workers.SendAttachmentWorkManager.IS_SHARING
import com.sceyt.chatuikit.persistence.workers.SendAttachmentWorkManager.MESSAGE_TID
import com.sceyt.chatuikit.persistence.workers.SendAttachmentWorkManager.NOTIFICATION_ID
import com.sceyt.chatuikit.persistence.workers.SendAttachmentWorkManager.UPLOAD_CHANNEL_ID
import com.sceyt.chatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.chatuikit.shared.utils.FileChecksumCalculator
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.inject

object SendAttachmentWorkManager : SceytKoinComponent {

    internal const val MESSAGE_TID = "MESSAGE_TID"
    internal const val IS_SHARING = "IS_SHARING"
    internal const val UPLOAD_CHANNEL_ID = "Sceyt_Upload_Attachment_Channel"
    internal const val NOTIFICATION_ID = 1223344

    fun schedule(context: Context, messageTid: Long, channelId: Long?,
                 workPolicy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP, isSharing: Boolean = false): Operation {
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
    private val fileChecksumDao: FileChecksumDao by inject()

    private suspend fun checkToUploadAttachmentsBeforeSend(tmpMessage: SceytMessage, isSharing: Boolean): Pair<Boolean, String?> {
        val payloads = attachmentLogic.getAllPayLoadsByMsgTid(tmpMessage.tid)
        tmpMessage.attachments?.let { attachments ->
            for (attachment in attachments) {
                if (attachment.type == AttachmentTypeEnum.Link.value())
                    continue

                val payload = payloads.find { it.payLoadEntity.messageTid == attachment.messageTid }?.payLoadEntity
                if (payload?.transferState == TransferState.Uploaded && payload.url.isNotNullOrBlank()) {
                    val transferData = payload.toTransferData(TransferState.Uploaded)
                    attachmentLogic.updateAttachmentWithTransferData(transferData)
                    return Pair(true, payload.url)
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

                    val result = suspendCancellableCoroutine { continuation ->
                        if (attachment.transferState != TransferState.PauseUpload) {

                            val transferData = TransferData(tmpMessage.tid, attachment.progressPercent
                                    ?: 0f, TransferState.WaitingToUpload,
                                attachment.filePath, attachment.url).withPrettySizes(attachment.fileSize)

                            FileTransferHelper.emitAttachmentTransferUpdate(transferData)

                            runBlocking {
                                attachmentLogic.updateAttachmentWithTransferData(transferData)
                            }

                            uploadFile(attachment, continuation, isSharing)
                        }
                    }

                    if (result.first && result.second.isNotNullOrBlank() && checksum != null)
                        fileChecksumDao.updateUrl(checksum, result.second)

                    return result
                }
            }
            return Pair(false, "Could not find any attachment to upload")
        } ?: return Pair(false, "Attachments are empty")
    }

    private fun uploadFile(attachment: SceytAttachment, continuation: CancellableContinuation<Pair<Boolean, String?>>, isSharing: Boolean) {
        if (isSharing) {
            fileTransferService.uploadSharedFile(attachment, FileTransferHelper.createTransferTask(attachment, true).also { task ->
                task.addOnCompletionListener(this.toString(), listener = { success: Boolean, url: String? ->
                    attachment.url = url
                    continuation.safeResume(Pair(success, url))
                })
            })
        } else {
            fileTransferService.upload(attachment, FileTransferHelper.createTransferTask(attachment, true).also { task ->
                task.addOnCompletionListener(this.toString(), listener = { success: Boolean, url: String? ->
                    attachment.url = url
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

        startForeground(tmpMessage.channelId)

        val result = checkToUploadAttachmentsBeforeSend(tmpMessage, isSharing)
        return if (result.first && result.second.isNotNullOrBlank() && !isStopped) {

            ConnectionEventsObserver.awaitToConnectSceyt()
            val response = SceytKitClient.getMessagesMiddleWare().sendMessageWithUploadedAttachments(tmpMessage.channelId, tmpMessage.toMessage())
            if (response is SceytResponse.Success) {
                response.data?.let {
                    messageLogic.attachmentSuccessfullySent(it)
                }
                Result.success()
            } else Result.retry()

        } else Result.failure()
    }

    private suspend fun startForeground(channelId: Long) {
        val notification = creteNotification(channelId)
        val foregroundInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else ForegroundInfo(NOTIFICATION_ID, notification)
        setForeground(foregroundInfo)
    }

    private suspend fun creteNotification(channelId: Long): Notification {
        val notificationBuilder = NotificationCompat.Builder(applicationContext, UPLOAD_CHANNEL_ID)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(UPLOAD_CHANNEL_ID, "Upload Attachments", NotificationManager.IMPORTANCE_MIN)
            NotificationManagerCompat.from(applicationContext).createNotificationChannel(notificationChannel)
        }

        notificationBuilder
            .setContentTitle(applicationContext.getString(R.string.sceyt_sending_attachment))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setSmallIcon(R.drawable.sceyt_ic_upload)

        val clickData = SceytKitConfig.backgroundUploadNotificationClickData
        val channel = if (clickData != null)
            SceytKitClient.getChannelsMiddleWare().getChannelFromDb(channelId) else null

        if (channel != null && clickData != null) {
            val pendingIntent = applicationContext.initPendingIntent(Intent(applicationContext, clickData.classToOpen).apply {
                clickData.channelToParcelKey?.let {
                    putExtra(it, channel)
                }
                clickData.intentFlags?.let {
                    flags = clickData.intentFlags
                }
            })

            notificationBuilder.setContentIntent(pendingIntent)
        }

        return notificationBuilder.build()
    }
}