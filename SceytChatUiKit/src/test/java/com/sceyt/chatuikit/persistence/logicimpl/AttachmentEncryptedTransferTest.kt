package com.sceyt.chatuikit.persistence.logicimpl

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.fileencryption.CiphertextRangeSource
import com.sceyt.chat.fileencryption.FileEncryptionCheckpoint
import com.sceyt.chat.fileencryption.FileEncryptionData
import com.sceyt.chat.fileencryption.FileEncryptionInfo
import com.sceyt.chat.fileencryption.FileEncryptionProvider
import com.sceyt.chat.fileencryption.PlaintextReader
import com.sceyt.chat.fileencryption.PreparedUpload
import com.sceyt.chat.fileencryption.StreamingAeadFileEncryptionProvider
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.config.SceytChatUIKitConfig
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.filetransfer.AttachmentTransferContext
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferCallback
import com.sceyt.chatuikit.filetransfer.FileTransferDestinationProvider
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import com.sceyt.chatuikit.filetransfer.FileTransferTransport
import com.sceyt.chatuikit.filetransfer.FileUploadRequest
import com.sceyt.chatuikit.filetransfer.SceytChatUIKitFileTransfer
import com.sceyt.chatuikit.filetransfer.defaults.DefaultFileTransferTransport
import com.sceyt.chatuikit.filetransfer.encryption.FileEncryptionMetadata
import com.sceyt.chatuikit.filetransfer.encryption.FileUploadSessionStore
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.InetSocketAddress

@RunWith(RobolectricTestRunner::class)
class AttachmentEncryptedTransferTest {
    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `multipart retry restores state and cancellation discards the completed session`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val previous = SceytChatUIKit.fileTransfer
        val file = File.createTempFile("multipart-resume", ".bin", context.cacheDir)
        file.writeText("original file")
        val provider = mock<FileEncryptionProvider>()
        val info = FileEncryptionInfo(
            StreamingAeadFileEncryptionProvider.FORMAT,
            FileEncryptionData("multipart-file", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", file.length()),
        )
        val first = mock<PreparedUpload>()
        val restored = mock<PreparedUpload>()
        val fresh = mock<PreparedUpload>()
        var sdkState = "initial-sdk-state"
        listOf(first, restored, fresh).forEach {
            whenever(it.sourceId).thenReturn("same-encrypted-object")
            whenever(it.plaintextSize).thenReturn(file.length())
            whenever(it.ciphertextSize).thenReturn(6_291_493L)
            whenever(it.encryptionInfo).thenReturn(info)
            whenever(it.checkpoint).thenAnswer { FileEncryptionCheckpoint(info, sdkState) }
            doAnswer { read ->
                sdkState = "sdk-state-after-first-part"
                ByteArray(read.getArgument(1))
            }.whenever(it).readCiphertextRange(any(), any())
        }
        whenever(fresh.sourceId).thenReturn("new-encrypted-object")
        whenever(provider.prepareUpload(any(), any())).thenReturn(first, fresh)
        whenever(provider.restoreUpload(any(), any())).thenReturn(restored)
        val backendState = """{"uploadId":"multipart-1","parts":[{"number":1,"etag":"etag-1"}]}"""
        var requests = 0
        var originalMetadata: String? = null
        var discardedCheckpoint: String? = null
        val transport = object : FileTransferTransport {
            override fun discardUpload(operationId: String, checkpointData: String?) {
                discardedCheckpoint = checkpointData
            }

            override suspend fun upload(
                request: FileUploadRequest,
                callback: FileTransferCallback,
            ): FileTransferResult<String> {
                val encrypted = request as FileUploadRequest.Encrypted
                val checkpoints = checkNotNull(encrypted.checkpointStore)
                val metadata = (request.context as AttachmentTransferContext).attachment.metadata
                if (requests == 2) {
                    requests++
                    assertThat(checkpoints.get()).isNull()
                    assertThat(checkpoints.sourceId).isEqualTo("new-encrypted-object")
                    return FileTransferResult.Success("https://waafi.test/fresh-upload")
                }
                if (++requests == 1) {
                    originalMetadata = metadata
                    checkpoints.save("""{"uploadId":"multipart-1","parts":[]}""")
                    encrypted.ciphertextSource.readCiphertextRange(0, 5_242_880)
                    checkpoints.save(backendState)
                    return FileTransferResult.Interrupted(IOException("Network lost"))
                }
                assertThat(checkpoints.sourceId).isEqualTo("same-encrypted-object")
                assertThat(checkpoints.get()).isEqualTo(backendState)
                assertThat(metadata).isEqualTo(originalMetadata)
                encrypted.ciphertextSource.readCiphertextRange(5_242_880, 1_048_613)
                return FileTransferResult.Success("https://waafi.test/completed")
            }

            override suspend fun download(
                request: FileDownloadRequest,
                callback: FileTransferCallback,
            ): FileTransferResult<String> = error("Uploads only")
        }
        try {
            SceytChatUIKit.fileTransfer = SceytChatUIKitFileTransfer().apply {
                encryptUploads = true
                encryptionProvider = provider
                this.transport = transport
            }
            val value = attachment(
                messageTid = System.nanoTime(),
                filePath = file.path,
                originalFilePath = file.path,
                type = "file",
                url = null,
            )
            repeat(3) { attempt ->
                val result = CompletableDeferred<SceytResponse<String>>()
                val coordinator = AttachmentUploadCoordinator(context, mock(), backgroundScope)
                val task = transferTask(value)
                coordinator.uploadFile(
                    attachment = value,
                    task = task,
                    onResult = { _, response -> result.complete(response) },
                    onPaused = {},
                )
                if (attempt == 0) {
                    assertThat(result.await()).isInstanceOf(SceytResponse.Error::class.java)
                } else if (attempt == 1) {
                    assertThat((result.await() as SceytResponse.Success).data)
                        .isEqualTo("https://waafi.test/completed")
                    coordinator.cancel(task)
                    runCurrent()
                    assertThat(discardedCheckpoint).isEqualTo(backendState)
                } else {
                    assertThat((result.await() as SceytResponse.Success).data)
                        .isEqualTo("https://waafi.test/fresh-upload")
                }
            }
            val checkpoint = argumentCaptor<FileEncryptionCheckpoint>()
            verify(provider, times(2)).prepareUpload(eq(file), any())
            verify(provider).restoreUpload(eq(file), checkpoint.capture())
            assertThat(checkpoint.firstValue.data).isEqualTo("sdk-state-after-first-part")
            assertThat(checkpoint.firstValue.encryptionInfo).isEqualTo(info)
            verify(first).close()
            verify(restored).close()
            verify(fresh).close()
        } finally {
            SceytChatUIKit.fileTransfer = previous
            file.delete()
        }
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
        val info = FileEncryptionInfo(
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
                encryptionProvider = provider
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
                    envelope = FileEncryptionMetadata.fromEncryptionInfo(info),
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

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `custom multipart transport receives ciphertext ranges and UIKit keeps SDK encryption metadata`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val previousTransfer = SceytChatUIKit.fileTransfer
        val previousConfig = SceytChatUIKit.config
        val file = File.createTempFile("encrypted-upload", ".bin", context.cacheDir)
        RandomAccessFile(file, "rw").use {
            it.setLength(6L * 1024 * 1024)
        }
        val provider = mock<FileEncryptionProvider>()
        val sdkUpload = mock<PreparedUpload>()
        val info = FileEncryptionInfo(
            StreamingAeadFileEncryptionProvider.FORMAT,
            FileEncryptionData("uploaded-file", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", file.length()),
        )
        whenever(provider.prepareUpload(any(), any())).thenReturn(sdkUpload)
        whenever(sdkUpload.plaintextSize).thenReturn(file.length())
        whenever(sdkUpload.ciphertextSize).thenReturn(6_291_493L)
        whenever(sdkUpload.sourceId).thenReturn("sdk-session")
        whenever(sdkUpload.encryptionInfo).thenReturn(info)
        whenever(sdkUpload.checkpoint).thenReturn(
            FileEncryptionCheckpoint(info, "sdk-checkpoint"),
        )
        doAnswer { ByteArray(it.getArgument<Int>(1)) { 0x5a } }
            .whenever(sdkUpload).readCiphertextRange(any(), any())
        val lengths = mutableListOf<Int>()
        var uploadMetadata: String? = null
        val transport = object : FileTransferTransport {
            override suspend fun upload(
                request: FileUploadRequest,
                callback: FileTransferCallback,
            ): FileTransferResult<String> {
                val encrypted = request as FileUploadRequest.Encrypted
                val source = encrypted.ciphertextSource
                uploadMetadata = (request.context as AttachmentTransferContext).attachment.metadata
                var offset = 0L
                while (offset < source.ciphertextSize) {
                    val length = minOf(5_242_880L, source.ciphertextSize - offset).toInt()
                    val bytes = source.readCiphertextRange(offset, length)
                    assertThat(bytes.first()).isEqualTo(0x5a.toByte())
                    lengths += bytes.size
                    offset += bytes.size
                    callback.onProgress(offset * 100f / source.ciphertextSize)
                }
                return FileTransferResult.Success("https://waafi.test/complete-object")
            }

            override suspend fun download(
                request: FileDownloadRequest,
                callback: FileTransferCallback,
            ): FileTransferResult<String> = error("This test uploads only")
        }

        try {
            SceytChatUIKit.config = SceytChatUIKitConfig().apply {
                preventDuplicateAttachmentUpload = false
            }
            SceytChatUIKit.fileTransfer = SceytChatUIKitFileTransfer().apply {
                encryptUploads = true
                encryptionProvider = provider
                this.transport = transport
            }
            val value = attachment(
                messageTid = System.nanoTime(),
                filePath = file.path,
                originalFilePath = file.path,
                type = "file",
                url = null,
            )
            val task = transferTask(value)
            val finished = CompletableDeferred<SceytResponse<String>>()
            val coordinator = AttachmentUploadCoordinator(context, mock(), backgroundScope)
            coordinator.uploadFile(
                attachment = value,
                task = task,
                onResult = { _, result -> finished.complete(result) },
                onPaused = {},
            )

            assertThat((finished.await() as SceytResponse.Success).data)
                .isEqualTo("https://waafi.test/complete-object")
            assertThat(lengths).containsExactly(5_242_880, 1_048_613).inOrder()
            assertThat(task.attachment.metadata).isEqualTo(uploadMetadata)
            val envelope = checkNotNull(FileEncryptionMetadata.read(task.attachment.metadata))
            assertThat(task.attachment.fileSize).isEqualTo(file.length())
            assertThat(envelope.toEncryptionInfo(task.attachment.fileSize)).isEqualTo(info)
            assertThat(task.attachment.metadata).contains(info.data.key)
            val sessions = FileUploadSessionStore(File(context.filesDir, "upload-sessions"))
            val operationId = "upload:" + value.messageTid
            assertThat(sessions.get(operationId)).isNotNull()

            coordinator.uploaded(task.attachment)
            runCurrent()

            assertThat(sessions.get(operationId)).isNull()
            verify(sdkUpload).close()
        } finally {
            SceytChatUIKit.fileTransfer = previousTransfer
            SceytChatUIKit.config = previousConfig
            file.delete()
        }
    }
}
