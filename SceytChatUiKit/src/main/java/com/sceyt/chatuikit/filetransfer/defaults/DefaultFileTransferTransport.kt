package com.sceyt.chatuikit.filetransfer.defaults

import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.fold
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferCallback
import com.sceyt.chatuikit.filetransfer.FileTransferEvent
import com.sceyt.chatuikit.filetransfer.FileTransferTransport
import com.sceyt.chatuikit.filetransfer.FileUploadRequest
import com.sceyt.chatuikit.persistence.logicimpl.FileTransferUtility
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

class DefaultFileTransferTransport : FileTransferTransport {
    private val transferUtility = FileTransferUtility()

    override suspend fun upload(
        request: FileUploadRequest,
        callback: FileTransferCallback,
    ): Result<String> {
        val response = transferUtility.uploadFile(
            sourceFile = request.sourceFile,
            onProgress = { progressPercent ->
                callback.onEvent(FileTransferEvent.Progress(progressPercent))
            },
        )

        return response.toResult("File upload failed")
    }

    override suspend fun download(
        request: FileDownloadRequest,
        callback: FileTransferCallback,
    ): Result<String> = suspendCancellableCoroutine { continuation ->
        val completed = AtomicBoolean()
        val call = transferUtility.downloadFile(
            request = request,
            onProgress = { progressPercent ->
                if (continuation.isActive) {
                    callback.onEvent(FileTransferEvent.Progress(progressPercent))
                }
            },
            onResult = { response ->
                if (completed.compareAndSet(false, true)) {
                    continuation.resume(response.toResult("File download failed"))
                }
            },
        )

        continuation.invokeOnCancellation {
            completed.set(true)
            call?.let { transferUtility.cancelDownload(request.operationId, it) }
        }
    }

    private fun SceytResponse<String>.toResult(
        fallbackErrorMessage: String,
    ): Result<String> = fold(
        onSuccess = { result ->
            result?.let { Result.success(it) } ?: Result.failure(
                IllegalStateException(fallbackErrorMessage)
            )
        },
        onError = { Result.failure(it ?: IllegalStateException(fallbackErrorMessage)) }
    )
}
