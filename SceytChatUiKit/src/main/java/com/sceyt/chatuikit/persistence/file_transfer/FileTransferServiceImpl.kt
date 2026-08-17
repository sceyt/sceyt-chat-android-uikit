package com.sceyt.chatuikit.persistence.file_transfer

import android.content.Context
import androidx.work.WorkManager
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ErrorUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PauseUpload
import com.sceyt.chatuikit.persistence.logic.FileTransferLogic
import com.sceyt.chatuikit.persistence.workers.UploadAndSendAttachmentWorkManager
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

    override fun resume(messageTid: Long, attachment: SceytAttachment, state: TransferState) {
        val workInfo = WorkManager.getInstance(context).getWorkInfosByTag(messageTid.toString())
        if ((state == PauseUpload || state == ErrorUpload) && (workInfo.get().isEmpty() || workInfo.isCancelled))
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
        return tasksMap.computeIfAbsent(attachment.messageTid.toString()) {
            FileTransferHelper.createTransferTask(attachment)
        }
    }

    override fun findTransferTask(attachment: SceytAttachment): TransferTask? {
        return tasksMap[attachment.messageTid.toString()]
    }

    override fun removeTransferTask(messageTid: Long) {
        tasksMap.remove(messageTid.toString())
    }

    override fun getTasks(): Map<String, TransferTask> = tasksMap

    override fun clearPreparingThumbPaths() {
        fileTransferLogic.clearPreparingThumbPaths()
    }

    override fun cancelAllTransfers() {
        fileTransferLogic.cancelAll()
        tasksMap.clear()
    }

    private fun startTransfer(
        attachment: SceytAttachment,
        configureTask: TransferTask.() -> Unit,
        start: (SceytAttachment, TransferTask) -> Unit,
    ): TransferTask {
        val task = findOrCreateTransferTask(attachment)
        task.configureTask()
        start(task.attachment, task)
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
