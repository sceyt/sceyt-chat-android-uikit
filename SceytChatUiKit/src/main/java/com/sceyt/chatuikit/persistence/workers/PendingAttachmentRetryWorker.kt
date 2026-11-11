package com.sceyt.chatuikit.persistence.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.await
import com.sceyt.chatuikit.data.constants.SceytConstants.SCEYT_WORKER_TAG
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageDb
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.mappers.isTransferTrackedType
import org.koin.core.component.inject

internal class PendingAttachmentRetryWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams), SceytKoinComponent {
    private val messageDao: MessageDao by inject()

    override suspend fun doWork(): Result {
        val messages = messageDao.getAllPendingMessages().filter(::needsAttachmentRetry)
        SceytLog.i(TAG, "Network is back, retrying pending attachment messages: ${messages.map { it.messageEntity.tid }}")
        messages.forEach { messageDb ->
                val message = messageDb.messageEntity
                UploadAndSendAttachmentWorkManager.schedule(
                    context = applicationContext,
                    messageTid = message.tid,
                    channelId = message.channelId,
                ).await()
            }
        return Result.success()
    }

    private fun needsAttachmentRetry(messageDb: MessageDb): Boolean {
        if (messageDb.messageEntity.forwardingDetailsDb != null)
            return false

        val files = messageDb.attachments.orEmpty().filter {
            isTransferTrackedType(it.attachmentEntity.type)
        }
        if (files.isEmpty())
            return false

        return files.none { it.payLoad?.transferState == TransferState.PauseUpload }
    }

    companion object {
        private const val TAG = "PendingAttachmentRetryWorker"
        private const val WORK_NAME = "PendingAttachmentRetry"

        fun schedule(context: Context): Operation {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequest.Builder(PendingAttachmentRetryWorker::class.java)
                .setConstraints(constraints)
                .addTag(SCEYT_WORKER_TAG)
                .build()

            return WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
