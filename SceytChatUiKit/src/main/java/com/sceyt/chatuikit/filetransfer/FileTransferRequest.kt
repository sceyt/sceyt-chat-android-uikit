package com.sceyt.chatuikit.filetransfer

import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import java.io.File

data class FileUploadRequest(
    val operationId: String,
    val sourceFile: File,
    val fileName: String,
    val mimeType: String?,
    val attachment: SceytAttachment,
    val isSharedUpload: Boolean = false,
    val role: TransferRole = TransferRole.Main,
)

data class FileDownloadRequest(
    val operationId: String,
    val url: String,
    val destinationFile: File,
    val attachment: SceytAttachment,
    val role: TransferRole = TransferRole.Main,
)