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

/** Selects an execution once; its controls keep targeting the same transport. */
internal class AttachmentTransferRouter(
    context: Context,
    attachmentLogic: PersistenceAttachmentLogic? = null,
) {
    private val preparer = attachmentLogic?.let { UploadPreparer(context, it) }
    private val downloader = EncryptedFileDownloader(File(context.filesDir, "download-sessions"))

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
            val prepared = checkNotNull(preparer).prepare(
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
        val info = result.encryptionInfo
        check(!encrypted || info != null) { "Encrypted upload returned no encryption information" }
        val metadata = if (info != null) {
            val renditions = FileEncryptionMetadata.read(attachment.metadata)?.renditions.orEmpty()
            val envelope = FileEncryptionMetadata.fromEncryptionInfo(info, renditions)
            FileEncryptionMetadata.write(attachment.metadata, envelope)
        } else {
            FileEncryptionMetadata.remove(attachment.metadata)
        }
        return attachment.copy(
            metadata = metadata,
            fileSize = if (info != null) plaintextSize else attachment.fileSize,
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
        val envelope = FileEncryptionMetadata.read(attachment.metadata)
        return TransferExecutor(
            operationId = operationId,
            transport = transport,
        ) { execution, callback ->
            if (!FileEncryptionMetadata.isEncrypted(attachment.metadata)) {
                execution.executeTransport { transport.download(request, callback) }
            } else {
                try {
                    val encryption = checkNotNull(envelope) {
                        "Malformed attachment encryption metadata"
                    }
                    downloader.download(
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
                    withContext(NonCancellable + Dispatchers.IO) {
                        execution.onDiscard { downloader.discard(operationId) }
                    }
                }
            }
        }
    }

    fun discardSavedDownload(attachment: SceytAttachment) {
        downloader.discard("download:" + attachment.transferKey)
    }

    fun cancelUpload(attachment: SceytAttachment) {
        SceytChatUIKit.fileTransfer.transport.cancel("upload:" + attachment.messageTid)
    }
}
