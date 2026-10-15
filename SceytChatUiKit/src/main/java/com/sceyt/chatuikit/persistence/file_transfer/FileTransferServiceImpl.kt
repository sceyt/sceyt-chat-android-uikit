package com.sceyt.chatuikit.persistence.file_transfer

import android.content.Context
import androidx.work.WorkManager
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ErrorUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PauseUpload
import com.sceyt.chatuikit.persistence.logic.FileTransferLogic
import com.sceyt.chatuikit.persistence.workers.UploadAndSendAttachmentWorkManager
import kotlinx.coroutines.CancellationException
import java.util.concurrent.ConcurrentHashMap

internal class FileTransferServiceImpl(
        private var context: Context,
        private var fileTransferLogic: FileTransferLogic,
) : FileTransferService {
    private val tasksMap = ConcurrentHashMap<String, TransferTask>()

    override fun upload(
        attachment: SceytAttachment,
        configureTask: TransferTask.() -> Unit,
    ) = startTransfer(attachment, configureTask, fileTransferLogic::uploadFile)

    override fun uploadSharedFile(
        attachment: SceytAttachment,
        configureTask: TransferTask.() -> Unit,
    ) = startTransfer(attachment, configureTask, fileTransferLogic::uploadSharedFile)

    override fun download(
        attachment: SceytAttachment,
        configureTask: TransferTask.() -> Unit,
    ) = startTransfer(attachment, configureTask, fileTransferLogic::downloadFile)

    override fun pause(messageTid: Long, attachment: SceytAttachment, state: TransferState) {
        fileTransferLogic.pauseLoad(attachment, state)
    }

    override fun cancelUpload(attachment: SceytAttachment) {
        val task = findTransferTask(attachment)
        task?.let(::removeTransferTask)
        task?.uploadSubscription?.cancel()
        task?.uploadSubscription = null
        task?.downloadSubscription?.cancel()
        task?.downloadSubscription = null
        fileTransferLogic.cancelUpload(attachment)
        task?.complete(Result.failure(CancellationException("Attachment upload cancelled")))
    }

    override fun resume(messageTid: Long, attachment: SceytAttachment, state: TransferState) {
        val workInfo = WorkManager.getInstance(context).getWorkInfosByTag(messageTid.toString())
        if ((state == PauseUpload || state == ErrorUpload) && workInfo.get().all { it.state.isFinished })
            UploadAndSendAttachmentWorkManager.schedule(
                context = context,
                messageTid = messageTid,
                channelId = null,
                resumePausedUpload = true,
            )
        else
            fileTransferLogic.resumeLoad(attachment, state)
    }

    override fun getThumb(messageTid: Long, attachment: SceytAttachment, thumbData: ThumbData) {
        fileTransferLogic.getAttachmentThumb(messageTid, attachment, thumbData)
    }

    override fun findOrCreateTransferTask(attachment: SceytAttachment): TransferTask {
        return tasksMap.computeIfAbsent(attachment.transferKey) {
            FileTransferHelper.createTransferTask(attachment)
        }
    }

    override fun findTransferTask(attachment: SceytAttachment): TransferTask? {
        return tasksMap[attachment.transferKey]
    }

    override fun removeTransferTask(task: TransferTask) {
        tasksMap.entries.removeIf { it.value === task }
    }

    override fun getTasks(): Map<String, TransferTask> = tasksMap

    override fun clearPreparingThumbPaths() {
        fileTransferLogic.clearPreparingThumbPaths()
    }

    override fun cancelAllTransfers() {
        fileTransferLogic.cancelAll()
        val tasks = tasksMap.values.toList()
        tasksMap.clear()
        tasks.forEach { it.complete(Result.failure(CancellationException("File transfers cancelled"))) }
    }

    private fun startTransfer(
        attachment: SceytAttachment,
        configureTask: TransferTask.() -> Unit,
        start: (SceytAttachment, TransferTask) -> Unit,
    ): TransferTask {
        val task = findOrCreateTransferTask(attachment)
        task.configureTask()
        start(attachment, task)
        return task
    }
}

fun interface TransferResultCallback {
    fun onResult(sceytResponse: SceytResponse<String>)
}

fun interface ProgressUpdateCallback {
    fun onProgress(date: TransferData)
}

fun interface PreparingCallback {
    fun onPreparing(date: TransferData)
}

fun interface ResumePauseCallback {
    fun onResumePause(date: TransferData)
}

fun interface UpdateFileLocationCallback {
    fun onUpdateFileLocation(path: String)
}

fun interface ThumbCallback {
    fun onThumb(path: String, thumbData: ThumbData)
}
