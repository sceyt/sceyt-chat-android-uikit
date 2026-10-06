package com.sceyt.chatuikit.persistence.logicimpl

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferCallback
import com.sceyt.chatuikit.filetransfer.FileTransferDestinationProvider
import com.sceyt.chatuikit.filetransfer.FileTransferTransport
import com.sceyt.chatuikit.filetransfer.SceytChatUIKitFileTransfer
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.ProgressUpdateCallback
import com.sceyt.chatuikit.persistence.file_transfer.ResumePauseCallback
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferResultCallback
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class AttachmentDownloadCoordinatorTest {
    private lateinit var context: Context
    private lateinit var destinationFile: File
    private lateinit var service: TestFileTransferService
    private lateinit var transport: RecordingFileTransferTransport
    private lateinit var coordinator: AttachmentDownloadCoordinator
    private lateinit var previousFileTransfer: SceytChatUIKitFileTransfer
    private lateinit var testScope: TestScope

    @Before
    fun setUp() {
        stopKoin()
        context = RuntimeEnvironment.getApplication()
        destinationFile = File(context.cacheDir, "transfer-tests/download.bin")
        destinationFile.parentFile?.mkdirs()
        destinationFile.delete()
        service = TestFileTransferService()
        transport = RecordingFileTransferTransport()
        testScope = TestScope(UnconfinedTestDispatcher())
        previousFileTransfer = SceytChatUIKit.fileTransfer
        SceytChatUIKit.fileTransfer = SceytChatUIKitFileTransfer().apply {
            this.transport = this@AttachmentDownloadCoordinatorTest.transport
            destinationProvider = FileTransferDestinationProvider { _, _ -> destinationFile }
        }
        SceytKoinApp.koinApp = startKoin {
            modules(module { single<FileTransferService> { service } })
        }
        coordinator = AttachmentDownloadCoordinator(context, testScope)
    }

    @After
    fun tearDown() {
        destinationFile.parentFile?.deleteRecursively()
        SceytChatUIKit.fileTransfer = previousFileTransfer
        SceytKoinApp.koinApp = null
        stopKoin()
    }

    @Test
    fun `invalid url returns existing error without invoking transport`() {
        val attachment = attachment(url = null, state = TransferState.PendingDownload)
        val task = transferTask(attachment)
        var result: SceytResponse<String>? = null
        task.downloadCallback = TransferResultCallback { result = it }

        coordinator.downloadFile(attachment, task)

        assertThat(transport.downloadCalls).isEmpty()
        assertThat(result).isInstanceOf(SceytResponse.Error::class.java)
        assertThat(result?.message).isEqualTo("Wrong url")
    }

    @Test
    fun `existing complete destination skips transport`() {
        destinationFile.writeBytes(byteArrayOf(1, 2, 3, 4))
        val attachment = attachment(fileSize = 4L, state = TransferState.PendingDownload)
        val task = transferTask(attachment)
        var result: SceytResponse<String>? = null
        task.downloadCallback = TransferResultCallback { result = it }

        coordinator.downloadFile(attachment, task)

        assertThat(transport.downloadCalls).isEmpty()
        assertThat(result).isInstanceOf(SceytResponse.Success::class.java)
        assertThat(result?.data).isEqualTo(destinationFile.path)
    }

    @Test
    fun `duplicate active download invokes transport once`() {
        val attachment = attachment(state = TransferState.PendingDownload)
        val task = transferTask(attachment)

        coordinator.downloadFile(attachment, task)
        coordinator.downloadFile(attachment, task)

        assertThat(transport.downloadCalls).hasSize(1)
        assertThat(transport.downloadCalls.single().request.operationId).isEqualTo("download:10")
    }

    @Test
    fun `progress and success are forwarded to transfer task`() {
        val attachment = attachment(state = TransferState.PendingDownload)
        val task = transferTask(attachment)
        val progress = mutableListOf<TransferData>()
        var result: SceytResponse<String>? = null
        task.progressCallback = ProgressUpdateCallback { progress += it }
        task.downloadCallback = TransferResultCallback { result = it }

        coordinator.downloadFile(attachment, task)
        transport.downloadCalls.single().progress(42f)
        transport.downloadCalls.single().succeed(destinationFile.path)

        assertThat(progress.map { it.progressPercent }).containsExactly(0f, 42f).inOrder()
        assertThat(progress.map { it.state }).containsExactly(
            TransferState.Downloading,
            TransferState.Downloading,
        ).inOrder()
        assertThat(result?.data).isEqualTo(destinationFile.path)
    }

    @Test
    fun `failure removes partial destination and forwards error`() {
        val attachment = attachment(state = TransferState.PendingDownload)
        val task = transferTask(attachment)
        val partialBytes = byteArrayOf(1, 2)
        var result: SceytResponse<String>? = null
        task.downloadCallback = TransferResultCallback { result = it }

        coordinator.downloadFile(attachment, task)
        destinationFile.writeBytes(partialBytes)
        transport.downloadCalls.single().fail(SceytException(7, "failed"))

        assertThat(destinationFile.exists()).isFalse()
        assertThat(result).isInstanceOf(SceytResponse.Error::class.java)
        assertThat(result?.code).isEqualTo(7)
    }

    @Test
    fun `invalid range removes partial destination so error resume starts fresh`() {
        destinationFile.writeBytes(byteArrayOf(1, 2, 3, 4, 5))
        val attachment = attachment(state = TransferState.PendingDownload)
        val results = mutableListOf<SceytResponse<String>>()
        val task = transferTask(attachment).apply {
            downloadCallback = TransferResultCallback { results += it }
        }
        service.addTransferTask(task)

        coordinator.downloadFile(attachment, task)
        transport.downloadCalls.single().fail(SceytException(416, "Range not satisfiable"))

        assertThat(results.single().code).isEqualTo(416)
        assertThat(destinationFile.exists()).isFalse()

        coordinator.resumeLoad(attachment, TransferState.ErrorDownload)

        assertThat(transport.downloadCalls).hasSize(2)
        assertThat(transport.downloadCalls.last().request.destinationFile.length()).isEqualTo(0L)
        destinationFile.writeBytes(byteArrayOf(1, 2, 3, 4))
        transport.downloadCalls.last().succeed(destinationFile.path)
        assertThat(results).hasSize(2)
        assertThat(results.last().data).isEqualTo(destinationFile.path)
    }

    @Test
    fun `transport cancellation is forwarded as failure and allows retry`() {
        val attachment = attachment(state = TransferState.PendingDownload)
        val results = mutableListOf<SceytResponse<String>>()
        val task = transferTask(attachment).apply {
            downloadCallback = TransferResultCallback { results += it }
        }

        coordinator.downloadFile(attachment, task)
        destinationFile.writeBytes(byteArrayOf(1, 2))
        transport.downloadCalls.single().fail(CancellationException("transport cancelled"))

        assertThat(results).hasSize(1)
        assertThat(results.single()).isInstanceOf(SceytResponse.Error::class.java)
        assertThat(results.single().message).isEqualTo("transport cancelled")
        assertThat(destinationFile.exists()).isFalse()
        coordinator.downloadFile(attachment, task)
        assertThat(transport.downloadCalls).hasSize(2)
        transport.downloadCalls.last().succeed(destinationFile.path)
        assertThat(results).hasSize(2)
        assertThat(results.last().data).isEqualTo(destinationFile.path)
    }

    @Test
    fun `transport timeout is forwarded as failure and allows retry`() {
        SceytChatUIKit.fileTransfer.transport = object : FileTransferTransport by transport {
            override suspend fun download(request: FileDownloadRequest, callback: FileTransferCallback) =
                withTimeout(10.milliseconds) { transport.download(request, callback) }
        }
        val attachment = attachment(state = TransferState.PendingDownload)
        val results = mutableListOf<SceytResponse<String>>()
        val task = transferTask(attachment).apply {
            downloadCallback = TransferResultCallback { results += it }
        }

        coordinator.downloadFile(attachment, task)
        destinationFile.writeBytes(byteArrayOf(1, 2))
        testScope.advanceTimeBy(10.milliseconds)
        testScope.runCurrent()

        assertThat(results).hasSize(1)
        assertThat(results.single()).isInstanceOf(SceytResponse.Error::class.java)
        assertThat(transport.downloadCalls.first().cancelled).isTrue()
        assertThat(destinationFile.exists()).isFalse()
        coordinator.downloadFile(attachment, task)
        assertThat(transport.downloadCalls).hasSize(2)
        transport.downloadCalls.last().succeed(destinationFile.path)
        assertThat(results).hasSize(2)
        assertThat(results.last().data).isEqualTo(destinationFile.path)
    }

    @Test
    fun `empty download result removes partial destination and forwards error`() {
        val attachment = attachment(state = TransferState.PendingDownload)
        val task = transferTask(attachment)
        val partialBytes = byteArrayOf(1, 2)
        var result: SceytResponse<String>? = null
        task.downloadCallback = TransferResultCallback { result = it }

        coordinator.downloadFile(attachment, task)
        destinationFile.writeBytes(partialBytes)
        transport.downloadCalls.single().succeed("")

        assertThat(destinationFile.exists()).isFalse()
        assertThat(result).isInstanceOf(SceytResponse.Error::class.java)
        assertThat(result?.message).isEqualTo("File download returned an empty local path")
    }

    @Test
    fun `waiting for network keeps partial destination and forwards error`() {
        val attachment = attachment(state = TransferState.PendingDownload)
        val task = transferTask(attachment)
        val partialBytes = byteArrayOf(1, 2)
        var result: SceytResponse<String>? = null
        task.downloadCallback = TransferResultCallback { result = it }

        coordinator.downloadFile(attachment, task)
        destinationFile.writeBytes(partialBytes)
        val call = transport.downloadCalls.single()
        call.waitingForNetwork()

        assertThat(call.cancelled).isTrue()
        assertThat(destinationFile.readBytes()).isEqualTo(partialBytes)
        assertThat(result).isInstanceOf(SceytResponse.Error::class.java)
        assertThat(result?.message).isEqualTo("Waiting for network")
    }

    @Test
    fun `pause cancels download and resume starts a new call`() {
        val attachment = attachment(state = TransferState.Downloading)
        val task = transferTask(attachment)
        val states = mutableListOf<TransferState>()
        task.resumePauseCallback = ResumePauseCallback { states += it.state }
        service.addTransferTask(task)

        coordinator.downloadFile(attachment, task)
        coordinator.pauseLoad(attachment, TransferState.Downloading)
        coordinator.resumeLoad(attachment, TransferState.PauseDownload)

        assertThat(transport.downloadCalls.first().cancelled).isTrue()
        assertThat(transport.downloadCalls).hasSize(2)
        assertThat(task.state).isEqualTo(TransferState.Downloading)
        assertThat(states).containsExactly(
            TransferState.PauseDownload,
            TransferState.Downloading,
        ).inOrder()
    }

    @Test
    fun `native download pause and resume keep the original call`() {
        transport.pauseResult = true
        transport.resumeResult = true
        val attachment = attachment(state = TransferState.Downloading)
        val task = transferTask(attachment)
        service.addTransferTask(task)

        coordinator.downloadFile(attachment, task)
        val call = transport.downloadCalls.single()
        coordinator.pauseLoad(attachment, TransferState.Downloading)
        coordinator.resumeLoad(attachment, TransferState.PauseDownload)

        assertThat(transport.pauseCalls).containsExactly("download:10")
        assertThat(transport.resumeCalls).containsExactly("download:10")
        assertThat(call.cancelled).isFalse()
        assertThat(transport.downloadCalls).hasSize(1)
        assertThat(task.state).isEqualTo(TransferState.Downloading)

        call.succeed(destinationFile.path)
    }

    @Test
    fun `native paused download ignores late progress until resumed`() {
        transport.pauseResult = true
        transport.resumeResult = true
        val attachment = attachment(state = TransferState.Downloading)
        val task = transferTask(attachment)
        val progress = mutableListOf<Float>()
        task.progressCallback = ProgressUpdateCallback { progress += it.progressPercent }
        service.addTransferTask(task)

        coordinator.downloadFile(attachment, task)
        val call = transport.downloadCalls.single()
        coordinator.pauseLoad(attachment, TransferState.Downloading)
        call.progress(25f)

        assertThat(progress).containsExactly(0f)

        coordinator.resumeLoad(attachment, TransferState.PauseDownload)
        call.progress(50f)

        assertThat(progress).containsExactly(0f, 50f).inOrder()
        call.succeed(destinationFile.path)
    }

    @Test
    fun `success callback exception is swallowed and does not invoke error callback`() {
        val attachment = attachment(state = TransferState.PendingDownload)
        val task = transferTask(attachment)
        var callbackCount = 0
        task.downloadCallback = TransferResultCallback {
            callbackCount++
            throw IllegalStateException("callback failed")
        }

        coordinator.downloadFile(attachment, task)
        transport.downloadCalls.single().succeed(destinationFile.path)

        assertThat(callbackCount).isEqualTo(1)
    }

    @Test
    fun `pausing active download cancels its job and publishes paused state`() {
        val attachment = attachment(state = TransferState.PendingDownload)
        val task = transferTask(attachment)
        val states = mutableListOf<TransferState>()
        task.resumePauseCallback = ResumePauseCallback { states += it.state }
        service.addTransferTask(task)

        coordinator.downloadFile(attachment, task)
        coordinator.pauseLoad(attachment, TransferState.PendingDownload)

        assertThat(transport.downloadCalls.single().cancelled).isTrue()
        assertThat(task.state).isEqualTo(TransferState.PauseDownload)
        assertThat(states).containsExactly(TransferState.PauseDownload)
    }

    @Test
    fun `pause uses stable operation id when url changes`() {
        val attachment = attachment(
            url = "https://cdn.test/old.txt",
            state = TransferState.Downloading,
        )
        val task = transferTask(attachment)
        service.addTransferTask(task)

        coordinator.downloadFile(attachment, task)
        coordinator.pauseLoad(
            attachment.copy(url = "https://cdn.test/new.txt"),
            TransferState.Downloading,
        )

        assertThat(transport.downloadCalls.single().cancelled).isTrue()
    }

    @Test
    fun `late result from cancelled download does not complete replacement`() {
        val attachment = attachment(state = TransferState.Downloading)
        val task = transferTask(attachment)
        val states = mutableListOf<TransferState>()
        val results = mutableListOf<String?>()
        task.resumePauseCallback = ResumePauseCallback { states += it.state }
        task.downloadCallback = TransferResultCallback { results += it.data }
        service.addTransferTask(task)

        coordinator.downloadFile(attachment, task)
        val pausedCall = transport.downloadCalls.single()
        coordinator.pauseLoad(attachment, TransferState.Downloading)
        coordinator.resumeLoad(attachment, TransferState.PauseDownload)
        val replacementCall = transport.downloadCalls.last()

        pausedCall.progress(30f)
        pausedCall.succeed("late-path")

        assertThat(results).isEmpty()
        assertThat(states).containsExactly(
            TransferState.PauseDownload,
            TransferState.Downloading,
        ).inOrder()

        coordinator.pauseLoad(attachment, TransferState.Downloading)

        assertThat(replacementCall.cancelled).isTrue()
        assertThat(states).containsExactly(
            TransferState.PauseDownload,
            TransferState.Downloading,
            TransferState.PauseDownload,
        ).inOrder()
        assertThat(results).isEmpty()
    }

    @Test
    fun `cancelled download cleanup does not remove replacement`() {
        val attachment = attachment(state = TransferState.Downloading)
        val task = transferTask(attachment)
        val cancellationGate = CompletableDeferred<Unit>()
        transport.downloadCancellationGate = cancellationGate
        service.addTransferTask(task)

        coordinator.downloadFile(attachment, task)
        coordinator.pauseLoad(attachment, TransferState.Downloading)
        coordinator.resumeLoad(attachment, TransferState.PauseDownload)

        assertThat(transport.downloadCalls).hasSize(2)
        val replacement = transport.downloadCalls.last()

        cancellationGate.complete(Unit)
        coordinator.downloadFile(attachment, task)

        assertThat(transport.downloadCalls).hasSize(2)
        assertThat(replacement.cancelled).isFalse()
    }

    @Test
    fun `late error from cancelled download keeps replacement partial destination`() {
        val cancellationGate = CompletableDeferred<Unit>()
        SceytChatUIKit.fileTransfer.transport = object : FileTransferTransport by transport {
            override suspend fun download(request: FileDownloadRequest, callback: FileTransferCallback): String? {
                return try {
                    transport.download(request, callback)
                } catch (_: CancellationException) {
                    withContext(NonCancellable) { cancellationGate.await() }
                    throw SceytException(416, "Late download error")
                }
            }
        }
        val attachment = attachment(state = TransferState.Downloading)
        val results = mutableListOf<SceytResponse<String>>()
        val task = transferTask(attachment).apply {
            downloadCallback = TransferResultCallback { results += it }
        }
        service.addTransferTask(task)

        coordinator.downloadFile(attachment, task)
        coordinator.pauseLoad(attachment, TransferState.Downloading)
        coordinator.resumeLoad(attachment, TransferState.PauseDownload)
        val partialBytes = byteArrayOf(1, 2)
        destinationFile.writeBytes(partialBytes)
        cancellationGate.complete(Unit)

        assertThat(destinationFile.readBytes()).isEqualTo(partialBytes)
        assertThat(results).isEmpty()
        transport.downloadCalls.last().succeed(destinationFile.path)
        assertThat(results.single().data).isEqualTo(destinationFile.path)
    }

    @Test
    fun `pausing download keeps partial destination so resume can range request`() {
        val attachment = attachment(state = TransferState.Downloading)
        val task = transferTask(attachment)
        val partialBytes = byteArrayOf(1, 2)
        var result: SceytResponse<String>? = null
        task.downloadCallback = TransferResultCallback { result = it }
        service.addTransferTask(task)

        coordinator.downloadFile(attachment, task)
        destinationFile.writeBytes(partialBytes)
        coordinator.pauseLoad(attachment, TransferState.Downloading)

        assertThat(transport.downloadCalls.single().cancelled).isTrue()
        assertThat(destinationFile.exists()).isTrue()
        assertThat(destinationFile.readBytes()).isEqualTo(partialBytes)
        assertThat(result).isNull()

        coordinator.resumeLoad(attachment, TransferState.PauseDownload)

        assertThat(transport.downloadCalls).hasSize(2)
        assertThat(destinationFile.readBytes()).isEqualTo(partialBytes)
    }

    @Test
    fun `cancel all stops active download and allows a new download`() {
        val active = attachment(messageTid = 80L, state = TransferState.Downloading)
        val next = attachment(messageTid = 81L, state = TransferState.PendingDownload)
        val results = mutableListOf<SceytResponse<String>>()
        val activeTask = transferTask(active).apply {
            downloadCallback = TransferResultCallback { results += it }
        }

        coordinator.downloadFile(active, activeTask)
        coordinator.cancelAll()
        coordinator.downloadFile(next, transferTask(next))

        assertThat(transport.downloadCalls).hasSize(2)
        assertThat(transport.downloadCalls[0].cancelled).isTrue()
        assertThat(transport.downloadCalls[1].request.operationId).isEqualTo("download:81")
        assertThat(results).isEmpty()
    }
}
