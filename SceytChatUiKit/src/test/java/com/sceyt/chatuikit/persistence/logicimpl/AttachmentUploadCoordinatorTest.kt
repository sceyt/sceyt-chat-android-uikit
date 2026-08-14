package com.sceyt.chatuikit.persistence.logicimpl

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.config.SceytChatUIKitConfig
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.FileChecksumData
import com.sceyt.chatuikit.filetransfer.SceytChatUIKitFileTransfer
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.ProgressUpdateCallback
import com.sceyt.chatuikit.persistence.file_transfer.ResumePauseCallback
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferResultCallback
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AttachmentUploadCoordinatorTest {
    private lateinit var context: Context
    private lateinit var service: TestFileTransferService
    private lateinit var transport: RecordingFileTransferTransport
    private lateinit var attachmentLogic: PersistenceAttachmentLogic
    private lateinit var coordinator: AttachmentUploadCoordinator
    private lateinit var previousFileTransfer: SceytChatUIKitFileTransfer
    private lateinit var previousConfig: SceytChatUIKitConfig

    @Before
    fun setUp() {
        stopKoin()
        context = RuntimeEnvironment.getApplication()
        service = TestFileTransferService()
        transport = RecordingFileTransferTransport()
        attachmentLogic = mock()
        previousFileTransfer = SceytChatUIKit.fileTransfer
        previousConfig = SceytChatUIKit.config
        SceytChatUIKit.fileTransfer = SceytChatUIKitFileTransfer().apply {
            this.transport = this@AttachmentUploadCoordinatorTest.transport
        }
        SceytChatUIKit.config = SceytChatUIKitConfig().apply {
            preventDuplicateAttachmentUpload = false
        }
        SceytKoinApp.koinApp = startKoin {
            modules(module { single<FileTransferService> { service } })
        }
        coordinator = AttachmentUploadCoordinator(context, attachmentLogic)
    }

    @After
    fun tearDown() {
        SceytChatUIKit.fileTransfer = previousFileTransfer
        SceytChatUIKit.config = previousConfig
        SceytKoinApp.koinApp = null
        stopKoin()
    }

    @Test
    fun `regular uploads run sequentially`() {
        val first = attachment(messageTid = 1L, filePath = "/tmp/first.txt")
        val second = attachment(messageTid = 2L, filePath = "/tmp/second.txt")

        coordinator.uploadFile(first, transferTask(first))
        coordinator.uploadFile(second, transferTask(second))

        assertThat(transport.uploadCalls).hasSize(1)
        assertThat(transport.uploadCalls.single().request.operationId).isEqualTo("upload:1")

        transport.uploadCalls.first().callback.onSuccess("first-url")

        assertThat(transport.uploadCalls).hasSize(2)
        assertThat(transport.uploadCalls[1].request.operationId).isEqualTo("upload:2")
    }

    @Test
    fun `duplicate queued upload is suppressed`() {
        val current = attachment(messageTid = 1L)
        val duplicate = attachment(messageTid = 2L)

        coordinator.uploadFile(current, transferTask(current))
        coordinator.uploadFile(duplicate, transferTask(duplicate))
        coordinator.uploadFile(duplicate, transferTask(duplicate))
        transport.uploadCalls.first().callback.onSuccess("current-url")

        assertThat(transport.uploadCalls.map { it.request.operationId })
            .containsExactly("upload:1", "upload:2").inOrder()
    }

    @Test
    fun `transport receives prepared request and forwards callbacks`() {
        val attachment = attachment(
            messageTid = 30L,
            name = "document.txt",
            filePath = "/tmp/document.txt",
        )
        val task = transferTask(attachment)
        val progress = mutableListOf<TransferData>()
        var result: SceytResponse<String>? = null
        task.progressCallback = ProgressUpdateCallback { progress += it }
        task.uploadResultCallback = TransferResultCallback { result = it }

        coordinator.uploadFile(attachment, task)
        val call = transport.uploadCalls.single()
        call.callback.onProgress(55f)
        call.callback.onSuccess("uploaded-url")

        assertThat(call.request.operationId).isEqualTo("upload:30")
        assertThat(call.request.sourceFile.path).isEqualTo("/tmp/document.txt")
        assertThat(call.request.fileName).isEqualTo("document.txt")
        assertThat(call.request.attachment).isEqualTo(attachment)
        assertThat(progress.single().progressPercent).isEqualTo(55f)
        assertThat(progress.single().state).isEqualTo(TransferState.Uploading)
        assertThat(result?.data).isEqualTo("uploaded-url")
    }

    @Test
    fun `existing checksum url skips transport`() = runBlocking {
        SceytChatUIKit.config.preventDuplicateAttachmentUpload = true
        val attachment = attachment(filePath = "/tmp/reused.txt")
        val task = transferTask(attachment)
        var result: SceytResponse<String>? = null
        task.uploadResultCallback = TransferResultCallback { result = it }
        whenever(attachmentLogic.getFileChecksumData("/tmp/reused.txt")).thenReturn(
            FileChecksumData(
                checksum = 1L,
                resizedFilePath = null,
                url = "reused-url",
                metadata = null,
                fileSize = 4L,
            ),
        )

        coordinator.uploadFile(attachment, task)

        assertThat(transport.uploadCalls).isEmpty()
        assertThat(result?.data).isEqualTo("reused-url")
    }

    @Test
    fun `shared attachments use one physical upload and fan out callbacks`() {
        val first = attachment(messageTid = 1L, filePath = "/tmp/shared.txt")
        val second = attachment(messageTid = 2L, filePath = "/tmp/shared.txt")
        val firstTask = transferTask(first)
        val secondTask = transferTask(second)
        val firstProgress = mutableListOf<Float>()
        val secondProgress = mutableListOf<Float>()
        val firstResults = mutableListOf<String?>()
        val secondResults = mutableListOf<String?>()
        firstTask.progressCallback = ProgressUpdateCallback { firstProgress += it.progressPercent }
        secondTask.progressCallback = ProgressUpdateCallback { secondProgress += it.progressPercent }
        firstTask.uploadResultCallback = TransferResultCallback { firstResults += it.data }
        secondTask.uploadResultCallback = TransferResultCallback { secondResults += it.data }

        coordinator.uploadSharedFile(first, firstTask)
        coordinator.uploadSharedFile(second, secondTask)
        val call = transport.uploadCalls.single()
        call.callback.onProgress(60f)
        call.callback.onSuccess("shared-url")

        assertThat(transport.uploadCalls).hasSize(1)
        assertThat(firstProgress).containsExactly(60f)
        assertThat(secondProgress).containsExactly(60f)
        assertThat(firstResults).containsExactly("shared-url")
        assertThat(secondResults).containsExactly("shared-url")
    }

    @Test
    fun `pause and unsupported resume preserve existing restart behavior`() {
        val attachment = attachment(messageTid = 40L, state = TransferState.Uploading)
        val task = transferTask(attachment)
        val states = mutableListOf<TransferState>()
        task.resumePauseCallback = ResumePauseCallback { states += it.state }
        service.addTransferTask(task)

        coordinator.uploadFile(attachment, task)
        coordinator.pauseLoad(attachment, TransferState.Uploading)
        coordinator.resumeLoad(attachment, TransferState.PauseUpload)

        assertThat(transport.pausedOperationIds).containsExactly("upload:40")
        assertThat(transport.resumedOperationIds).contains("upload:40")
        assertThat(transport.uploadCalls).hasSize(2)
        assertThat(states).containsExactly(
            TransferState.PauseUpload,
            TransferState.WaitingToUpload,
        ).inOrder()
    }
}
