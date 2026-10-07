package com.sceyt.chatuikit.persistence.workers

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.work.Data
import androidx.work.WorkInfo
import androidx.work.WorkerParameters
import androidx.work.impl.WorkManagerImpl
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.Futures
import com.sceyt.chat.models.message.Message
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.config.SceytChatUIKitConfig
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.filetransfer.SceytChatUIKitFileTransfer
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.notifications.SceytNotifications
import com.sceyt.chatuikit.persistence.database.dao.FileChecksumDao
import com.sceyt.chatuikit.persistence.di.CoroutineContextType
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferServiceImpl
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceChannelsLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceMessagesLogic
import com.sceyt.chatuikit.persistence.logicimpl.FileTransferLogicImpl
import com.sceyt.chatuikit.persistence.logicimpl.RecordingFileTransferTransport
import com.sceyt.chatuikit.persistence.logicimpl.attachment
import com.sceyt.chatuikit.persistence.workers.UploadAndSendAttachmentWorkManager.IS_SHARING
import com.sceyt.chatuikit.persistence.workers.UploadAndSendAttachmentWorkManager.MESSAGE_TID
import com.sceyt.chatuikit.persistence.workers.UploadAndSendAttachmentWorkManager.RESUME_PAUSED_UPLOAD
import com.sceyt.chatuikit.providers.ChatConnectionProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import kotlin.coroutines.CoroutineContext

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class UploadAndSendAttachmentWorkerTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val context: Context = RuntimeEnvironment.getApplication()
    private val attachmentLogic = mock<PersistenceAttachmentLogic>()
    private val messageLogic = mock<PersistenceMessagesLogic>()
    private val workManager = mock<WorkManagerImpl>()
    private val transport = RecordingFileTransferTransport()
    private val testScope = TestScope(UnconfinedTestDispatcher())
    private val logic = FileTransferLogicImpl(context, attachmentLogic, mock(), testScope.coroutineContext)
    private val service = FileTransferServiceImpl(context, logic)
    private lateinit var previousFileTransfer: SceytChatUIKitFileTransfer
    private lateinit var previousConfig: SceytChatUIKitConfig
    private var previousConnectionProvider: ChatConnectionProvider? = null

    @Before
    fun setUp() = runBlocking {
        previousFileTransfer = SceytChatUIKit.fileTransfer
        previousConfig = SceytChatUIKit.config
        previousConnectionProvider = SceytChatUIKit.chatConnectionProvider
        SceytChatUIKit.fileTransfer = SceytChatUIKitFileTransfer().apply {
            transport = this@UploadAndSendAttachmentWorkerTest.transport
        }
        SceytChatUIKit.config = SceytChatUIKitConfig().apply {
            preventDuplicateAttachmentUpload = false
        }
        SceytChatUIKit.chatConnectionProvider = ChatConnectionProvider { Result.success(Unit) }
        SceytChatUIKit.notifications = SceytNotifications(context).apply {
            fileTransferServiceNotification.notificationHandler = mock()
        }
        WorkManagerImpl.setDelegate(workManager)
        val runningWork = mock<WorkInfo>()
        whenever(runningWork.state).thenReturn(WorkInfo.State.RUNNING)
        whenever(workManager.getWorkInfosByTag(any()))
            .thenReturn(Futures.immediateFuture(listOf(runningWork)))
        whenever(attachmentLogic.getAllPayLoadsByMsgTid(any())).thenReturn(emptyList())
        whenever(messageLogic.sendMessageWithUploadedAttachments(any(), any()))
            .thenReturn(SceytResponse.Success(null))
        stopKoin()
        SceytKoinApp.koinApp = startKoin {
            modules(module {
                single<FileTransferService> { service }
                single<PersistenceAttachmentLogic> { attachmentLogic }
                single<PersistenceMessagesLogic> { messageLogic }
                single<PersistenceChannelsLogic> { mock() }
                single<FileChecksumDao> { mock() }
                single<CoroutineContext>(named(CoroutineContextType.SingleThreaded)) {
                    testScope.coroutineContext
                }
            })
        }
    }

    @After
    fun tearDown() {
        service.cancelAllTransfers()
        testScope.cancel()
        WorkManagerImpl.setDelegate(null)
        SceytChatUIKit.fileTransfer = previousFileTransfer
        SceytChatUIKit.config = previousConfig
        SceytChatUIKit.chatConnectionProvider = previousConnectionProvider
        SceytKoinApp.koinApp = null
        stopKoin()
    }

    @Test
    fun `unrelated worker run does not restart paused upload`() {
        assertThat(
            shouldStartUpload(
                state = TransferState.PauseUpload,
                resumePausedUpload = false,
            ),
        ).isFalse()
    }

    @Test
    fun `explicit worker resume restarts paused upload`() {
        assertThat(
            shouldStartUpload(
                state = TransferState.PauseUpload,
                resumePausedUpload = true,
            ),
        ).isTrue()
    }

    @Test
    fun `normal upload does not require explicit resume`() {
        assertThat(
            shouldStartUpload(
                state = TransferState.PendingUpload,
                resumePausedUpload = false,
            ),
        ).isTrue()
    }

    @Test
    fun `explicit worker resume clears pause and sends uploaded message`() {
        val attachment = localAttachment()
        service.upload(attachment)
        service.pause(attachment.messageTid, attachment, TransferState.Uploading)

        val worker = worker(attachment.copy(transferState = TransferState.PauseUpload), resume = true)
        val work = testScope.async { worker.doWork() }

        assertThat(transport.uploadCalls).hasSize(2)
        assertThat(transport.uploadCalls.first().cancelled).isTrue()
        transport.uploadCalls.last().succeed("resumed-url")

        assertThat(work.isCompleted).isTrue()
        assertSentUrl(attachment, "resumed-url")
    }

    @Test
    fun `explicit worker resume uses native resume instead of uploading twice`() {
        transport.pauseResult = true
        transport.resumeResult = true
        val attachment = localAttachment()
        service.upload(attachment)
        val call = transport.uploadCalls.single()
        service.pause(attachment.messageTid, attachment, TransferState.Uploading)

        val worker = worker(attachment.copy(transferState = TransferState.PauseUpload), resume = true)
        val work = testScope.async { worker.doWork() }

        assertThat(transport.resumeCalls).containsExactly("upload:${attachment.messageTid}")
        assertThat(transport.uploadCalls).hasSize(1)
        assertThat(call.cancelled).isFalse()
        call.succeed("native-resumed-url")

        assertThat(work.isCompleted).isTrue()
        assertSentUrl(attachment, "native-resumed-url")
    }

    @Test
    fun `explicit worker resume reuses completed shared upload`() {
        val first = localAttachment()
        val paused = first.copy(messageTid = 11L)
        service.uploadSharedFile(first)
        service.uploadSharedFile(paused)
        service.pause(paused.messageTid, paused, TransferState.Uploading)
        transport.uploadCalls.single().succeed("shared-url")

        val worker = worker(paused.copy(transferState = TransferState.PauseUpload), resume = true, shared = true)
        val work = testScope.async { worker.doWork() }

        assertThat(transport.uploadCalls).hasSize(1)
        assertThat(work.isCompleted).isTrue()
        assertSentUrl(paused, "shared-url")
    }

    @Test
    fun `automatic worker run leaves user paused upload stopped`() {
        val attachment = localAttachment()
        service.upload(attachment)
        service.pause(attachment.messageTid, attachment, TransferState.Uploading)

        val worker = worker(attachment.copy(transferState = TransferState.PauseUpload))
        val work = testScope.async { worker.doWork() }

        assertThat(work.isCompleted).isTrue()
        assertThat(transport.uploadCalls).hasSize(1)
        assertThat(transport.resumeCalls).isEmpty()
        runBlocking {
            verify(messageLogic, never()).sendMessageWithUploadedAttachments(any(), any())
        }
    }

    @Test
    fun `explicit resume after process restart starts a fresh shared upload`() {
        val attachment = localAttachment().copy(transferState = TransferState.PauseUpload)
        val worker = worker(attachment, resume = true, shared = true)
        val work = testScope.async { worker.doWork() }

        assertThat(transport.uploadCalls).hasSize(1)
        assertThat(transport.uploadCalls.single().request.isSharedUpload).isTrue()
        transport.uploadCalls.single().succeed("fresh-url")

        assertThat(work.isCompleted).isTrue()
        assertSentUrl(attachment, "fresh-url")
    }

    private fun localAttachment(): SceytAttachment {
        val file = File(context.cacheDir, "worker-upload.txt").apply { writeText("test") }
        return attachment(filePath = file.path, url = null)
    }

    private fun worker(
        attachment: SceytAttachment,
        resume: Boolean = false,
        shared: Boolean = false,
    ): UploadAndSendAttachmentWorker {
        val message = createMessage(createdAt = 1L, tid = attachment.messageTid).copy(
            deliveryStatus = MessageDeliveryStatus.Pending,
            attachments = listOf(attachment),
        )
        runBlocking {
            whenever(messageLogic.getMessageFromDbByTid(attachment.messageTid)).thenReturn(message)
        }
        val params = mock<WorkerParameters>()
        whenever(params.inputData).thenReturn(
            Data.Builder()
                .putLong(MESSAGE_TID, attachment.messageTid)
                .putBoolean(RESUME_PAUSED_UPLOAD, resume)
                .putBoolean(IS_SHARING, shared)
                .build(),
        )
        return UploadAndSendAttachmentWorker(context, params)
    }

    private fun assertSentUrl(attachment: SceytAttachment, url: String) = runBlocking {
        val message = argumentCaptor<Message>()
        verify(messageLogic).sendMessageWithUploadedAttachments(eq(0L), message.capture())
        assertThat(message.firstValue.tid).isEqualTo(attachment.messageTid)
        assertThat(message.firstValue.attachments.single().url).isEqualTo(url)
    }
}
