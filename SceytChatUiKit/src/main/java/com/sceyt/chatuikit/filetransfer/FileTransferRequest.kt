package com.sceyt.chatuikit.filetransfer

import com.sceyt.chat.fileencryption.CiphertextRangeSource
import java.io.File

sealed interface FileUploadRequest {
    val operationId: String
    val fileName: String
    val mimeType: String?
    val context: FileTransferContext?

    data class Plain(
        override val operationId: String,
        val sourceFile: File,
        override val fileName: String,
        override val mimeType: String?,
        override val context: FileTransferContext? = null,
    ) : FileUploadRequest

    /**
     * Read exact ciphertext ranges, including encryption overhead, for the network request.
     * UIKit owns the source. Stop all reads before returning, including on cancellation.
     * S3 multipart transports can request a range for each presigned part URL.
     */
    data class Encrypted(
        override val operationId: String,
        val ciphertextSource: CiphertextRangeSource,
        override val fileName: String,
        override val mimeType: String?,
        override val context: FileTransferContext? = null,
        val checkpointStore: TransportCheckpointStore? = null,
    ) : FileUploadRequest
}

data class FileDownloadRequest(
    val operationId: String,
    val url: String,
    val destinationFile: File,
    val context: FileTransferContext? = null,
)
