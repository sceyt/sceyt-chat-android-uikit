package com.sceyt.sceytchatuikit.persistence.workers

import android.content.Context
import androidx.work.*
import com.sceyt.sceytchatuikit.SceytKitClient
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageEventsObserver
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.toAttachment
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.persistence.dao.MessageDao
import com.sceyt.sceytchatuikit.persistence.extensions.resizeImage
import com.sceyt.sceytchatuikit.persistence.extensions.transcodeVideo
import com.sceyt.sceytchatuikit.persistence.filetransfer.FileTransferService
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import com.sceyt.sceytchatuikit.persistence.mappers.toMessage
import com.sceyt.sceytchatuikit.persistence.mappers.toSceytMessage
import com.sceyt.sceytchatuikit.presentation.uicomponents.searchinput.DebounceHelper
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
    private val messageDao: MessageDao by inject()
    private val fileTransferService: FileTransferService by inject()
    private val debounceHelper by lazy { DebounceHelper(200) }

    private suspend fun checkToUploadAttachmentsBeforeSend(tmpMessage: SceytMessage): Boolean {
        checkAndResizeMessageAttachments(context, tmpMessage)

        return suspendCancellableCoroutine { continuation ->
            if (tmpMessage.attachments.isNullOrEmpty().not()) {
                val size = tmpMessage.attachments?.size ?: continuation.resume(false)
                var processedCount = 0
                val successUploadedAttachments = mutableSetOf<Long>()
                tmpMessage.attachments?.forEach {
                    fileTransferService.upload(tmpMessage.tid,
                        it.toAttachment(),
                        progressCallback = { updateDate ->
                            if (it.fileTransferData?.state != TransferState.Uploaded) {
                                MessageEventsObserver.emitAttachmentTransferUpdate(updateDate)
                                it.fileTransferData = updateDate
                                debounceHelper.submitSuspendable {
                                    messageDao.updateAttachmentTransferDataWithTid(updateDate.attachmentTid, updateDate.progressPercent, updateDate.state)
                                }
                            }
                        },
                        resultCallback = { result ->
                            debounceHelper.cancelLastDebounce()
                            if (result is SceytResponse.Success) {
                                val transferData = TransferData(tmpMessage.tid,
                                    it.tid, 100f, TransferState.Uploaded, it.filePath, result.data.toString())
                                it.fileTransferData = transferData
                                it.url = transferData.url
                                MessageEventsObserver.emitAttachmentTransferUpdate(transferData)
                                successUploadedAttachments.add(it.tid)
                                messageDao.updateAttachmentTransferData(it.tid, 100f, TransferState.Uploaded, it.filePath, result.data)
                            } else {
                                val percent = it.fileTransferData?.progressPercent ?: 0f
                                MessageEventsObserver.emitAttachmentTransferUpdate(TransferData(tmpMessage.tid,
                                    it.tid, percent, TransferState.Error, it.filePath, null))
                                messageDao.updateAttachmentTransferData(it.tid, percent,
                                    TransferState.Error, it.filePath, null)
                            }
                            processedCount++
                            if (processedCount == size)
                                continuation.resume(successUploadedAttachments.size == size)
                        })
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

        if (checkToUploadAttachmentsBeforeSend(tmpMessage)) {
            SceytKitClient.getMessagesMiddleWare().sendMessageWithUploadedAttachments(tmpMessage.channelId, tmpMessage.toMessage())
        }
        return Result.success()
    }
}