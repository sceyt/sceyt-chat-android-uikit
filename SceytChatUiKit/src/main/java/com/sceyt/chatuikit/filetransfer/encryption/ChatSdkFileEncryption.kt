package com.sceyt.chatuikit.filetransfer.encryption

import com.sceyt.chat.fileencryption.AeadFileEncryptionProvider
import com.sceyt.chat.fileencryption.AttachmentCipher
import com.sceyt.chat.fileencryption.FileEncryptionProvider
import com.sceyt.chat.fileencryption.PreparedUpload
import com.sceyt.chat.fileencryption.RandomAccessStream
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
        provider.prepareUpload(file, AeadFileEncryptionProvider.newAttachmentId())
    }

    override suspend fun restoreEncryption(
        file: File,
        checkpoint: FileEncryptionCheckpoint
    ): FileEncryptionSession =
        createSession {
            AttachmentCipher.restore(checkpoint.attachmentKey, checkpoint.header).use { cipher ->
                provider.restoreUpload(file, SdkFileEncryptionCheckpoint(cipher, checkpoint.data))
            }
        }

    override suspend fun prepareDecryption(
        encryptedFile: File,
        attachmentKey: String,
    ): FileDecryptionSession {
        var source: FileRandomAccessStream? = null
        var reader: RandomAccessStream? = null
        return try {
            runInterruptible(Dispatchers.IO) {
                val ciphertext = FileRandomAccessStream(encryptedFile).also { source = it }
                val header = ciphertext.read(0, AttachmentCipher.HEADER_SIZE)
                val plaintext = AttachmentCipher.restore(attachmentKey, header).use { cipher ->
                    cipher.newDecryptingRandomAccessStream(ciphertext).also { reader = it }
                }
                DecryptionSession(ciphertext.sourceId, plaintext)
            }
        } catch (error: Throwable) {
            withContext(NonCancellable + Dispatchers.IO) {
                // A returned view owns the source, including cleanup after cancellation.
                if (reader != null) reader.close() else source?.close()
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
        override val attachmentKey: String = upload.cipher.attachmentKey

        override suspend fun read(offset: Long, length: Int): ByteArray =
            runInterruptible(Dispatchers.IO) {
                check(!closed.get()) { "Encryption session is closed" }
                upload.readCiphertextRange(offset, length)
            }

        override fun checkpoint(): FileEncryptionCheckpoint {
            check(!closed.get()) { "Encryption session is closed" }
            val checkpoint = upload.checkpoint
            return FileEncryptionCheckpoint(
                attachmentKey = checkpoint.cipher.attachmentKey,
                header = checkpoint.cipher.header,
                data = checkpoint.data,
            )
        }

        override fun close() {
            if (closed.compareAndSet(false, true)) upload.closeResources()
        }
    }

    private class DecryptionSession(
        override val sourceId: String,
        private val reader: RandomAccessStream,
    ) : FileDecryptionSession {
        private val closed = AtomicBoolean()
        override val plaintextSize: Long = reader.size()

        override suspend fun read(offset: Long, length: Int): ByteArray =
            runInterruptible(Dispatchers.IO) {
                check(!closed.get()) { "Decryption session is closed" }
                reader.read(offset, length)
            }

        override fun close() {
            if (!closed.compareAndSet(false, true)) return
            reader.close()
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
