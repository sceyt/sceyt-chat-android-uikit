package com.sceyt.sceytchatuikit.persistence.workers

import android.content.Context
import androidx.work.*
import com.sceyt.sceytchatuikit.SceytKitClient
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageEventsObserver
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.persistence.dao.MessageDao
import com.sceyt.sceytchatuikit.persistence.extensions.resizeImage
import com.sceyt.sceytchatuikit.persistence.extensions.transcodeVideo
import com.sceyt.sceytchatuikit.persistence.filetransfer.FileTransferService
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferTask
import com.sceyt.sceytchatuikit.persistence.logics.messageslogic.MessagesCash
import com.sceyt.sceytchatuikit.persistence.mappers.toMessage
import com.sceyt.sceytchatuikit.persistence.mappers.toSceytMessage
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.inject
import kotlin.coroutines.resume

object SendAttachmentWorkManager {
    const val MESSAGE_TID = "MESSAGE_TID"

    fun schedule(context: Context, messageTid: Long) {
        val dataBuilder = Data.Builder()
        dataBuilder.putLong(MESSAGE_TID, messageTid)

        val myWorkRequest = OneTimeWorkRequest.Builder(SendAttachmentWorker::class.java)
            .addTag(messageTid.toString())
            .setInputData(dataBuilder.build())
            .build()

        WorkManager.getInstance(context).beginUniqueWork(messageTid.toString(), ExistingWorkPolicy.KEEP, myWorkRequest)
            .enqueue()
    }
}

class SendAttachmentWorker(private val context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams), SceytKoinComponent {
    private val fileTransferService: FileTransferService by inject()
    private val messageDao: MessageDao by inject()
    private val messagesCash: MessagesCash by inject()

    private suspend fun checkToUploadAttachmentsBeforeSend(tmpMessage: SceytMessage): Boolean {
        // checkAndResizeMessageAttachments(context, tmpMessage)
        return suspendCancellableCoroutine { continuation ->
            if (tmpMessage.attachments.isNullOrEmpty().not()) {
                tmpMessage.attachments?.forEach { attachment ->
                    fileTransferService.upload(attachment, TransferTask(tmpMessage.tid,
                        state = attachment.transferState,
                        progressCallback = { updateDate ->
                            MessageEventsObserver.emitAttachmentTransferUpdate(updateDate)
                            attachment.transferState = updateDate.state
                            attachment.progressPercent = updateDate.progressPercent
                            messageDao.updateAttachmentTransferProgressAndStateWithMsgTid(updateDate.attachmentTid, updateDate.progressPercent, updateDate.state)
                            messagesCash.updateAttachmentTransferData(updateDate)
                        },
                        resultCallback = { result ->
                            if (result is SceytResponse.Success) {
                                val transferData = TransferData(tmpMessage.tid,
                                    attachment.tid, 100f, TransferState.Uploaded, attachment.filePath, result.data.toString())
                                attachment.updateWithTransferData(transferData)
                                MessageEventsObserver.emitAttachmentTransferUpdate(transferData)
                                messageDao.updateAttachmentAndPayLoad(transferData)
                                messagesCash.updateAttachmentTransferData(transferData)
                            } else {
                                val transferData = TransferData(tmpMessage.tid,
                                    attachment.tid, attachment.progressPercent
                                            ?: 0f, TransferState.PendingUpload, attachment.filePath, null)

                                MessageEventsObserver.emitAttachmentTransferUpdate(transferData)
                                messageDao.updateAttachmentAndPayLoad(transferData)
                                messagesCash.updateAttachmentTransferData(transferData)
                            }
                            continuation.resume(result is SceytResponse.Success)
                        }))
                }
            } else continuation.resume(false)
        }
    }

    private suspend fun checkAndResizeMessageAttachments(context: Context, message: SceytMessage) {
        message.attachments?.forEach { attachment ->
            when (attachment.type) {
                AttachmentTypeEnum.Image.value() -> {
                    attachment.resizeImage(context)
                }
                AttachmentTypeEnum.Video.value() -> {
                    attachment.transcodeVideo(context)
                }
            }
        }
    }

    override suspend fun doWork(): Result {
        val data = inputData
        val messageTid = data.getLong(SendAttachmentWorkManager.MESSAGE_TID, 0)

        val tmpMessage = messageDao.getMessageByTid(messageTid)?.toSceytMessage()
                ?: return Result.success()

        if (checkToUploadAttachmentsBeforeSend(tmpMessage))
            SceytKitClient.getMessagesMiddleWare().sendMessageWithUploadedAttachments(tmpMessage.channelId, tmpMessage.toMessage())

        return Result.success()
    }
}