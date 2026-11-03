package com.sceyt.chatuikit.filetransfer.encryption

import com.sceyt.chat.fileencryption.FileEncryptionData
import com.sceyt.chat.fileencryption.FileEncryptionProvider
import com.sceyt.chat.fileencryption.PreparedUpload
import com.sceyt.chat.fileencryption.StreamingAeadFileEncryptionProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import com.sceyt.chat.fileencryption.FileEncryptionCheckpoint as SdkFileEncryptionCheckpoint
import com.sceyt.chat.fileencryption.FileEncryptionInfo as SdkFileEncryptionInfo

/** Keeps the SDK's types and blocking calls behind UIKit's encryption interface. */
internal class ChatSdkFileEncryption(
    internal val provider: FileEncryptionProvider = StreamingAeadFileEncryptionProvider(),
) : FileEncryption {

    override suspend fun prepare(file: File): FileEncryptionSession = createSession {
        provider.prepareUpload(file, StreamingAeadFileEncryptionProvider.newUniqueId())
    }

    override suspend fun restore(
        file: File,
        checkpoint: FileEncryptionCheckpoint
    ): FileEncryptionSession =
        createSession {
            provider.restoreUpload(
                file,
                SdkFileEncryptionCheckpoint(
                    checkpoint.encryptionInfo.toSdkInfo(checkpoint.plaintextSize),
                    checkpoint.data,
                ),
            )
        }

    private suspend fun createSession(prepare: () -> PreparedUpload): Session {
        var upload: PreparedUpload? = null
        return try {
            runInterruptible(Dispatchers.IO) {
                val prepared = prepare()
                upload = prepared
                Session(prepared)
            }
        } catch (error: Throwable) {
            withContext(NonCancellable + Dispatchers.IO) { upload?.close() }
            throw error
        }
    }

    internal class Session(internal val upload: PreparedUpload) : FileEncryptionSession {
        private val closed = AtomicBoolean()

        override val sourceId: String = upload.sourceId
        override val plaintextSize: Long = upload.plaintextSize
        override val ciphertextSize: Long = upload.ciphertextSize
        override val encryptionInfo: FileEncryptionInfo = upload.encryptionInfo.toUIKitInfo()

        override suspend fun read(offset: Long, length: Int): ByteArray =
            runInterruptible(Dispatchers.IO) {
                check(!closed.get()) { "Encryption session is closed" }
                upload.readCiphertextRange(offset, length)
            }

        override fun checkpoint(): FileEncryptionCheckpoint {
            check(!closed.get()) { "Encryption session is closed" }
            val checkpoint = upload.checkpoint
            return FileEncryptionCheckpoint(
                encryptionInfo = checkpoint.encryptionInfo.toUIKitInfo(),
                plaintextSize = checkpoint.encryptionInfo.data.plainSize,
                data = checkpoint.data,
            )
        }

        override fun close() {
            if (closed.compareAndSet(false, true)) upload.close()
        }
    }
}

internal fun SdkFileEncryptionInfo.toUIKitInfo(): FileEncryptionInfo =
    FileEncryptionInfo(format, data.uniqueId, data.key)

internal fun FileEncryptionInfo.toSdkInfo(plaintextSize: Long): SdkFileEncryptionInfo =
    SdkFileEncryptionInfo(algorithm, FileEncryptionData(attachmentId, key, plaintextSize))
