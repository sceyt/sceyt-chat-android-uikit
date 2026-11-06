package com.sceyt.chatuikit.filetransfer.defaults

import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.fold
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferCallback
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import com.sceyt.chatuikit.filetransfer.FileTransferTransport
import com.sceyt.chatuikit.filetransfer.FileTransferType
import com.sceyt.chatuikit.filetransfer.FileUploadRequest
import com.sceyt.chatuikit.filetransfer.FileUploadResult
import com.sceyt.chatuikit.filetransfer.download.ChatSdkFileDownloader
import com.sceyt.chatuikit.filetransfer.encryption.ChatSdkFileEncryption
import com.sceyt.chatuikit.filetransfer.encryption.FileEncryptionInfo
import com.sceyt.chatuikit.persistence.logicimpl.FileTransferUtility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class DefaultFileTransferTransport : FileTransferTransport {
    private val transferUtility = FileTransferUtility()
    private val downloader = ChatSdkFileDownloader()

    override suspend fun upload(
        request: FileUploadRequest,
        callback: FileTransferCallback,
    ): FileTransferResult<FileUploadResult> = when (request.type) {
        FileTransferType.PLAIN -> transferUtility.uploadFile(
            sourceFile = request.sourceFile,
            onProgress = callback::onProgress,
        ).toUploadResult()

        FileTransferType.ENCRYPTED -> uploadEncrypted(request, callback)
    }

    private suspend fun uploadEncrypted(
        request: FileUploadRequest,
        callback: FileTransferCallback,
    ): FileTransferResult<FileUploadResult> {
        val session = SceytChatUIKit.fileTransfer.encryption.prepareEncryption(request.sourceFile)
        return try {
            // The native uploader needs the prepared upload handle owned by an SDK session.
            if (session !is ChatSdkFileEncryption.EncryptionSession) {
                return FileTransferResult.Failure(
                    UnsupportedOperationException("The native uploader requires Chat SDK encryption"),
                )
            }

            transferUtility.uploadEncryptedFile(
                source = session.upload,
                onProgress = callback::onProgress,
            ).toUploadResult(session.encryptionInfo)
        } finally {
            withContext(NonCancellable + Dispatchers.IO) { session.close() }
        }
    }

    override fun pause(operationId: String): Boolean = downloader.pause(operationId)

    override fun resume(operationId: String): Boolean = downloader.resume(operationId)

    override fun cancel(operationId: String) = downloader.cancel(operationId)

    override suspend fun download(
        request: FileDownloadRequest,
        callback: FileTransferCallback,
    ): FileTransferResult<String> = downloader.download(request, callback)

    private fun SceytResponse<String>.toUploadResult(
        encryptionInfo: FileEncryptionInfo? = null,
    ): FileTransferResult<FileUploadResult> = fold(
        onSuccess = { url ->
            if (url != null)
                FileTransferResult.Success(FileUploadResult(url, encryptionInfo))
            else FileTransferResult.Failure(IllegalStateException("File upload failed"))
        },
        onError = { error ->
            FileTransferResult.Failure(error ?: IllegalStateException("File upload failed"))
        },
    )
}
