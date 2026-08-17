package com.sceyt.chatuikit.filetransfer

interface FileTransferTransport {
    /**
     * Uploads the prepared source and returns its remote reference.
     * Implementations must stop their work when the calling coroutine is cancelled.
     */
    suspend fun upload(
        request: FileUploadRequest,
        callback: FileTransferCallback,
    ): String?

    /**
     * Downloads the remote file and returns the resulting local path.
     * Implementations must stop their work when the calling coroutine is cancelled.
     */
    suspend fun download(
        request: FileDownloadRequest,
        callback: FileTransferCallback,
    ): String?

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
}
