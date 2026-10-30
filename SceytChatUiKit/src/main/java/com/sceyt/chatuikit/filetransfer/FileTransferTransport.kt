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
    ): FileTransferResult<String>

    /**
     * Downloads the remote file and returns the resulting local path.
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
     * Discards a cancelled upload, including one retained after an interruption or restart.
     * [checkpointData] is the last opaque backend checkpoint, when available.
     * Implementations must be idempotent and schedule cleanup without blocking the caller.
     */
    fun discardUpload(operationId: String, checkpointData: String?) = Unit
}
