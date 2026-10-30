package com.sceyt.chatuikit.filetransfer.defaults

import com.sceyt.chat.fileencryption.PreparedUpload
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.fold
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferCallback
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import com.sceyt.chatuikit.filetransfer.FileTransferTransport
import com.sceyt.chatuikit.filetransfer.FileUploadRequest
import com.sceyt.chatuikit.filetransfer.encryption.EncryptedUploadSource
import com.sceyt.chatuikit.persistence.logicimpl.FileTransferUtility
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

class DefaultFileTransferTransport : FileTransferTransport {
    private val transferUtility = FileTransferUtility()

    override suspend fun upload(
        request: FileUploadRequest,
        callback: FileTransferCallback,
    ): FileTransferResult<String> {
        val response = when (request) {
            is FileUploadRequest.Plain -> transferUtility.uploadFile(
                sourceFile = request.sourceFile,
                onProgress = callback::onProgress,
            )
            is FileUploadRequest.Encrypted -> {
                val source = (request.ciphertextSource as? EncryptedUploadSource)?.sdkUpload
                    ?: request.ciphertextSource as? PreparedUpload
                    ?: return FileTransferResult.Failure(
                        UnsupportedOperationException("The Chat SDK uploader requires an SDK PreparedUpload"),
                    )
                transferUtility.uploadEncryptedFile(source, callback::onProgress)
            }
        }

        return response.toResult("File upload failed")
    }

    override suspend fun download(
        request: FileDownloadRequest,
        callback: FileTransferCallback,
    ): FileTransferResult<String> {
        val writerStopped = CompletableDeferred<Unit>()
        var call: Call? = null
        try {
            return suspendCancellableCoroutine { continuation ->
                val completed = AtomicBoolean()
                call = transferUtility.downloadFile(
                    request = request,
                    onProgress = { progressPercent ->
                        if (continuation.isActive) {
                            callback.onProgress(progressPercent)
                        }
                    },
                    onResult = { response ->
                        if (completed.compareAndSet(false, true)) {
                            continuation.resume(response)
                        }
                    },
                    onFinished = { writerStopped.complete(Unit) },
                )

                continuation.invokeOnCancellation {
                    completed.set(true)
                    call?.let(transferUtility::cancelDownload)
                }
            }
        } finally {
            // Joining this coroutine must also wait for the HTTP writer to close its file.
            if (call != null) withContext(NonCancellable) { writerStopped.await() }
        }
    }

    private fun SceytResponse<String>.toResult(
        fallbackErrorMessage: String,
    ): FileTransferResult<String> = fold(
        onSuccess = { result ->
            result?.let { FileTransferResult.Success(it) } ?: FileTransferResult.Failure(
                IllegalStateException(fallbackErrorMessage)
            )
        },
        onError = { FileTransferResult.Failure(it ?: IllegalStateException(fallbackErrorMessage)) }
    )
}
