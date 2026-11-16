package com.sceyt.chatuikit.persistence.logicimpl

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.google.gson.JsonParser
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferDestinationProvider
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import com.sceyt.chatuikit.filetransfer.FileTransferType
import com.sceyt.chatuikit.filetransfer.SceytChatUIKitFileTransfer
import com.sceyt.chatuikit.filetransfer.defaults.DefaultFileTransferProvider
import com.sceyt.chatuikit.filetransfer.download.ChatSdkFileDownloader
import com.sceyt.chatuikit.filetransfer.encryption.FileEncryption
import com.sceyt.chatuikit.filetransfer.encryption.FileEncryptionMetadata
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
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
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class AttachmentEncryptedTransferTest {
    @get:Rule
    val temporary = TemporaryFolder()

    private lateinit var previous: SceytChatUIKitFileTransfer
    private lateinit var transport: RecordingFileTransferProvider
    private lateinit var encryption: FileEncryption

    @Before
    fun setUp() {
        previous = SceytChatUIKit.fileTransfer
        transport = RecordingFileTransferProvider()
        encryption = mock()
        SceytChatUIKit.fileTransfer = SceytChatUIKitFileTransfer().apply {
            this.provider = this@AttachmentEncryptedTransferTest.transport
            this.encryption = this@AttachmentEncryptedTransferTest.encryption
            encryptionEnabled = true
        }
    }

    @After
    fun tearDown() {
        SceytChatUIKit.fileTransfer = previous
        SceytKoinApp.koinApp?.close()
        SceytKoinApp.koinApp = null
    }

    @Test
    fun `custom uploader prepares metadata and plaintext size before returning its URL`() =
        runTest {
            val file = temporary.newFile("attachment.bin").apply { writeText("original plaintext") }
            val originalSize = file.length()
            val value =
                attachment(filePath = file.path, originalFilePath = file.path, fileSize = 999L)
                    .copy(
                        metadata = """{"szw":1280,"szh":203,"tmb":"blur-thumbnail"}""",
                    )
            val task = transferTask(value)
            val finished = CompletableDeferred<SceytResponse<String>>()
            val token = "custom-sdk-token"
            transport.onPrepareUpload = {
                mapOf("sceyt.enc" to token)
            }
            val coordinator = AttachmentUploadCoordinator(
                RuntimeEnvironment.getApplication(),
                mock(),
                backgroundScope
            )
            coordinator.upload(value, task, { _, result -> finished.complete(result) }, {})
            runCurrent()
            val call = transport.uploadCalls.single()
            val request = call.request
            assertThat(request.sourceFile).isEqualTo(file)
            assertThat(request.type).isEqualTo(FileTransferType.ENCRYPTED)
            assertThat(FileEncryptionMetadata.read(request.attachment.metadata)).isEqualTo(token)
            assertThat(task.attachment.metadata).isEqualTo(request.attachment.metadata)
            // The transport has read the original bytes; the completion response can arrive later.
            file.appendText("edited after the transport read the file")
            call.succeed("https://waafi.test/completed")

            assertThat((finished.await() as SceytResponse.Success).data)
                .isEqualTo("https://waafi.test/completed")
            assertThat(task.attachment.fileSize).isEqualTo(originalSize)
            assertThat(FileEncryptionMetadata.read(task.attachment.metadata)).isEqualTo(token)
            val metadata = JsonParser.parseString(task.attachment.metadata).asJsonObject
            assertThat(metadata.get("szw").asInt).isEqualTo(1280)
            assertThat(metadata.get("tmb").asString).isEqualTo("blur-thumbnail")
            verify(encryption, never()).prepareEncryption(any())
        }

    @Test
    fun `custom encrypted upload keeps its own string metadata without standard encryption information`() =
        runTest {
            val file = temporary.newFile("missing-info.bin").apply { writeText("plaintext") }
            val value = attachment(filePath = file.path, originalFilePath = file.path)
            val task = transferTask(value)
            val finished = CompletableDeferred<SceytResponse<String>>()
            transport.onPrepareUpload = { mapOf("app.enc" to "custom-format", "customKey" to "app-key") }
            AttachmentUploadCoordinator(
                RuntimeEnvironment.getApplication(),
                mock(),
                backgroundScope
            )
                .upload(value, task, { _, result -> finished.complete(result) }, {})
            runCurrent()

            transport.uploadCalls.single().succeed("https://waafi.test/missing-key")

            assertThat(finished.await()).isInstanceOf(SceytResponse.Success::class.java)
            val metadata = JsonParser.parseString(task.attachment.metadata).asJsonObject
            assertThat(metadata.get("app.enc").asString).isEqualTo("custom-format")
            assertThat(metadata.get("customKey").asString).isEqualTo("app-key")
        }

    @Test
    fun `pause retains the upload while explicit cancel notifies the custom transport`() = runTest {
        val file = temporary.newFile("paused.bin").apply { writeText("plaintext") }
        val value = attachment(filePath = file.path, originalFilePath = file.path)
        val task = transferTask(value)
        val coordinator = AttachmentUploadCoordinator(
            RuntimeEnvironment.getApplication(),
            mock(),
            backgroundScope
        )
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
        val first =
            attachment(messageTid = 1001, filePath = file.path, originalFilePath = file.path)
                .copy(metadata = """{"caption":"first","szw":100}""")
        val second =
            attachment(messageTid = 1002, filePath = file.path, originalFilePath = file.path)
                .copy(metadata = """{"caption":"second","szw":200}""")
        val firstTask = transferTask(first).apply { isSharedUpload = true }
        val secondTask = transferTask(second).apply { isSharedUpload = true }
        val firstResult = CompletableDeferred<SceytResponse<String>>()
        val secondResult = CompletableDeferred<SceytResponse<String>>()
        val token = "shared-sdk-token"
        transport.onPrepareUpload = {
            mapOf("sceyt.enc" to token)
        }
        val coordinator = AttachmentUploadCoordinator(
            RuntimeEnvironment.getApplication(),
            mock(),
            backgroundScope
        )
        coordinator.upload(first, firstTask, { _, result -> firstResult.complete(result) }, {})
        coordinator.upload(second, secondTask, { _, result -> secondResult.complete(result) }, {})
        runCurrent()

        transport.uploadCalls.single().succeed("https://waafi.test/shared")

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
    fun `receiver forwards saved encryption metadata to the native downloader`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        context.setValidatedNetwork()
        SceytKoinApp.koinApp = koinApplication {
            modules(module { single<Context> { context } })
        }
        val destination = temporary.newFile("decrypted-attachment.bin")
        destination.delete()
        val plaintext = "verified attachment".toByteArray()
        val token = "received-sdk-token"
        Mockito.mockConstruction(ChatSdkFileDownloader::class.java) { downloader, _ ->
            doSuspendableAnswer { invocation ->
                val request = invocation.getArgument<FileDownloadRequest>(0)
                assertThat(FileEncryptionMetadata.read(request.attachment.metadata)).isEqualTo(token)
                assertThat(request.type).isEqualTo(FileTransferType.ENCRYPTED)
                assertThat(request.attachment.fileSize).isEqualTo(plaintext.size.toLong())
                request.destinationFile.writeBytes(plaintext)
                FileTransferResult.Success(request.destinationFile.path)
            }.whenever(downloader) { download(any(), any()) }
        }.use { native ->
            SceytChatUIKit.fileTransfer = SceytChatUIKitFileTransfer().apply {
                provider = DefaultFileTransferProvider()
                destinationProvider = FileTransferDestinationProvider { _, _ -> destination }
                encryptionEnabled = true
            }
            val value = attachment(
                messageTid = System.nanoTime(),
                filePath = null,
                originalFilePath = null,
                state = TransferState.PendingDownload,
                fileSize = plaintext.size.toLong(),
                url = "https://cdn.test/blob",
            ).copy(
                metadata = FileEncryptionMetadata.write(
                    metadata = null,
                    attachmentKey = token,
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
            verify(native.constructed().single()).download(any(), any())
        }
    }
}
