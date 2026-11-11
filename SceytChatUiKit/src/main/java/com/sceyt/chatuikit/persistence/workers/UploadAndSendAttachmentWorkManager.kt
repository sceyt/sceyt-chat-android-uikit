package com.sceyt.chatuikit.persistence.workers

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.Operation
import androidx.work.WorkManager
import com.sceyt.chatuikit.data.constants.SceytConstants.SCEYT_WORKER_TAG

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