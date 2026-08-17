package com.sceyt.chatuikit.persistence.logicimpl

import android.content.Context
import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferEvent
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Downloading
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ErrorDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PauseDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PendingDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferTask
import com.sceyt.chatuikit.persistence.mappers.toTransferData
import com.sceyt.chatuikit.presentation.extensions.isAttachmentExistAndFullyLoaded
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

internal class AttachmentDownloadCoordinator(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : SceytKoinComponent {
    private val fileTransferService: FileTransferService by inject()

    private val downloadJobs = ConcurrentHashMap<String, Job>()
    // Native pause keeps its coroutine active, so its callbacks need a separate thread-safe gate.
    private val pausedOperationIds = ConcurrentHashMap.newKeySet<String>()

    fun downloadFile(
        attachment: SceytAttachment,
        task: TransferTask,
    ) {
        val url = attachment.url

        if (url.isNullOrBlank()) {
            task.downloadCallback?.onResult(SceytResponse.Error(SceytException(0, "Wrong url")))
            return
        }

        val destinationFile =
            SceytChatUIKit.fileTransfer.destinationProvider.provideDestination(context, attachment)

        val existingFile = attachment.isAttachmentExistAndFullyLoaded(destinationFile)

        if (existingFile != null) {
            task.downloadCallback?.onResult(SceytResponse.Success(existingFile.path))
            return
        }

        startDownload(attachment, task, url, destinationFile)
    }

    private fun startDownload(
        attachment: SceytAttachment,
        task: TransferTask,
        url: String,
        destinationFile: File,
    ) {
        val operationId = attachment.downloadOperationId
        val request = FileDownloadRequest(
            operationId = operationId,
            url = url,
            destinationFile = destinationFile,
            attachment = attachment,
        )

        val job = scope.launch(start = CoroutineStart.LAZY) {
            performDownload(request, task, url, operationId)
        }

        if (downloadJobs.putIfAbsent(operationId, job) != null) {
            job.cancel()
            return
        }

        task.progressCallback?.onProgress(
            TransferData(
                messageTid = task.messageTid,
                progressPercent = attachment.progressPercent ?: 0f,
                state = Downloading,
                filePath = attachment.filePath,
                url = url,
            ),
        )

        job.start()
    }

    private suspend fun performDownload(
        request: FileDownloadRequest,
        task: TransferTask,
        url: String,
        operationId: String,
    ) {
        val job = currentCoroutineContext().job
        val waitingForNetwork = AtomicBoolean()

        try {
            val response = try {
                val result = SceytChatUIKit.fileTransfer.transport.download(
                    request = request,
                    callback = { event ->
                        if (job.isActive && !pausedOperationIds.contains(operationId)) {
                            if (event is FileTransferEvent.WaitingForNetwork) {
                                waitingForNetwork.set(true)
                            }
                            handleDownloadEvent(event, task, url)
                        }
                    },
                ).takeUnless { it.isNullOrBlank() }
                    ?: throw IllegalStateException("File download returned an empty local path")
                currentCoroutineContext().ensureActive()
                SceytResponse.Success(result)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (!waitingForNetwork.get()) {
                    request.destinationFile.delete()
                }
                SceytResponse.Error(error.toSceytException())
            }

            notifyDownloadResult(task, response)
        } finally {
            if (downloadJobs.remove(operationId, job)) {
                pausedOperationIds.remove(operationId)
            }
        }
    }

    private fun handleDownloadEvent(
        event: FileTransferEvent,
        task: TransferTask,
        url: String,
    ) {
        if (event !is FileTransferEvent.Progress) return

        task.progressCallback?.onProgress(
            TransferData(
                messageTid = task.messageTid,
                progressPercent = event.progressPercent,
                state = Downloading,
                filePath = null,
                url = url,
            ),
        )
    }

    fun pauseLoad(
        attachment: SceytAttachment,
        state: TransferState,
    ) {
        if (state != PendingDownload && state != Downloading) {
            return
        }

        val operationId = attachment.downloadOperationId
        val currentJob = downloadJobs[operationId]
        if (currentJob?.isActive == true) {
            pausedOperationIds.add(operationId)
        }
        val pausedByTransport = currentJob?.isActive == true && pauseTransport(operationId)

        if (!pausedByTransport) {
            if (currentJob != null && downloadJobs.remove(operationId, currentJob)) {
                currentJob.cancel()
            }
            pausedOperationIds.remove(operationId)
        }

        fileTransferService.findTransferTask(attachment)?.let { task ->
            task.state = PauseDownload
            task.resumePauseCallback?.onResumePause(attachment.toTransferData(PauseDownload))
        }
    }

    fun resumeLoad(
        attachment: SceytAttachment,
        state: TransferState,
    ) {
        if (
            state != PendingDownload &&
            state != PauseDownload &&
            state != ErrorDownload
        ) {
            return
        }

        val operationId = attachment.downloadOperationId
        val currentJob = downloadJobs[operationId]
        val task = fileTransferService.findTransferTask(attachment)

        val resumedByTransport = state == PauseDownload &&
                currentJob?.isActive == true &&
                resumeTransport(operationId)

        if (resumedByTransport) {
            pausedOperationIds.remove(operationId)
            task?.state = Downloading
            task?.resumePauseCallback?.onResumePause(attachment.toTransferData(Downloading))
            return
        }

        if (state != PendingDownload || currentJob?.isActive != true) {
            if (currentJob != null && downloadJobs.remove(operationId, currentJob)) {
                currentJob.cancel()
            }
        }

        pausedOperationIds.remove(operationId)

        val destinationFile =
            SceytChatUIKit.fileTransfer.destinationProvider
                .provideDestination(context, attachment)

        val existingFile =
            attachment.isAttachmentExistAndFullyLoaded(destinationFile)

        val transferTask = task ?: fileTransferService.findOrCreateTransferTask(attachment)
        transferTask.state = Downloading

        if (existingFile != null) {
            notifyDownloadResult(transferTask, SceytResponse.Success(existingFile.path))
            return
        }

        downloadFile(
            attachment = attachment,
            task = transferTask,
        )

        transferTask.resumePauseCallback?.onResumePause(attachment.toTransferData(Downloading))
    }

    private fun pauseTransport(operationId: String): Boolean = runCatching {
        SceytChatUIKit.fileTransfer.transport.pause(operationId)
    }.getOrDefault(false)

    private fun resumeTransport(operationId: String): Boolean = runCatching {
        SceytChatUIKit.fileTransfer.transport.resume(operationId)
    }.getOrDefault(false)

    fun cancelAll() {
        downloadJobs.values.forEach { it.cancel() }
        downloadJobs.clear()
        pausedOperationIds.clear()
    }

    private fun Throwable?.toSceytException(): SceytException? {
        return when (this) {
            null -> null
            is SceytException -> this
            else -> SceytException(0, message)
        }
    }

    private fun notifyDownloadResult(
        task: TransferTask,
        response: SceytResponse<String>,
    ) {
        runCatching {
            task.downloadCallback?.onResult(response)
        }.onFailure { error ->
            SceytLog.e(TAG, "File transfer callback failed", error)
        }
    }

    private val SceytAttachment.downloadOperationId: String
        get() = "download:$messageTid"

    companion object {
        private const val TAG = "AttachmentDownloadCoordinator"
    }
}
