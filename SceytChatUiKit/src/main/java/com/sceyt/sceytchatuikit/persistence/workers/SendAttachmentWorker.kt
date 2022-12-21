package com.sceyt.sceytchatuikit.persistence.workers

import android.content.Context
import androidx.work.*
import com.sceyt.sceytchatuikit.SceytKitClient
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.extensions.isNotNullOrBlank
import com.sceyt.sceytchatuikit.persistence.dao.MessageDao
import com.sceyt.sceytchatuikit.persistence.extensions.safeResume
import com.sceyt.sceytchatuikit.persistence.filetransfer.FileTransferHelper
import com.sceyt.sceytchatuikit.persistence.filetransfer.FileTransferService
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import com.sceyt.sceytchatuikit.persistence.logics.messageslogic.MessagesCash
import com.sceyt.sceytchatuikit.persistence.mappers.toMessage
import com.sceyt.sceytchatuikit.persistence.mappers.toSceytMessage
import com.sceyt.sceytchatuikit.persistence.mappers.toTransferData
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.inject

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

class SendAttachmentWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams), SceytKoinComponent {
    private val fileTransferService: FileTransferService by inject()
    private val messageDao: MessageDao by inject()
    private val messagesCash: MessagesCash by inject()

    private suspend fun checkToUploadAttachmentsBeforeSend(tmpMessage: SceytMessage): Boolean {
        val payloads = messageDao.getAllPayLoadsByMsgTid(tmpMessage.tid)

        return suspendCancellableCoroutine { continuation ->
            if (tmpMessage.attachments.isNullOrEmpty().not()) {
                tmpMessage.attachments?.forEach { attachment ->
                    val payload = payloads.find { it.messageTid == attachment.messageTid }
                    if (payload?.transferState == TransferState.Uploaded && payload.url.isNotNullOrBlank()) {
                        val transferData = payload.toTransferData(attachment.tid, TransferState.Uploaded)
                        messageDao.updateAttachmentAndPayLoad(transferData)
                        messagesCash.updateAttachmentTransferData(transferData)
                        continuation.safeResume(true)
                    } else {
                        fileTransferService.upload(attachment, FileTransferHelper.createTransferTask(attachment, true).also { task ->
                            task.addOnCompletionListener(this.toString()) {
                                continuation.safeResume(it)
                            }
                        })
                    }
                }
            } else continuation.safeResume(false)
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