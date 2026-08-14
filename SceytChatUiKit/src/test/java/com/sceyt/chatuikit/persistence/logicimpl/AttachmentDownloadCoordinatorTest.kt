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
class AttachmentDownloadCoordinatorTest {
    private lateinit var context: Context
    private lateinit var destinationFile: File
    private lateinit var service: TestFileTransferService
    private lateinit var transport: RecordingFileTransferTransport
    private lateinit var coordinator: AttachmentDownloadCoordinator
    private lateinit var previousFileTransfer: SceytChatUIKitFileTransfer

    @Before
    fun setUp() {
        stopKoin()
        context = RuntimeEnvironment.getApplication()
        destinationFile = File(context.cacheDir, "transfer-tests/download.bin")
        destinationFile.parentFile?.mkdirs()
        destinationFile.delete()
        service = TestFileTransferService()
        transport = RecordingFileTransferTransport()
        previousFileTransfer = SceytChatUIKit.fileTransfer
        SceytChatUIKit.fileTransfer = SceytChatUIKitFileTransfer().apply {
            this.transport = this@AttachmentDownloadCoordinatorTest.transport
            destinationProvider = FileTransferDestinationProvider { _, _ -> destinationFile }
        }
        SceytKoinApp.koinApp = startKoin {
            modules(module { single<FileTransferService> { service } })
        }
        coordinator = AttachmentDownloadCoordinator(context)
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
        transport.downloadCalls.single().callback.onProgress(42f)
        transport.downloadCalls.single().callback.onSuccess(destinationFile.path)

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
        transport.downloadCalls.single().callback.onFailure(SceytException(7, "failed"))

        assertThat(destinationFile.exists()).isFalse()
        assertThat(result).isInstanceOf(SceytResponse.Error::class.java)
        assertThat(result?.code).isEqualTo(7)
    }

    @Test
    fun `pause and unsupported resume preserve existing restart behavior`() {
        val attachment = attachment(state = TransferState.Downloading)
        val task = transferTask(attachment)
        val states = mutableListOf<TransferState>()
        task.resumePauseCallback = ResumePauseCallback { states += it.state }
        service.addTransferTask(task)

        coordinator.downloadFile(attachment, task)
        coordinator.pauseLoad(attachment, TransferState.Downloading)
        coordinator.resumeLoad(attachment, TransferState.PauseDownload)

        assertThat(transport.pausedOperationIds).containsExactly("download:10")
        assertThat(transport.resumedOperationIds).containsExactly("download:10")
        assertThat(transport.downloadCalls).hasSize(2)
        assertThat(task.state).isEqualTo(TransferState.PauseUpload)
        assertThat(states).containsExactly(
            TransferState.PauseDownload,
            TransferState.Downloading,
        ).inOrder()
    }
}
