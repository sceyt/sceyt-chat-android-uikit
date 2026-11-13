package com.sceyt.chatuikit.persistence.logicimpl

import android.content.Context
import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.extensions.boundedSynchronizedMap
import com.sceyt.chatuikit.extensions.takeIfNotBlank
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Downloading
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PauseDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferTask
import com.sceyt.chatuikit.persistence.file_transfer.transferKey
import com.sceyt.chatuikit.persistence.mappers.toTransferData
import com.sceyt.chatuikit.presentation.extensions.isAttachmentExistAndFullyLoaded
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Owns file checks and the current download attempt for each attachment. */
internal class AttachmentDownloadCoordinator(
    private val context: Context,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val router = AttachmentTransferRouter(context)
    private val lock = Any()
    private val downloads = hashMapOf<String, Download>()
    private val cleanups = hashMapOf<String, Job>()
    private val interruptions = boundedSynchronizedMap<String, SceytAttachment>(MAX_INTERRUPTED_DOWNLOADS)

    fun download(
        attachment: SceytAttachment,
        task: TransferTask,
        onResult: (SceytResponse<String>) -> Unit,
        isResuming: Boolean = false,
    ) {
        // A paused task starts again only through an explicit resume request.
        if (!isResuming && task.state == PauseDownload) return

        val key = attachment.transferKey
        val registered = CompletableDeferred<Unit>()
        val download = synchronized(lock) {
            val previous = downloads[key]
            if (previous?.job?.isActive == true) {
                // Reuse an ongoing attempt or one still delivering its result to this task.
                if (!previous.completed || previous.task === task) return
            }

            val download = Download(attachment, task, onResult)
            val previousCleanup = cleanups[key]
            download.job = scope.launch(start = CoroutineStart.UNDISPATCHED) {
                // Start waiting immediately so cancellation cannot skip joining older file writers.
                withContext(NonCancellable) {
                    previous?.job?.join()
                    previousCleanup?.join()
                }

                // Register this attempt before it can publish progress or results.
                registered.await()
                currentCoroutineContext().ensureActive()
                performDownload(download)
            }

            downloads[key] = download
            download.job.invokeOnCompletion {
                synchronized(lock) {
                    // Retain paused attempts so resume can reuse their provider.
                    if (!download.paused || download.completed) downloads.remove(key, download)
                }
            }
            download
        }
        publishStarted(download)
        if (isResuming) publishResumed(download, task)
        registered.complete(Unit)
    }

    fun pause(attachment: SceytAttachment): Boolean {
        val download = synchronized(lock) {
            // Only registered, unfinished attempts can be paused.
            val current = downloads[attachment.transferKey] ?: return false
            if (current.completed) return false

            // A repeated pause succeeds using the existing paused state.
            if (current.paused) return true

            // Suppress progress before the provider can call back during pause.
            current.paused = true
            interruptions.remove(current.key)
            current
        }

        val transportPaused = download.job.isActive && runCatching {
            download.transfer?.pause() == true
        }.getOrDefault(false)

        // Cancel the attempt when provider pause is unsupported or fails, keeping resumable files.
        if (!transportPaused) cancelAttempt(download)
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
            val transportResumed = runCatching {
                previous.transfer?.resume() == true
            }.getOrDefault(false)

            if (transportResumed) {
                // Resume can complete the attempt immediately, so publish only while it is active.
                if (previous.job.isActive) publishResumed(previous, task)
                return
            }

            // Stop the old writer before a replacement can use the same destination.
            cancelAttempt(previous)
        }

        // A replacement joins the previous attempt and continues from its saved files.
        download(attachment, task, onResult, isResuming = true)
    }

    fun cancel(attachment: SceytAttachment, discard: Boolean = true) {
        interruptions.remove(attachment.transferKey)
        synchronized(lock) {
            val download = downloads[attachment.transferKey]
            if (download != null) {
                download.paused = false
                if (download.job.isCompleted) downloads.remove(download.key, download)
            }

            // A saved session can need cleanup even when no attempt is registered.
            if (discard) scheduleCleanup(attachment, download)
            download?.let(::cancelAttempt)
        }
    }

    /** The caller holds [lock]. New attempts wait for this cleanup before opening their files. */
    private fun scheduleCleanup(attachment: SceytAttachment, download: Download?) {
        val key = attachment.transferKey
        val previousCleanup = cleanups[key]
        val cleanup = scope.launch(start = CoroutineStart.LAZY) {
            withContext(NonCancellable + ioDispatcher) {
                previousCleanup?.join()
                download?.job?.join()

                val transfer = download?.transfer
                if (transfer != null)
                    transfer.discard()
                else router.discardSavedDownload(attachment)
            }
        }

        // Register before starting because cleanup can finish immediately.
        cleanups[key] = cleanup
        cleanup.invokeOnCompletion {
            synchronized(lock) { cleanups.remove(key, cleanup) }
        }
        cleanup.start()
    }

    fun cancelAll() {
        // Cancellation can remove attempts from the map while this snapshot is processed.
        interruptions.clear()
        val attachments = synchronized(lock) { downloads.values.map { it.attachment } }
        attachments.forEach(::cancel)
    }

    fun getInterruptedDownloads(): List<SceytAttachment> = synchronized(interruptions) {
        interruptions.values.toList()
    }

    suspend fun retryInterruptedDownloads(
        findStored: suspend (SceytAttachment) -> SceytAttachment?,
        start: (SceytAttachment) -> Unit,
    ) {
        getInterruptedDownloads().forEach { attachment ->
            val stored = findStored(attachment)
            if (stored == null) cancel(attachment) else start(stored)
        }
    }

    private fun cancelAttempt(download: Download) {
        // Detach the result callback before cancellation can finish the coroutine.
        synchronized(lock) { download.onResult = null }
        download.job.cancel()
    }

    private suspend fun performDownload(download: Download) {
        val result = try {
            val response = downloadFile(download)
            currentCoroutineContext().ensureActive()
            if (response is FileTransferResult.Success) {
                check(response.result.isNotBlank()) { "File download returned an empty local path" }
            }
            response
        } catch (error: Throwable) {
            // Propagate coroutine cancellation before reporting a download failure.
            currentCoroutineContext().ensureActive()
            FileTransferResult.Failure(error)
        }

        completeDownload(download, result)
    }

    private suspend fun downloadFile(download: Download): FileTransferResult<String> {
        val attachment = download.attachment
        requireNotNull(attachment.url.takeIfNotBlank()) { "Wrong url" }
        val destination = SceytChatUIKit.fileTransfer.destinationProvider
            .provideDestination(context, attachment)
        download.destination = destination

        val existingFile = attachment.isAttachmentExistAndFullyLoaded(destination)
        if (existingFile != null) {
            // A complete local file satisfies the request immediately.
            return FileTransferResult.Success(existingFile.path)
        }

        // The initial loading callback can pause or cancel before the provider starts.
        currentCoroutineContext().ensureActive()

        val transfer = router.download(attachment, download.operationId, destination)
        download.transfer = transfer
        return transfer.execute { progress -> handleProgress(download, progress) }
    }

    private fun completeDownload(download: Download, result: FileTransferResult<String>) {
        download.completed = true
        val onResult = synchronized(lock) {
            // A replaced or canceled attempt must leave the current task and its files alone.
            if (downloads[download.key] !== download) return
            val callback = download.onResult ?: return

            if (result is FileTransferResult.Failure) download.destination?.delete()
            download.onResult = null
            callback
        }

        val response = when (result) {
            is FileTransferResult.Success -> {
                interruptions.remove(download.key)
                SceytResponse.Success(result.result)
            }

            is FileTransferResult.Failure -> {
                interruptions.remove(download.key)
                SceytResponse.Error(
                    result.error as? SceytException ?: SceytException(0, result.error.message),
                )
            }

            is FileTransferResult.Interrupted -> {
                if (!download.paused) {
                    synchronized(interruptions) {
                        interruptions.remove(download.key)
                        interruptions[download.key] = download.attachment
                    }
                }
                SceytResponse.Error(
                    result.error as? SceytException ?: SceytException(0, result.error.message),
                )
            }
        }
        notify { onResult(response) }
    }

    private fun handleProgress(download: Download, progressPercent: Float) {
        // Late callbacks from a replaced attempt must not update the current task.
        if (!isCurrent(download)) return
        if (download.paused || !download.job.isActive) return

        notify {
            download.task.progressCallback?.onProgress(
                TransferData(
                    messageTid = download.task.messageTid,
                    progressPercent = progressPercent,
                    state = Downloading,
                    filePath = null,
                    url = download.attachment.url,
                ),
            )
        }
    }

    private fun isCurrent(download: Download): Boolean = synchronized(lock) {
        downloads[download.key] === download
    }

    private fun publishStarted(download: Download) {
        synchronized(lock) {
            if (downloads[download.key] !== download || download.paused) return
            download.task.state = Downloading
        }

        // Show loading while file checks and network requests are still queued.
        notify {
            download.task.progressCallback?.onProgress(download.attachment.toTransferData(Downloading))
        }
    }

    private fun publishResumed(download: Download, task: TransferTask) {
        synchronized(lock) {
            // A replacement or newer pause takes precedence over this resume notification.
            if (downloads[download.key] !== download || download.paused) return
            task.state = Downloading
        }

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
    ) {
        val key = attachment.transferKey
        val operationId = "download:$key"
        lateinit var job: Job
        var destination: File? = null

        @Volatile
        var transfer: TransferExecutor? = null

        @Volatile
        var paused = false

        @Volatile
        var completed = false
    }

    private companion object {
        const val TAG = "AttachmentDownloadCoordinator"
        const val MAX_INTERRUPTED_DOWNLOADS = 10
    }
}
