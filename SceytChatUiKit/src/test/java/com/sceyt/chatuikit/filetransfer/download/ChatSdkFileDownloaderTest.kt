package com.sceyt.chatuikit.filetransfer.download

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.download.DownloadRequest
import com.sceyt.chat.download.MediaDownload
import com.sceyt.chat.download.MediaDownloadException
import com.sceyt.chat.download.MediaDownloadStatus
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import com.sceyt.chatuikit.filetransfer.FileTransferType
import com.sceyt.chatuikit.persistence.logicimpl.attachment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ChatSdkFileDownloaderTest {
    @get:Rule
    val temporary = TemporaryFolder()

    private val operation = mock<MediaDownload>()
    private lateinit var listener: MediaDownload.Listener

    @Test
    fun `legacy partial destination is removed without discarding native resume files`() = runTest {
        val request = request()
        request.destinationFile.writeText("legacy partial bytes")
        val partial = File(request.destinationFile.path + ".part")
            .apply { writeText("verified ranges") }
        val checkpoint = File(partial.path + ".json").apply { writeText("saved ranges") }
        setStatus(MediaDownload.State.DOWNLOADING)
        Mockito.mockStatic(MediaDownload::class.java).use { native ->
            native.preparePlain(request)
            val downloader = ChatSdkFileDownloader(StandardTestDispatcher(testScheduler))
            val result = async { downloader.download(request) {} }
            runCurrent()

            assertThat(request.destinationFile.exists()).isFalse()
            assertThat(partial.readText()).isEqualTo("verified ranges")
            assertThat(checkpoint.readText()).isEqualTo("saved ranges")
            assertThat(result.isCompleted).isFalse()
            result.cancelAndJoin()
        }
    }

    @Test
    fun `plain download forwards operation ID URL headers and destination`() = runTest {
        val request = request()
            .copy(headers = mapOf("X-Chat-Token" to "token", "X-Message-Id" to "42"))
        val progress = mutableListOf<Float>()
        setStatus(MediaDownload.State.DOWNLOADING, downloaded = 25)
        Mockito.mockStatic(MediaDownload::class.java).use { native ->
            native.preparePlain(request)
            val downloader = ChatSdkFileDownloader(StandardTestDispatcher(testScheduler))
            val result = async { downloader.download(request, progress::add) }
            runCurrent()
            assertThat(progress).containsExactly(25f)

            setStatus(MediaDownload.State.COMPLETED, downloaded = 100)
            listener.onStatusChanged(operation.status)
            runCurrent()

            assertThat(result.await())
                .isEqualTo(FileTransferResult.Success(request.destinationFile.absolutePath))
            assertThat(progress).containsExactly(25f, 100f).inOrder()
            verify(operation).setListener(null)
            verify(operation).close()
            verify(operation, never()).cancel()
        }
    }

    @Test
    fun `encrypted download passes the attachment key directly to native decryption`() = runTest {
        val attachmentKey = "opaque-attachment-key"
        val request = request().copy(
            attachment = attachment(fileSize = 100).copy(metadata = """{"sceyt.enc":"$attachmentKey"}"""),
            type = FileTransferType.ENCRYPTED,
        )
        setStatus(MediaDownload.State.COMPLETED, downloaded = 100)
        Mockito.mockStatic(MediaDownload::class.java).use { native ->
            native.`when`<MediaDownload> {
                MediaDownload.downloadEncrypted(
                    eq(request.toSdkRequest()), eq(request.destinationFile.absolutePath),
                    eq(attachmentKey), any(), isNull(),
                )
            }.thenReturn(operation)

            val result = ChatSdkFileDownloader(StandardTestDispatcher(testScheduler)).download(request) {}

            assertThat(result).isEqualTo(FileTransferResult.Success(request.destinationFile.absolutePath))
            assertThat(request.toString()).doesNotContain(attachmentKey)
            verify(operation).close()
        }
    }

    @Test
    fun `native rejects an invalid attachment key without a plaintext fallback`() = runTest {
        val request = request().copy(
            attachment = attachment().copy(metadata = """{"sceyt.enc":"invalid"}"""),
            type = FileTransferType.ENCRYPTED,
        )
        val error = mock<MediaDownloadException>()
        setStatus(MediaDownload.State.FAILED, error = error)
        Mockito.mockStatic(MediaDownload::class.java).use { native ->
            native.`when`<MediaDownload> {
                MediaDownload.downloadEncrypted(
                    eq(request.toSdkRequest()), eq(request.destinationFile.absolutePath),
                    eq("invalid"), any(), isNull(),
                )
            }.thenReturn(operation)

            val result = ChatSdkFileDownloader(StandardTestDispatcher(testScheduler)).download(request) {}

            assertThat((result as FileTransferResult.Failure).error).isSameInstanceAs(error)
            verify(operation).cancel()
            verify(operation).close()
        }
    }

    @Test
    fun `encrypted requests without metadata fail before starting native downloads`() = runTest {
        for (metadata in listOf(null, "{}")) {
            val request = request().copy(
                attachment = attachment().copy(metadata = metadata),
                type = FileTransferType.ENCRYPTED,
            )
            Mockito.mockStatic(MediaDownload::class.java).use { native ->
                val result =
                    ChatSdkFileDownloader(StandardTestDispatcher(testScheduler)).download(request) {}

                assertThat(result).isInstanceOf(FileTransferResult.Failure::class.java)
                assertThat((result as FileTransferResult.Failure).error.message)
                    .isEqualTo("Encrypted download requires valid encryption metadata")
                native.verifyNoInteractions()
            }
        }
    }

    @Test
    fun `plain requests ignore encryption fields instead of changing download mode`() = runTest {
        val request = request()
            .copy(attachment = attachment().copy(metadata = """{"sceyt.enc":"invalid"}"""))
        setStatus(MediaDownload.State.COMPLETED, downloaded = 100)
        Mockito.mockStatic(MediaDownload::class.java).use { native ->
            native.preparePlain(request)

            val result =
                ChatSdkFileDownloader(StandardTestDispatcher(testScheduler)).download(request) {}

            assertThat(result)
                .isEqualTo(FileTransferResult.Success(request.destinationFile.absolutePath))
            native.verify {
                MediaDownload.download(
                    eq(request.toSdkRequest()),
                    eq(request.destinationFile.absolutePath),
                    any(),
                    isNull()
                )
            }
            native.verifyNoMoreInteractions()
        }
    }

    @Test
    fun `network loss interrupts the attempt without discarding resumable files`() =
        runTest {
            val request = request()
            val partial = File(request.destinationFile.path + ".part").apply { writeText("verified bytes") }
            val checkpoint = File(partial.path + ".json").apply { writeText("saved ranges") }
            val error = mock<MediaDownloadException>()
            val progress = mutableListOf<Float>()
            setStatus(MediaDownload.State.DOWNLOADING, downloaded = 25)
            Mockito.mockStatic(MediaDownload::class.java).use { native ->
                native.preparePlain(request)
                val downloader = ChatSdkFileDownloader(StandardTestDispatcher(testScheduler))
                val result = async { downloader.download(request, progress::add) }
                try {
                    runCurrent()
                    setStatus(MediaDownload.State.WAITING_FOR_NETWORK, downloaded = 25, error = error)
                    listener.onStatusChanged(operation.status)
                    runCurrent()

                    assertThat(result.isCompleted).isTrue()
                    assertThat((result.await() as FileTransferResult.Interrupted).error).isSameInstanceAs(error)
                    assertThat(progress).containsExactly(25f)
                    assertThat(partial.readText()).isEqualTo("verified bytes")
                    assertThat(checkpoint.readText()).isEqualTo("saved ranges")
                    verify(operation).pause()
                    verify(operation).close()
                    verify(operation, never()).cancel()
                } finally {
                    result.cancelAndJoin()
                }
            }
        }

    @Test
    fun `network wait without a native cause returns a recoverable IO error`() = runTest {
        val request = request()
        setStatus(MediaDownload.State.WAITING_FOR_NETWORK)
        Mockito.mockStatic(MediaDownload::class.java).use { native ->
            native.preparePlain(request)
            val downloader = ChatSdkFileDownloader(StandardTestDispatcher(testScheduler))
            val result = async { downloader.download(request) {} }
            try {
                runCurrent()

                assertThat(result.isCompleted).isTrue()
                val error = (result.await() as FileTransferResult.Interrupted).error
                assertThat(error).isInstanceOf(IOException::class.java)
                assertThat(error.message).isEqualTo("No network connection")
                verify(operation, never()).cancel()
            } finally {
                result.cancelAndJoin()
            }
        }
    }

    @Test
    fun `cancelling the coroutine pauses native writers and closes the handle without discarding bytes`() =
        runTest {
            val request = request()
            val progress = mutableListOf<Float>()
            setStatus(MediaDownload.State.DOWNLOADING)
            Mockito.mockStatic(MediaDownload::class.java).use { native ->
                native.preparePlain(request)
                val downloader = ChatSdkFileDownloader(StandardTestDispatcher(testScheduler))
                val result = async { downloader.download(request, progress::add) }
                runCurrent()

                result.cancelAndJoin()
                listener.onStatusChanged(status(MediaDownload.State.DOWNLOADING, downloaded = 50))
                runCurrent()

                val cleanup = Mockito.inOrder(operation)
                cleanup.verify(operation).setListener(null)
                cleanup.verify(operation).pause()
                cleanup.verify(operation).close()
                verify(operation, never()).cancel()
                assertThat(progress).containsExactly(0f)
                assertThat(result.isCancelled).isTrue()
                assertThat(downloader.resume(request.operationId)).isFalse()
            }
        }

    @Test
    fun `pause and resume run in order on the download coroutine without blocking the caller`() =
        runTest {
            val request = request()
            setStatus(MediaDownload.State.DOWNLOADING)
            Mockito.mockStatic(MediaDownload::class.java).use { native ->
                native.preparePlain(request)
                val downloader = ChatSdkFileDownloader(StandardTestDispatcher(testScheduler))
                val result = async { downloader.download(request) {} }
                runCurrent()

                assertThat(downloader.pause("download:other")).isFalse()
                assertThat(downloader.pause(request.operationId)).isTrue()
                assertThat(downloader.resume(request.operationId)).isTrue()
                verify(operation, never()).pause()
                verify(operation, never()).resume()

                runCurrent()

                val controls = Mockito.inOrder(operation)
                controls.verify(operation).pause()
                controls.verify(operation).resume()
                assertThat(result.isCompleted).isFalse()
                result.cancelAndJoin()
            }
        }

    @Test
    fun `resume is rejected once native handle cleanup starts`() = runTest {
        val request = request()
        setStatus(MediaDownload.State.DOWNLOADING)
        Mockito.mockStatic(MediaDownload::class.java).use { native ->
            native.preparePlain(request)
            val downloader = ChatSdkFileDownloader(StandardTestDispatcher(testScheduler))
            var resumedDuringCleanup = false
            Mockito.doAnswer {
                resumedDuringCleanup = downloader.resume(request.operationId)
                null
            }.`when`(operation).pause()
            val result = async { downloader.download(request) {} }
            runCurrent()

            result.cancelAndJoin()

            assertThat(resumedDuringCleanup).isFalse()
            verify(operation, never()).resume()
            verify(operation).close()
        }
    }

    @Test
    fun `explicit cancellation uses native operation ID lookup even without an active coroutine`() {
        Mockito.mockStatic(MediaDownload::class.java).use { native ->
            ChatSdkFileDownloader().cancel("download:after-restart")

            native.verify { MediaDownload.cancelDownloadById("download:after-restart") }
            native.verifyNoMoreInteractions()
        }
    }

    @Test
    fun `native terminal failure is preserved and partial files are discarded`() = runTest {
        val request = request()
        val error = mock<MediaDownloadException>()
        setStatus(MediaDownload.State.FAILED, error = error)
        Mockito.mockStatic(MediaDownload::class.java).use { native ->
            native.preparePlain(request)

            val result =
                ChatSdkFileDownloader(StandardTestDispatcher(testScheduler)).download(request) {}

            assertThat((result as FileTransferResult.Failure).error).isSameInstanceAs(error)
            verify(operation).cancel()
            verify(operation).close()
        }
    }

    @Test
    fun `duplicate operation cannot replace the active native listener`() = runTest {
        val request = request()
        setStatus(MediaDownload.State.DOWNLOADING)
        Mockito.mockStatic(MediaDownload::class.java).use { native ->
            native.preparePlain(request)
            val downloader = ChatSdkFileDownloader(StandardTestDispatcher(testScheduler))
            val first = async { downloader.download(request) {} }
            runCurrent()

            val duplicate = downloader.download(request) {}

            assertThat(duplicate).isInstanceOf(FileTransferResult.Failure::class.java)
            assertThat(first.isCompleted).isFalse()
            assertThat(downloader.pause(request.operationId)).isTrue()
            first.cancelAndJoin()
        }
    }

    private fun MockedStatic<MediaDownload>.preparePlain(request: FileDownloadRequest) {
        `when`<MediaDownload> {
            MediaDownload.download(
                eq(request.toSdkRequest()),
                eq(request.destinationFile.absolutePath),
                any(),
                isNull()
            )
        }.thenAnswer {
            listener = it.getArgument(2)
            operation
        }
    }

    private fun setStatus(
        state: MediaDownload.State,
        downloaded: Long = 0,
        error: MediaDownloadException? = null,
    ) {
        val value = status(state, downloaded, error)
        whenever(operation.status).thenReturn(value)
    }

    private fun status(
        state: MediaDownload.State,
        downloaded: Long = 0,
        error: MediaDownloadException? = null,
    ): MediaDownloadStatus = mock {
        on { this.state }.thenReturn(state)
        on { this.downloaded }.thenReturn(downloaded)
        on { size }.thenReturn(100L)
        on { this.error }.thenReturn(error)
    }

    private fun request() = FileDownloadRequest(
        operationId = "download:42",
        url = "https://cdn.test/file",
        destinationFile = File(temporary.root, "file.bin"),
        attachment = attachment(),
    )

    private fun FileDownloadRequest.toSdkRequest() = DownloadRequest(url, headers, operationId)
}
