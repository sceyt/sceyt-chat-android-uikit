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
}
