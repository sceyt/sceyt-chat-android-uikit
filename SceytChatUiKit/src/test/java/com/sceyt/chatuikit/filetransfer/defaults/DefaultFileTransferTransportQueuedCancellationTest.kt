package com.sceyt.chatuikit.filetransfer.defaults

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import com.sceyt.chatuikit.filetransfer.download.ChatSdkFileDownloader
import com.sceyt.chatuikit.persistence.logicimpl.attachment
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.whenever
import java.io.File

class DefaultFileTransferTransportQueuedCancellationTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `cancelling a queued download does not wait for an unrelated download`() = runTest {
        val activeStarted = CompletableDeferred<Unit>()
        val releaseActive = CompletableDeferred<Unit>()
        val queue = Mutex()
        Mockito.mockConstruction(ChatSdkFileDownloader::class.java) { downloader, _ ->
            doSuspendableAnswer { invocation ->
                val request = invocation.getArgument<FileDownloadRequest>(0)
                queue.withLock {
                    activeStarted.complete(Unit)
                    releaseActive.await()
                    request.destinationFile.writeText("data")
                    FileTransferResult.Success(request.destinationFile.absolutePath)
                }
            }.whenever(downloader) { download(any(), any()) }
        }.use {
            val transport = DefaultFileTransferTransport()
            val activeRequest = request("active")
            val activeDownload = async(start = CoroutineStart.UNDISPATCHED) {
                transport.download(activeRequest) {}
            }
            activeStarted.await()
            val queuedRequest = request("queued")
            val queuedDownload = async(start = CoroutineStart.UNDISPATCHED) {
                transport.download(queuedRequest) {}
            }
            try {
                queuedDownload.cancel()
                withTimeout(5_000) { queuedDownload.join() }

                assertThat(activeDownload.isCompleted).isFalse()
                assertThat(queuedRequest.destinationFile.exists()).isFalse()
                releaseActive.complete(Unit)
                val result = activeDownload.await()
                assertThat(result)
                    .isEqualTo(FileTransferResult.Success(activeRequest.destinationFile.absolutePath))
                assertThat(activeRequest.destinationFile.readText()).isEqualTo("data")
            } finally {
                releaseActive.complete(Unit)
                activeDownload.cancelAndJoin()
                queuedDownload.cancelAndJoin()
            }
        }
    }

    private fun request(name: String) = FileDownloadRequest(
        operationId = "download:$name",
        url = "https://cdn.test/$name",
        destinationFile = File(temporaryFolder.root, name),
        attachment = attachment(),
    )
}
