package com.sceyt.chatuikit.persistence.logicimpl

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferCallback
import com.sceyt.chatuikit.filetransfer.FileTransferDestinationProvider
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import com.sceyt.chatuikit.filetransfer.FileTransferTransport
import com.sceyt.chatuikit.filetransfer.SceytChatUIKitFileTransfer
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.persistence.database.dao.FileChecksumDao
import com.sceyt.chatuikit.persistence.di.CoroutineContextType
import com.sceyt.chatuikit.persistence.file_transfer.AttachmentTransferStateStore
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferServiceImpl
import com.sceyt.chatuikit.persistence.file_transfer.ProgressUpdateCallback
import com.sceyt.chatuikit.persistence.file_transfer.ResumePauseCallback
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferResultCallback
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.file_transfer.TransferTask
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import java.io.File
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class AttachmentDownloadCoordinatorTest {
    private lateinit var context: Context
    private lateinit var destinationFile: File
    private lateinit var service: FileTransferServiceImpl
    private lateinit var transport: RecordingFileTransferTransport
    private lateinit var previousFileTransfer: SceytChatUIKitFileTransfer
    private lateinit var testScope: TestScope

    @Before
    fun setUp() {
        stopKoin()
        context = RuntimeEnvironment.getApplication()
        destinationFile = File(context.cacheDir, "transfer-tests/download.bin")
        destinationFile.parentFile?.mkdirs()
        destinationFile.delete()
        transport = RecordingFileTransferTransport()
        testScope = TestScope(UnconfinedTestDispatcher())
        service = FileTransferServiceImpl(context, mock(), AttachmentDownloadCoordinator(context, testScope), mock())
        previousFileTransfer = SceytChatUIKit.fileTransfer
        SceytChatUIKit.fileTransfer = SceytChatUIKitFileTransfer().apply {
            this.transport = this@AttachmentDownloadCoordinatorTest.transport
            destinationProvider = FileTransferDestinationProvider { _, _ -> destinationFile }
        }
        SceytKoinApp.koinApp = startKoin {
            modules(module {
                single<FileTransferService> { service }
                single<PersistenceAttachmentLogic> { mock() }
                single<FileChecksumDao> { mock() }
                single<CoroutineContext>(named(CoroutineContextType.SingleThreaded)) { Dispatchers.Unconfined }
            })
        }
    }

    @After
    fun tearDown() {
        service.cancelAllTransfers()
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

        service.download(attachment)

        assertThat(transport.downloadCalls).isEmpty()
        assertThat(result).isInstanceOf(SceytResponse.Error::class.java)
        assertThat(result?.message).isEqualTo("Wrong url")
    }

    @Test
    fun `failing destination provider reports download error instead of throwing`() {
        SceytChatUIKit.fileTransfer.destinationProvider = FileTransferDestinationProvider { _, _ ->
            throw IllegalStateException("no destination")
        }
        val attachment = attachment(state = TransferState.PendingDownload)
        val task = transferTask(attachment)
        var result: SceytResponse<String>? = null
        task.downloadCallback = TransferResultCallback { result = it }

        service.download(attachment)

        assertThat(transport.downloadCalls).isEmpty()
        assertThat(result).isInstanceOf(SceytResponse.Error::class.java)
        assertThat(result?.message).isEqualTo("no destination")
    }

    @Test
    fun `failing destination provider on resume reports download error instead of throwing`() {
        SceytChatUIKit.fileTransfer.destinationProvider = FileTransferDestinationProvider { _, _ ->
            throw IllegalStateException("no destination")
        }
        val attachment = attachment(state = TransferState.ErrorDownload)
        val task = transferTask(attachment)
        var result: SceytResponse<String>? = null
        task.downloadCallback = TransferResultCallback { result = it }

        service.resume(attachment.messageTid, attachment, TransferState.ErrorDownload)

        assertThat(transport.downloadCalls).isEmpty()
        assertThat(result).isInstanceOf(SceytResponse.Error::class.java)
        assertThat(result?.message).isEqualTo("no destination")
    }

    @Test
    fun `resume resolves destination only once`() {
        var destinationCalls = 0
        SceytChatUIKit.fileTransfer.destinationProvider = FileTransferDestinationProvider { _, _ ->
            check(++destinationCalls == 1) { "destination resolved twice" }
            destinationFile
        }
        val attachment = attachment(state = TransferState.ErrorDownload)
        val task = transferTask(attachment)
        val results = mutableListOf<SceytResponse<String>>()
        task.downloadCallback = TransferResultCallback { results += it }

        service.resume(attachment.messageTid, attachment, TransferState.ErrorDownload)

        assertThat(destinationCalls).isEqualTo(1)
        assertThat(transport.downloadCalls).hasSize(1)
        assertThat(results).isEmpty()
        transport.downloadCalls.single().succeed(destinationFile.path)
        assertThat(results.single().data).isEqualTo(destinationFile.path)
    }

    @Test
    fun `resume does not overwrite an immediate transfer failure with downloading`() {
        SceytChatUIKit.fileTransfer.transport = object : FileTransferTransport by transport {
            override suspend fun download(request: FileDownloadRequest, callback: FileTransferCallback): FileTransferResult<String> {
                throw IllegalStateException("download failed")
            }
        }
        val attachment = attachment(state = TransferState.ErrorDownload)
        val task = transferTask(attachment)
        val states = mutableListOf<TransferState>()
        task.resumePauseCallback = ResumePauseCallback { states += it.state }
        task.downloadCallback = TransferResultCallback {
            assertThat(it).isInstanceOf(SceytResponse.Error::class.java)
            states += TransferState.ErrorDownload
        }

        service.resume(attachment.messageTid, attachment, TransferState.ErrorDownload)

        assertThat(states).containsExactly(TransferState.Downloading, TransferState.ErrorDownload).inOrder()
    }

    @Test
    fun `pause from resume callback stops download before transport starts`() {
        val attachment = attachment(state = TransferState.PauseDownload)
        val task = transferTask(attachment)
        task.resumePauseCallback = ResumePauseCallback {
            if (it.state == TransferState.Downloading) {
                service.pause(attachment.messageTid, attachment, TransferState.Downloading)
            }
        }

        service.resume(attachment.messageTid, attachment, TransferState.PauseDownload)

        assertThat(transport.downloadCalls).isEmpty()
        assertThat(task.state).isEqualTo(TransferState.PauseDownload)
    }

    @Test
    fun `existing complete destination skips transport`() {
        destinationFile.writeBytes(byteArrayOf(1, 2, 3, 4))
        val attachment = attachment(fileSize = 4L, state = TransferState.PendingDownload)
        val task = transferTask(attachment)
        var result: SceytResponse<String>? = null
        task.downloadCallback = TransferResultCallback { result = it }

        service.download(attachment)

        assertThat(transport.downloadCalls).isEmpty()
        assertThat(result).isInstanceOf(SceytResponse.Success::class.java)
        assertThat(result?.data).isEqualTo(destinationFile.path)
    }

    @Test
    fun `duplicate active download invokes transport once`() {
        val attachment = attachment(state = TransferState.PendingDownload)
        val task = transferTask(attachment)

        service.download(attachment)
        service.download(attachment)

        assertThat(transport.downloadCalls).hasSize(1)
        assertThat(transport.downloadCalls.single().request.operationId)
            .isEqualTo("download:10")
    }

    @Test
    fun `attachments from different messages download concurrently`() {
        val first = attachment(state = TransferState.PendingDownload)
        val second = first.copy(messageTid = 11L, name = "second.txt", url = "second-url")
        val firstTask = transferTask(first)
        val secondTask = transferTask(second)
        val firstResults = mutableListOf<String?>()
        val secondResults = mutableListOf<String?>()
        firstTask.downloadCallback = TransferResultCallback { firstResults += it.data }
        secondTask.downloadCallback = TransferResultCallback { secondResults += it.data }
        SceytChatUIKit.fileTransfer.destinationProvider = FileTransferDestinationProvider { _, value ->
            File(destinationFile.parentFile, value.name)
        }

        service.download(first)
        service.download(second)

        assertThat(transport.downloadCalls).hasSize(2)
        val firstCall = transport.downloadCalls.first()
        val secondCall = transport.downloadCalls.last()
        assertThat(firstCall.request.operationId).isNotEqualTo(secondCall.request.operationId)
        assertThat(firstCall.request.url).isEqualTo(first.url)
        assertThat(secondCall.request.url).isEqualTo(second.url)

        secondCall.succeed(secondCall.request.destinationFile.path)
        firstCall.succeed(firstCall.request.destinationFile.path)

        assertThat(firstResults).containsExactly(firstCall.request.destinationFile.path)
        assertThat(secondResults).containsExactly(secondCall.request.destinationFile.path)
    }

    @Test
    fun `different messages with unassigned ids and the same url have separate downloads`() {
        val first = attachment(state = TransferState.Downloading).copy(id = null)
        val second = first.copy(messageTid = 11L, id = 0L, name = "second.txt")
        SceytChatUIKit.fileTransfer.destinationProvider = FileTransferDestinationProvider { _, value ->
            File(destinationFile.parentFile, value.name)
        }

        service.download(first)
        service.download(second)

        assertThat(transport.downloadCalls).hasSize(2)
        val firstCall = transport.downloadCalls.first()
        val secondCall = transport.downloadCalls.last()
        assertThat(firstCall.request.operationId)
            .isEqualTo("download:10")
        assertThat(secondCall.request.operationId)
            .isEqualTo("download:11")

        service.pause(first.messageTid, first, TransferState.Downloading)

        assertThat(firstCall.cancelled).isTrue()
        assertThat(secondCall.cancelled).isFalse()
        secondCall.succeed(secondCall.request.destinationFile.path)
    }

    @Test
    fun `native pause and resume affect only the selected messages download`() {
        transport.pauseResult = true
        transport.resumeResult = true
        val first = attachment(state = TransferState.Downloading)
        val second = first.copy(messageTid = 11L, name = "second.txt", url = "second-url")
        val firstTask = transferTask(first)
        val secondTask = transferTask(second)
        val firstProgress = mutableListOf<Float>()
        val secondProgress = mutableListOf<Float>()
        firstTask.progressCallback = ProgressUpdateCallback { firstProgress += it.progressPercent }
        secondTask.progressCallback = ProgressUpdateCallback { secondProgress += it.progressPercent }

        SceytChatUIKit.fileTransfer.destinationProvider = FileTransferDestinationProvider { _, value ->
            File(destinationFile.parentFile, value.name)
        }

        service.download(first)
        service.download(second)
        val firstCall = transport.downloadCalls.first()
        val secondCall = transport.downloadCalls.last()

        service.pause(first.messageTid, first, TransferState.Downloading)
        firstCall.progress(30f)
        secondCall.progress(50f)

        assertThat(transport.pauseCalls).containsExactly(firstCall.request.operationId)
        assertThat(firstTask.state).isEqualTo(TransferState.PauseDownload)
        assertThat(secondTask.state).isEqualTo(TransferState.Downloading)
        assertThat(firstProgress).containsExactly(0f)
        assertThat(secondProgress).containsExactly(0f, 50f).inOrder()

        service.resume(first.messageTid, first, TransferState.PauseDownload)
        firstCall.progress(40f)

        assertThat(transport.resumeCalls).containsExactly(firstCall.request.operationId)
        assertThat(transport.downloadCalls).hasSize(2)
        assertThat(firstProgress).containsExactly(0f, 40f).inOrder()
        assertThat(secondCall.cancelled).isFalse()
        firstCall.succeed(firstCall.request.destinationFile.path)
        secondCall.succeed(secondCall.request.destinationFile.path)
    }

    @Test
    fun `progress and success are forwarded to transfer task`() {
        val attachment = attachment(state = TransferState.PendingDownload)
        val task = transferTask(attachment)
        val progress = mutableListOf<TransferData>()
        var result: SceytResponse<String>? = null
        task.progressCallback = ProgressUpdateCallback { progress += it }
        task.downloadCallback = TransferResultCallback { result = it }

        service.download(attachment)
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

        service.download(attachment)
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

        service.download(attachment)
        transport.downloadCalls.single().fail(SceytException(416, "Range not satisfiable"))

        assertThat(results.single().code).isEqualTo(416)
        assertThat(destinationFile.exists()).isFalse()
        assertThat(service.findTransferTask(attachment)).isNull()
        service.findOrCreateTransferTask(attachment).downloadCallback = TransferResultCallback { results += it }

        service.resume(attachment.messageTid, attachment, TransferState.ErrorDownload)

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

        service.download(attachment)
        destinationFile.writeBytes(byteArrayOf(1, 2))
        transport.downloadCalls.single().fail(CancellationException("transport cancelled"))

        assertThat(results).hasSize(1)
        assertThat(results.single()).isInstanceOf(SceytResponse.Error::class.java)
        assertThat(results.single().message).isEqualTo("transport cancelled")
        assertThat(destinationFile.exists()).isFalse()
        service.download(attachment) {
            downloadCallback = TransferResultCallback { results += it }
        }
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

        service.download(attachment)
        destinationFile.writeBytes(byteArrayOf(1, 2))
        testScope.advanceTimeBy(10.milliseconds)
        testScope.runCurrent()

        assertThat(results).hasSize(1)
        assertThat(results.single()).isInstanceOf(SceytResponse.Error::class.java)
        assertThat(transport.downloadCalls.first().cancelled).isTrue()
        assertThat(destinationFile.exists()).isFalse()
        service.download(attachment) {
            downloadCallback = TransferResultCallback { results += it }
        }
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

        service.download(attachment)
        destinationFile.writeBytes(partialBytes)
        transport.downloadCalls.single().succeed("")

        assertThat(destinationFile.exists()).isFalse()
        assertThat(result).isInstanceOf(SceytResponse.Error::class.java)
        assertThat(result?.message).isEqualTo("File download returned an empty local path")
    }

    @Test
    fun `interruption keeps partial destination without cancelling or retrying`() {
        val attachment = attachment(state = TransferState.PendingDownload)
        val task = transferTask(attachment)
        val partialBytes = byteArrayOf(1, 2)
        var result: SceytResponse<String>? = null
        task.downloadCallback = TransferResultCallback { result = it }

        service.download(attachment)
        destinationFile.writeBytes(partialBytes)
        val call = transport.downloadCalls.single()
        call.interrupt()

        assertThat(call.cancelled).isFalse()
        assertThat(destinationFile.readBytes()).isEqualTo(partialBytes)
        assertThat(result).isInstanceOf(SceytResponse.Error::class.java)
        assertThat(result?.message).isEqualTo("Network interrupted")
        assertThat(service.findTransferTask(attachment)).isNull()
        assertThat(transport.downloadCalls).hasSize(1)
    }

    @Test
    fun `interruption preserves partial destination and allows explicit retry`() {
        var interrupted = true
        SceytChatUIKit.fileTransfer.transport = object : FileTransferTransport by transport {
            override suspend fun download(request: FileDownloadRequest, callback: FileTransferCallback): FileTransferResult<String> {
                if (interrupted) {
                    interrupted = false
                    return FileTransferResult.Interrupted(java.io.IOException("No network connection"))
                }
                return transport.download(request, callback)
            }
        }
        val partialBytes = byteArrayOf(1, 2)
        destinationFile.writeBytes(partialBytes)
        val attachment = attachment(state = TransferState.PendingDownload)
        val results = mutableListOf<SceytResponse<String>>()
        val task = transferTask(attachment).apply {
            downloadCallback = TransferResultCallback { results += it }
        }

        service.download(attachment)

        assertThat(results).hasSize(1)
        assertThat(results.single()).isInstanceOf(SceytResponse.Error::class.java)
        assertThat(results.single().message).isEqualTo("No network connection")
        assertThat(destinationFile.readBytes()).isEqualTo(partialBytes)
        assertThat(service.findTransferTask(attachment)).isNull()
        service.findOrCreateTransferTask(attachment).downloadCallback = TransferResultCallback { results += it }

        service.resume(attachment.messageTid, attachment, TransferState.ErrorDownload)

        assertThat(transport.downloadCalls).hasSize(1)
        assertThat(destinationFile.readBytes()).isEqualTo(partialBytes)
        destinationFile.writeBytes(byteArrayOf(1, 2, 3, 4))
        transport.downloadCalls.single().succeed(destinationFile.path)
        assertThat(results).hasSize(2)
        assertThat(results.last().data).isEqualTo(destinationFile.path)
    }

    @Test
    fun `pause cancels download and resume starts a new call`() {
        val attachment = attachment(state = TransferState.Downloading)
        val task = transferTask(attachment)
        val states = mutableListOf<TransferState>()
        task.resumePauseCallback = ResumePauseCallback { states += it.state }

        service.download(attachment)
        service.pause(attachment.messageTid, attachment, TransferState.Downloading)
        service.resume(attachment.messageTid, attachment, TransferState.PauseDownload)

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

        service.download(attachment)
        val call = transport.downloadCalls.single()
        service.pause(attachment.messageTid, attachment, TransferState.Downloading)
        service.resume(attachment.messageTid, attachment, TransferState.PauseDownload)

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

        service.download(attachment)
        val call = transport.downloadCalls.single()
        service.pause(attachment.messageTid, attachment, TransferState.Downloading)
        call.progress(25f)

        assertThat(progress).containsExactly(0f)

        service.resume(attachment.messageTid, attachment, TransferState.PauseDownload)
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

        service.download(attachment)
        transport.downloadCalls.single().succeed(destinationFile.path)

        assertThat(callbackCount).isEqualTo(1)
    }

    @Test
    fun `pausing active download cancels its job and publishes paused state`() {
        val attachment = attachment(state = TransferState.PendingDownload)
        val task = transferTask(attachment)
        val states = mutableListOf<TransferState>()
        task.resumePauseCallback = ResumePauseCallback { states += it.state }

        service.download(attachment)
        service.pause(attachment.messageTid, attachment, TransferState.PendingDownload)

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

        service.download(attachment)
        service.pause((attachment.copy(url = "https://cdn.test/new.txt")).messageTid, attachment.copy(url = "https://cdn.test/new.txt"), TransferState.Downloading)

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

        service.download(attachment)
        val pausedCall = transport.downloadCalls.single()
        service.pause(attachment.messageTid, attachment, TransferState.Downloading)
        service.resume(attachment.messageTid, attachment, TransferState.PauseDownload)
        val replacementCall = transport.downloadCalls.last()

        pausedCall.progress(30f)
        pausedCall.succeed("late-path")

        assertThat(results).isEmpty()
        assertThat(states).containsExactly(
            TransferState.PauseDownload,
            TransferState.Downloading,
        ).inOrder()

        service.pause(attachment.messageTid, attachment, TransferState.Downloading)

        assertThat(replacementCall.cancelled).isTrue()
        assertThat(states).containsExactly(
            TransferState.PauseDownload,
            TransferState.Downloading,
            TransferState.PauseDownload,
        ).inOrder()
        assertThat(results).isEmpty()
    }

    @Test
    fun `resume waits for cancelled download before starting replacement`() {
        val attachment = attachment(state = TransferState.Downloading)
        val task = transferTask(attachment)
        val cancellationGate = CompletableDeferred<Unit>()
        transport.downloadCancellationGate = cancellationGate

        service.download(attachment)
        service.pause(attachment.messageTid, attachment, TransferState.Downloading)
        service.resume(attachment.messageTid, attachment, TransferState.PauseDownload)

        assertThat(transport.downloadCalls).hasSize(1)

        cancellationGate.complete(Unit)
        assertThat(transport.downloadCalls).hasSize(2)
        val replacement = transport.downloadCalls.last()
        service.download(attachment)

        assertThat(transport.downloadCalls).hasSize(2)
        assertThat(replacement.cancelled).isFalse()
    }

    @Test
    fun `repeated resume waits for original download cleanup`() {
        val attachment = attachment(state = TransferState.Downloading)
        val results = mutableListOf<SceytResponse<String>>()
        val task = transferTask(attachment).apply {
            downloadCallback = TransferResultCallback { results += it }
        }
        val cancellationGate = CompletableDeferred<Unit>()
        transport.downloadCancellationGate = cancellationGate
        service.download(attachment)
        destinationFile.writeBytes(byteArrayOf(1, 2))

        try {
            service.pause(attachment.messageTid, attachment, TransferState.Downloading)
            assertThat(transport.downloadCalls.single().cancelled).isTrue()
            service.resume(attachment.messageTid, attachment, TransferState.PauseDownload)
            service.resume(attachment.messageTid, attachment, TransferState.PauseDownload)

            assertThat(transport.downloadCalls).hasSize(1)
            service.pause(attachment.messageTid, attachment, TransferState.Downloading)
            service.resume(attachment.messageTid, attachment, TransferState.PauseDownload)
            assertThat(transport.downloadCalls).hasSize(1)
            assertThat(results).isEmpty()

            cancellationGate.complete(Unit)
            assertThat(transport.downloadCalls).hasSize(2)
            destinationFile.writeBytes(byteArrayOf(1, 2, 3, 4))
            transport.downloadCalls.last().succeed(destinationFile.path)

            assertThat(results.single().data).isEqualTo(destinationFile.path)
            assertThat(service.findTransferTask(attachment)).isNull()
        } finally {
            cancellationGate.complete(Unit)
        }
    }

    @Test
    fun `resume waits for original cleanup when a queued replacement never starts`() {
        testScope = TestScope(StandardTestDispatcher())
        service = FileTransferServiceImpl(context, mock(), AttachmentDownloadCoordinator(context, testScope), mock())
        val attachment = attachment(state = TransferState.Downloading)
        transferTask(attachment)
        val cancellationGate = CompletableDeferred<Unit>()
        transport.downloadCancellationGate = cancellationGate
        service.download(attachment)
        testScope.runCurrent()

        try {
            service.pause(attachment.messageTid, attachment, TransferState.Downloading)
            testScope.runCurrent()
            service.resume(attachment.messageTid, attachment, TransferState.PauseDownload)
            service.resume(attachment.messageTid, attachment, TransferState.PauseDownload)
            testScope.runCurrent()

            assertThat(transport.downloadCalls).hasSize(1)

            cancellationGate.complete(Unit)
            testScope.runCurrent()
            assertThat(transport.downloadCalls).hasSize(2)
        } finally {
            cancellationGate.complete(Unit)
            testScope.runCurrent()
        }
    }

    @Test
    fun `late error from cancelled download keeps replacement partial destination`() {
        val cancellationGate = CompletableDeferred<Unit>()
        SceytChatUIKit.fileTransfer.transport = object : FileTransferTransport by transport {
            override suspend fun download(request: FileDownloadRequest, callback: FileTransferCallback): FileTransferResult<String> {
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

        service.download(attachment)
        service.pause(attachment.messageTid, attachment, TransferState.Downloading)
        service.resume(attachment.messageTid, attachment, TransferState.PauseDownload)
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

        service.download(attachment)
        destinationFile.writeBytes(partialBytes)
        service.pause(attachment.messageTid, attachment, TransferState.Downloading)

        assertThat(transport.downloadCalls.single().cancelled).isTrue()
        assertThat(destinationFile.exists()).isTrue()
        assertThat(destinationFile.readBytes()).isEqualTo(partialBytes)
        assertThat(result).isNull()

        service.resume(attachment.messageTid, attachment, TransferState.PauseDownload)

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

        service.download(active)
        service.cancelAllTransfers()
        service.download(next)

        assertThat(transport.downloadCalls).hasSize(2)
        assertThat(transport.downloadCalls[0].cancelled).isTrue()
        assertThat(transport.downloadCalls[1].request.operationId)
            .isEqualTo("download:81")
        assertThat(results).isEmpty()
    }

    @Test
    fun `download uses the transport configured after coordinator creation`() {
        val replacement = RecordingFileTransferTransport()
        SceytChatUIKit.fileTransfer = SceytChatUIKitFileTransfer().apply {
            transport = replacement
            destinationProvider = FileTransferDestinationProvider { _, _ -> destinationFile }
        }
        val attachment = attachment(state = TransferState.PendingDownload)

        service.download(attachment)

        assertThat(transport.downloadCalls).isEmpty()
        assertThat(replacement.downloadCalls).hasSize(1)
    }

    @Test
    fun `pause from initial download progress stops it before transport starts`() {
        val first = attachment(state = TransferState.PendingDownload)
        val second = first.copy(messageTid = 11L, name = "second.txt", url = "second-url")
        val firstTask = transferTask(first)
        val secondTask = transferTask(second)
        SceytChatUIKit.fileTransfer.destinationProvider = FileTransferDestinationProvider { _, value ->
            File(destinationFile.parentFile, value.name)
        }
        var triggered = false
        firstTask.progressCallback = ProgressUpdateCallback {
            if (triggered || it.progressPercent != 50f) return@ProgressUpdateCallback
            triggered = true
            secondTask.progressCallback = ProgressUpdateCallback {
                service.pause(second.messageTid, second, TransferState.Downloading)
            }
            service.download(second)
        }
        service.download(first)

        transport.downloadCalls.single().progress(50f)

        assertThat(transport.downloadCalls).hasSize(1)
        assertThat(secondTask.state).isEqualTo(TransferState.PauseDownload)
    }

    @Test
    fun `pause during destination resolution is not overwritten by initial progress`() {
        val attachment = attachment(state = TransferState.PendingDownload)
        val states = mutableListOf<TransferState>()
        SceytChatUIKit.fileTransfer.destinationProvider = FileTransferDestinationProvider { _, _ ->
            service.pause(attachment.messageTid, attachment, TransferState.Downloading)
            destinationFile
        }

        val task = service.download(attachment) {
            progressCallback = ProgressUpdateCallback { states += it.state }
            resumePauseCallback = ResumePauseCallback { states += it.state }
        }

        assertThat(task.state).isEqualTo(TransferState.PauseDownload)
        assertThat(states).containsExactly(TransferState.PauseDownload)
        assertThat(transport.downloadCalls).isEmpty()
    }

    @Test
    fun `repeated pause republishes paused state without pausing backend twice`() {
        transport.pauseResult = true
        val attachment = attachment(state = TransferState.PendingDownload)
        val states = mutableListOf<TransferState>()
        val task = service.download(attachment) {
            resumePauseCallback = ResumePauseCallback { states += it.state }
        }

        repeat(2) { service.pause(attachment.messageTid, attachment, TransferState.Downloading) }

        assertThat(task.state).isEqualTo(TransferState.PauseDownload)
        assertThat(states).containsExactly(TransferState.PauseDownload, TransferState.PauseDownload)
        assertThat(transport.pauseCalls).hasSize(1)
    }

    @Test
    fun `assigning a server id reuses the running download and its pause resume operation`() {
        transport.pauseResult = true
        transport.resumeResult = true
        val pending = attachment(state = TransferState.PendingDownload).copy(id = null)
        val sent = pending.copy(id = 200L, url = "updated-url")
        val task = service.download(pending)
        val operationId = transport.downloadCalls.single().request.operationId

        assertThat(service.download(sent)).isSameInstanceAs(task)
        service.pause(sent.messageTid, sent, TransferState.Downloading)
        assertThat(task.state).isEqualTo(TransferState.PauseDownload)
        service.resume(sent.messageTid, sent, TransferState.PauseDownload)

        assertThat(transport.downloadCalls).hasSize(1)
        assertThat(transport.pauseCalls).containsExactly(operationId)
        assertThat(transport.resumeCalls).containsExactly(operationId)
        transport.downloadCalls.single().succeed(destinationFile.path)
        assertThat(service.getTasks()).isEmpty()
    }

    @Test
    fun `server id assigned during cancelled download cleanup does not allow overlapping writers`() {
        val cleanup = CompletableDeferred<Unit>()
        transport.downloadCancellationGate = cleanup
        val pending = attachment(state = TransferState.PendingDownload).copy(id = null)
        val sent = pending.copy(id = 200L, url = "updated-url")
        val task = service.download(pending)

        service.pause(sent.messageTid, sent, TransferState.Downloading)
        service.resume(sent.messageTid, sent, TransferState.PauseDownload)

        assertThat(transport.downloadCalls).hasSize(1)
        assertThat(service.findTransferTask(sent)).isSameInstanceAs(task)
        cleanup.complete(Unit)
        assertThat(transport.downloadCalls).hasSize(2)
        assertThat(transport.downloadCalls.last().request.operationId)
            .isEqualTo(transport.downloadCalls.first().request.operationId)
        transport.pauseResult = true
        transport.resumeResult = true
        service.pause(pending.messageTid, pending, TransferState.Downloading)
        assertThat(task.state).isEqualTo(TransferState.PauseDownload)
        service.resume(sent.messageTid, sent, TransferState.PauseDownload)
        assertThat(transport.downloadCalls).hasSize(2)
        transport.downloadCalls.last().succeed(destinationFile.path)
        assertThat(service.getTasks()).isEmpty()
    }

    @Test
    fun `progress already being delivered cannot undo a pause`() {
        val attachment = attachment(state = TransferState.PendingDownload)
        val task = service.download(attachment) {
            val defaultProgress = progressCallback
            progressCallback = ProgressUpdateCallback { data ->
                if (data.progressPercent == 50f) {
                    service.pause(attachment.messageTid, attachment, TransferState.Downloading)
                }
                defaultProgress?.onProgress(data)
            }
        }

        transport.downloadCalls.single().progress(50f)

        assertThat(task.state).isEqualTo(TransferState.PauseDownload)
        assertThat(task.attachment.transferState).isEqualTo(TransferState.PauseDownload)
    }

    @Test
    fun `explicit cancellation suppresses a delayed interruption result`() {
        for (cancelAll in listOf(false, true)) {
            val attachment = attachment(
                messageTid = if (cancelAll) 91L else 90L,
                state = TransferState.PendingDownload,
            )
            val cancellationGate = CompletableDeferred<Unit>()
            SceytChatUIKit.fileTransfer.transport = object : FileTransferTransport by transport {
                override suspend fun download(request: FileDownloadRequest, callback: FileTransferCallback): FileTransferResult<String> {
                    try {
                        return transport.download(request, callback)
                    } finally {
                        withContext(NonCancellable) { cancellationGate.await() }
                    }
                }
            }
            val results = mutableListOf<SceytResponse<String>>()
            service.download(attachment) {
                downloadCallback = TransferResultCallback { results += it }
            }
            destinationFile.writeBytes(byteArrayOf(1, 2))
            transport.downloadCalls.last().interrupt()

            if (cancelAll) service.cancelAllTransfers() else service.cancelUpload(attachment)
            cancellationGate.complete(Unit)

            assertThat(results).isEmpty()
            assertThat(service.findTransferTask(attachment)).isNull()
            assertThat(destinationFile.readBytes()).isEqualTo(byteArrayOf(1, 2))
        }
    }

    @Test
    fun `interruption while natively paused preserves partial file and finishes without retrying`() {
        transport.pauseResult = true
        val attachment = attachment(state = TransferState.PendingDownload)
        var completion: Result<SceytAttachment>? = null
        val task = service.download(attachment) {
            addOnCompletionListener("download") { completion = it }
        }
        destinationFile.writeText("partial")
        service.pause(attachment.messageTid, attachment, TransferState.Downloading)
        val call = transport.downloadCalls.single()
        assertThat(call.cancelled).isFalse()

        call.interrupt()

        assertThat(task.state).isEqualTo(TransferState.ErrorDownload)
        assertThat(completion?.isFailure).isTrue()
        assertThat(destinationFile.readText()).isEqualTo("partial")
        assertThat(transport.downloadCalls).hasSize(1)
        assertThat(service.getTasks()).isEmpty()
    }

    @Test
    fun `throwing pause callback does not escape or prevent resume`() {
        val attachment = attachment(state = TransferState.PendingDownload)
        val task = service.download(attachment) {
            resumePauseCallback = ResumePauseCallback { error("callback failed") }
        }

        service.pause(attachment.messageTid, attachment, TransferState.Downloading)

        assertThat(task.state).isEqualTo(TransferState.PauseDownload)
        assertThat(transport.downloadCalls.single().cancelled).isTrue()
        task.resumePauseCallback = null
        service.resume(attachment.messageTid, attachment, TransferState.PauseDownload)
        assertThat(transport.downloadCalls).hasSize(2)
        transport.downloadCalls.last().succeed(destinationFile.path)
    }

    @Test
    fun `pause from progress during native resume is not overwritten`() {
        transport.pauseResult = true
        val attachment = attachment(state = TransferState.Downloading)
        val task = transferTask(attachment)
        val states = mutableListOf<TransferState>()
        task.resumePauseCallback = ResumePauseCallback { states += it.state }
        task.progressCallback = ProgressUpdateCallback {
            if (it.progressPercent == 50f) {
                service.pause(attachment.messageTid, attachment, TransferState.Downloading)
            }
        }
        SceytChatUIKit.fileTransfer.transport = object : FileTransferTransport by transport {
            override fun resume(operationId: String): Boolean {
                transport.downloadCalls.single().progress(50f)
                return true
            }
        }
        service.download(attachment)
        service.pause(attachment.messageTid, attachment, TransferState.Downloading)

        service.resume(attachment.messageTid, attachment, TransferState.PauseDownload)

        assertThat(task.state).isEqualTo(TransferState.PauseDownload)
        assertThat(states).containsExactly(TransferState.PauseDownload, TransferState.PauseDownload).inOrder()
        assertThat(transport.downloadCalls).hasSize(1)
        assertThat(transport.downloadCalls.single().cancelled).isFalse()
    }

    @Test
    fun `pause after a terminal result does not revive the task`() {
        val attachment = attachment(state = TransferState.PendingDownload)
        val results = mutableListOf<SceytResponse<String>>()
        service.download(attachment) {
            downloadCallback = TransferResultCallback { results += it }
        }
        transport.downloadCalls.single().succeed(destinationFile.path)

        service.pause(attachment.messageTid, attachment, TransferState.Downloading)

        assertThat(results).hasSize(1)
        assertThat(service.findTransferTask(attachment)).isNull()
        assertThat(transport.pauseCalls).isEmpty()
    }

    @Test
    fun `delayed resume callback cannot overwrite a newer pause`() {
        for (native in listOf(false, true)) {
            transport.pauseResult = native
            transport.resumeResult = native
            val attachment = attachment(
                messageTid = if (native) 9_002L else 9_001L,
                state = TransferState.PendingDownload,
            )
            var pauseOnResume = false
            val task = service.download(attachment) {
                val defaultCallback = resumePauseCallback
                resumePauseCallback = ResumePauseCallback { data ->
                    if (pauseOnResume && data.state == TransferState.Downloading) {
                        pauseOnResume = false
                        service.pause(attachment.messageTid, attachment, TransferState.Downloading)
                    }
                    defaultCallback?.onResumePause(data)
                }
            }
            service.pause(attachment.messageTid, attachment, TransferState.Downloading)
            pauseOnResume = true

            service.resume(attachment.messageTid, attachment, TransferState.PauseDownload)

            assertThat(task.state).isEqualTo(TransferState.PauseDownload)
            assertThat(task.attachment.transferState).isEqualTo(TransferState.PauseDownload)
            assertThat(AttachmentTransferStateStore.getTransferData(attachment)?.state)
                .isEqualTo(TransferState.PauseDownload)
            service.cancelUpload(attachment)
        }
    }

    @Test
    fun `delayed pause callback cannot overwrite a newer resume`() {
        transport.pauseResult = true
        transport.resumeResult = true
        val attachment = attachment(messageTid = 9_003L, state = TransferState.PendingDownload)
        var resumeOnPause = true
        val task = service.download(attachment) {
            val defaultCallback = resumePauseCallback
            resumePauseCallback = ResumePauseCallback { data ->
                if (resumeOnPause && data.state == TransferState.PauseDownload) {
                    resumeOnPause = false
                    service.resume(attachment.messageTid, attachment, TransferState.PauseDownload)
                }
                defaultCallback?.onResumePause(data)
            }
        }

        service.pause(attachment.messageTid, attachment, TransferState.Downloading)

        assertThat(task.state).isEqualTo(TransferState.Downloading)
        assertThat(task.attachment.transferState).isEqualTo(TransferState.Downloading)
        assertThat(AttachmentTransferStateStore.getTransferData(attachment)?.state)
            .isEqualTo(TransferState.Downloading)
        assertThat(transport.downloadCalls).hasSize(1)
    }

    @Test
    fun `duplicate during result delivery joins the finishing task and explicit retry succeeds`() {
        val attachment = attachment(messageTid = 9_010L, state = TransferState.PendingDownload)
        var originalCompletions = 0
        var duplicateCompletions = 0
        val task = service.download(attachment) {
            addOnCompletionListener("original") { originalCompletions++ }
        }
        var duplicate: TransferTask? = null
        val failure = object : IllegalStateException() {
            override val message: String
                get() {
                    if (duplicate == null) {
                        duplicate = service.download(attachment) {
                            addOnCompletionListener("duplicate") { duplicateCompletions++ }
                        }
                    }
                    return "download failed"
                }
        }

        transport.downloadCalls.single().fail(failure)

        assertThat(duplicate).isSameInstanceAs(task)
        assertThat(transport.downloadCalls).hasSize(1)
        assertThat(task.state).isEqualTo(TransferState.ErrorDownload)
        assertThat(originalCompletions).isEqualTo(1)
        assertThat(duplicateCompletions).isEqualTo(1)
        assertThat(service.findTransferTask(attachment)).isNull()

        service.resume(attachment.messageTid, attachment, TransferState.ErrorDownload)
        val retry = checkNotNull(service.findTransferTask(attachment))
        assertThat(retry).isNotSameInstanceAs(task)
        transport.downloadCalls.last().succeed(destinationFile.path)

        assertThat(transport.downloadCalls).hasSize(2)
        assertThat(retry.state).isEqualTo(TransferState.Downloaded)
        assertThat(service.getTasks()).isEmpty()
    }

    private fun transferTask(attachment: SceytAttachment): TransferTask =
        service.findOrCreateTransferTask(attachment).apply {
            progressCallback = null
            downloadCallback = null
            resumePauseCallback = null
        }
}
