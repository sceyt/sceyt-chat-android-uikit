package com.sceyt.chatuikit.persistence.workers

import android.content.Context
import androidx.work.WorkInfo
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

internal class AttachmentSendOrder(
    private val context: Context,
    private val messageDao: MessageDao,
) {
    suspend fun awaitPreviousUploads(messageTid: Long) {
        val previousUploads = messageDao.getPreviousPendingMessageTids(messageTid).map { previousTid ->
            UploadAndSendAttachmentWorkManager.observeWork(context, previousTid)
        }
        if (previousUploads.isEmpty()) return

        combine(previousUploads) { works -> works.none { it.hasUnfinishedWork() } }.first { it }
    }

    private fun List<WorkInfo>.hasUnfinishedWork(): Boolean = any { !it.state.isFinished }
}
