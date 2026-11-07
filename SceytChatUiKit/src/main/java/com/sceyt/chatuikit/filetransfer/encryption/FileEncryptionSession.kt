package com.sceyt.chatuikit.filetransfer.encryption

/** One encrypted file, generated on demand without a complete ciphertext copy. */
interface FileEncryptionSession : AutoCloseable {
    val sourceId: String
    val plaintextSize: Long
    val ciphertextSize: Long
    val token: String

    /** Exact ciphertext range, including overhead. Repeated reads return identical bytes. */
    suspend fun read(offset: Long, length: Int): ByteArray

    /** Save after reads and before submitting bytes; contains key material. */
    fun checkpoint(): FileEncryptionCheckpoint

    /** Releases resources without aborting an upload or deleting its saved state. */
    override fun close()
}
