package com.sceyt.chatuikit.persistence.logicimpl

import android.content.Context
import com.google.gson.JsonObject
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.extensions.getMimeType
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferCallback
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import com.sceyt.chatuikit.filetransfer.FileTransferTransport
import com.sceyt.chatuikit.filetransfer.FileTransferType
import com.sceyt.chatuikit.filetransfer.FileUploadRequest
import com.sceyt.chatuikit.filetransfer.FileUploadResult
import com.sceyt.chatuikit.filetransfer.encryption.FileEncryptionMetadata
import com.sceyt.chatuikit.persistence.extensions.fileTransferData
import com.sceyt.chatuikit.persistence.extensions.withFileTransferData
import com.sceyt.chatuikit.persistence.file_transfer.transferKey
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import java.io.File

/** Creates transfer requests and keeps each execution bound to its selected transport. */
internal class AttachmentTransferRouter(
    context: Context,
    attachmentLogic: PersistenceAttachmentLogic? = null,
) {
    private val uploadPreparer = attachmentLogic?.let { UploadPreparer(context, it) }

    fun upload(
        attachment: SceytAttachment,
        operationId: String,
        isSharedUpload: Boolean,
        onFilePrepared: (String) -> Unit = {},
        onPreparing: (Float) -> Unit = {},
        preparedFilePath: String? = null,
        onUploadPrepared: suspend (SceytAttachment) -> SceytAttachment,
    ): TransferExecutor {
        val config = SceytChatUIKit.fileTransfer
        val transport = config.transport

        return TransferExecutor(
            operationId = operationId,
            transport = transport,
        ) { execution, callback ->
            execution.onDiscard { transport.cancel(operationId) }

            val encrypted = config.encryptionEnabled
            var prepared = checkNotNull(uploadPreparer).prepare(
                attachment = attachment,
                onProgress = onPreparing,
                reuseUploadedUrl = !encrypted,
                preparedFilePath = preparedFilePath,
            )
            // A completed checksum match skips both preparation and upload on the transport.
            if (prepared.reusedUrl == null) {
                val request = createUploadRequest(
                    attachment = prepared.attachment,
                    operationId = operationId,
                    encrypted = encrypted,
                    isSharedUpload = isSharedUpload
                )
                prepared = prepared.copy(
                    attachment = prepareUpload(
                        request = request,
                        transport = transport,
                        onUploadPrepared = onUploadPrepared
                    )
                )
            }

            // File-path callbacks may queue metadata writes, so publish after app data is saved.
            prepared.preparedFilePath?.let(onFilePrepared)
            execution.attachment = prepared.attachment

            uploadPrepared(
                prepared = prepared,
                execution = execution,
                transport = transport,
                operationId = operationId,
                isSharedUpload = isSharedUpload,
                encrypted = encrypted,
                callback = callback,
            )
        }
    }

    private suspend fun prepareUpload(
        request: FileUploadRequest,
        transport: FileTransferTransport,
        onUploadPrepared: suspend (SceytAttachment) -> SceytAttachment,
    ): SceytAttachment {
        val attachment = request.attachment.copy(fileSize = request.sourceFile.length())
        val data = transport.prepareUpload(request.copy(attachment = attachment))
            ?: return request.attachment
        val metadata = attachment.metadata.withFileTransferData(data)
        return onUploadPrepared(attachment.copy(metadata = metadata))
    }

    private suspend fun uploadPrepared(
        prepared: PreparedAttachment,
        execution: TransferExecutor,
        transport: FileTransferTransport,
        operationId: String,
        isSharedUpload: Boolean,
        encrypted: Boolean,
        callback: FileTransferCallback,
    ): FileTransferResult<String> {
        val reusedUrl = prepared.reusedUrl
        val plaintextSize: Long
        val result: FileTransferResult<FileUploadResult>

        // A cached URL skips the transport and keeps the attachment's stored size.
        if (reusedUrl != null) {
            plaintextSize = prepared.attachment.fileSize
            result = FileTransferResult.Success(FileUploadResult(reusedUrl))
        } else {
            val request = createUploadRequest(
                attachment = prepared.attachment,
                operationId = operationId,
                encrypted = encrypted,
                isSharedUpload = isSharedUpload
            )
            plaintextSize = request.sourceFile.length()
            result = execution.executeTransport { transport.upload(request, callback) }
        }

        return when (result) {
            is FileTransferResult.Success -> {
                execution.attachment = applyUploadResult(
                    attachment = prepared.attachment,
                    result = result.result,
                    plaintextSize = plaintextSize,
                    encrypted = encrypted
                )
                FileTransferResult.Success(result.result.url)
            }

            is FileTransferResult.Failure -> result
            is FileTransferResult.Interrupted -> result
        }
    }

    private fun createUploadRequest(
        attachment: SceytAttachment,
        operationId: String,
        encrypted: Boolean,
        isSharedUpload: Boolean,
    ): FileUploadRequest {
        val file = File(checkNotNull(attachment.filePath))
        return FileUploadRequest(
            operationId = operationId,
            sourceFile = file,
            fileName = attachment.name,
            mimeType = getMimeType(file.path),
            attachment = attachment,
            isSharedUpload = isSharedUpload,
            type = if (encrypted) FileTransferType.ENCRYPTED else FileTransferType.PLAIN,
            metadata = attachment.metadata.fileTransferData() ?: JsonObject(),
        )
    }

    private fun applyUploadResult(
        attachment: SceytAttachment,
        result: FileUploadResult,
        plaintextSize: Long,
        encrypted: Boolean,
    ): SceytAttachment {
        val encryptionInfo = result.encryptionInfo
        if (encrypted && encryptionInfo == null) {
            // A custom transport may keep its encryption information in the app's saved object.
            checkNotNull(attachment.metadata.fileTransferData()) { "Encrypted upload returned no encryption information" }
        }

        val metadata = if (encryptionInfo != null) {
            val renditions = FileEncryptionMetadata.read(attachment.metadata)?.renditions.orEmpty()
            val envelope = FileEncryptionMetadata.fromEncryptionInfo(encryptionInfo, renditions)
            FileEncryptionMetadata.write(attachment.metadata, envelope)
        } else {
            FileEncryptionMetadata.remove(attachment.metadata)
        }

        return attachment.copy(
            metadata = metadata,
            fileSize = if (encryptionInfo != null) plaintextSize else attachment.fileSize,
        )
    }

    fun download(
        attachment: SceytAttachment,
        operationId: String,
        destination: File,
    ): TransferExecutor {
        val config = SceytChatUIKit.fileTransfer
        val transport = config.transport

        return TransferExecutor(
            operationId = operationId,
            transport = transport,
        ) { execution, callback ->
            execution.onDiscard { transport.cancel(operationId) }
            val request = FileDownloadRequest(
                operationId = operationId,
                url = requireNotNull(attachment.url),
                destinationFile = destination,
                attachment = attachment,
                type = if (config.encryptionEnabled) FileTransferType.ENCRYPTED else FileTransferType.PLAIN,
                metadata = attachment.metadata.fileTransferData() ?: JsonObject(),
            )
            execution.executeTransport { transport.download(request, callback) }
        }
    }

    fun discardSavedDownload(attachment: SceytAttachment) {
        SceytChatUIKit.fileTransfer.transport.cancel("download:" + attachment.transferKey)
    }

    fun cancelUpload(attachment: SceytAttachment) {
        SceytChatUIKit.fileTransfer.transport.cancel("upload:" + attachment.messageTid)
    }
}
