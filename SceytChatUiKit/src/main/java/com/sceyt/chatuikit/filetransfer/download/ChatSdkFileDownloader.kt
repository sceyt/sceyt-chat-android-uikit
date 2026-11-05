package com.sceyt.chatuikit.filetransfer.download

import com.sceyt.chat.download.DownloadRequest
import com.sceyt.chat.download.MediaDownload
import com.sceyt.chat.download.MediaDownload.State
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileDownloadType
import com.sceyt.chatuikit.filetransfer.FileTransferCallback
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import com.sceyt.chatuikit.filetransfer.encryption.toSdkInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/** Adapts native downloads while the SDK owns file writing, decryption, and resume checkpoints. */
internal class ChatSdkFileDownloader(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val downloads = ConcurrentHashMap<String, Channel<DownloadEvent>>()

    suspend fun download(
        request: FileDownloadRequest,
        callback: FileTransferCallback,
    ): FileTransferResult<String> = withContext(dispatcher) {
        val events = Channel<DownloadEvent>(Channel.UNLIMITED)

        // Keep the active attempt's native listener until its coroutine has finished.
        if (downloads.putIfAbsent(request.operationId, events) != null) {
            events.close()
            val error = IllegalStateException("Download is already running")
            return@withContext FileTransferResult.Failure(error)
        }

        var operation: MediaDownload? = null
        try {
            operation = startDownload(request, events)

            // An existing file or startup failure may finish without a native callback.
            events.trySend(DownloadEvent.StatusChanged)
            awaitResult(request, operation, events, callback)
        } catch (error: Throwable) {
            currentCoroutineContext().ensureActive()
            operation?.cancel()
            FileTransferResult.Failure(error)
        } finally {
            events.close()
            closeDownload(request.operationId, events, operation)
        }
    }

    fun pause(operationId: String): Boolean {
        val events = downloads[operationId] ?: return false
        return events.trySend(DownloadEvent.Pause).isSuccess
    }

    fun resume(operationId: String): Boolean {
        val events = downloads[operationId] ?: return false
        return events.trySend(DownloadEvent.Resume).isSuccess
    }

    fun cancel(operationId: String) {
        // Native also resolves checkpoints left before an app restart.
        MediaDownload.cancelDownloadById(operationId)
    }

    private fun startDownload(
        request: FileDownloadRequest,
        events: Channel<DownloadEvent>,
    ): MediaDownload {
        request.destinationFile.parentFile?.mkdirs()

        val sdkRequest = DownloadRequest(request.url, request.headers, request.operationId)
        val destination = request.destinationFile.absolutePath

        // Keep native callbacks short; the coroutine handles progress and results.
        val listener = MediaDownload.Listener { events.trySend(DownloadEvent.StatusChanged) }
        return when (val type = request.type) {
            is FileDownloadType.Plain -> {
                MediaDownload.download(sdkRequest, destination, listener, null)
            }

            is FileDownloadType.Encrypted -> {
                val encryptionInfo = type.encryptionInfo.toSdkInfo(type.plaintextSize)
                MediaDownload.downloadEncrypted(
                    /* request = */ sdkRequest,
                    /* destinationPath = */ destination,
                    /* encryption = */ encryptionInfo,
                    /* listener = */ listener,
                    /* requestProvider = */ null
                )
            }
        }
    }

    private suspend fun awaitResult(
        request: FileDownloadRequest,
        operation: MediaDownload,
        events: Channel<DownloadEvent>,
        callback: FileTransferCallback,
    ): FileTransferResult<String> {
        while (true) {
            // Controls run in this I/O coroutine, in order, alongside status updates.
            when (events.receive()) {
                DownloadEvent.Pause -> {
                    operation.pause()
                    continue
                }

                DownloadEvent.Resume -> {
                    operation.resume()
                    continue
                }

                DownloadEvent.StatusChanged -> Unit
            }

            val status = operation.status

            when (status.state) {
                State.COMPLETED -> {
                    callback.onProgress(100f)
                    return FileTransferResult.Success(request.destinationFile.absolutePath)
                }

                State.FAILED, State.CANCELLED -> {
                    operation.cancel()
                    val error = status.error ?: IllegalStateException("File download failed")
                    return FileTransferResult.Failure(error)
                }

                State.DOWNLOADING -> {
                    // Native reports an unknown size until the first response supplies it.
                    if (status.size > 0) {
                        val progress = status.downloaded.toDouble() / status.size * 100
                        callback.onProgress(progress.toFloat())
                    }
                }

                // Native keeps the same download available for resume and network recovery.
                State.IDLE, State.PAUSED, State.WAITING_FOR_NETWORK -> Unit
            }
        }
    }

    private suspend fun closeDownload(
        operationId: String,
        events: Channel<DownloadEvent>,
        operation: MediaDownload?,
    ) {
        withContext(NonCancellable) {
            try {
                operation?.use {
                    operation.setListener(null)

                    // Stop native writers and retain partial files when the coroutine is canceled.
                    operation.pause()
                }
            } finally {
                downloads.remove(operationId, events)
            }
        }
    }

    private enum class DownloadEvent {
        StatusChanged,
        Pause,
        Resume,
    }
}
