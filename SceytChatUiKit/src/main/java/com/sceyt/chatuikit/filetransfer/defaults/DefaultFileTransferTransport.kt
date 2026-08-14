package com.sceyt.chatuikit.filetransfer.defaults

import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferCallback
import com.sceyt.chatuikit.filetransfer.FileTransferEvent
import com.sceyt.chatuikit.filetransfer.FileTransferTransport
import com.sceyt.chatuikit.filetransfer.FileUploadRequest
import com.sceyt.chatuikit.persistence.logicimpl.FileTransferUtility
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class DefaultFileTransferTransport : FileTransferTransport {
    private val transferUtility = FileTransferUtility()

    override suspend fun upload(
        request: FileUploadRequest,
        callback: FileTransferCallback,
    ): String? = suspendCancellableCoroutine { continuation ->
        val completed = AtomicBoolean()
        val attachment = request.attachment.copy(
            filePath = request.sourceFile.path,
        )

        transferUtility.uploadFile(
            attachment = attachment,
            onProgress = { progressPercent ->
                if (continuation.isActive) {
                    callback.onEvent(FileTransferEvent.Progress(progressPercent))
                }
            },
            onResult = { response ->
                continuation.completeWith(response, completed, "File upload failed")
            },
        )

        continuation.invokeOnCancellation {
            completed.set(true)
            transferUtility.pauseUpload(attachment)
        }
    }

    override suspend fun download(
        request: FileDownloadRequest,
        callback: FileTransferCallback,
    ): String? = suspendCancellableCoroutine { continuation ->
        val completed = AtomicBoolean()
        val attachment = request.attachment.copy(url = request.url)

        transferUtility.downloadFile(
            attachment = attachment,
            destFile = request.destinationFile,
            onProgress = { progressPercent ->
                if (continuation.isActive) {
                    callback.onEvent(FileTransferEvent.Progress(progressPercent))
                }
            },
            onResult = { response ->
                continuation.completeWith(response, completed, "File download failed")
            },
        )

        continuation.invokeOnCancellation {
            completed.set(true)
            transferUtility.pauseDownload(attachment)
        }
    }

    private fun CancellableContinuation<String?>.completeWith(
        response: SceytResponse<String>,
        completed: AtomicBoolean,
        fallbackErrorMessage: String,
    ) {
        if (!completed.compareAndSet(false, true)) return

        when (response) {
            is SceytResponse.Success -> resume(response.data)
            is SceytResponse.Error -> resumeWithException(
                response.exception ?: IllegalStateException(fallbackErrorMessage),
            )
        }
    }
}
