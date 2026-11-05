package com.sceyt.chatuikit.persistence.logicimpl

import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.filetransfer.FileTransferCallback
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import com.sceyt.chatuikit.filetransfer.FileTransferTransport
import com.sceyt.chatuikit.filetransfer.encryption.FileEncryptionMetadata
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Executes a routed transfer and keeps its controls bound to the selected transport. */
internal class TransferExecutor(
    private val operationId: String,
    private val transport: FileTransferTransport,
    private val execute: suspend (TransferExecutor, FileTransferCallback) -> FileTransferResult<String>,
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

    suspend fun <T> executeTransport(
        block: suspend () -> FileTransferResult<T>,
    ): FileTransferResult<T> {
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
        // Discard may be requested before registration, so run the cleanup as soon as it is available.
        if (discarded) action()
    }

    fun discard() = synchronized(lock) {
        discarded = true
        discard?.invoke()
    }

    fun pause(): Boolean = transferring && transport.pause(operationId)

    fun resume(): Boolean = transferring && transport.resume(operationId)
}
