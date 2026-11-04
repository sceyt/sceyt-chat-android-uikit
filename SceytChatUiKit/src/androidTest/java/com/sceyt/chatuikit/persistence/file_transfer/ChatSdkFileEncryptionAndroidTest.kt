package com.sceyt.chatuikit.persistence.file_transfer

import android.content.Context
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferCallback
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import com.sceyt.chatuikit.filetransfer.encryption.ChatSdkFileEncryption
import com.sceyt.chatuikit.filetransfer.encryption.EncryptedFileDownloader
import com.sceyt.chatuikit.filetransfer.encryption.FileDownloadEncryption
import com.sceyt.chatuikit.filetransfer.encryption.FileEncryptionCheckpoint
import com.sceyt.chatuikit.filetransfer.encryption.FileEncryptionMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.RandomAccessFile
import java.security.GeneralSecurityException
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ChatSdkFileEncryptionAndroidTest {
    @Test
    fun generatedAttachmentIdsWorkWithNativePreparationAndRestore() = runBlocking(Dispatchers.IO) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File.createTempFile("attachment-id", ".bin", context.cacheDir)
        try {
            file.writeBytes(ByteArray(64 * 1024 + 1) { (it % 251).toByte() })
            val encryption = ChatSdkFileEncryption()
            val (checkpoint, ciphertext) = encryption.prepareEncryption(file).use { session ->
                val id = session.encryptionInfo.attachmentId
                assertThat(id).matches("[A-Za-z0-9_-]{22}")
                assertThat(Base64.decode(id, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)).hasLength(16)
                val bytes = session.read(0, session.ciphertextSize.toInt())
                session.checkpoint() to bytes
            }

            val gson = Gson()
            val saved = gson.fromJson(gson.toJson(checkpoint), FileEncryptionCheckpoint::class.java)
            ChatSdkFileEncryption().restoreEncryption(file, saved).use { restored ->
                assertThat(restored.encryptionInfo.attachmentId).isEqualTo(checkpoint.encryptionInfo.attachmentId)
                assertThat(restored.read(0, ciphertext.size)).isEqualTo(ciphertext)
            }
            encryption.prepareEncryption(file).use { next ->
                assertThat(next.encryptionInfo.attachmentId).isNotEqualTo(checkpoint.encryptionInfo.attachmentId)
            }
        } finally {
            file.delete()
        }
    }

    @Test
    fun nativeRoundTripUsesSerializedAttachmentMetadata() = runBlocking(Dispatchers.IO) {
        listOf(0, 1, RECORD_SIZE - 1, RECORD_SIZE, RECORD_SIZE + 1, PART_SIZE + 37).forEach { size ->
            prepareAttachment(size).use { attachment ->
                attachment.assertPublished(attachment.download())
            }
        }
    }

    @Test
    fun nativeDecryptionResumesWithANewDownloaderAfterCancellation() = runBlocking(Dispatchers.IO) {
        prepareAttachment().use { attachment ->
            lateinit var attempt: Deferred<FileTransferResult<String>>
            attempt = async(start = CoroutineStart.LAZY) {
                attachment.download { progress ->
                    if (progress > 90f && progress < 100f) attempt.cancel()
                }
            }
            attempt.start()
            val failure = runCatching { attempt.await() }.exceptionOrNull()
            attempt.join()

            assertThat(failure).isInstanceOf(CancellationException::class.java)
            val partial = File(attachment.destination.path + ".sceyt.part")
            assertThat(partial.length()).isGreaterThan(0L)
            assertThat(partial.length()).isLessThan(attachment.source.length())
            assertThat(attachment.destination.exists()).isFalse()

            attachment.assertPublished(attachment.download())
            assertThat(attachment.downloads).isEqualTo(1)
        }
    }

    @Test
    fun modifiedNativeCiphertextRemovesAlreadyVerifiedPartialPlaintext() = runBlocking(Dispatchers.IO) {
        prepareAttachment().use { attachment ->
            RandomAccessFile(attachment.ciphertext, "rw").use { file ->
                val offset = attachment.source.length() - 1024
                file.seek(offset)
                val byte = file.read()
                file.seek(offset)
                file.write(byte xor 1)
            }
            val progress = mutableListOf<Float>()

            attachment.assertRejected(attachment.download { progress += it })

            assertThat(progress.any { it > 90f }).isTrue()
        }
    }

    @Test
    fun nativeDownloadRejectsAKeyChangedInAttachmentMetadata() = runBlocking(Dispatchers.IO) {
        prepareAttachment().use { attachment ->
            val message = attachment.readAttachment()
            val metadata = message.get("metadata").asString
            val envelope = checkNotNull(FileEncryptionMetadata.read(metadata))
            val differentKey = ChatSdkFileEncryption().prepareEncryption(attachment.source).use {
                it.encryptionInfo.key
            }
            message.addProperty(
                "metadata",
                FileEncryptionMetadata.write(metadata, envelope.copy(key = differentKey)),
            )
            attachment.metadataFile.writeText(message.toString())

            attachment.assertRejected(attachment.download())
        }
    }

    @Test
    fun nativeDownloadRejectsTruncatedCiphertext() = runBlocking(Dispatchers.IO) {
        prepareAttachment().use { attachment ->
            RandomAccessFile(attachment.ciphertext, "rw").use {
                it.setLength(it.length() - 1)
            }

            attachment.assertRejected(attachment.download())
        }
    }

    @Test
    fun nativeDownloadRejectsAnIncorrectPlaintextSize() = runBlocking(Dispatchers.IO) {
        prepareAttachment().use { attachment ->
            val message = attachment.readAttachment()
            message.addProperty("fileSize", message.get("fileSize").asLong + 1)
            attachment.metadataFile.writeText(message.toString())

            attachment.assertRejected(attachment.download())
        }
    }

    private suspend fun prepareAttachment(size: Int = 2 * 1024 * 1024 + 37): NativeAttachment {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directory = File(context.cacheDir, "native-transfer-" + UUID.randomUUID()).apply {
            check(mkdirs())
        }
        val attachment = NativeAttachment(directory)
        try {
            attachment.source.writeBytes(ByteArray(size) { (it % 251).toByte() })
            ChatSdkFileEncryption().prepareEncryption(attachment.source).use { session ->
                attachment.ciphertext.outputStream().use { output ->
                    var offset = 0L
                    while (offset < session.ciphertextSize) {
                        val length = minOf(PART_SIZE.toLong(), session.ciphertextSize - offset).toInt()
                        output.write(session.read(offset, length))
                        offset += length
                    }
                }
                val message = JsonObject().apply {
                    addProperty("fileSize", session.plaintextSize)
                    addProperty(
                        "metadata",
                        FileEncryptionMetadata.write(
                            metadata = """{"caption":"native attachment"}""",
                            envelope = FileEncryptionMetadata.fromEncryptionInfo(session.encryptionInfo),
                        ),
                    )
                }
                attachment.metadataFile.writeText(message.toString())
            }
            return attachment
        } catch (error: Throwable) {
            attachment.close()
            throw error
        }
    }

    private class NativeAttachment(private val directory: File) : AutoCloseable {
        val source = File(directory, "source.bin")
        val ciphertext = File(directory, "uploaded.bin")
        val metadataFile = File(directory, "attachment.json")
        val destination = File(directory, "received.bin")
        private val checkpoints = File(directory, "checkpoints")
        var downloads = 0
            private set

        fun readAttachment(): JsonObject = JsonParser.parseString(metadataFile.readText()).asJsonObject

        suspend fun download(
            callback: FileTransferCallback = FileTransferCallback {},
        ): FileTransferResult<String> {
            val message = readAttachment()
            val envelope = checkNotNull(FileEncryptionMetadata.read(message.get("metadata").asString))
            return EncryptedFileDownloader(checkpoints).download(
                request = FileDownloadRequest(
                    "download:native", "native-fixture", destination,
                ),
                encryptionInfo = FileDownloadEncryption(
                    info = envelope.toEncryptionInfo(),
                    plaintextSize = message.get("fileSize").asLong,
                ),
                encryption = ChatSdkFileEncryption(),
                callback = callback,
            ) { request, progress ->
                downloads++
                ciphertext.copyTo(request.destinationFile, overwrite = true)
                progress.onProgress(100f)
                FileTransferResult.Success(request.destinationFile.path)
            }
        }

        fun assertPublished(result: FileTransferResult<String>) {
            assertThat(result).isEqualTo(FileTransferResult.Success(destination.path))
            assertThat(destination.readBytes()).isEqualTo(source.readBytes())
            assertStagingRemoved()
        }

        fun assertRejected(result: FileTransferResult<String>) {
            assertThat(result).isInstanceOf(FileTransferResult.Failure::class.java)
            assertThat((result as FileTransferResult.Failure).error)
                .isInstanceOf(GeneralSecurityException::class.java)
            assertThat(destination.exists()).isFalse()
            assertStagingRemoved()
        }

        private fun assertStagingRemoved() {
            listOf(".sceyt.part", ".sceyt.enc", ".sceyt.enc.part").forEach { suffix ->
                assertThat(File(destination.path + suffix).exists()).isFalse()
            }
            assertThat(checkpoints.listFiles().orEmpty().toList()).isEmpty()
        }

        override fun close() {
            directory.deleteRecursively()
        }
    }

    private companion object {
        const val RECORD_SIZE = 64 * 1024
        const val PART_SIZE = 5 * 1024 * 1024
    }
}
