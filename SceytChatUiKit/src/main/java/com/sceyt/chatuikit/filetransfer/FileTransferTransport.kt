package com.sceyt.chatuikit.filetransfer

interface FileTransferTransport {
    fun upload(
        request: FileUploadRequest,
        callback: FileTransferCallback<String>,
    )

    fun download(
        request: FileDownloadRequest,
        callback: FileTransferCallback<String>,
    )

    fun pause(operationId: String): Boolean = false

    fun resume(operationId: String): Boolean = false
}
