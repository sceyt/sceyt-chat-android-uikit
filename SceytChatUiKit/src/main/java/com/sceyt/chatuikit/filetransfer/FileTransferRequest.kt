package com.sceyt.chatuikit.filetransfer

import java.io.File

data class FileUploadRequest(
    val operationId: String,
    val sourceFile: File,
    val fileName: String,
    val mimeType: String?,
    val context: FileTransferContext? = null,
)

data class FileDownloadRequest(
    val operationId: String,
    val url: String,
    val destinationFile: File,
    val context: FileTransferContext? = null,
)
