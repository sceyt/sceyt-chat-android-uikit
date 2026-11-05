package com.sceyt.chatuikit.persistence.logicimpl

import com.google.common.truth.Truth.assertThat
import com.google.gson.JsonParser
import com.sceyt.chat.fileencryption.CiphertextRangeSource
import com.sceyt.chat.fileencryption.FileEncryptionData
import com.sceyt.chat.fileencryption.FileEncryptionProvider
import com.sceyt.chat.fileencryption.PlaintextReader
import com.sceyt.chat.fileencryption.StreamingAeadFileEncryptionProvider
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.filetransfer.AttachmentTransferContext
import com.sceyt.chatuikit.filetransfer.FileTransferDestinationProvider
import com.sceyt.chatuikit.filetransfer.FileUploadType
import com.sceyt.chatuikit.filetransfer.SceytChatUIKitFileTransfer
import com.sceyt.chatuikit.filetransfer.defaults.DefaultFileTransferTransport
import com.sceyt.chatuikit.filetransfer.encryption.ChatSdkFileEncryption
import com.sceyt.chatuikit.filetransfer.encryption.FileEncryption
import com.sceyt.chatuikit.filetransfer.encryption.FileEncryptionInfo
import com.sceyt.chatuikit.filetransfer.encryption.FileEncryptionMetadata
import com.sceyt.chatuikit.filetransfer.encryption.toUIKitInfo
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.net.InetSocketAddress
import com.sceyt.chat.fileencryption.FileEncryptionInfo as SdkFileEncryptionInfo

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class AttachmentEncryptedTransferTest {
    @get:Rule
    val temporary = TemporaryFolder()

    private lateinit var previous: SceytChatUIKitFileTransfer
    private lateinit var transport: RecordingFileTransferTransport
    private lateinit var encryption: FileEncryption

    @Before
    fun setUp() {
        previous = SceytChatUIKit.fileTransfer
        transport = RecordingFileTransferTransport()
        encryption = mock()
        SceytChatUIKit.fileTransfer = SceytChatUIKitFileTransfer().apply {
            this.transport = this@AttachmentEncryptedTransferTest.transport
            this.encryption = this@AttachmentEncryptedTransferTest.encryption
            encryptUploads = true
        }
    }

    @After
    fun tearDown() {
        SceytChatUIKit.fileTransfer = previous
    }

    @Test
    fun `custom uploader receives a file and its result supplies metadata and plaintext size`() = runTest {
        val file = temporary.newFile("attachment.bin").apply { writeText("original plaintext") }
        val originalSize = file.length()
        val value = attachment(filePath = file.path, originalFilePath = file.path, fileSize = 999L).copy(
            metadata = """{"szw":1280,"szh":203,"tmb":"blur-thumbnail"}""",
        )
        val task = transferTask(value)
        val finished = CompletableDeferred<SceytResponse<String>>()
        val coordinator = AttachmentUploadCoordinator(RuntimeEnvironment.getApplication(), mock(), backgroundScope)
        coordinator.upload(value, task, { _, result -> finished.complete(result) }, {})
        runCurrent()
        val call = transport.uploadCalls.single()
        val request = call.request
        val info = FileEncryptionInfo("custom-returned-format", "custom-file-id", "custom-file-key")

        assertThat(request.sourceFile).isEqualTo(file)
        assertThat(request.type).isEqualTo(FileUploadType.ENCRYPTED)
        assertThat((request.context as AttachmentTransferContext).attachment.metadata).isEqualTo(value.metadata)
        // The transport has read the original bytes; the completion response can arrive later.
        file.appendText("edited after the transport read the file")
        call.succeed("https://waafi.test/completed", info)

        assertThat((finished.await() as SceytResponse.Success).data).isEqualTo("https://waafi.test/completed")
        assertThat(task.attachment.fileSize).isEqualTo(originalSize)
        assertThat(checkNotNull(FileEncryptionMetadata.read(task.attachment.metadata)).toEncryptionInfo()).isEqualTo(info)
        val metadata = JsonParser.parseString(task.attachment.metadata).asJsonObject
        assertThat(metadata.get("szw").asInt).isEqualTo(1280)
        assertThat(metadata.get("tmb").asString).isEqualTo("blur-thumbnail")
        verify(encryption, never()).prepareEncryption(any())
    }

    @Test
    fun `encrypted success without encryption information fails instead of publishing an unusable attachment`() = runTest {
        val file = temporary.newFile("missing-info.bin").apply { writeText("plaintext") }
        val value = attachment(filePath = file.path, originalFilePath = file.path)
        val task = transferTask(value)
        val finished = CompletableDeferred<SceytResponse<String>>()
        AttachmentUploadCoordinator(RuntimeEnvironment.getApplication(), mock(), backgroundScope)
            .upload(value, task, { _, result -> finished.complete(result) }, {})
        runCurrent()

        transport.uploadCalls.single().succeed("https://waafi.test/missing-key")

        assertThat(finished.await()).isInstanceOf(SceytResponse.Error::class.java)
        assertThat(FileEncryptionMetadata.read(task.attachment.metadata)).isNull()
    }

    @Test
    fun `pause retains the upload while explicit cancel notifies the custom transport`() = runTest {
        val file = temporary.newFile("paused.bin").apply { writeText("plaintext") }
        val value = attachment(filePath = file.path, originalFilePath = file.path)
        val task = transferTask(value)
        val coordinator = AttachmentUploadCoordinator(RuntimeEnvironment.getApplication(), mock(), backgroundScope)
        coordinator.upload(value, task, { _, _ -> }, {})
        runCurrent()

        coordinator.pause(task) {}
        runCurrent()
        assertThat(transport.uploadCalls.single().cancelled).isTrue()
        assertThat(transport.cancelCalls).isEmpty()

        coordinator.upload(value, task, { _, _ -> }, {})
        runCurrent()
        assertThat(transport.uploadCalls).hasSize(2)
        coordinator.cancel(task)
        runCurrent()

        assertThat(transport.cancelCalls).containsExactly("upload:" + value.messageTid)
        assertThat(transport.uploadCalls.last().cancelled).isTrue()
    }

    @Test
    fun `shared encrypted upload preserves each callers media metadata`() = runTest {
        val file = temporary.newFile("shared.bin").apply { writeText("shared plaintext") }
        val first = attachment(messageTid = 1001, filePath = file.path, originalFilePath = file.path)
            .copy(metadata = """{"caption":"first","szw":100}""")
        val second = attachment(messageTid = 1002, filePath = file.path, originalFilePath = file.path)
            .copy(metadata = """{"caption":"second","szw":200}""")
        val firstTask = transferTask(first)
        val secondTask = transferTask(second)
        val firstResult = CompletableDeferred<SceytResponse<String>>()
        val secondResult = CompletableDeferred<SceytResponse<String>>()
        val coordinator = AttachmentUploadCoordinator(RuntimeEnvironment.getApplication(), mock(), backgroundScope)
        coordinator.upload(first, firstTask, { _, result -> firstResult.complete(result) }, {}, shared = true)
        coordinator.upload(second, secondTask, { _, result -> secondResult.complete(result) }, {}, shared = true)
        runCurrent()
        val info = FileEncryptionInfo("shared-format", "shared-id", "shared-key")

        transport.uploadCalls.single().succeed("https://waafi.test/shared", info)

        assertThat(firstResult.await()).isInstanceOf(SceytResponse.Success::class.java)
        assertThat(secondResult.await()).isInstanceOf(SceytResponse.Success::class.java)
        assertThat(transport.uploadCalls).hasSize(1)
        assertThat(JsonParser.parseString(firstTask.attachment.metadata).asJsonObject.get("caption").asString)
            .isEqualTo("first")
        assertThat(JsonParser.parseString(secondTask.attachment.metadata).asJsonObject.get("caption").asString)
            .isEqualTo("second")
        assertThat(FileEncryptionMetadata.read(firstTask.attachment.metadata))
            .isEqualTo(FileEncryptionMetadata.read(secondTask.attachment.metadata))
        assertThat(secondTask.attachment.fileSize).isEqualTo(file.length())
    }

    @Test
    fun `receiver downloads a full HTTP 200 response then publishes SDK verified plaintext`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val previous = SceytChatUIKit.fileTransfer
        val destination = File.createTempFile("decrypted-attachment", ".bin", context.cacheDir)
        destination.delete()
        val plaintext = "verified attachment".toByteArray()
        val provider = mock<FileEncryptionProvider>()
        val reader = mock<PlaintextReader>()
        val ciphertext = "encrypted attachment fixture".toByteArray()
        val info = SdkFileEncryptionInfo(
            StreamingAeadFileEncryptionProvider.FORMAT,
            FileEncryptionData("received-file", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", plaintext.size.toLong()),
        )
        doAnswer {
            val source = it.getArgument<CiphertextRangeSource>(0)
            assertThat(source.readCiphertextRange(0, ciphertext.size)).isEqualTo(ciphertext)
            reader
        }.whenever(provider).openDownload(any(), eq(info))
        whenever(reader.plaintextSize).thenReturn(plaintext.size.toLong())
        doAnswer {
            val offset = it.getArgument<Long>(0).toInt()
            plaintext.copyOfRange(offset, offset + it.getArgument<Int>(1))
        }.whenever(reader).read(any(), any())
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/blob") { exchange ->
            exchange.sendResponseHeaders(200, ciphertext.size.toLong())
            exchange.responseBody.use { it.write(ciphertext) }
            exchange.close()
        }
        server.start()
        try {
            SceytChatUIKit.fileTransfer = SceytChatUIKitFileTransfer().apply {
                encryption = ChatSdkFileEncryption(provider)
                transport = DefaultFileTransferTransport()
                destinationProvider = FileTransferDestinationProvider { _, _ -> destination }
            }
            val value = attachment(
                messageTid = System.nanoTime(),
                filePath = null,
                originalFilePath = null,
                state = TransferState.PendingDownload,
                fileSize = plaintext.size.toLong(),
                url = "http://127.0.0.1:${server.address.port}/blob",
            ).copy(
                metadata = FileEncryptionMetadata.write(
                    metadata = null,
                    envelope = FileEncryptionMetadata.fromEncryptionInfo(info.toUIKitInfo()),
                ),
            )
            val finished = CompletableDeferred<SceytResponse<String>>()
            AttachmentDownloadCoordinator(context, backgroundScope).download(
                attachment = value,
                task = transferTask(value),
                onResult = { finished.complete(it) },
            )

            assertThat((finished.await() as SceytResponse.Success).data).isEqualTo(destination.path)
            assertThat(destination.readBytes()).isEqualTo(plaintext)
            assertThat(File(destination.path + ".sceyt.part").exists()).isFalse()
            assertThat(File(destination.path + ".sceyt.enc").exists()).isFalse()
            assertThat(File(destination.path + ".sceyt.enc.part").exists()).isFalse()
            verify(reader).close()
        } finally {
            server.stop(0)
            SceytChatUIKit.fileTransfer = previous
            destination.delete()
        }
    }
}
