package com.sceyt.chatuikit.persistence.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sceyt.chatuikit.di.SceytKoinComponent
import com.sceyt.chatuikit.persistence.logics.messageslogic.PersistenceMessagesLogic
import com.sceyt.chatuikit.persistence.mappers.toMessage
import org.koin.core.component.inject

object SendForwardMessagesWorkManager : SceytKoinComponent {

    internal const val CHANNEL_ID = "CHANNEL_ID"
    internal const val MESSAGE_TID = "MESSAGE_TID"

    fun schedule(context: Context, channelId: Long, vararg messageTid: Long): Operation {
        val dataBuilder = Data.Builder()
        dataBuilder.putLong(CHANNEL_ID, channelId)
        dataBuilder.putLongArray(MESSAGE_TID, messageTid)

        val myWorkRequest = OneTimeWorkRequest.Builder(SendForwardMessagesWorker::class.java)
            .setInputData(dataBuilder.build())
            .addTag(channelId.toString())
            .build()

        return WorkManager.getInstance(context).beginWith(myWorkRequest).enqueue()
    }

    fun cancelWorksByTag(context: Context, tag: String) {
        WorkManager.getInstance(context).cancelAllWorkByTag(tag)
    }
}

class SendForwardMessagesWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams), SceytKoinComponent {
    private val messageLogic: PersistenceMessagesLogic by inject()

    override suspend fun doWork(): Result {
        val data = inputData
        val channelId = data.getLong(SendForwardMessagesWorkManager.CHANNEL_ID, 0L)
        if (channelId == 0L) return Result.failure()
        val tIds = data.getLongArray(SendForwardMessagesWorkManager.MESSAGE_TID)
                ?: return Result.failure()

        val messages = messageLogic.getMessagesDbByTid(tIds.toList()).sortedBy { it.createdAt }
        messages.forEach {
            messageLogic.sendMessageWithUploadedAttachments(channelId, it.toMessage())
        }
        return Result.success()
    }
}
