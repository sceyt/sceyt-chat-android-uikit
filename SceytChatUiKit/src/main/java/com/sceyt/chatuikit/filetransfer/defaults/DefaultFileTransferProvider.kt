package com.sceyt.chatuikit.filetransfer.defaults

import android.content.Context
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.fold
import com.sceyt.chatuikit.extensions.hasConnectedNetwork
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferCallback
import com.sceyt.chatuikit.filetransfer.FileTransferProvider
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import com.sceyt.chatuikit.filetransfer.FileTransferType
import com.sceyt.chatuikit.filetransfer.FileUploadRequest
import com.sceyt.chatuikit.filetransfer.download.ChatSdkFileDownloader
import com.sceyt.chatuikit.filetransfer.encryption.ChatSdkFileEncryption
import com.sceyt.chatuikit.filetransfer.encryption.FileEncryptionCheckpoint
import com.sceyt.chatuikit.filetransfer.encryption.FileEncryptionMetadata
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.logicimpl.FileTransferUtility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import java.io.File
import java.io.IOException

class DefaultFileTransferProvider : FileTransferProvider, SceytKoinComponent {
    private val appContext by inject<Context>()
    private val transferUtility = FileTransferUtility()
    private val downloader = ChatSdkFileDownloader()
    private val checkpoints = NativeUploadCheckpointStore {
        File(appContext.noBackupFilesDir, "sceyt-upload-checkpoints")
    }

    override suspend fun prepareUpload(request: FileUploadRequest): Map<String, String> {
        if (request.type == FileTransferType.PLAIN) return emptyMap()

        val attachmentKey = FileEncryptionMetadata.read(request.attachment.metadata)
        if (attachmentKey != null && checkpoints.read(request.operationId, attachmentKey) != null)
            return mapOf(FileEncryptionMetadata.FIELD to attachmentKey)

        val checkpoint = prepareCheckpoint(request)
        checkpoints.save(request.operationId, checkpoint)
        return mapOf(FileEncryptionMetadata.FIELD to checkpoint.attachmentKey)
    }

    private suspend fun prepareCheckpoint(request: FileUploadRequest): FileEncryptionCheckpoint {
        val session = SceytChatUIKit.fileTransfer.encryption.prepareEncryption(request.sourceFile)
        return try {
            session.checkpoint()
        } finally {
            withContext(NonCancellable + Dispatchers.IO) { session.close() }
        }
    }

    override suspend fun upload(
        request: FileUploadRequest,
        callback: FileTransferCallback,
    ): FileTransferResult<String> = when (request.type) {
        FileTransferType.PLAIN -> transferUtility.uploadFile(
            sourceFile = request.sourceFile,
            onProgress = callback::onProgress,
        ).toUploadResult()

        FileTransferType.ENCRYPTED -> uploadEncrypted(request, callback)
    }

    private suspend fun uploadEncrypted(
        request: FileUploadRequest,
        callback: FileTransferCallback,
    ): FileTransferResult<String> {
        val attachmentKey = FileEncryptionMetadata.read(request.attachment.metadata)
        val checkpoint = attachmentKey?.let { checkpoints.read(request.operationId, it) }
        if (checkpoint == null) return FileTransferResult.Failure(
            IllegalStateException("Encrypted upload must be prepared before upload")
        )

        val encryption = SceytChatUIKit.fileTransfer.encryption
        val session = encryption.restoreEncryption(request.sourceFile, checkpoint)
        val result = try {
            // The native uploader needs the prepared upload handle owned by an SDK session.
            if (session !is ChatSdkFileEncryption.EncryptionSession) {
                return FileTransferResult.Failure(
                    UnsupportedOperationException("The native uploader requires Chat SDK encryption"),
                )
            }

            transferUtility.uploadEncryptedFile(
                source = session.upload,
                onProgress = callback::onProgress,
            ).toUploadResult()
        } finally {
            withContext(NonCancellable + Dispatchers.IO) {
                session.use { session ->
                    checkpoints.update(request.operationId, session.checkpoint())
                }
            }
        }
        if (result is FileTransferResult.Success) checkpoints.remove(request.operationId)
        return result
    }

    override fun pause(operationId: String): Boolean = downloader.pause(operationId)

    override fun resume(operationId: String): Boolean = downloader.resume(operationId)

    override fun cancel(operationId: String) {
        if (operationId.startsWith("upload:")) checkpoints.remove(operationId)
        else downloader.cancel(operationId)
    }

    override suspend fun download(
        request: FileDownloadRequest,
        callback: FileTransferCallback,
    ): FileTransferResult<String> {
        // Keep offline attempts retryable without starting native networking.
        if (!appContext.hasConnectedNetwork())
            return FileTransferResult.Interrupted(IOException("No network connection"))

        return downloader.download(request, callback)
    }

    private fun SceytResponse<String>.toUploadResult(): FileTransferResult<String> = fold(
        onSuccess = { url ->
            if (url != null)
                FileTransferResult.Success(url)
            else FileTransferResult.Failure(IllegalStateException("File upload failed"))
        },
        onError = { error ->
            FileTransferResult.Failure(error ?: IllegalStateException("File upload failed"))
        },
    )
}
