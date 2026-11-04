package com.sceyt.chatuikit.filetransfer

import com.sceyt.chatuikit.filetransfer.encryption.FileEncryptionInfo
import java.io.File

enum class FileUploadType {
    PLAIN,
    ENCRYPTED,
}

/** The source is plaintext in both modes; the transport owns encryption when requested. */
data class FileUploadRequest(
    val operationId: String,
    val sourceFile: File,
    val fileName: String,
    val mimeType: String?,
    val context: FileTransferContext? = null,
    val type: FileUploadType = FileUploadType.PLAIN,
)

/** Encryption information for the same uploaded bytes as [url]. */
data class FileUploadResult(
    val url: String,
    val encryptionInfo: FileEncryptionInfo? = null,
)

/** [destinationFile] is the file where the transport writes the downloaded bytes. */
data class FileDownloadRequest(
    val operationId: String,
    val url: String,
    val destinationFile: File,
    val context: FileTransferContext? = null,
    val headers: Map<String, String> = emptyMap(),
)
