package com.sceyt.chatuikit.persistence.logicimpl

import android.content.Context
import com.sceyt.chat.fileencryption.FileEncryptionCheckpoint
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.extensions.getMimeType
import com.sceyt.chatuikit.filetransfer.AttachmentTransferContext
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferCallback
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import com.sceyt.chatuikit.filetransfer.FileTransferTransport
import com.sceyt.chatuikit.filetransfer.FileUploadRequest
import com.sceyt.chatuikit.filetransfer.encryption.EncryptedFileDownloader
import com.sceyt.chatuikit.filetransfer.encryption.EncryptedUploadSource
import com.sceyt.chatuikit.filetransfer.encryption.FileEncryptionMetadata
import com.sceyt.chatuikit.filetransfer.encryption.FileUploadSessionStore
import com.sceyt.chatuikit.filetransfer.encryption.UploadSession
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.file_transfer.transferKey
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Selects an execution once; its controls keep targeting the same transport. */
internal class AttachmentTransferRouter(
    context: Context,
    attachmentLogic: PersistenceAttachmentLogic? = null,
) {
    private val appContext = context.applicationContext
    private val preparer = attachmentLogic?.let { UploadPreparer(context, it) }
    private val sessions = FileUploadSessionStore(File(context.filesDir, "upload-sessions"))
    private val downloader = EncryptedFileDownloader(File(context.filesDir, "download-sessions"))
    private val completedSessions = ConcurrentHashMap<String, Pair<String, String>>()

    fun upload(
        attachment: SceytAttachment,
        operationId: String,
        isSharedUpload: Boolean,
        onFilePrepared: (String) -> Unit = {},
        onPreparing: (Float) -> Unit = {},
    ): Transfer {
        val config = SceytChatUIKit.fileTransfer
        val transport = config.transport
        return Transfer(operationId, transport) { execution, callback ->
            var encryptedSource: EncryptedUploadSource? = null
            try {
                val originalPath = attachment.originalFilePath ?: attachment.filePath
                val saved = sessions.get(operationId)
                val encrypted = config.encryptUploads || saved != null
                val restored = if (saved?.transportCheckpointData != null) {
                    runInterruptible(Dispatchers.IO) {
                        check(saved.originalFilePath == originalPath) { "The upload source changed" }
                        val sdkUpload = config.encryptionProvider.restoreUpload(
                            File(saved.sourceFilePath),
                            FileEncryptionCheckpoint(
                                saved.encryptionInfo,
                                saved.sdkCheckpointData,
                            ),
                        )
                        try {
                            check(sdkUpload.sourceId == saved.sourceId) { "The encrypted source identity changed" }
                            encryptedSource = EncryptedUploadSource(sdkUpload, saved, sessions)
                            attachment.copy(
                                filePath = saved.sourceFilePath,
                                fileSize = sdkUpload.plaintextSize,
                            )
                        } catch (error: Throwable) {
                            sdkUpload.close()
                            throw error
                        }
                    }.also { onFilePrepared(checkNotNull(it.filePath)) }
                } else null
                val prepared = restored?.let { PreparedAttachment(it) } ?: checkNotNull(preparer).prepare(
                    attachment = attachment,
                    onFilePrepared = onFilePrepared,
                    onProgress = onPreparing,
                    reuseUploadedUrl = !encrypted,
                )
                if (encrypted && encryptedSource == null) {
                    runInterruptible(Dispatchers.IO) {
                        val file = File(checkNotNull(prepared.attachment.filePath))
                        val sdkUpload = config.encryptionProvider.prepareUpload(
                            file,
                            UUID.randomUUID().toString(),
                        )
                        try {
                            val session = UploadSession(
                                operationId = operationId,
                                sourceFilePath = file.absolutePath,
                                originalFilePath = originalPath,
                                sourceId = sdkUpload.sourceId,
                                encryptionInfo = sdkUpload.encryptionInfo,
                                sdkCheckpointData = sdkUpload.checkpoint.data,
                            )
                            sessions.save(session)
                            encryptedSource = EncryptedUploadSource(sdkUpload, session, sessions)
                        } catch (error: Throwable) {
                            sdkUpload.close()
                            throw error
                        }
                    }
                }
                val source = encryptedSource
                val updated = prepared.attachment.copy(
                    fileSize = source?.sdkUpload?.plaintextSize ?: prepared.attachment.fileSize,
                    metadata = source?.let {
                        FileEncryptionMetadata.write(prepared.attachment.metadata, it.envelope)
                    } ?: FileEncryptionMetadata.remove(prepared.attachment.metadata),
                )
                execution.attachment = updated
                if (source != null) {
                    execution.onDiscard { discardSource(operationId, transport, source) }
                }
                val result = prepared.reusedUrl?.let { FileTransferResult.Success(it) } ?: run {
                    val file = File(checkNotNull(updated.filePath))
                    val request = if (source == null) {
                        FileUploadRequest.Plain(
                            operationId = operationId,
                            sourceFile = file,
                            fileName = updated.name,
                            mimeType = getMimeType(file.path),
                            context = AttachmentTransferContext(updated, isSharedUpload),
                        )
                    } else {
                        FileUploadRequest.Encrypted(
                            operationId = operationId,
                            ciphertextSource = source,
                            fileName = updated.name,
                            mimeType = getMimeType(file.path),
                            context = AttachmentTransferContext(updated, isSharedUpload),
                            checkpointStore = source.checkpointStore,
                        )
                    }
                    execution.executeTransport { transport.upload(request, callback) }
                }
                if (result is FileTransferResult.Success && source != null) {
                    completedSessions[source.envelope.uniqueId] = operationId to source.getSourceId()
                }
                result
            } finally {
                encryptedSource?.let { source ->
                    withContext(NonCancellable + Dispatchers.IO) {
                        execution.onDiscard { discardSource(operationId, transport, source) }
                        runCatching { source.checkpoint() }.onFailure {
                            SceytLog.e("AttachmentTransferRouter", "Could not save upload checkpoint", it)
                        }
                        source.close()
                    }
                }
            }
        }
    }

    fun download(
        attachment: SceytAttachment,
        operationId: String,
        destination: File,
    ): Transfer {
        val request = FileDownloadRequest(
            operationId = operationId,
            url = requireNotNull(attachment.url),
            destinationFile = destination,
            context = AttachmentTransferContext(attachment),
        )
        val config = SceytChatUIKit.fileTransfer
        val transport = config.transport
        val envelope = FileEncryptionMetadata.read(attachment.metadata)
        return Transfer(operationId, transport) { execution, callback ->
            if (!FileEncryptionMetadata.isEncrypted(attachment.metadata)) {
                execution.executeTransport { transport.download(request, callback) }
            } else {
                try {
                    val encryption = checkNotNull(envelope) { "Malformed attachment encryption metadata" }
                    downloader.download(
                        request = request,
                        provider = config.encryptionProvider,
                        encryptionInfo = encryption.toEncryptionInfo(attachment.fileSize),
                        callback = callback,
                    ) { downloadRequest, progress ->
                        execution.executeTransport { transport.download(downloadRequest, progress) }
                    }
                } finally {
                    withContext(NonCancellable + Dispatchers.IO) {
                        execution.onDiscard { downloader.discard(operationId, destination) }
                    }
                }
            }
        }
    }

    fun discardSavedDownload(attachment: SceytAttachment) {
        if (!FileEncryptionMetadata.isEncrypted(attachment.metadata)) return
        val destination = SceytChatUIKit.fileTransfer.destinationProvider.provideDestination(appContext, attachment)
        downloader.discard("download:" + attachment.transferKey, destination)
    }

    fun uploaded(attachment: SceytAttachment) {
        val envelope = FileEncryptionMetadata.read(attachment.metadata) ?: return
        val (operationId, sourceId) = completedSessions.remove(envelope.uniqueId) ?: return
        if (sessions.get(operationId)?.sourceId == sourceId) {
            sessions.delete(operationId)
        }
    }

    fun discardSavedUpload(attachment: SceytAttachment) {
        val operationId = "upload:" + attachment.messageTid
        val saved = sessions.get(operationId)
        SceytChatUIKit.fileTransfer.transport.discardUpload(operationId, saved?.transportCheckpointData)
        sessions.delete(operationId)
        completedSessions.entries.removeAll { it.value.first == operationId }
    }

    private fun discardSource(
        operationId: String,
        transport: FileTransferTransport,
        source: EncryptedUploadSource,
    ) {
        val saved = sessions.get(operationId)
        if (saved?.sourceId == source.getSourceId()) {
            transport.discardUpload(operationId, saved.transportCheckpointData)
        }
        source.discard()
    }

    internal class Transfer(
        private val operationId: String,
        private val transport: FileTransferTransport,
        private val execute: suspend (Transfer, FileTransferCallback) -> FileTransferResult<String>,
    ) {
        var attachment: SceytAttachment? = null
            internal set
        private val lock = Any()
        private var discarded = false
        private var discard: (() -> Unit)? = null

        @Volatile
        private var transferring = false

        suspend fun execute(callback: FileTransferCallback): FileTransferResult<String> =
            execute.invoke(this, callback)

        fun applyUploadMetadata(current: SceytAttachment): SceytAttachment {
            val prepared = attachment ?: return current
            val envelope = FileEncryptionMetadata.read(prepared.metadata)
            val metadata = if (envelope != null) {
                FileEncryptionMetadata.write(current.metadata, envelope)
            } else {
                FileEncryptionMetadata.remove(current.metadata)
            }
            return current.copy(
                metadata = metadata,
                fileSize = if (envelope != null) prepared.fileSize else current.fileSize,
            )
        }

        suspend fun executeTransport(
            block: suspend () -> FileTransferResult<String>,
        ): FileTransferResult<String> {
            currentCoroutineContext().ensureActive()
            transferring = true
            return try {
                block()
            } finally {
                transferring = false
            }
        }

        fun onDiscard(action: () -> Unit) = synchronized(lock) {
            discard = action
            if (discarded) action()
        }

        fun discard() = synchronized(lock) {
            discarded = true
            discard?.invoke()
        }

        fun pause(): Boolean = transferring && transport.pause(operationId)

        fun resume(): Boolean = transferring && transport.resume(operationId)
    }
}
