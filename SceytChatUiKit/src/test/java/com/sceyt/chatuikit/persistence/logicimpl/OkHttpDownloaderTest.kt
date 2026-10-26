package com.sceyt.chatuikit.persistence.logicimpl

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import com.sceyt.chatuikit.filetransfer.defaults.DefaultFileTransferTransport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.MockedConstruction
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(RobolectricTestRunner::class)
class OkHttpDownloaderTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val client = mock<OkHttpClient>()
    private val firstCall = mock<Call>()
    private val secondCall = mock<Call>()
    private lateinit var builders: MockedConstruction<OkHttpClient.Builder>

    @Before
    fun setUp() {
        builders = Mockito.mockConstruction(
            OkHttpClient.Builder::class.java,
            Mockito.withSettings().defaultAnswer(Mockito.RETURNS_SELF),
        ) { builder, _ ->
            whenever(builder.build()).thenReturn(client)
        }
        whenever(client.newCall(any())).thenReturn(firstCall, secondCall)
    }

    @After
    fun tearDown() {
        builders.close()
    }

    @Test
    fun `same URL with different operation IDs keeps both downloads`() {
        val downloader = OkHttpDownloader()
        val first = request("download:20:first")
        val second = request("download:20:second")

        val firstResults = mutableListOf<FileTransferResult<String>>()
        val secondResults = mutableListOf<FileTransferResult<String>>()
        downloader.downloadFile(first, {}, firstResults::add)
        downloader.downloadFile(second, {}, secondResults::add)
        val firstCallback = argumentCaptor<Callback>()
        val secondCallback = argumentCaptor<Callback>()
        verify(firstCall).enqueue(firstCallback.capture())
        verify(secondCall).enqueue(secondCallback.capture())
        val error = IOException("Connection lost")

        firstCallback.firstValue.onFailure(firstCall, error)

        assertThat(firstResults).containsExactly(FileTransferResult.Interrupted(error))
        assertThat(secondResults).isEmpty()
        secondCallback.firstValue.onFailure(secondCall, error)
        assertThat(secondResults).containsExactly(FileTransferResult.Interrupted(error))
    }

    @Test
    fun `cancelling one download does not cancel another with the same URL`() {
        val downloader = OkHttpDownloader()
        val first = request("download:20:first")
        val second = request("download:20:second")
        val call = checkNotNull(downloader.downloadFile(first, {}, {}))
        downloader.downloadFile(second, {}, {})

        downloader.cancelDownload(call)

        verify(firstCall).cancel()
        verify(secondCall, never()).cancel()
    }

    @Test
    fun `old failure is delivered only to the old request`() {
        val downloader = OkHttpDownloader()
        val request = request("download:20:first")
        val callback = argumentCaptor<Callback>()
        val oldResults = mutableListOf<FileTransferResult<String>>()
        val replacementResults = mutableListOf<FileTransferResult<String>>()
        downloader.downloadFile(request, {}, oldResults::add)
        verify(firstCall).enqueue(callback.capture())
        downloader.downloadFile(request, {}, replacementResults::add)
        val error = IOException("old failure")

        callback.firstValue.onFailure(firstCall, error)

        assertThat(oldResults).containsExactly(FileTransferResult.Interrupted(error))
        assertThat(replacementResults).isEmpty()
        verify(secondCall, never()).cancel()
    }

    @Test
    fun `cancelling an old call does not cancel its replacement`() {
        val downloader = OkHttpDownloader()
        val request = request("download:20:first")
        val oldCall = checkNotNull(downloader.downloadFile(request, {}, {}))
        downloader.downloadFile(request, {}, {})

        downloader.cancelDownload(oldCall)

        verify(firstCall).cancel()
        verify(secondCall, never()).cancel()
        downloader.cancelDownload(secondCall)
        verify(secondCall).cancel()
    }

    @Test
    fun `cancellation before a response still signals cleanup completion`() {
        val downloader = OkHttpDownloader()
        val request = request("download:cancel-before-response")
        val callback = argumentCaptor<Callback>()
        var completions = 0
        downloader.downloadFile(request, {}, { error("Cancelled download must not report a result") }) {
            completions++
        }
        verify(firstCall).enqueue(callback.capture())
        whenever(firstCall.isCanceled()).thenReturn(true)

        callback.firstValue.onFailure(firstCall, IOException("cancelled"))

        assertThat(completions).isEqualTo(1)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `progress uses old bytes only when the server honors range`() = runTest(UnconfinedTestDispatcher()) {
        for (code in listOf(200, 206)) {
            val request = request("download:range-$code")
            request.destinationFile.writeText("old!")
            val source = mock<BufferedSource>()
            val body = mock<ResponseBody>()
            whenever(body.source()).thenReturn(source)
            whenever(body.contentLength()).thenReturn(4L)
            var reads = 0
            doAnswer { invocation ->
                when (reads++) {
                    0 -> {
                        invocation.getArgument<Buffer>(0).writeUtf8("ab")
                        2L
                    }
                    1 -> {
                        invocation.getArgument<Buffer>(0).writeUtf8("cd")
                        2L
                    }
                    else -> -1L
                }
            }.whenever(source).read(any(), any())
            val progress = mutableListOf<Float>()
            val transport = DefaultFileTransferTransport()
            val download = async {
                transport.download(request) { progress += it }
            }
            val call = if (code == 200) firstCall else secondCall
            val callback = argumentCaptor<Callback>()
            verify(call).enqueue(callback.capture())
            val response = Response.Builder()
                .request(Request.Builder().url(request.url).build())
                .protocol(Protocol.HTTP_1_1).code(code).message("OK").body(body).build()

            callback.firstValue.onResponse(call, response)

            assertThat(download.await()).isEqualTo(FileTransferResult.Success(request.destinationFile.path))
            assertThat(progress.first()).isEqualTo(if (code == 200) 50f else 75f)
            assertThat(request.destinationFile.readText()).isEqualTo(if (code == 200) "abcd" else "old!abcd")
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `connection failure returns interruption and retains the partial file`() = runTest(UnconfinedTestDispatcher()) {
        val request = request("download:connection-loss")
        request.destinationFile.writeText("partial")
        val transport = DefaultFileTransferTransport()
        val download = async { transport.download(request) {} }
        val callback = argumentCaptor<Callback>()
        verify(firstCall).enqueue(callback.capture())
        val error = IOException("Connection lost")

        callback.firstValue.onFailure(firstCall, error)

        assertThat(download.await()).isEqualTo(FileTransferResult.Interrupted(error))
        assertThat(request.destinationFile.readText()).isEqualTo("partial")
        verify(firstCall, never()).cancel()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `response read failure retains bytes for a later range request`() = runTest(UnconfinedTestDispatcher()) {
        val request = request("download:read-loss")
        request.destinationFile.writeText("old")
        val source = mock<BufferedSource>()
        val body = mock<ResponseBody>()
        val error = IOException("Connection reset")
        whenever(body.source()).thenReturn(source)
        whenever(body.contentLength()).thenReturn(6L)
        var reads = 0
        doAnswer { invocation ->
            if (reads++ > 0) throw error
            invocation.getArgument<Buffer>(0).writeUtf8("new")
            3L
        }.whenever(source).read(any(), any())
        val transport = DefaultFileTransferTransport()
        val download = async { transport.download(request) {} }
        val callback = argumentCaptor<Callback>()
        verify(firstCall).enqueue(callback.capture())
        val response = Response.Builder()
            .request(Request.Builder().url(request.url).build())
            .protocol(Protocol.HTTP_1_1).code(206).message("Partial Content").body(body).build()

        callback.firstValue.onResponse(firstCall, response)

        assertThat(download.await()).isEqualTo(FileTransferResult.Interrupted(error))
        assertThat(request.destinationFile.readText()).isEqualTo("oldnew")
        verify(body).close()
        verify(firstCall, never()).cancel()

        val retry = async { transport.download(request) {} }
        val requests = argumentCaptor<Request>()
        verify(client, Mockito.times(2)).newCall(requests.capture())
        assertThat(requests.lastValue.header("Range")).isEqualTo("bytes=6-")
        val retryCallback = argumentCaptor<Callback>()
        verify(secondCall).enqueue(retryCallback.capture())
        retryCallback.firstValue.onFailure(secondCall, IOException("Still offline"))
        assertThat(retry.await()).isInstanceOf(FileTransferResult.Interrupted::class.java)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `destination write error is a failure not an interruption`() = runTest(UnconfinedTestDispatcher()) {
        val request = request("download:disk-error").copy(destinationFile = temporaryFolder.newFolder())
        val body = mock<ResponseBody>()
        whenever(body.source()).thenReturn(Buffer().writeUtf8("data"))
        whenever(body.contentLength()).thenReturn(4L)
        val transport = DefaultFileTransferTransport()
        val download = async { transport.download(request) {} }
        val callback = argumentCaptor<Callback>()
        verify(firstCall).enqueue(callback.capture())
        val response = Response.Builder()
            .request(Request.Builder().url(request.url).build())
            .protocol(Protocol.HTTP_1_1).code(200).message("OK").body(body).build()

        callback.firstValue.onResponse(firstCall, response)

        assertThat(download.await()).isInstanceOf(FileTransferResult.Failure::class.java)
        verify(body).close()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `default transport cancellation waits for the response writer to stop`() = runTest(UnconfinedTestDispatcher()) {
        val enteredRead = CountDownLatch(1)
        val releaseRead = CountDownLatch(1)
        val cancelled = AtomicBoolean()
        val source = mock<BufferedSource>()
        val body = mock<ResponseBody>()
        whenever(body.source()).thenReturn(source)
        whenever(body.contentLength()).thenReturn(3L)
        whenever(firstCall.isCanceled()).thenAnswer { cancelled.get() }
        doAnswer { cancelled.set(true) }.whenever(firstCall).cancel()
        doAnswer { invocation ->
            enteredRead.countDown()
            check(releaseRead.await(2, TimeUnit.SECONDS))
            invocation.getArgument<Buffer>(0).writeUtf8("old")
            3L
        }.whenever(source).read(any(), any())

        val request = request("download:writer-stop")
        val transport = DefaultFileTransferTransport()
        val job = launch { transport.download(request) {} }
        val callback = argumentCaptor<Callback>()
        verify(firstCall).enqueue(callback.capture())
        val response = Response.Builder()
            .request(Request.Builder().url(request.url).build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(body)
            .build()
        val executor = Executors.newSingleThreadExecutor()
        val writer = executor.submit { callback.firstValue.onResponse(firstCall, response) }
        try {
            assertThat(enteredRead.await(2, TimeUnit.SECONDS)).isTrue()
            job.cancel()

            assertThat(cancelled.get()).isTrue()
            assertThat(job.isCompleted).isFalse()
            releaseRead.countDown()
            writer.get(2, TimeUnit.SECONDS)
            job.join()

            assertThat(job.isCompleted).isTrue()
            assertThat(request.destinationFile.readText()).isEqualTo("old")
            verify(body).close()
        } finally {
            releaseRead.countDown()
            writer.get(2, TimeUnit.SECONDS)
            job.cancelAndJoin()
            executor.shutdownNow()
        }
    }

    private fun request(operationId: String) = FileDownloadRequest(
        operationId = operationId,
        url = "https://cdn.test/file",
        destinationFile = temporaryFolder.newFile(),
    )
}
