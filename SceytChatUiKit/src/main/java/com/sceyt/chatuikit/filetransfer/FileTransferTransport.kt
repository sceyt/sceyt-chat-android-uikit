package com.sceyt.chatuikit.filetransfer

/**
 * Transfers prepared files. Expected failures are returned as [FileTransferResult.Failure];
 * recoverable interruptions are returned as [FileTransferResult.Interrupted].
 * Coroutine cancellation must propagate as CancellationException, not as a failed result.
 */
interface FileTransferTransport {
    /**
     * Uploads the prepared source and returns its remote reference.
     * Implementations must stop their work when the calling coroutine is cancelled.
     */
    suspend fun upload(
        request: FileUploadRequest,
        callback: FileTransferCallback,
    ): FileTransferResult<FileUploadResult>

    /**
     * Downloads the remote bytes into [FileDownloadRequest.destinationFile]. UIKit currently wraps
     * encrypted downloads and handles decryption; the transport receives a byte-only request.
     * Implementations must stop their work when the calling coroutine is cancelled.
     */
    suspend fun download(
        request: FileDownloadRequest,
        callback: FileTransferCallback,
    ): FileTransferResult<String>

    /**
     * Pauses the existing operation.
     *
     * @return `true` when the operation remains available for [resume], or `false` when the
     * UI kit must cancel it and use restart fallback.
     */
    fun pause(operationId: String): Boolean = false

    /**
     * Resumes the existing paused operation.
     *
     * @return `true` when the same operation continues, or `false` when the UI kit must restart it.
     */
    fun resume(operationId: String): Boolean = false

    /**
     * Notifies the transport that the caller explicitly abandoned an operation.
     * The caller also cancels its coroutine to stop active work. Implementations clean up
     * retained backend state without blocking, including after a restart. Must be idempotent.
     * Pause fallback and recoverable interruptions do not call this method.
     * UIKit's encrypted-download wrapper currently cleans its temporary files after writers stop.
     */
    fun cancel(operationId: String)
}
