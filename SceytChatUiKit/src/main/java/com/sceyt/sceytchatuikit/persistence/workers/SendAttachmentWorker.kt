package com.sceyt.sceytchatuikit.persistence.workers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
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
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.SceytKitClient
import com.sceyt.sceytchatuikit.data.connectionobserver.ConnectionEventsObserver
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.extensions.isNotNullOrBlank
import com.sceyt.sceytchatuikit.logger.SceytLog
import com.sceyt.sceytchatuikit.persistence.dao.FileChecksumDao
import com.sceyt.sceytchatuikit.persistence.entity.FileChecksumEntity
import com.sceyt.sceytchatuikit.persistence.extensions.safeResume
import com.sceyt.sceytchatuikit.persistence.filetransfer.FileTransferHelper
import com.sceyt.sceytchatuikit.persistence.filetransfer.FileTransferService
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData.Companion.withPrettySizes
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import com.sceyt.sceytchatuikit.persistence.logics.attachmentlogic.PersistenceAttachmentLogic
import com.sceyt.sceytchatuikit.persistence.logics.messageslogic.PersistenceMessagesLogic
import com.sceyt.sceytchatuikit.persistence.mappers.toMessage
import com.sceyt.sceytchatuikit.persistence.mappers.toTransferData
import com.sceyt.sceytchatuikit.persistence.workers.SendAttachmentWorkManager.IS_SHARING
import com.sceyt.sceytchatuikit.persistence.workers.SendAttachmentWorkManager.MESSAGE_TID
import com.sceyt.sceytchatuikit.persistence.workers.SendAttachmentWorkManager.NOTIFICATION_ID
import com.sceyt.sceytchatuikit.persistence.workers.SendAttachmentWorkManager.UPLOAD_CHANNEL_ID
import com.sceyt.sceytchatuikit.shared.utils.FileResizeUtil.calculateChecksumFor10Mb
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

                val payload = payloads.find { it.messageTid == attachment.messageTid }
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

                    val checksum = calculateChecksumFor10Mb(filePath)

                    if (checksum != null) {
                        val checksumEntity = FileChecksumEntity(checksum, null, null, attachment.metadata, attachment.fileSize)
                        fileChecksumDao.insert(checksumEntity)
                    }

                    val result = suspendCancellableCoroutine { continuation ->
                        if (attachment.transferState != TransferState.PauseUpload) {

                            val transferData = TransferData(tmpMessage.tid, attachment.progressPercent
                                    ?: 0f, TransferState.Preparing,
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
        startForeground()
        val tmpMessage = messageLogic.getMessageDbByTid(messageTid)
                ?: return Result.failure()

        if (tmpMessage.deliveryStatus != DeliveryStatus.Pending)
            return Result.success()


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

    private suspend fun startForeground() {
        val foregroundInfo = ForegroundInfo(NOTIFICATION_ID, creteNotification())
        setForeground(foregroundInfo)
    }

    private fun creteNotification(): Notification {
        val notificationBuilder = NotificationCompat.Builder(applicationContext, UPLOAD_CHANNEL_ID)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(UPLOAD_CHANNEL_ID, "Upload Attachments", NotificationManager.IMPORTANCE_MIN)
            NotificationManagerCompat.from(applicationContext).createNotificationChannel(channel)
        }

        notificationBuilder
            .setContentTitle(applicationContext.getString(R.string.sceyt_sending_attachment))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setSmallIcon(R.drawable.sceyt_ic_upload)

        return notificationBuilder.build()
    }
}