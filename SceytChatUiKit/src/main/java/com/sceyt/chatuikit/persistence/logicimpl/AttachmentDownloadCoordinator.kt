package com.sceyt.chatuikit.persistence.logicimpl

import android.content.Context
import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.filetransfer.AttachmentTransferContext
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import com.sceyt.chatuikit.filetransfer.FileTransferTransport
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Downloading
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PauseDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferTask
import com.sceyt.chatuikit.persistence.file_transfer.transferKey
import com.sceyt.chatuikit.persistence.mappers.toTransferData
import com.sceyt.chatuikit.presentation.extensions.isAttachmentExistAndFullyLoaded
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.io.File

/** Owns file checks and the current download attempt for each attachment. */
internal class AttachmentDownloadCoordinator(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val lock = Any()
    private val downloads = hashMapOf<String, Download>()

    fun download(
        attachment: SceytAttachment,
        task: TransferTask,
        onResult: (SceytResponse<String>) -> Unit,
        isResuming: Boolean = false,
    ) {
        if (!isResuming && task.state == PauseDownload) return
        val key = attachment.transferKey
        val download = synchronized(lock) {
            val previous = downloads[key]
            if (previous != null && !previous.job.isCancelled && !previous.job.isCompleted) return
            Download(attachment, task, onResult, isResuming).also { next ->
                next.job = scope.launch(start = CoroutineStart.LAZY) {
                    // Wait for the previous attempt to stop before reusing the destination file.
                    previous?.job?.join()
                    performDownload(next)
                }
                downloads[key] = next
                next.job.invokeOnCompletion {
                    synchronized(lock) {
                        if (!next.paused) downloads.remove(key, next)
                    }
                }
            }
        }
        download.job.start()
    }

    fun pause(attachment: SceytAttachment): Boolean {
        val download = synchronized(lock) {
            val current = downloads[attachment.transferKey] ?: return false
            if (current.completed || current.paused) return false
            current.paused = true
            current
        }
        val paused = download.job.isActive && runCatching {
            download.transport?.pause(download.operationId) == true
        }.getOrDefault(false)
        if (!paused) cancelAttempt(download)
        return true
    }

    fun resume(
        attachment: SceytAttachment,
        task: TransferTask,
        onResult: (SceytResponse<String>) -> Unit,
    ) {
        val previous = synchronized(lock) {
            downloads[attachment.transferKey]?.also { it.paused = false }
        }
        if (previous?.job?.isActive == true) {
            val resumed = runCatching {
                previous.transport?.resume(previous.operationId) == true
            }.getOrDefault(false)
            if (resumed) {
                if (isCurrent(previous) && !previous.paused && previous.job.isActive)
                    publishResumed(task)
                return
            }
            cancelAttempt(previous)
        }
        download(attachment, task, onResult, isResuming = true)
    }

    fun cancel(attachment: SceytAttachment) {
        val download = synchronized(lock) {
            downloads[attachment.transferKey]?.also {
                it.paused = false
                if (it.job.isCompleted) downloads.remove(attachment.transferKey, it)
            }
        }
        download?.let(::cancelAttempt)
    }

    fun cancelAll() {
        val active = synchronized(lock) { downloads.values.map { it.attachment } }
        active.forEach(::cancel)
    }

    private fun cancelAttempt(download: Download) {
        synchronized(lock) { download.onResult = null }
        download.job.cancel()
    }

    private suspend fun performDownload(download: Download) {
        val result = try {
            val attachment = download.attachment
            val url = attachment.url
            require(!url.isNullOrBlank()) { "Wrong url" }
            val destination = SceytChatUIKit.fileTransfer.destinationProvider
                .provideDestination(context, attachment)
            download.destination = destination
            val existing = attachment.isAttachmentExistAndFullyLoaded(destination)
            val response = if (existing != null) {
                FileTransferResult.Success(existing.path)
            } else {
                val task = download.task
                task.state = Downloading
                notify {
                    task.progressCallback?.onProgress(
                        TransferData(
                            messageTid = task.messageTid,
                            progressPercent = attachment.progressPercent ?: 0f,
                            state = Downloading,
                            filePath = attachment.filePath,
                            url = url
                        ),
                    )
                }
                currentCoroutineContext().ensureActive()
                if (download.isResuming && isCurrent(download)) publishResumed(task)
                currentCoroutineContext().ensureActive()
                val transport = SceytChatUIKit.fileTransfer.transport
                download.transport = transport
                transport.download(
                    FileDownloadRequest(
                        operationId = download.operationId,
                        url = url,
                        destinationFile = destination,
                        context = AttachmentTransferContext(attachment)
                    ),
                ) { progress -> handleProgress(download, progress) }
            }
            currentCoroutineContext().ensureActive()
            if (response is FileTransferResult.Success) {
                check(response.result.isNotBlank()) { "File download returned an empty local path" }
            }
            response
        } catch (error: Throwable) {
            currentCoroutineContext().ensureActive()
            FileTransferResult.Failure(error)
        }
        download.completed = true
        val onResult = synchronized(lock) {
            if (downloads[download.attachment.transferKey] !== download) return
            val callback = download.onResult ?: return
            if (result is FileTransferResult.Failure) download.destination?.delete()
            downloads.remove(download.attachment.transferKey)
            download.onResult = null
            callback
        }
        val response = when (result) {
            is FileTransferResult.Success -> SceytResponse.Success(result.result)
            is FileTransferResult.Failure -> SceytResponse.Error(
                result.error as? SceytException ?: SceytException(0, result.error.message),
            )

            is FileTransferResult.Interrupted -> SceytResponse.Error(
                result.error as? SceytException ?: SceytException(0, result.error.message),
            )
        }
        notify { onResult(response) }
    }

    private fun handleProgress(download: Download, progressPercent: Float) {
        if (!isCurrent(download) || download.paused || !download.job.isActive) return
        notify {
            download.task.progressCallback?.onProgress(
                TransferData(
                    messageTid = download.task.messageTid,
                    progressPercent = progressPercent,
                    state = Downloading,
                    filePath = null,
                    url = download.attachment.url
                ),
            )
        }
    }

    private fun isCurrent(download: Download): Boolean = synchronized(lock) {
        downloads[download.attachment.transferKey] === download
    }

    private fun publishResumed(task: TransferTask) {
        task.state = Downloading
        notify { task.resumePauseCallback?.onResumePause(task.attachment.toTransferData(Downloading)) }
    }

    private fun notify(callback: () -> Unit) {
        runCatching(callback).onFailure {
            SceytLog.e(TAG, "File transfer callback failed", it)
        }
    }

    private class Download(
        val attachment: SceytAttachment,
        val task: TransferTask,
        var onResult: ((SceytResponse<String>) -> Unit)?,
        val isResuming: Boolean,
    ) {
        val operationId = "download:${attachment.transferKey}"
        lateinit var job: Job
        var destination: File? = null
        @Volatile
        var transport: FileTransferTransport? = null
        @Volatile
        var paused = false
        @Volatile
        var completed = false
    }

    private companion object {
        const val TAG = "AttachmentDownloadCoordinator"
    }
}
