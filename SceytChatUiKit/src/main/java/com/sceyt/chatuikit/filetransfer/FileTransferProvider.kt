package com.sceyt.chatuikit.filetransfer

/**
 * Transfers prepared files. Expected failures are returned as [FileTransferResult.Failure];
 * recoverable interruptions are returned as [FileTransferResult.Interrupted].
 * Coroutine cancellation must propagate as CancellationException, not as a failed result.
 */
interface FileTransferProvider {
    /**
     * Prepares metadata for an upload of the final source file.
     * Read previously saved values from [FileUploadRequest.attachment]'s metadata on retry or restart.
     * UIKit merges the returned string values into that metadata and saves it before calling [upload].
     * Matching keys are replaced; other keys are preserved. An empty map skips this write.
     * Completed checksum matches skip both methods. Throwing prevents upload.
     */
    suspend fun prepareUpload(request: FileUploadRequest): Map<String, String> = emptyMap()

    /**
     * Uploads the prepared source and returns its remote reference.
     * Encryption metadata must be supplied through [prepareUpload]; the provider validates its own data.
     * Implementations must stop their work when the calling coroutine is canceled.
     */
    suspend fun upload(
        request: FileUploadRequest,
        callback: FileTransferCallback,
    ): FileTransferResult<String>

    /**
     * Downloads to [FileDownloadRequest.destinationFile] using [FileDownloadRequest.type].
     * Encrypted requests require valid encryption metadata; missing data must fail without a plain fallback.
     * Success returns the final plaintext path. Implementations must stop file writers before the
     * calling coroutine finishes, including when it is canceled. Keep resumable files on coroutine
     * cancellation; remove them only on explicit [cancel].
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
     * Notifies the provider that the caller explicitly abandoned an operation.
     * The caller also cancels its coroutine to stop active work. Implementations clean up
     * retained state, including after a restart. Must be idempotent. UIKit calls this on a background
     * thread after the operation's coroutine stops, so partial files are no longer being written.
     * Pause fallback and recoverable interruptions do not call this method.
     */
    fun cancel(operationId: String)
}
