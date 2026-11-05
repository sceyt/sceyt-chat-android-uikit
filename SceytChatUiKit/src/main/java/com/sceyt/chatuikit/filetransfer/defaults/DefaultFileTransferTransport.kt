package com.sceyt.chatuikit.filetransfer.defaults

import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.fold
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferCallback
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import com.sceyt.chatuikit.filetransfer.FileTransferTransport
import com.sceyt.chatuikit.filetransfer.FileUploadRequest
import com.sceyt.chatuikit.filetransfer.FileUploadResult
import com.sceyt.chatuikit.filetransfer.FileUploadType
import com.sceyt.chatuikit.filetransfer.encryption.ChatSdkFileEncryption
import com.sceyt.chatuikit.filetransfer.encryption.FileEncryptionInfo
import com.sceyt.chatuikit.persistence.logicimpl.FileTransferUtility
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
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
    ): FileTransferResult<FileUploadResult> = when (request.type) {
        FileUploadType.PLAIN -> transferUtility.uploadFile(
            sourceFile = request.sourceFile,
            onProgress = callback::onProgress,
        ).toUploadResult()

        FileUploadType.ENCRYPTED -> uploadEncrypted(request, callback)
    }

    private suspend fun uploadEncrypted(
        request: FileUploadRequest,
        callback: FileTransferCallback,
    ): FileTransferResult<FileUploadResult> {
        val session = SceytChatUIKit.fileTransfer.encryption.prepareEncryption(request.sourceFile)
        return try {
            // The native uploader needs the prepared upload handle owned by an SDK session.
            if (session !is ChatSdkFileEncryption.EncryptionSession) {
                return FileTransferResult.Failure(
                    UnsupportedOperationException("The native uploader requires Chat SDK encryption"),
                )
            }

            transferUtility.uploadEncryptedFile(
                source = session.upload,
                onProgress = callback::onProgress,
            ).toUploadResult(session.encryptionInfo)
        } finally {
            withContext(NonCancellable + Dispatchers.IO) { session.close() }
        }
    }

    // Active SDK and HTTP work is stopped through coroutine cancellation.
    override fun cancel(operationId: String) = Unit

    override suspend fun download(
        request: FileDownloadRequest,
        callback: FileTransferCallback,
    ): FileTransferResult<String> {
        val writerStopped = CompletableDeferred<Unit>()
        var call: Call? = null
        return try {
            suspendCancellableCoroutine { continuation ->
                val completed = AtomicBoolean()
                call = transferUtility.downloadFile(
                    request = request,
                    onProgress = { progressPercent ->
                        if (continuation.isActive) callback.onProgress(progressPercent)
                    },
                    onResult = { response ->
                        // Results can arrive after cancellation or be delivered more than once.
                        if (!completed.compareAndSet(false, true)) return@downloadFile
                        continuation.resume(response)
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
            if (call != null)
                withContext(NonCancellable) { writerStopped.await() }
        }
    }

    private fun SceytResponse<String>.toUploadResult(
        encryptionInfo: FileEncryptionInfo? = null,
    ): FileTransferResult<FileUploadResult> = fold(
        onSuccess = { url ->
            if (url != null)
                FileTransferResult.Success(FileUploadResult(url, encryptionInfo))
            else FileTransferResult.Failure(IllegalStateException("File upload failed"))
        },
        onError = { error ->
            FileTransferResult.Failure(error ?: IllegalStateException("File upload failed"))
        },
    )
}
