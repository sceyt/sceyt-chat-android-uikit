package com.sceyt.chatuikit.persistence.logicimpl

import android.content.Context
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferCallback
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import com.sceyt.chatuikit.filetransfer.FileTransferProvider
import com.sceyt.chatuikit.filetransfer.FileTransferType
import com.sceyt.chatuikit.filetransfer.FileUploadRequest
import com.sceyt.chatuikit.persistence.extensions.withMetadata
import com.sceyt.chatuikit.persistence.file_transfer.transferKey
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import java.io.File

/** Creates transfer requests and keeps each execution bound to its selected provider. */
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
        onFileLocationChanged: (String) -> Unit = {},
        onPreparing: (Float) -> Unit = {},
        preparedFilePath: String? = null,
        onUploadPrepared: suspend (SceytAttachment, Map<String, String>) -> SceytAttachment,
    ): TransferExecutor {
        val config = SceytChatUIKit.fileTransfer
        val transport = config.provider

        return TransferExecutor(
            operationId = operationId,
            provider = transport,
        ) { execution, callback ->
            execution.onDiscard { transport.cancel(operationId) }

            val encrypted = config.encryptionEnabled
            var prepared = checkNotNull(uploadPreparer).prepare(
                attachment = attachment,
                onProgress = onPreparing,
                reuseUploadedUrl = !encrypted,
                preparedFilePath = preparedFilePath,
            )
            // Remember completed media before the provider can suspend or fail.
            prepared.preparedFilePath?.let(onFilePrepared)
            try {
                // A completed checksum match skips both provider methods.
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
            } finally {
                // These callbacks queue metadata writes, so publish only after app preparation.
                // Cancellation keeps the cached path without changing the paused task's state.
                if (currentCoroutineContext().isActive)
                    prepared.preparedFilePath?.let(onFileLocationChanged)
            }

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
        transport: FileTransferProvider,
        onUploadPrepared: suspend (SceytAttachment, Map<String, String>) -> SceytAttachment,
    ): SceytAttachment {
        val attachment = request.attachment.copy(fileSize = request.sourceFile.length())
        val data = transport.prepareUpload(request.copy(attachment = attachment))
        if (data.isEmpty()) return request.attachment

        return onUploadPrepared(attachment.copy(metadata = attachment.metadata.withMetadata(data)), data)
    }

    private suspend fun uploadPrepared(
        prepared: PreparedAttachment,
        execution: TransferExecutor,
        transport: FileTransferProvider,
        operationId: String,
        isSharedUpload: Boolean,
        encrypted: Boolean,
        callback: FileTransferCallback,
    ): FileTransferResult<String> {
        // A cached URL skips the provider and keeps the attachment's stored size.
        prepared.reusedUrl?.let { return FileTransferResult.Success(it) }

        val request = createUploadRequest(
            attachment = prepared.attachment,
            operationId = operationId,
            encrypted = encrypted,
            isSharedUpload = isSharedUpload
        )
        return execution.executeTransport { transport.upload(request, callback) }
    }

    private fun createUploadRequest(
        attachment: SceytAttachment,
        operationId: String,
        encrypted: Boolean,
        isSharedUpload: Boolean,
    ): FileUploadRequest = FileUploadRequest(
        operationId = operationId,
        sourceFile = File(checkNotNull(attachment.filePath)),
        attachment = attachment,
        isSharedUpload = isSharedUpload,
        type = if (encrypted) FileTransferType.ENCRYPTED else FileTransferType.PLAIN,
    )

    fun download(
        attachment: SceytAttachment,
        operationId: String,
        destination: File,
    ): TransferExecutor {
        val config = SceytChatUIKit.fileTransfer
        val transport = config.provider

        return TransferExecutor(
            operationId = operationId,
            provider = transport,
        ) { execution, callback ->
            execution.onDiscard { transport.cancel(operationId) }
            val request = FileDownloadRequest(
                operationId = operationId,
                url = requireNotNull(attachment.url),
                destinationFile = destination,
                attachment = attachment,
                type = if (config.encryptionEnabled) FileTransferType.ENCRYPTED else FileTransferType.PLAIN,
            )
            execution.executeTransport { transport.download(request, callback) }
        }
    }

    fun discardSavedDownload(attachment: SceytAttachment) {
        SceytChatUIKit.fileTransfer.provider.cancel("download:" + attachment.transferKey)
    }

    fun cancelUpload(attachment: SceytAttachment) {
        val provider = SceytChatUIKit.fileTransfer.provider
        provider.cancel("upload:" + attachment.messageTid)
        provider.cancel(previewUploadOperationId(attachment.messageTid))
    }
}
