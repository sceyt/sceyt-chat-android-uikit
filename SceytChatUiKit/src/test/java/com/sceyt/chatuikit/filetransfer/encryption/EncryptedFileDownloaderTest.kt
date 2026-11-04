package com.sceyt.chatuikit.filetransfer.encryption

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import javax.crypto.AEADBadTagException

@RunWith(RobolectricTestRunner::class)
class EncryptedFileDownloaderTest {
    @get:Rule
    val temporary = TemporaryFolder()

    @Test
    fun `a custom crypto provider publishes plaintext and the transport receives only ciphertext work`() =
        runTest {
            val crypto = TestEncryption("verified plaintext".toByteArray())
            val request = request()
            val directory = temporary.newFolder()
            val progress = CopyOnWriteArrayList<Float>()

            val result = EncryptedFileDownloader(directory).download(
                request,
                crypto.downloadEncryption,
                crypto,
                progress::add
            ) { bytesRequest, callback ->
                assertThat(bytesRequest.operationId).isEqualTo(request.operationId)
                assertThat(bytesRequest.headers).isEqualTo(request.headers)
                assertThat(bytesRequest.destinationFile.path).isEqualTo(request.destinationFile.path + ".sceyt.enc.part")
                bytesRequest.destinationFile.writeText("ciphertext fixture")
                callback.onProgress(100f)
                FileTransferResult.Success(bytesRequest.destinationFile.path)
            }

            assertThat(result).isEqualTo(FileTransferResult.Success(request.destinationFile.path))
            assertThat(request.destinationFile.readBytes()).isEqualTo(crypto.plaintext)
            assertThat(crypto.prepares).isEqualTo(1)
            assertThat(crypto.closes).isEqualTo(1)
            assertThat(progress.last()).isEqualTo(100f)
            assertStagingRemoved(request.destinationFile)
            assertThat(EncryptedDownloadSessionStore(directory).get(request.operationId)).isNull()
        }

    @Test
    fun `destination is durable before network starts and interruption defers decryption`() =
        runTest {
            val crypto = TestEncryption(byteArrayOf(7, 8))
            val request = request()
            val directory = temporary.newFolder()
            val result = EncryptedFileDownloader(directory).download(
                request,
                crypto.downloadEncryption,
                crypto,
                {}) { bytesRequest, _ ->
                val saved = EncryptedDownloadSessionStore(directory).get(request.operationId)
                assertThat(saved?.destination).isEqualTo(request.destinationFile.absolutePath)
                assertThat(saved?.plaintextOffset).isEqualTo(0L)
                bytesRequest.destinationFile.writeText("partial")
                FileTransferResult.Interrupted(IOException("Network lost"))
            }

            assertThat(result).isInstanceOf(FileTransferResult.Interrupted::class.java)
            assertThat(crypto.prepares).isEqualTo(0)
            assertThat(File(request.destinationFile.path + ".sceyt.enc.part").readText()).isEqualTo(
                "partial"
            )
            assertThat(request.destinationFile.exists()).isFalse()

            EncryptedFileDownloader(directory).discard(request.operationId)

            assertStagingRemoved(request.destinationFile)
            assertThat(EncryptedDownloadSessionStore(directory).get(request.operationId)).isNull()
        }

    @Test
    fun `cancel before the first byte can be discarded by ID after recreation`() = runTest {
        val crypto = TestEncryption(byteArrayOf(7, 8))
        val request = request()
        val directory = temporary.newFolder()
        val started = CompletableDeferred<Unit>()
        val download = async(Dispatchers.Default) {
            EncryptedFileDownloader(directory).download(request, crypto.downloadEncryption, crypto, {}) { _, _ ->
                started.complete(Unit)
                awaitCancellation()
            }
        }
        withContext(Dispatchers.Default) { withTimeout(5_000) { started.await() } }
        download.cancelAndJoin()
        assertThat(EncryptedDownloadSessionStore(directory).get(request.operationId)).isNotNull()

        EncryptedFileDownloader(directory).discard(request.operationId)

        assertThat(EncryptedDownloadSessionStore(directory).get(request.operationId)).isNull()
        assertThat(crypto.prepares).isEqualTo(0)
        assertStagingRemoved(request.destinationFile)
    }

    @Test
    fun `resume truncates an uncommitted tail and reads from the durable plaintext offset`() =
        runTest {
            assertResume(keepCommittedPrefix = true)
        }

    @Test
    fun `a plaintext file shorter than its checkpoint is decrypted again from zero`() = runTest {
        assertResume(keepCommittedPrefix = false)
    }

    @Test
    fun `authentication failure closes the session and removes staging without touching published plaintext`() =
        runTest {
            val crypto = TestEncryption(ByteArray(BUFFER_SIZE + 37) { (it % 251).toByte() })
            crypto.onRead = { offset, length ->
                if (offset > 0) throw AEADBadTagException("Changed encrypted record")
                crypto.plaintext.copyOfRange(offset.toInt(), offset.toInt() + length)
            }
            val request = request()
            request.destinationFile.writeText("already published")
            val directory = temporary.newFolder()

            val result = EncryptedFileDownloader(directory).download(
                request,
                crypto.downloadEncryption,
                crypto,
                {}) { bytesRequest, _ ->
                bytesRequest.destinationFile.writeText("ciphertext")
                FileTransferResult.Success(bytesRequest.destinationFile.path)
            }

            assertThat((result as FileTransferResult.Failure).error).isInstanceOf(
                AEADBadTagException::class.java
            )
            assertThat(request.destinationFile.readText()).isEqualTo("already published")
            assertThat(crypto.closes).isEqualTo(1)
            assertStagingRemoved(request.destinationFile)
            assertThat(EncryptedDownloadSessionStore(directory).get(request.operationId)).isNull()
        }

    @Test
    fun `cleanup failure is retained for retry without throwing from discard`() {
        val directory = temporary.newFolder()
        val destination = temporary.newFile()
        val blocked = File(destination.path + ".sceyt.enc").apply { mkdir() }
        val child = File(blocked, "blocked").apply { writeText("in use") }
        val store = EncryptedDownloadSessionStore(directory)
        store.save("download:42", EncryptedDownloadSession("identity", destination.path))

        EncryptedFileDownloader(directory).discard("download:42")

        assertThat(store.get("download:42")).isNotNull()
        child.delete()
        EncryptedFileDownloader(directory).discard("download:42")
        assertThat(store.get("download:42")).isNull()
        assertThat(destination.exists()).isTrue()
    }

    @Test
    fun `a cancelled checkpoint cannot be reused until its staging files are removed`() = runTest {
        val crypto = TestEncryption("verified plaintext".toByteArray())
        val request = request()
        val directory = temporary.newFolder()
        val blocked = File(request.destinationFile.path + ".sceyt.enc").apply { mkdir() }
        val child = File(blocked, "blocked").apply { writeText("in use") }
        EncryptedDownloadSessionStore(directory).save(
            request.operationId,
            EncryptedDownloadSession("identity", request.destinationFile.absolutePath),
        )
        EncryptedFileDownloader(directory).discard(request.operationId)

        val failed = EncryptedFileDownloader(directory).download(
            request,
            crypto.downloadEncryption,
            crypto,
            {},
        ) { _, _ ->
            error("Cancelled staging files must be removed before networking starts")
        }

        assertThat((failed as FileTransferResult.Failure).error).isInstanceOf(IOException::class.java)
        assertThat(crypto.prepares).isEqualTo(0)
        assertThat(EncryptedDownloadSessionStore(directory).get(request.operationId)?.discardRequested)
            .isTrue()

        child.delete()
        val result = EncryptedFileDownloader(directory).download(
            request,
            crypto.downloadEncryption,
            crypto,
            {},
        ) { bytesRequest, _ ->
            assertThat(blocked.exists()).isFalse()
            bytesRequest.destinationFile.writeText("ciphertext")
            FileTransferResult.Success(bytesRequest.destinationFile.path)
        }

        assertThat(result).isEqualTo(FileTransferResult.Success(request.destinationFile.path))
        assertThat(request.destinationFile.readBytes()).isEqualTo(crypto.plaintext)
        assertThat(crypto.prepares).isEqualTo(1)
        assertThat(crypto.closes).isEqualTo(1)
        assertStagingRemoved(request.destinationFile)
        assertThat(EncryptedDownloadSessionStore(directory).get(request.operationId)).isNull()
    }

    private suspend fun assertResume(keepCommittedPrefix: Boolean) {
        val crypto = TestEncryption(ByteArray(BUFFER_SIZE * 2 + 37) { (it % 251).toByte() })
        val request = request()
        val directory = temporary.newFolder()
        val reading = CompletableDeferred<Unit>()
        var downloads = 0
        crypto.onRead = { offset, length ->
            if (offset > 0) {
                reading.complete(Unit)
                awaitCancellation()
            }
            crypto.plaintext.copyOfRange(offset.toInt(), offset.toInt() + length)
        }
        coroutineScope {
            val download = async(Dispatchers.Default) {
                EncryptedFileDownloader(directory).download(
                    request,
                    crypto.downloadEncryption,
                    crypto,
                    {}) { bytesRequest, _ ->
                    downloads++
                    bytesRequest.destinationFile.writeText("ciphertext")
                    FileTransferResult.Success(bytesRequest.destinationFile.path)
                }
            }
            withContext(Dispatchers.Default) { withTimeout(5_000) { reading.await() } }
            download.cancelAndJoin()
        }
        val store = EncryptedDownloadSessionStore(directory)
        assertThat(store.get(request.operationId)?.plaintextOffset).isEqualTo(BUFFER_SIZE.toLong())
        val partial = File(request.destinationFile.path + ".sceyt.part")
        if (keepCommittedPrefix) partial.appendBytes(ByteArray(103) { 99 }) else partial.writeText("short")
        val offsets = mutableListOf<Long>()
        crypto.onRead = { offset, length ->
            offsets += offset
            crypto.plaintext.copyOfRange(offset.toInt(), offset.toInt() + length)
        }

        val result = EncryptedFileDownloader(directory).download(
            request,
            crypto.downloadEncryption,
            crypto,
            {},
        ) { _, _ ->
            error("A complete ciphertext file must be reused")
        }

        assertThat(result).isInstanceOf(FileTransferResult.Success::class.java)
        assertThat(offsets.first()).isEqualTo(if (keepCommittedPrefix) BUFFER_SIZE.toLong() else 0L)
        assertThat(request.destinationFile.readBytes()).isEqualTo(crypto.plaintext)
        assertThat(downloads).isEqualTo(1)
        assertThat(crypto.closes).isEqualTo(2)
        assertStagingRemoved(request.destinationFile)
    }

    private fun request() = FileDownloadRequest(
        operationId = "download:42",
        url = "https://fixture.test/file",
        destinationFile = File(temporary.newFolder(), "received.bin"),
        headers = mapOf("X-Chat-Token" to "test-token"),
    )

    private fun assertStagingRemoved(destination: File) {
        for (suffix in listOf(".sceyt.part", ".sceyt.enc", ".sceyt.enc.part")) {
            assertThat(File(destination.path + suffix).exists()).isFalse()
        }
    }

    private class TestEncryption(val plaintext: ByteArray) : FileEncryption {
        private val info = FileEncryptionInfo("fixture-format", "fixture-id", "fixture-key")
        val downloadEncryption = FileDownloadEncryption(info, plaintext.size.toLong())
        var prepares = 0
        var closes = 0
        var onRead: suspend (Long, Int) -> ByteArray = { offset, length ->
            plaintext.copyOfRange(offset.toInt(), offset.toInt() + length)
        }

        override suspend fun prepareEncryption(file: File): FileEncryptionSession =
            error("Decryption only")

        override suspend fun restoreEncryption(
            file: File,
            checkpoint: FileEncryptionCheckpoint
        ): FileEncryptionSession =
            error("Decryption only")

        override suspend fun prepareDecryption(
            encryptedFile: File,
            encryptionInfo: FileEncryptionInfo,
            plaintextSize: Long,
        ): FileDecryptionSession {
            assertThat(encryptionInfo).isEqualTo(info)
            assertThat(plaintextSize).isEqualTo(plaintext.size.toLong())
            prepares++
            return object : FileDecryptionSession {
                override val sourceId =
                    encryptedFile.path + ":" + encryptedFile.length() + ":" + encryptedFile.lastModified()
                override val plaintextSize = plaintext.size.toLong()
                override suspend fun read(offset: Long, length: Int): ByteArray =
                    onRead(offset, length)

                override fun close() {
                    closes++
                }
            }
        }
    }

    private companion object {
        const val BUFFER_SIZE = 1024 * 1024
    }
}
