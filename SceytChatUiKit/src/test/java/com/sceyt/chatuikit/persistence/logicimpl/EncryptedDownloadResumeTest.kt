package com.sceyt.chatuikit.persistence.logicimpl

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.fileencryption.FileEncryptionData
import com.sceyt.chat.fileencryption.FileEncryptionInfo
import com.sceyt.chat.fileencryption.FileEncryptionProvider
import com.sceyt.chat.fileencryption.PlaintextReader
import com.sceyt.chat.fileencryption.StreamingAeadFileEncryptionProvider
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferCallback
import com.sceyt.chatuikit.filetransfer.FileTransferDestinationProvider
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import com.sceyt.chatuikit.filetransfer.FileTransferTransport
import com.sceyt.chatuikit.filetransfer.FileUploadRequest
import com.sceyt.chatuikit.filetransfer.SceytChatUIKitFileTransfer
import com.sceyt.chatuikit.filetransfer.encryption.FileEncryptionMetadata
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.crypto.AEADBadTagException

@RunWith(RobolectricTestRunner::class)
class EncryptedDownloadResumeTest {
    @Test
    fun `network interruption keeps encrypted partial and defers SDK decryption until retry`() = runTest {
        Scenario().use { scenario ->
            scenario.interruptDownloadOnce = true
            val reader = scenario.reader { offset, length ->
                scenario.plaintext.copyOfRange(offset.toInt(), offset.toInt() + length)
            }
            whenever(scenario.provider.openDownload(any(), any())).thenReturn(reader)

            assertThat(scenario.download(backgroundScope)).isInstanceOf(SceytResponse.Error::class.java)
            assertThat(File(scenario.destination.path + ".sceyt.enc.part").exists()).isTrue()
            verify(scenario.provider, never()).openDownload(any(), any())

            assertThat(scenario.download(backgroundScope)).isInstanceOf(SceytResponse.Success::class.java)
            assertThat(scenario.downloads).isEqualTo(2)
            assertThat(scenario.destination.readBytes()).isEqualTo(scenario.plaintext)
            assertThat(File(scenario.destination.path + ".sceyt.enc").exists()).isFalse()
        }
    }

    @Test
    fun `pause during decryption keeps verified plaintext and reuses the encrypted download`() = runTest {
        Scenario().use { scenario ->
            val reading = CompletableDeferred<Unit>()
            val closed = CompletableDeferred<Unit>()
            val release = CountDownLatch(1)
            val firstReader = scenario.reader { offset, length ->
                if (offset > 0) {
                    reading.complete(Unit)
                    check(release.await(5, TimeUnit.SECONDS))
                }
                scenario.plaintext.copyOfRange(0, length)
            }
            doAnswer {
                closed.complete(Unit)
                Unit
            }.whenever(firstReader).close()
            val resumedOffsets = mutableListOf<Long>()
            val secondReader = scenario.reader { offset, length ->
                resumedOffsets += offset
                scenario.plaintext.copyOfRange(offset.toInt(), offset.toInt() + length)
            }
            whenever(scenario.provider.openDownload(any(), any())).thenReturn(firstReader, secondReader)

            val coordinator = AttachmentDownloadCoordinator(RuntimeEnvironment.getApplication(), backgroundScope)
            coordinator.download(scenario.attachment, transferTask(scenario.attachment), {})
            try {
                withContext(Dispatchers.Default) {
                    withTimeout(5_000) {
                        reading.await()
                        assertThat(coordinator.pause(scenario.attachment)).isTrue()
                        closed.await()
                    }
                }
                assertThat(scenario.destination.exists()).isFalse()
                assertThat(scenario.partial.length()).isEqualTo(1_048_576L)

                assertThat((scenario.download(backgroundScope) as SceytResponse.Success).data)
                    .isEqualTo(scenario.destination.path)
                assertThat(resumedOffsets.first()).isEqualTo(1_048_576L)
                assertThat(scenario.destination.readBytes()).isEqualTo(scenario.plaintext)
                assertThat(scenario.partial.exists()).isFalse()
                assertThat(scenario.downloads).isEqualTo(1)
                assertThat(File(scenario.destination.path + ".sceyt.enc").exists()).isFalse()
                verify(firstReader).close()
                verify(secondReader).close()
            } finally {
                release.countDown()
            }
        }
    }

    @Test
    fun `local SDK read failure removes temporary files instead of waiting for network`() = runTest {
        Scenario().use { scenario ->
            val reader = scenario.reader { offset, length ->
                if (offset > 0) throw IOException("Cannot read local encrypted file")
                scenario.plaintext.copyOfRange(0, length)
            }
            whenever(scenario.provider.openDownload(any(), any())).thenReturn(reader)

            assertThat(scenario.download(backgroundScope)).isInstanceOf(SceytResponse.Error::class.java)
            assertThat(scenario.destination.exists()).isFalse()
            assertThat(scenario.partial.exists()).isFalse()
            assertThat(File(scenario.destination.path + ".sceyt.enc").exists()).isFalse()
            verify(reader).close()
        }
    }

    @Test
    fun `authentication failure discards partial plaintext and never publishes the file`() = runTest {
        Scenario().use { scenario ->
            val reader = scenario.reader { offset, length ->
                if (offset > 0) throw AEADBadTagException("Modified encrypted record")
                scenario.plaintext.copyOfRange(0, length)
            }
            whenever(scenario.provider.openDownload(any(), any())).thenReturn(reader)

            assertThat(scenario.download(backgroundScope)).isInstanceOf(SceytResponse.Error::class.java)
            assertThat(scenario.destination.exists()).isFalse()
            assertThat(scenario.partial.exists()).isFalse()
            verify(reader).close()
        }
    }

    private class Scenario : AutoCloseable {
        private val context = RuntimeEnvironment.getApplication()
        private val previous = SceytChatUIKit.fileTransfer
        val destination: File = File.createTempFile("encrypted-resume", ".bin", context.cacheDir)
        val partial = File(destination.path + ".sceyt.part")
        val plaintext = ByteArray(2_097_159) { (it % 251).toByte() }
        val provider = mock<FileEncryptionProvider>()
        var downloads = 0
        var interruptDownloadOnce = false
        private val info = FileEncryptionInfo(
            StreamingAeadFileEncryptionProvider.FORMAT,
            FileEncryptionData("resumed-file", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", plaintext.size.toLong()),
        )
        val attachment = attachment(
            messageTid = System.nanoTime(),
            filePath = null,
            originalFilePath = null,
            state = TransferState.PendingDownload,
            fileSize = plaintext.size.toLong(),
        ).copy(
            metadata = FileEncryptionMetadata.write(
                metadata = null,
                envelope = FileEncryptionMetadata.fromEncryptionInfo(info),
            ),
        )

        init {
            destination.delete()
            SceytChatUIKit.fileTransfer = SceytChatUIKitFileTransfer().apply {
                encryptionProvider = provider
                destinationProvider = FileTransferDestinationProvider { _, _ -> destination }
                transport = object : FileTransferTransport {
                    override suspend fun upload(
                        request: FileUploadRequest,
                        callback: FileTransferCallback,
                    ): FileTransferResult<String> = error("Downloads only")

                    override suspend fun download(
                        request: FileDownloadRequest,
                        callback: FileTransferCallback,
                    ): FileTransferResult<String> {
                        downloads++
                        request.destinationFile.writeBytes(byteArrayOf(1, 2, 3))
                        if (interruptDownloadOnce) {
                            interruptDownloadOnce = false
                            return FileTransferResult.Interrupted(IOException("Network lost"))
                        }
                        return FileTransferResult.Success(request.destinationFile.path)
                    }
                }
            }
        }

        fun reader(read: (Long, Int) -> ByteArray): PlaintextReader = mock<PlaintextReader>().also { reader ->
            whenever(reader.plaintextSize).thenReturn(plaintext.size.toLong())
            doAnswer { read(it.getArgument(0), it.getArgument(1)) }
                .whenever(reader).read(any(), any())
        }

        suspend fun download(scope: CoroutineScope): SceytResponse<String> {
            val result = CompletableDeferred<SceytResponse<String>>()
            AttachmentDownloadCoordinator(context, scope).download(
                attachment = attachment,
                task = transferTask(attachment),
                onResult = { result.complete(it) },
            )
            return result.await()
        }

        override fun close() {
            SceytChatUIKit.fileTransfer = previous
            destination.delete()
            partial.delete()
            File(destination.path + ".sceyt.enc").delete()
            File(destination.path + ".sceyt.enc.part").delete()
        }
    }
}
