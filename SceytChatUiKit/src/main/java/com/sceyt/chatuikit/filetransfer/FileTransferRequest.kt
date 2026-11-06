package com.sceyt.chatuikit.filetransfer

import com.google.gson.JsonObject
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.filetransfer.encryption.FileEncryptionInfo
import java.io.File

enum class FileTransferType {
    PLAIN,
    ENCRYPTED,
}

/** The source is plaintext in both modes; the transport owns encryption when requested. */
data class FileUploadRequest(
    val operationId: String,
    val sourceFile: File,
    val fileName: String,
    val mimeType: String?,
    val attachment: SceytAttachment,
    val type: FileTransferType = FileTransferType.PLAIN,
    val metadata: JsonObject = JsonObject(),
    val isSharedUpload: Boolean = false,
) {
    override fun toString(): String =
        "FileUploadRequest(operationId=$operationId, sourceFile=$sourceFile, type=$type)"
}

/** Encryption information for the same uploaded bytes as [url]. */
data class FileUploadResult(
    val url: String,
    val encryptionInfo: FileEncryptionInfo? = null,
)

/** The transport follows [type], interprets [metadata], and publishes plaintext at [destinationFile]. */
data class FileDownloadRequest(
    val operationId: String,
    val url: String,
    val destinationFile: File,
    val attachment: SceytAttachment,
    val type: FileTransferType = FileTransferType.PLAIN,
    val headers: Map<String, String> = emptyMap(),
    val metadata: JsonObject = JsonObject(),
) {
    override fun toString(): String =
        "FileDownloadRequest(operationId=$operationId, destinationFile=$destinationFile, type=$type)"
}
