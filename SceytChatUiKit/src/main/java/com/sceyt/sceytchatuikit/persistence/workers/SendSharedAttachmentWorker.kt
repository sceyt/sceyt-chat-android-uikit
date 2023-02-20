package com.sceyt.sceytchatuikit.persistence.workers

import android.content.Context
import androidx.work.*
import com.sceyt.sceytchatuikit.SceytKitClient
import com.sceyt.sceytchatuikit.data.connectionobserver.ConnectionEventsObserver
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.extensions.isNotNullOrBlank
import com.sceyt.sceytchatuikit.persistence.extensions.safeResume
import com.sceyt.sceytchatuikit.persistence.filetransfer.FileTransferHelper
import com.sceyt.sceytchatuikit.persistence.filetransfer.FileTransferService
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import com.sceyt.sceytchatuikit.persistence.logics.attachmentlogic.PersistenceAttachmentLogic
import com.sceyt.sceytchatuikit.persistence.logics.messageslogic.PersistenceMessagesLogic
import com.sceyt.sceytchatuikit.persistence.mappers.toMessage
import com.sceyt.sceytchatuikit.persistence.mappers.toTransferData
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.inject

object SendSharedAttachmentWorkManager {
    const val MESSAGE_TID = "MESSAGE_TID"

    fun schedule(context: Context, messageTid: Long) {
        val dataBuilder = Data.Builder()
        dataBuilder.putLong(MESSAGE_TID, messageTid)

        val networkConstraint = Constraints.Builder().apply {
            setRequiredNetworkType(NetworkType.CONNECTED)
        }.build()

        val myWorkRequest = OneTimeWorkRequest.Builder(SendSharedAttachmentWorker::class.java)
            .addTag(messageTid.toString())
            .setInputData(dataBuilder.build())
            .setConstraints(networkConstraint)
            .build()

        WorkManager.getInstance(context).beginUniqueWork(messageTid.toString(), ExistingWorkPolicy.KEEP, myWorkRequest)
            .enqueue()
    }
}

class SendSharedAttachmentWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams), SceytKoinComponent {
    private val fileTransferService: FileTransferService by inject()
    private val attachmentLogic: PersistenceAttachmentLogic by inject()
    private val messageLogic: PersistenceMessagesLogic by inject()

    private suspend fun checkToUploadAttachmentsBeforeSend(tmpMessage: SceytMessage): Pair<Boolean, String?> {
        val payloads = attachmentLogic.getAllPayLoadsByMsgTid(tmpMessage.tid)

        return suspendCancellableCoroutine { continuation ->
            tmpMessage.attachments?.let { attachments ->
                for (attachment in attachments) {
                    if (attachment.type == AttachmentTypeEnum.Link.value())
                        continue

                    val payload = payloads.find { it.messageTid == attachment.messageTid }
                    if (payload?.transferState == TransferState.Uploaded && payload.url.isNotNullOrBlank()) {
                        val transferData = payload.toTransferData(attachment.tid, TransferState.Uploaded)
                        attachmentLogic.updateAttachmentWithTransferData(transferData)
                        continuation.safeResume(Pair(true, payload.url))
                    } else {
                        val transferData = TransferData(tmpMessage.tid, attachment.tid, 0f,
                            TransferState.Uploading, attachment.filePath, attachment.url)
                        attachmentLogic.updateAttachmentWithTransferData(transferData)

                        fileTransferService.uploadSharedFile(attachment, FileTransferHelper.createTransferTask(attachment, true).also { task ->
                            task.addOnCompletionListener(this.toString(), listener = { success: Boolean, url: String? ->
                                continuation.safeResume(Pair(success, url))
                            })
                        })
                    }
                }
            } ?: kotlin.run {
                continuation.safeResume(Pair(false, null))
            }
        }
    }

    override suspend fun doWork(): Result {
        val data = inputData
        val messageTid = data.getLong(SendSharedAttachmentWorkManager.MESSAGE_TID, 0)

        val tmpMessage = messageLogic.getMessageFromDbByTid(messageTid)
                ?: return Result.failure()

        val result = checkToUploadAttachmentsBeforeSend(tmpMessage)
        if (result.first && result.second.isNotNullOrBlank()) {
            tmpMessage.attachments?.getOrNull(0)?.url = result.second

            ConnectionEventsObserver.awaitToConnectSceyt()
            val response = SceytKitClient.getMessagesMiddleWare().sendMessageWithUploadedAttachments(tmpMessage.channelId, tmpMessage.toMessage())
            return if (response is SceytResponse.Success) {
                response.data?.let {
                    messageLogic.attachmentSuccessfullySent(it)
                }
                Result.success()
            } else Result.retry()
        }

        return Result.success()
    }
}