package com.sceyt.chatuikit.persistence.file_transfer

import android.content.Context
import androidx.work.ExistingWorkPolicy
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Downloading
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ErrorDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ErrorUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.FilePathChanged
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PauseDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PauseUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PendingDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PendingUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Preparing
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Uploaded
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Uploading
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.WaitingToUpload
import com.sceyt.chatuikit.persistence.logicimpl.AttachmentDownloadCoordinator
import com.sceyt.chatuikit.persistence.logicimpl.AttachmentThumbCoordinator
import com.sceyt.chatuikit.persistence.logicimpl.AttachmentUploadCoordinator
import com.sceyt.chatuikit.persistence.mappers.toTransferData
import com.sceyt.chatuikit.persistence.workers.UploadAndSendAttachmentWorkManager
import kotlinx.coroutines.CancellationException
import java.util.concurrent.ConcurrentHashMap

internal class FileTransferServiceImpl(
    private val context: Context,
    private val uploader: AttachmentUploadCoordinator,
    private val downloader: AttachmentDownloadCoordinator,
    private val thumbs: AttachmentThumbCoordinator,
) : FileTransferService {
    private val tasksMap = ConcurrentHashMap<String, TransferTask>()

    override fun upload(
        attachment: SceytAttachment,
        configureTask: TransferTask.() -> Unit,
    ) = startTransfer(attachment, configureTask) { value, task ->
        startUpload(value, task, shared = false)
    }

    override fun uploadSharedFile(
        attachment: SceytAttachment,
        configureTask: TransferTask.() -> Unit,
    ) = startTransfer(attachment, configureTask) { value, task ->
        startUpload(value, task, shared = true)
    }

    override fun download(
        attachment: SceytAttachment,
        configureTask: TransferTask.() -> Unit,
    ) = startTransfer(attachment, configureTask) { value, task ->
        downloader.download(value, task, onResult = { completeTransfer(task, it, upload = false) })
    }

    override fun redownload(attachment: SceytAttachment) {
        findTransferTask(attachment)?.let { task ->
            if (task.state == PendingDownload || task.state == Downloading || task.state == PauseDownload) return
            removeTransferTask(task)
        }
        val pendingAttachment = attachment.copy(
            transferState = PendingDownload,
            progressPercent = 0f,
            filePath = null
        )
        FileTransferHelper.resetToPendingDownload(pendingAttachment)
        download(pendingAttachment)
    }

    override fun pause(messageTid: Long, attachment: SceytAttachment, state: TransferState) {
        when (state) {
            PendingDownload, Downloading -> pauseDownload(attachment)
            PendingUpload, WaitingToUpload, Preparing, FilePathChanged, Uploading -> {
                pauseUpload(attachment, state)
            }

            else -> Unit
        }
    }

    override fun cancelUpload(attachment: SceytAttachment) {
        val task = findTransferTask(attachment)
        task?.isCancelled = true
        task?.let(::removeTransferTask)
        task?.let(uploader::cancel)
        downloader.cancel(attachment)
        task?.complete(Result.failure(CancellationException("Attachment upload cancelled")))
    }

    override fun resume(
        messageTid: Long,
        attachment: SceytAttachment,
        state: TransferState,
        channelId: Long?
    ) {
        if (state == PendingDownload || state == PauseDownload || state == ErrorDownload) {
            val task = findOrCreateTransferTask(attachment)
            val onResult: (SceytResponse<String>) -> Unit =
                { completeTransfer(task, it, upload = false) }
            if (state == PauseDownload) downloader.resume(attachment, task, onResult)
            else {
                if (state == ErrorDownload) downloader.cancel(attachment)
                downloader.download(attachment, task, onResult, isResuming = true)
            }
            return
        }
        if (state == PauseUpload || state == ErrorUpload) {
            // Replace even a finishing worker: pause has already released its completion listener.
            UploadAndSendAttachmentWorkManager.schedule(
                context = context,
                messageTid = messageTid,
                channelId = channelId,
                workPolicy = ExistingWorkPolicy.REPLACE,
                resumePausedUpload = true,
            )
        } else if (state == PendingUpload) {
            val task = findOrCreateTransferTask(attachment)
            task.state = WaitingToUpload
            task.resumePauseCallback?.onResumePause(attachment.toTransferData(WaitingToUpload))
            startUpload(attachment, task, task.isSharedUpload)
        }
    }

    override fun getThumb(messageTid: Long, attachment: SceytAttachment, thumbData: ThumbData) {
        thumbs.getAttachmentThumb(messageTid, attachment, thumbData)
    }

    override fun findOrCreateTransferTask(attachment: SceytAttachment): TransferTask {
        val key = if (!tasksMap.containsKey(attachment.transferKey) && tasksMap.containsKey(attachment.urlTransferKey))
            attachment.urlTransferKey else attachment.transferKey
        return tasksMap.computeIfAbsent(key) {
            FileTransferHelper.createTransferTask(attachment)
        }
    }

    override fun findTransferTask(attachment: SceytAttachment): TransferTask? {
        return tasksMap[attachment.transferKey] ?: tasksMap[attachment.urlTransferKey]
    }

    override fun removeTransferTask(task: TransferTask) {
        tasksMap.entries.removeIf { it.value === task }
    }

    override fun getTasks(): Map<String, TransferTask> = tasksMap

    override fun clearPreparingThumbPaths() {
        thumbs.clearPreparingThumbPaths()
    }

    override fun cancelAllTransfers() {
        val tasks = tasksMap.values.toList()
        tasks.forEach { it.isCancelled = true }
        downloader.cancelAll()
        uploader.cancelAll()
        thumbs.clear()
        tasksMap.clear()
        tasks.forEach {
            it.complete(Result.failure(CancellationException("File transfers cancelled")))
        }
    }

    internal fun completeUpload(task: TransferTask, response: SceytResponse<String>) {
        completeTransfer(task, response, upload = true)
    }

    private fun pauseUpload(attachment: SceytAttachment, state: TransferState) {
        val existing = findTransferTask(attachment)
        // Only pending/queued uploads can be paused before the worker registers a task.
        if (existing == null && state != PendingUpload && state != WaitingToUpload) return
        if (existing == null && AttachmentTransferStateStore.getTransferData(attachment)?.state?.isCompleted() == true) return
        val task = existing ?: findOrCreateTransferTask(attachment)
        uploader.pause(task) {
            if (findTransferTask(attachment) === task) finishPausedUpload(task)
        }
    }

    private fun pauseDownload(attachment: SceytAttachment) {
        val task = findTransferTask(attachment) ?: return
        if (downloader.pause(attachment) || task.state == PendingDownload) {
            if (findTransferTask(attachment) !== task) return
            task.state = PauseDownload
            runCatching {
                task.resumePauseCallback?.onResumePause(task.attachment.toTransferData(PauseDownload))
            }.onFailure { SceytLog.e(TAG, "Download pause callback failed", it) }
        }
    }

    private fun startUpload(attachment: SceytAttachment, task: TransferTask, shared: Boolean) {
        if (shared) task.isSharedUpload = true
        if (shared)
            uploader.uploadSharedFile(attachment, task, ::completeUpload, ::finishPausedUpload)
        else uploader.uploadFile(attachment, task, ::completeUpload, ::finishPausedUpload)
    }

    private fun finishPausedUpload(task: TransferTask) {
        if (findTransferTask(task.attachment) !== task) return
        task.state = PauseUpload
        runCatching {
            task.resumePauseCallback?.onResumePause(task.attachment.toTransferData(PauseUpload))
        }.onFailure { SceytLog.e(TAG, "Upload pause callback failed", it) }
        // Release this worker without failing the attachment or discarding its shared/native upload.
        task.complete(Result.failure(IllegalStateException("Attachment upload is paused")))
    }

    private fun completeTransfer(
        task: TransferTask,
        response: SceytResponse<String>,
        upload: Boolean
    ) {
        // Cancellation can remove a task while its result is waiting in the upload command queue.
        if (findTransferTask(task.attachment) !== task) return
        removeTransferTask(task)
        if (upload) task.state = if (response is SceytResponse.Success) Uploaded else ErrorUpload
        val callback = if (upload) task.uploadResultCallback else task.downloadCallback
        runCatching { callback?.onResult(response) }.onFailure {
            SceytLog.e(TAG, "File transfer callback failed", it)
        }
        val result = when (response) {
            is SceytResponse.Success -> Result.success(
                if (upload) task.attachment.copy(url = response.data) else task.attachment,
            )
            is SceytResponse.Error -> Result.failure(
                if (upload) Throwable(response.message.orEmpty())
                else response.exception ?: IllegalStateException("File download failed"),
            )
        }
        runCatching { task.complete(result) }.onFailure {
            SceytLog.e(TAG, "File transfer completion listener failed", it)
        }
    }

    private fun startTransfer(
        attachment: SceytAttachment,
        configureTask: TransferTask.() -> Unit,
        start: (SceytAttachment, TransferTask) -> Unit,
    ): TransferTask {
        val task = findOrCreateTransferTask(attachment)
        task.configureTask()
        if (task.isCancelled) {
            task.complete(Result.failure(CancellationException("Attachment upload cancelled")))
        } else start(attachment, task)
        return task
    }

    private companion object {
        const val TAG = "FileTransferService"
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
