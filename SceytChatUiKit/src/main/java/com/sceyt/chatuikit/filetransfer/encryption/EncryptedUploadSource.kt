package com.sceyt.chatuikit.filetransfer.encryption

import com.sceyt.chat.fileencryption.CiphertextRangeSource
import com.sceyt.chat.fileencryption.PreparedUpload
import com.sceyt.chatuikit.filetransfer.TransportCheckpointStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible

/** SDK byte reader plus serialized checkpoint updates for one active upload. */
internal class EncryptedUploadSource(
    val sdkUpload: PreparedUpload,
    initialSession: UploadSession,
    private val store: UploadSessionStore,
) : CiphertextRangeSource {
    private val lock = Any()
    private var session = initialSession
    private var closed = false
    private var discarded = false

    val envelope = FileEncryptionMetadata.fromEncryptionInfo(session.encryptionInfo)

    val checkpointStore = object : TransportCheckpointStore {
        override val sourceId: String = session.sourceId

        override suspend fun get(): String? = runInterruptible(Dispatchers.IO) {
            synchronized(lock) { session.transportCheckpointData }
        }

        override suspend fun save(data: String) = runInterruptible(Dispatchers.IO) {
            synchronized(lock) {
                check(!closed) { "Upload has stopped" }
                session = session.copy(transportCheckpointData = data)
                store.save(session)
            }
        }
    }

    override fun getCiphertextSize(): Long = sdkUpload.ciphertextSize

    override fun getSourceId(): String = sdkUpload.sourceId

    override fun readCiphertextRange(offset: Long, length: Int): ByteArray {
        val bytes = sdkUpload.readCiphertextRange(offset, length)
        checkpoint()
        return bytes
    }

    fun checkpoint() = synchronized(lock) {
        check(!closed) { "Upload has stopped" }
        session = session.copy(sdkCheckpointData = sdkUpload.checkpoint.data)
        store.save(session)
    }

    fun discard() = synchronized(lock) {
        discarded = true
        if (closed) deleteSession()
    }

    override fun close() {
        synchronized(lock) {
            if (closed) return
            closed = true
            try {
                sdkUpload.close()
            } finally {
                if (discarded) deleteSession()
            }
        }
    }

    private fun deleteSession() {
        if (store.get(session.operationId)?.sourceId == session.sourceId) {
            store.delete(session.operationId)
        }
    }
}
