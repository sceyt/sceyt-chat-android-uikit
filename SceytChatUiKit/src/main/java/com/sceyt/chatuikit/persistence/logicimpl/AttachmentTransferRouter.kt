package com.sceyt.chatuikit.persistence.logicimpl

import android.content.Context
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.extensions.getMimeType
import com.sceyt.chatuikit.filetransfer.AttachmentTransferContext
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import com.sceyt.chatuikit.filetransfer.FileUploadRequest
import com.sceyt.chatuikit.filetransfer.FileUploadResult
import com.sceyt.chatuikit.filetransfer.FileUploadType
import com.sceyt.chatuikit.filetransfer.encryption.EncryptedFileDownloader
import com.sceyt.chatuikit.filetransfer.encryption.FileDownloadEncryption
import com.sceyt.chatuikit.filetransfer.encryption.FileEncryptionMetadata
import com.sceyt.chatuikit.persistence.file_transfer.transferKey
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.File

/** Creates transfer requests and keeps each execution bound to its selected transport. */
internal class AttachmentTransferRouter(
    context: Context,
    attachmentLogic: PersistenceAttachmentLogic? = null,
) {
    private val uploadPreparer = attachmentLogic?.let { UploadPreparer(context, it) }
    private val encryptedDownloader = EncryptedFileDownloader(File(context.filesDir, "download-sessions"))

    fun upload(
        attachment: SceytAttachment,
        operationId: String,
        isSharedUpload: Boolean,
        onFilePrepared: (String) -> Unit = {},
        onPreparing: (Float) -> Unit = {},
        preparedFilePath: String? = null,
    ): TransferExecutor {
        val config = SceytChatUIKit.fileTransfer
        val transport = config.transport

        return TransferExecutor(
            operationId = operationId,
            transport = transport,
        ) { execution, callback ->
            execution.onDiscard { transport.cancel(operationId) }

            val encrypted = config.encryptUploads
            val prepared = checkNotNull(uploadPreparer).prepare(
                attachment = attachment,
                onFilePrepared = onFilePrepared,
                onProgress = onPreparing,
                reuseUploadedUrl = !encrypted,
                preparedFilePath = preparedFilePath,
            )
            execution.attachment = prepared.attachment

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
                    isSharedUpload = isSharedUpload,
                )
                plaintextSize = request.sourceFile.length()
                result = execution.executeTransport { transport.upload(request, callback) }
            }

            when (result) {
                is FileTransferResult.Success -> {
                    execution.attachment = applyUploadResult(
                        attachment = prepared.attachment,
                        result = result.result,
                        plaintextSize = plaintextSize,
                        encrypted = encrypted,
                    )
                    FileTransferResult.Success(result.result.url)
                }

                is FileTransferResult.Failure -> result
                is FileTransferResult.Interrupted -> result
            }
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
            context = AttachmentTransferContext(attachment, isSharedUpload),
            type = if (encrypted) FileUploadType.ENCRYPTED else FileUploadType.PLAIN,
        )
    }

    private fun applyUploadResult(
        attachment: SceytAttachment,
        result: FileUploadResult,
        plaintextSize: Long,
        encrypted: Boolean,
    ): SceytAttachment {
        val encryptionInfo = result.encryptionInfo
        if (encrypted) {
            checkNotNull(encryptionInfo) { "Encrypted upload returned no encryption information" }
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
        val request = FileDownloadRequest(
            operationId = operationId,
            url = requireNotNull(attachment.url),
            destinationFile = destination,
            context = AttachmentTransferContext(attachment),
        )
        val config = SceytChatUIKit.fileTransfer
        val transport = config.transport
        val encryptionMetadata = FileEncryptionMetadata.read(attachment.metadata)

        return TransferExecutor(
            operationId = operationId,
            transport = transport,
        ) { execution, callback ->
            if (!FileEncryptionMetadata.isEncrypted(attachment.metadata)) {
                // Plain attachments can go directly to the selected transport.
                return@TransferExecutor execution.executeTransport { transport.download(request, callback) }
            }

            try {
                val encryption = checkNotNull(encryptionMetadata) {
                    "Malformed attachment encryption metadata"
                }
                encryptedDownloader.download(
                    request = request,
                    encryptionInfo = FileDownloadEncryption(
                        info = encryption.toEncryptionInfo(),
                        plaintextSize = attachment.fileSize,
                    ),
                    encryption = config.encryption,
                    callback = callback,
                ) { downloadRequest, progress ->
                    execution.executeTransport { transport.download(downloadRequest, progress) }
                }
            } finally {
                // Discard runs after the download and decryption have released their files.
                withContext(NonCancellable + Dispatchers.IO) {
                    execution.onDiscard { encryptedDownloader.discard(operationId) }
                }
            }
        }
    }

    fun discardSavedDownload(attachment: SceytAttachment) {
        encryptedDownloader.discard("download:" + attachment.transferKey)
    }

    fun cancelUpload(attachment: SceytAttachment) {
        SceytChatUIKit.fileTransfer.transport.cancel("upload:" + attachment.messageTid)
    }
}
