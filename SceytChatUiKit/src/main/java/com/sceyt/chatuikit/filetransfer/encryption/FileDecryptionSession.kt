package com.sceyt.chatuikit.filetransfer.encryption

/** Verified plaintext from a completed encrypted file. Owns its open file resources. */
interface FileDecryptionSession : AutoCloseable {
    val sourceId: String
    val plaintextSize: Long

    /** Returns exactly [length] authenticated plaintext bytes, or throws. */
    suspend fun read(offset: Long, length: Int): ByteArray

    override fun close()
}
