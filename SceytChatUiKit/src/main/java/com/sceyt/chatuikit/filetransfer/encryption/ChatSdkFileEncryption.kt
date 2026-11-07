package com.sceyt.chatuikit.filetransfer.encryption

import com.sceyt.chat.fileencryption.AeadFileEncryptionProvider
import com.sceyt.chat.fileencryption.FileEncryptionProvider
import com.sceyt.chat.fileencryption.PlaintextReader
import com.sceyt.chat.fileencryption.PreparedUpload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import com.sceyt.chat.fileencryption.FileEncryptionCheckpoint as SdkFileEncryptionCheckpoint

/** Keeps the SDK's types and blocking calls behind UIKit's encryption interface. */
internal class ChatSdkFileEncryption(
    private val provider: FileEncryptionProvider = AeadFileEncryptionProvider(),
) : FileEncryption {

    override suspend fun prepareEncryption(file: File): FileEncryptionSession = createSession {
        provider.prepareUpload(file, AeadFileEncryptionProvider.newUniqueId())
    }

    override suspend fun restoreEncryption(
        file: File,
        checkpoint: FileEncryptionCheckpoint
    ): FileEncryptionSession =
        createSession {
            AeadFileEncryptionProvider.restore(checkpoint.token).use { cipher ->
                provider.restoreUpload(file, SdkFileEncryptionCheckpoint(cipher, checkpoint.data))
            }
        }

    override suspend fun prepareDecryption(
        encryptedFile: File,
        token: String,
    ): FileDecryptionSession {
        var source: FileCiphertextRangeSource? = null
        var reader: PlaintextReader? = null
        return try {
            runInterruptible(Dispatchers.IO) {
                val ciphertext = FileCiphertextRangeSource(encryptedFile).also { source = it }
                val plaintext = AeadFileEncryptionProvider.restore(token).use { cipher ->
                    provider.openDownload(ciphertext, cipher).also { reader = it }
                }
                DecryptionSession(ciphertext, plaintext)
            }
        } catch (error: Throwable) {
            withContext(NonCancellable + Dispatchers.IO) {
                try {
                    reader?.close()
                } finally {
                    source?.close()
                }
            }
            throw error
        }
    }

    private suspend fun createSession(prepare: () -> PreparedUpload): EncryptionSession {
        var upload: PreparedUpload? = null
        return try {
            runInterruptible(Dispatchers.IO) {
                val prepared = prepare()
                upload = prepared
                EncryptionSession(prepared)
            }
        } catch (error: Throwable) {
            withContext(NonCancellable + Dispatchers.IO) { upload?.closeResources() }
            throw error
        }
    }

    internal class EncryptionSession(
        internal val upload: PreparedUpload
    ) : FileEncryptionSession {
        private val closed = AtomicBoolean()

        override val sourceId: String = upload.sourceId
        override val plaintextSize: Long = upload.plaintextSize
        override val ciphertextSize: Long = upload.ciphertextSize
        override val token: String = upload.cipher.token

        override suspend fun read(offset: Long, length: Int): ByteArray =
            runInterruptible(Dispatchers.IO) {
                check(!closed.get()) { "Encryption session is closed" }
                upload.readCiphertextRange(offset, length)
            }

        override fun checkpoint(): FileEncryptionCheckpoint {
            check(!closed.get()) { "Encryption session is closed" }
            val checkpoint = upload.checkpoint
            return FileEncryptionCheckpoint(
                token = checkpoint.cipher.token,
                data = checkpoint.data,
            )
        }

        override fun close() {
            if (closed.compareAndSet(false, true)) upload.closeResources()
        }
    }

    private class DecryptionSession(
        private val source: FileCiphertextRangeSource,
        private val reader: PlaintextReader,
    ) : FileDecryptionSession {
        private val closed = AtomicBoolean()
        override val sourceId: String = source.sourceId
        override val plaintextSize: Long = reader.plaintextSize

        override suspend fun read(offset: Long, length: Int): ByteArray =
            runInterruptible(Dispatchers.IO) {
                check(!closed.get()) { "Decryption session is closed" }
                reader.read(offset, length)
            }

        override fun close() {
            if (!closed.compareAndSet(false, true)) return
            source.use {
                reader.close()
            }
        }
    }
}

private fun PreparedUpload.closeResources() {
    try {
        close()
    } finally {
        cipher.close()
    }
}
