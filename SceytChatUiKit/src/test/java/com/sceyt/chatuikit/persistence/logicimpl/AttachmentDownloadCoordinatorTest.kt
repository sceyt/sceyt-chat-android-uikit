package com.sceyt.chatuikit.persistence.logicimpl

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.filetransfer.FileTransferDestinationProvider
import com.sceyt.chatuikit.filetransfer.SceytChatUIKitFileTransfer
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.ProgressUpdateCallback
import com.sceyt.chatuikit.persistence.file_transfer.ResumePauseCallback
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferResultCallback
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
    fun `failure deletes partial destination and forwards error`() {
        val attachment = attachment(state = TransferState.PendingDownload)
        val task = transferTask(attachment)
        var result: SceytResponse<String>? = null
        task.downloadCallback = TransferResultCallback { result = it }

        coordinator.downloadFile(attachment, task)
        destinationFile.writeBytes(byteArrayOf(1, 2))
        transport.downloadCalls.single().fail(SceytException(7, "failed"))

        assertThat(destinationFile.exists()).isFalse()
        assertThat(result).isInstanceOf(SceytResponse.Error::class.java)
        assertThat(result?.code).isEqualTo(7)
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
        transport.downloadCalls.single().waitingForNetwork()
        transport.downloadCalls.single().fail(IllegalStateException("No network connection"))

        assertThat(destinationFile.readBytes()).isEqualTo(partialBytes)
        assertThat(result).isInstanceOf(SceytResponse.Error::class.java)
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
        assertThat(task.state).isEqualTo(TransferState.PauseDownload)
        assertThat(states).containsExactly(
            TransferState.PauseDownload,
            TransferState.Downloading,
        ).inOrder()
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

        coordinator.downloadFile(active, transferTask(active))
        coordinator.cancelAll()
        coordinator.downloadFile(next, transferTask(next))

        assertThat(transport.downloadCalls).hasSize(2)
        assertThat(transport.downloadCalls[0].cancelled).isTrue()
        assertThat(transport.downloadCalls[1].request.operationId).isEqualTo("download:81")
    }
}
