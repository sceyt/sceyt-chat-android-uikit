package com.sceyt.chatuikit.filetransfer.defaults

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class DefaultFileTransferTransportQueuedCancellationTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `cancelling a queued download does not wait for an unrelated download`() = runBlocking<Unit> {
        val activeStarted = CountDownLatch(1)
        val releaseActive = CountDownLatch(1)
        val dispatcher = Dispatcher().apply {
            maxRequests = 1
            maxRequestsPerHost = 1
        }
        val client = OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .addInterceptor { chain ->
                activeStarted.countDown()
                check(releaseActive.await(10, TimeUnit.SECONDS))
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("data".toResponseBody())
                    .build()
            }
            .build()

        try {
            Mockito.mockConstruction(
                OkHttpClient.Builder::class.java,
                Mockito.withSettings().defaultAnswer(Mockito.RETURNS_SELF),
            ) { builder, _ ->
                whenever(builder.build()).thenReturn(client)
            }.use {
                val transport = DefaultFileTransferTransport()
                val activeRequest = request("active")
                val activeDownload = async(start = CoroutineStart.UNDISPATCHED) {
                    transport.download(activeRequest) {}
                }
                val queuedRequest = request("queued")
                val queuedDownload = async(start = CoroutineStart.UNDISPATCHED) {
                    transport.download(queuedRequest) {}
                }
                try {
                    assertThat(activeStarted.await(5, TimeUnit.SECONDS)).isTrue()
                    assertThat(dispatcher.queuedCallsCount()).isEqualTo(1)

                    queuedDownload.cancel()
                    withTimeout(5_000) { queuedDownload.join() }

                    assertThat(activeDownload.isCompleted).isFalse()
                    assertThat(queuedRequest.destinationFile.exists()).isFalse()
                    releaseActive.countDown()
                    val result = withTimeout(5_000) { activeDownload.await() }
                    assertThat(result)
                        .isEqualTo(FileTransferResult.Success(activeRequest.destinationFile.absolutePath))
                    assertThat(activeRequest.destinationFile.readText()).isEqualTo("data")
                } finally {
                    releaseActive.countDown()
                    withTimeout(5_000) {
                        activeDownload.cancelAndJoin()
                        queuedDownload.cancelAndJoin()
                    }
                }
            }
        } finally {
            releaseActive.countDown()
            dispatcher.executorService.shutdownNow()
            client.connectionPool.evictAll()
        }
    }

    private fun request(name: String) = FileDownloadRequest(
        operationId = "download:$name",
        url = "https://cdn.test/$name",
        destinationFile = File(temporaryFolder.root, name),
    )
}
