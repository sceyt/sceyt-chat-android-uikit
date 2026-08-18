package com.sceyt.chatuikit.persistence.logicimpl

import android.util.Size
import android.content.Context
import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.extensions.isNotNullOrBlank
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferEvent
import com.sceyt.chatuikit.filetransfer.TransferRole
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.ThumbData
import com.sceyt.chatuikit.persistence.file_transfer.ThumbFor
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Downloading
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ErrorDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PauseDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PendingDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferTask
import com.sceyt.chatuikit.persistence.mappers.getVideoThumbUrl
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
        // The uploaded poster is much smaller, so it is shown while the video downloads
        downloadVideoThumb(attachment, task)

        val url = attachment.url

        if (url.isNullOrBlank()) {
            task.downloadCallback?.onResult(SceytResponse.Error(SceytException(0, "Wrong url")))
            return
        }

        val destinationFile =
            SceytChatUIKit.fileTransfer.destinationProvider
                .provideDestination(context, attachment, TransferRole.Main)

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

        val downloadJob = scope.launch(start = CoroutineStart.LAZY) {
            performDownload(request, task, url, operationId)
        }

        if (downloadJobs.putIfAbsent(operationId, downloadJob) != null) {
            downloadJob.cancel()
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

        downloadJob.start()
    }

    /**
     * Downloads the poster frame uploaded with the video, so the message can show a real preview
     * without waiting for the video. It runs beside the video download, under its own operation id,
     * and stays silent on failure because the blurred thumb in the metadata remains as a fallback.
     */
    private fun downloadVideoThumb(attachment: SceytAttachment, task: TransferTask) {
        if (attachment.filePath.isNotNullOrBlank()) return
        val thumbUrl = attachment.getVideoThumbUrl()?.takeIf { it.isNotBlank() } ?: return

        val destinationFile = SceytChatUIKit.fileTransfer.destinationProvider
            .provideDestination(context, attachment, TransferRole.Thumbnail)

        if (destinationFile.exists() && destinationFile.length() > 0L) {
            notifyVideoThumbLoaded(task, destinationFile.path)
            return
        }

        val operationId = downloadOperationId(attachment.messageTid, TransferRole.Thumbnail)
        val request = FileDownloadRequest(
            operationId = operationId,
            url = thumbUrl,
            destinationFile = destinationFile,
            attachment = attachment,
            role = TransferRole.Thumbnail,
        )

        val thumbJob = scope.launch(start = CoroutineStart.LAZY) {
            val job = currentCoroutineContext().job
            try {
                SceytChatUIKit.fileTransfer.transport.download(request) {}
                currentCoroutineContext().ensureActive()
                notifyVideoThumbLoaded(task, destinationFile.path)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                destinationFile.delete()
                SceytLog.e(
                    TAG, "Couldn't download the video thumb for messageTid:" +
                            " ${attachment.messageTid}, reason: ${error.message}"
                )
            } finally {
                downloadJobs.remove(operationId, job)
            }
        }

        if (downloadJobs.putIfAbsent(operationId, thumbJob) != null) {
            thumbJob.cancel()
            return
        }

        thumbJob.start()
    }

    /** Notifies every screen which shows the attachment about the loaded poster. */
    private fun notifyVideoThumbLoaded(task: TransferTask, path: String) {
        ThumbFor.entries.forEach { thumbFor ->
            runCatching {
                task.thumbCallback?.onThumb(path, ThumbData(thumbFor.value, null, EMPTY_THUMB_SIZE))
            }.onFailure { error ->
                SceytLog.e(TAG, "File transfer callback failed", error)
            }
        }
    }

    private suspend fun performDownload(
        request: FileDownloadRequest,
        task: TransferTask,
        url: String,
        operationId: String,
    ) {
        val downloadJob = currentCoroutineContext().job
        val networkWaitTriggered = AtomicBoolean()

        try {
            val response = try {
                val result = SceytChatUIKit.fileTransfer.transport.download(
                    request = request,
                    callback = { event ->
                        if (downloadJob.isActive && operationId !in pausedOperationIds) {
                            when (event) {
                                is FileTransferEvent.WaitingForNetwork -> {
                                    if (networkWaitTriggered.compareAndSet(false, true)) {
                                        downloadJob.cancel()
                                    }
                                }

                                else -> handleDownloadEvent(event, task, url)
                            }
                        }
                    },
                ).takeUnless { it.isNullOrBlank() }
                    ?: throw IllegalStateException("File download returned an empty local path")
                currentCoroutineContext().ensureActive()
                SceytResponse.Success(result)
            } catch (error: CancellationException) {
                if (!networkWaitTriggered.get()) throw error
                SceytResponse.Error(SceytException(0, "Waiting for network"))
            } catch (error: Throwable) {
                SceytResponse.Error(error.toSceytException())
            }

            notifyDownloadResult(task, response)
        } finally {
            if (downloadJobs.remove(operationId, downloadJob)) {
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
                .provideDestination(context, attachment, TransferRole.Main)

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
        get() = downloadOperationId(messageTid)

    companion object {
        private const val TAG = "AttachmentDownloadCoordinator"
        private val EMPTY_THUMB_SIZE = Size(0, 0)
    }
}
