package com.sceyt.chatuikit.persistence.file_transfer

import android.content.Context
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.sceyt.chat.ChatClient
import com.sceyt.chat.download.MediaDownload
import com.sceyt.chat.download.MediaDownloadException
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferCallback
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import com.sceyt.chatuikit.filetransfer.FileTransferType
import com.sceyt.chatuikit.filetransfer.download.ChatSdkFileDownloader
import com.sceyt.chatuikit.filetransfer.encryption.ChatSdkFileEncryption
import com.sceyt.chatuikit.filetransfer.encryption.FileEncryptionCheckpoint
import com.sceyt.chatuikit.filetransfer.encryption.FileEncryptionMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.RandomAccessFile
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
                val flags = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
                assertThat(Base64.decode(id, flags)).hasLength(16)
                val bytes = session.read(0, session.ciphertextSize.toInt())
                session.checkpoint() to bytes
            }

            val gson = Gson()
            val saved = gson.fromJson(gson.toJson(checkpoint), FileEncryptionCheckpoint::class.java)
            ChatSdkFileEncryption().restoreEncryption(file, saved).use { restored ->
                assertThat(restored.encryptionInfo.attachmentId)
                    .isEqualTo(checkpoint.encryptionInfo.attachmentId)
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
        val sizes = listOf(0, 1, RECORD_SIZE - 1, RECORD_SIZE, RECORD_SIZE + 1, PART_SIZE + 37)
        sizes.forEach { size ->
            prepareAttachment(size).use { attachment ->
                attachment.assertPublished(attachment.download())
            }
        }
    }

    @Test
    fun nativeDownloadAcceptsFullResponsesWhenRangesAreIgnored() = runBlocking(Dispatchers.IO) {
        prepareAttachment().use { attachment ->
            attachment.server.ignoreRanges = true
            attachment.assertPublished(attachment.download())
        }
    }

    @Test
    fun nativeRequestsCarryChatHeadersWithoutWaafiCredentials() = runBlocking(Dispatchers.IO) {
        prepareAttachment().use { attachment ->
            val headers = mapOf("X-Chat-Token" to "fixture-token", "X-Message-Id" to "1001")

            attachment.assertPublished(attachment.download(headers = headers))

            assertThat(attachment.server.requests).isNotEmpty()
            attachment.server.requests.forEach { request ->
                assertThat(request["x-chat-token"]).isEqualTo("fixture-token")
                assertThat(request["x-message-id"]).isEqualTo("1001")
                assertThat(request["authorization"]).isNull()
                assertThat(request["cookie"]).isNull()
            }
        }
    }

    @Test
    fun nativeRedirectDoesNotForwardChatCredentialsToAnotherOrigin() = runBlocking(Dispatchers.IO) {
        prepareAttachment().use { attachment ->
            AttachmentHttpServer(attachment.ciphertext).use { redirect ->
                redirect.redirectTo = attachment.server.url
                val headers = mapOf("X-Chat-Token" to "fixture-token", "X-Message-Id" to "1001")

                val result = attachment.download(url = redirect.url, headers = headers)
                attachment.assertPublished(result)

                assertThat(redirect.requests).isNotEmpty()
                assertThat(attachment.server.requests).isNotEmpty()
                attachment.server.requests.forEach { request ->
                    assertThat(request["x-chat-token"]).isNull()
                    assertThat(request["x-message-id"]).isNull()
                }
            }
        }
    }

    @Test
    fun nativeDecryptionResumesWithANewDownloaderAfterCancellation() = runBlocking(Dispatchers.IO) {
        prepareAttachment(8 * 1024 * 1024 + 37).use { attachment ->
            attachment.server.chunkDelayMillis = 8
            lateinit var attempt: Deferred<FileTransferResult<String>>
            attempt = async(start = CoroutineStart.LAZY) {
                attachment.download { progress ->
                    // Wait for a verified range to reach the native resume journal.
                    val saved = File(attachment.destination.path + ".part.json").isFile
                    if (saved && progress > 0f && progress < 100f) attempt.cancel()
                }
            }
            attempt.start()
            val failure = runCatching { attempt.await() }.exceptionOrNull()
            attempt.join()

            assertThat(failure).isInstanceOf(CancellationException::class.java)
            assertThat(File(attachment.destination.path + ".part").isFile).isTrue()
            assertThat(File(attachment.destination.path + ".part.json").isFile).isTrue()
            assertThat(attachment.destination.exists()).isFalse()

            attachment.server.chunkDelayMillis = 0
            attachment.assertPublished(attachment.download())
        }
    }

    @Test
    fun modifiedNativeCiphertextIsRejectedBeforePublishingPlaintext() =
        runBlocking(Dispatchers.IO) {
            prepareAttachment().use { attachment ->
                RandomAccessFile(attachment.ciphertext, "rw").use { file ->
                    val offset = attachment.source.length() - 1024
                    file.seek(offset)
                    val byte = file.read()
                    file.seek(offset)
                    file.write(byte xor 1)
                }
                attachment.assertRejected(attachment.download())
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
                        val length =
                            minOf(PART_SIZE.toLong(), session.ciphertextSize - offset).toInt()
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
        val server = AttachmentHttpServer(ciphertext)

        fun readAttachment(): JsonObject =
            JsonParser.parseString(metadataFile.readText()).asJsonObject

        suspend fun download(
            url: String = server.url,
            headers: Map<String, String> = emptyMap(),
            callback: FileTransferCallback = FileTransferCallback {},
        ): FileTransferResult<String> {
            val message = readAttachment()
            val attachment = SceytAttachment(
                id = null,
                messageId = 0,
                messageTid = 1,
                userId = null,
                name = "attachment.bin",
                type = "file",
                metadata = message.get("metadata").asString,
                fileSize = message.get("fileSize").asLong,
                createdAt = 0,
                url = url,
                filePath = null,
                transferState = TransferState.PendingDownload,
                progressPercent = 0f,
                originalFilePath = null,
                linkPreviewDetails = null,
            )
            return withTimeout(20_000) {
                ChatSdkFileDownloader().download(
                    request = FileDownloadRequest(
                        operationId = "download:${directory.name}",
                        url = url,
                        destinationFile = destination,
                        attachment = attachment,
                        type = FileTransferType.ENCRYPTED,
                        headers = headers,
                    ),
                    callback = callback,
                )
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
                .isInstanceOf(MediaDownloadException::class.java)
            assertThat((result.error as MediaDownloadException).reason)
                .isEqualTo(MediaDownloadException.Reason.INTEGRITY)
            assertThat(destination.exists()).isFalse()
            assertStagingRemoved()
        }

        private fun assertStagingRemoved() {
            listOf(".part", ".part.json", ".part.json.tmp").forEach { suffix ->
                assertThat(File(destination.path + suffix).exists()).isFalse()
            }
        }

        override fun close() {
            MediaDownload.cancelDownload(destination.path)
            server.close()
            directory.deleteRecursively()
        }
    }

    private companion object {
        const val RECORD_SIZE = 64 * 1024
        const val PART_SIZE = 5 * 1024 * 1024

        @BeforeClass
        @JvmStatic
        fun initializeSdk() {
            // Operation IDs use the SDK's private session directory; no chat connection is needed.
            val context = ApplicationProvider.getApplicationContext<Context>()
            ChatClient.initialize(
                context,
                "https://127.0.0.1",
                "native-transfer-test",
                "android-test"
            )
        }
    }
}
