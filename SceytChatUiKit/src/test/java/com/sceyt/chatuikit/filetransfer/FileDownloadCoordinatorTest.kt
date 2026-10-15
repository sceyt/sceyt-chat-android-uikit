package com.sceyt.chatuikit.filetransfer

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.persistence.logicimpl.RecordingFileTransferTransport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class FileDownloadCoordinatorTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `duplicate operation keeps both participants and passes context unchanged`() = runTest(UnconfinedTestDispatcher()) {
        val transport = RecordingFileTransferTransport()
        val coordinator = FileDownloadCoordinator(this) { transport }
        val context = object : FileTransferContext {}
        val request = request().copy(context = context)
        val first = Results()
        val second = Results()
        coordinator.download(request, "first", first) {}
        coordinator.download(request, "second", second) {}

        assertThat(transport.downloadCalls).hasSize(1)
        assertThat(transport.downloadCalls.single().request.context).isSameInstanceAs(context)
        transport.downloadCalls.single().succeed(request.destinationFile.path)

        assertThat(first.values.single().getOrThrow()).isEqualTo(request.destinationFile.path)
        assertThat(second.values.single().getOrThrow()).isEqualTo(request.destinationFile.path)
    }

    @Test
    fun `pause from initial notification never consults optimistic transport`() = runTest(UnconfinedTestDispatcher()) {
        val transport = RecordingFileTransferTransport().apply { pauseResult = true }
        val coordinator = FileDownloadCoordinator(this) { transport }
        lateinit var subscription: FileTransferSubscription
        val observer = object : FileTransferObserver {
            override fun onStarted() = subscription.pause()
            override fun onResult(result: Result<String>) = error("Paused download must not complete")
        }

        coordinator.download(request(), "first", observer) { subscription = it }

        assertThat(subscription.isPaused).isTrue()
        assertThat(transport.downloadCalls).isEmpty()
        assertThat(transport.pauseCalls).isEmpty()
        coordinator.cancelAll()
    }

    @Test
    fun `cancelled attempt cannot delete replacement partial or complete it`() = runTest(UnconfinedTestDispatcher()) {
        val transport = RecordingFileTransferTransport()
        val coordinator = FileDownloadCoordinator(this) { transport }
        val request = request()
        val observer = Results()
        lateinit var subscription: FileTransferSubscription
        coordinator.download(request, "first", observer) { subscription = it }
        val oldCall = transport.downloadCalls.single()
        subscription.pause()
        subscription.resume()
        request.destinationFile.writeText("partial")

        oldCall.fail(IOException("late failure"))
        assertThat(request.destinationFile.readText()).isEqualTo("partial")
        assertThat(observer.values).isEmpty()
        transport.downloadCalls.last().succeed(request.destinationFile.path)
        assertThat(observer.values).hasSize(1)
    }

    @Test
    fun `network wait preserves partial and completes without auto retry`() = runTest(UnconfinedTestDispatcher()) {
        val transport = RecordingFileTransferTransport()
        val coordinator = FileDownloadCoordinator(this) { transport }
        val request = request()
        request.destinationFile.writeText("partial")
        val observer = Results()
        coordinator.download(request, "first", observer) {}

        transport.downloadCalls.single().waitingForNetwork()

        assertThat(request.destinationFile.readText()).isEqualTo("partial")
        assertThat(observer.values.single().exceptionOrNull()?.message).isEqualTo("Waiting for network")
        assertThat(transport.downloadCalls).hasSize(1)
    }

    @Test
    fun `pause arriving after transport result keeps the result`() = runTest(UnconfinedTestDispatcher()) {
        val transport = RecordingFileTransferTransport()
        val coordinator = FileDownloadCoordinator(this) { transport }
        val request = request()
        lateinit var subscription: FileTransferSubscription
        val values = mutableListOf<Result<String>>()
        val observer = object : FileTransferObserver {
            override fun onProgress(progressPercent: Float) {
                subscription.pause()
                transport.downloadCalls.single().succeed(request.destinationFile.path)
            }

            override fun onResult(result: Result<String>) { values += result }
        }
        coordinator.download(request, "file", observer) { subscription = it }

        transport.downloadCalls.single().progress(50f)

        assertThat(values.single().getOrThrow()).isEqualTo(request.destinationFile.path)
        assertThat(transport.downloadCalls).hasSize(1)
    }

    private fun request() = FileDownloadRequest(
        operationId = "download:file",
        url = "opaque-storage-key",
        destinationFile = temporaryFolder.newFile(),
    )

    private class Results : FileTransferObserver {
        val values = mutableListOf<Result<String>>()
        override fun onResult(result: Result<String>) { values += result }
    }
}
