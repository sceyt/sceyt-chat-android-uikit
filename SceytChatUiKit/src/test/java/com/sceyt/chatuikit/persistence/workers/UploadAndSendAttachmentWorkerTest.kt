package com.sceyt.chatuikit.persistence.workers

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.work.Data
import androidx.work.Operation
import androidx.work.WorkInfo
import androidx.work.WorkerParameters
import androidx.work.impl.WorkManagerImpl
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.SettableFuture
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.config.SceytChatUIKitConfig
import com.sceyt.chatuikit.config.ChannelListConfig
import com.sceyt.chatuikit.createChannel
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.managers.channel.event.ChannelActionEvent
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.SyncResult
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.filetransfer.AttachmentTransferContext
import com.sceyt.chatuikit.filetransfer.FileTransferCallback
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import com.sceyt.chatuikit.filetransfer.FileTransferTransport
import com.sceyt.chatuikit.filetransfer.FileUploadRequest
import com.sceyt.chatuikit.filetransfer.SceytChatUIKitFileTransfer
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.notifications.SceytNotifications
import com.sceyt.chatuikit.persistence.database.dao.FileChecksumDao
import com.sceyt.chatuikit.persistence.database.dao.ChannelDao
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.di.CoroutineContextType
import com.sceyt.chatuikit.persistence.file_transfer.AttachmentTransferStateStore
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferHelper
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferServiceImpl
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceChannelsLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceMessagesLogic
import com.sceyt.chatuikit.persistence.logicimpl.AttachmentDownloadCoordinator
import com.sceyt.chatuikit.persistence.logicimpl.AttachmentUploadCoordinator
import com.sceyt.chatuikit.persistence.logicimpl.RecordingFileTransferTransport
import com.sceyt.chatuikit.persistence.logicimpl.attachment
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelsCache
import com.sceyt.chatuikit.persistence.logicimpl.channel.PersistenceChannelsLogicImpl
import com.sceyt.chatuikit.persistence.mappers.toMessageDb
import com.sceyt.chatuikit.persistence.repositories.ChannelsRepository
import com.sceyt.chatuikit.persistence.workers.UploadAndSendAttachmentWorkManager.IS_SHARING
import com.sceyt.chatuikit.persistence.workers.UploadAndSendAttachmentWorkManager.MESSAGE_TID
import com.sceyt.chatuikit.persistence.workers.UploadAndSendAttachmentWorkManager.RESUME_PAUSED_UPLOAD
import com.sceyt.chatuikit.providers.ChatConnectionProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.withTimeout
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
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import kotlin.coroutines.CoroutineContext

@RunWith(RobolectricTestRunner::class)
// Isolate the real service from other suites' FileTransferHelper singleton dependencies.
@Config(instrumentedPackages = ["com.sceyt.chatuikit.persistence.workers"])
@OptIn(ExperimentalCoroutinesApi::class)
class UploadAndSendAttachmentWorkerTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val context: Context = RuntimeEnvironment.getApplication()
    private val messageLogic = mock<PersistenceMessagesLogic>()
    private val workManager = mock<WorkManagerImpl>()
    private val transport = RecordingFileTransferTransport()
    private lateinit var previousFileTransfer: SceytChatUIKitFileTransfer
    private lateinit var previousConfig: SceytChatUIKitConfig
    private lateinit var previousNotifications: SceytNotifications
    private var previousConnectionProvider: ChatConnectionProvider? = null

    private companion object {
        // FileTransferHelper retains injected dependencies, so reuse them across worker tests.
        val attachmentLogic = mock<PersistenceAttachmentLogic>()
        val testScope = TestScope(UnconfinedTestDispatcher())
        val service by lazy {
            val context = RuntimeEnvironment.getApplication()
            FileTransferServiceImpl(
                context,
                AttachmentUploadCoordinator(context, attachmentLogic, testScope),
                AttachmentDownloadCoordinator(context, testScope),
                mock(),
            )
        }
    }

    @Before
    fun setUp() = runBlocking {
        reset(attachmentLogic)
        AttachmentTransferStateStore.clear()
        previousFileTransfer = SceytChatUIKit.fileTransfer
        previousConfig = SceytChatUIKit.config
        previousConnectionProvider = SceytChatUIKit.chatConnectionProvider
        // UIKit is not initialized here, so its lazy notifications may not exist yet.
        previousNotifications = runCatching { SceytChatUIKit.notifications }
            .getOrElse { SceytNotifications(context) }
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
        val cancellation = mock<Operation>()
        whenever(cancellation.result).thenReturn(Futures.immediateFuture(Operation.SUCCESS))
        whenever(workManager.cancelAllWorkByTag(any())).thenReturn(cancellation)
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
        AttachmentTransferStateStore.clear()
        WorkManagerImpl.setDelegate(null)
        SceytChatUIKit.fileTransfer = previousFileTransfer
        SceytChatUIKit.config = previousConfig
        SceytChatUIKit.chatConnectionProvider = previousConnectionProvider
        SceytChatUIKit.notifications = previousNotifications
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
    fun `worker cancellation stops a suspended state update without starting upload`() = runBlocking<Unit> {
        val updateStarted = CompletableDeferred<Unit>()
        val releaseUpdate = CompletableDeferred<Unit>()
        whenever(attachmentLogic.updateAttachmentWithTransferData(any())).doSuspendableAnswer {
            updateStarted.complete(Unit)
            releaseUpdate.await()
        }
        val uploadWorker = worker(localAttachment())
        val work = testScope.async(Dispatchers.Default) { uploadWorker.doWork() }

        try {
            withTimeout(5_000) { updateStarted.await() }
            work.cancel()
            withTimeout(5_000) { work.join() }
            assertThat(transport.uploadCalls).isEmpty()
        } finally {
            releaseUpdate.complete(Unit)
            work.cancelAndJoin()
        }
    }

    @Test
    fun `pause before task registration publishes and persists paused state`() = runBlocking {
        val attachment = localAttachment().copy(transferState = TransferState.WaitingToUpload)

        service.pause(attachment.messageTid, attachment, TransferState.WaitingToUpload)

        assertThat(service.findTransferTask(attachment)?.state).isEqualTo(TransferState.PauseUpload)
        assertThat(FileTransferHelper.onTransferUpdatedLiveData.value?.state)
            .isEqualTo(TransferState.PauseUpload)
        val update = argumentCaptor<TransferData>()
        verify(attachmentLogic).updateTransferDataByMsgTid(update.capture())
        assertThat(update.firstValue.state).isEqualTo(TransferState.PauseUpload)
    }

    @Test
    fun `worker with stale state finishes when paused before task registration`() {
        for (shared in listOf(false, true)) {
            for (state in listOf(TransferState.PendingUpload, TransferState.WaitingToUpload)) {
                val attachment = localAttachment().copy(
                    messageTid = (if (shared) 100L else 200L) + state.ordinal,
                    transferState = state,
                )
                service.pause(attachment.messageTid, attachment, state)

                val worker = worker(attachment, shared = shared)
                val work = testScope.async { worker.doWork() }

                assertThat(work.isCompleted).isTrue()
                runBlocking {
                    assertThat(work.await()).isEqualTo(androidx.work.ListenableWorker.Result.success())
                }
                assertThat(service.findTransferTask(attachment)?.state).isEqualTo(TransferState.PauseUpload)
                assertThat(FileTransferHelper.onTransferUpdatedLiveData.value?.state)
                    .isEqualTo(TransferState.PauseUpload)
            }
        }
        assertThat(transport.uploadCalls).isEmpty()
        runBlocking {
            verify(messageLogic, never()).sendMessageWithUploadedAttachments(any(), any())
        }
    }

    @Test
    fun `explicit worker resume starts upload paused before task registration`() {
        val attachment = localAttachment().copy(transferState = TransferState.WaitingToUpload)
        service.pause(attachment.messageTid, attachment, TransferState.WaitingToUpload)

        val worker = worker(attachment.copy(transferState = TransferState.PauseUpload), resume = true)
        val work = testScope.async { worker.doWork() }

        assertThat(transport.uploadCalls).hasSize(1)
        transport.uploadCalls.single().succeed("resumed-url")
        assertThat(work.isCompleted).isTrue()
        assertSentUrl(attachment, "resumed-url")
    }

    @Test
    fun `rerun of a resume worker keeps a later pause`() {
        val attachment = localAttachment().copy(messageTid = 40L, transferState = TransferState.WaitingToUpload)
        service.pause(attachment.messageTid, attachment, TransferState.WaitingToUpload)

        val worker = worker(
            attachment.copy(transferState = TransferState.PauseUpload),
            resume = true,
            runAttemptCount = 1,
        )
        val work = testScope.async { worker.doWork() }

        assertThat(work.isCompleted).isTrue()
        assertThat(transport.uploadCalls).isEmpty()
        assertThat(service.findTransferTask(attachment)?.state).isEqualTo(TransferState.PauseUpload)
        runBlocking {
            verify(messageLogic, never()).sendMessageWithUploadedAttachments(any(), any())
        }
    }

    @Test
    fun `explicit shared resume joins existing upload after pause before registration`() {
        val first = localAttachment()
        val paused = first.copy(messageTid = 11L, transferState = TransferState.WaitingToUpload)
        service.uploadSharedFile(first)
        service.pause(paused.messageTid, paused, TransferState.WaitingToUpload)

        val worker = worker(paused.copy(transferState = TransferState.PauseUpload), resume = true, shared = true)
        val work = testScope.async { worker.doWork() }

        assertThat(transport.uploadCalls).hasSize(1)
        transport.uploadCalls.single().succeed("shared-url")
        assertThat(work.isCompleted).isTrue()
        assertSentUrl(paused, "shared-url")
    }

    @Test
    fun `production resume worker keeps sharing of upload paused before joining group`() {
        val first = localAttachment()
        val paused = first.copy(messageTid = 12L, transferState = TransferState.WaitingToUpload)
        service.uploadSharedFile(first)
        service.pause(paused.messageTid, paused, TransferState.WaitingToUpload)
        service.uploadSharedFile(paused)

        val worker = worker(paused.copy(transferState = TransferState.PauseUpload), resume = true)
        val work = testScope.async { worker.doWork() }

        assertThat(transport.uploadCalls).hasSize(1)
        transport.uploadCalls.single().succeed("shared-url")
        assertThat(work.isCompleted).isTrue()
        assertSentUrl(paused, "shared-url")
    }

    @Test
    fun `explicit worker resume clears pause and sends uploaded message`() {
        val attachment = localAttachment()
        val pausedWork = testScope.async { worker(attachment).doWork() }
        service.pause(attachment.messageTid, attachment, TransferState.Uploading)
        assertThat(pausedWork.isCompleted).isTrue()

        val worker = worker(attachment.copy(transferState = TransferState.PauseUpload), resume = true)
        val work = testScope.async { worker.doWork() }

        assertThat(transport.uploadCalls).hasSize(2)
        assertThat(transport.uploadCalls.first().cancelled).isTrue()
        transport.uploadCalls.last().succeed("resumed-url")

        assertThat(work.isCompleted).isTrue()
        assertSentUrl(attachment, "resumed-url")
    }

    @Test
    fun `worker resume waits for cancelled upload cleanup`() {
        val cleanup = CompletableDeferred<Unit>()
        transport.uploadCancellationGate = cleanup
        val attachment = localAttachment()
        val pausedWork = testScope.async { worker(attachment).doWork() }
        service.pause(attachment.messageTid, attachment, TransferState.Uploading)
        assertThat(pausedWork.isCompleted).isTrue()
        assertThat(transport.uploadCalls.single().cancelled).isTrue()

        try {
            val resumedWork = testScope.async {
                worker(attachment.copy(transferState = TransferState.PauseUpload), resume = true).doWork()
            }

            assertThat(transport.uploadCalls).hasSize(1)
            assertThat(resumedWork.isCompleted).isFalse()

            service.pause(attachment.messageTid, attachment, TransferState.WaitingToUpload)
            assertThat(resumedWork.isCompleted).isTrue()
            val retriedWork = testScope.async {
                worker(attachment.copy(transferState = TransferState.PauseUpload), resume = true).doWork()
            }
            assertThat(transport.uploadCalls).hasSize(1)

            cleanup.complete(Unit)
            assertThat(transport.uploadCalls).hasSize(2)
            transport.uploadCalls.last().succeed("resumed-url")

            assertThat(retriedWork.isCompleted).isTrue()
            assertSentUrl(attachment, "resumed-url")
        } finally {
            cleanup.complete(Unit)
        }
    }

    @Test
    fun `explicit worker resume uses native resume instead of uploading twice`() {
        transport.pauseResult = true
        transport.resumeResult = true
        val attachment = localAttachment()
        val pausedWork = testScope.async { worker(attachment).doWork() }
        val call = transport.uploadCalls.single()
        service.pause(attachment.messageTid, attachment, TransferState.Uploading)
        assertThat(pausedWork.isCompleted).isTrue()

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
    fun `completed native upload stays paused until its worker resumes`() {
        transport.pauseResult = true
        val attachment = localAttachment()
        val pausedWork = testScope.async { worker(attachment).doWork() }
        service.pause(attachment.messageTid, attachment, TransferState.Uploading)

        transport.uploadCalls.single().succeed("completed-while-paused")

        assertThat(pausedWork.isCompleted).isTrue()
        assertThat(service.findTransferTask(attachment)?.state).isEqualTo(TransferState.PauseUpload)
        runBlocking {
            verify(messageLogic, never()).sendMessageWithUploadedAttachments(any(), any())
        }

        val resumedWork = testScope.async {
            worker(attachment.copy(transferState = TransferState.PauseUpload), resume = true).doWork()
        }

        assertThat(resumedWork.isCompleted).isTrue()
        assertThat(transport.uploadCalls).hasSize(1)
        assertSentUrl(attachment, "completed-while-paused")
    }

    @Test
    fun `pause queued with upload completion retains the result for resume`() {
        val attachment = localAttachment()
        SceytChatUIKit.fileTransfer.transport = object : FileTransferTransport by transport {
            override suspend fun upload(
                request: FileUploadRequest,
                callback: FileTransferCallback,
            ): FileTransferResult<String> {
                val result = transport.upload(request, callback)
                service.pause(attachment.messageTid, attachment, TransferState.Uploading)
                return result
            }
        }
        val pausedWork = testScope.async { worker(attachment).doWork() }

        transport.uploadCalls.single().succeed("completed-before-pause-applied")

        assertThat(pausedWork.isCompleted).isTrue()
        assertThat(service.findTransferTask(attachment)?.state).isEqualTo(TransferState.PauseUpload)
        runBlocking {
            verify(messageLogic, never()).sendMessageWithUploadedAttachments(any(), any())
        }

        val resumedWork = testScope.async {
            worker(attachment.copy(transferState = TransferState.PauseUpload), resume = true).doWork()
        }

        assertThat(resumedWork.isCompleted).isTrue()
        assertThat(transport.uploadCalls).hasSize(1)
        assertSentUrl(attachment, "completed-before-pause-applied")
    }

    @Test
    fun `pausing an active upload finishes its worker without sending the message`() {
        for (nativePause in listOf(false, true)) {
            transport.pauseResult = nativePause
            val attachment = localAttachment().copy(messageTid = if (nativePause) 51L else 50L)
            val work = testScope.async { worker(attachment).doWork() }
            assertThat(work.isCompleted).isFalse()

            service.pause(attachment.messageTid, attachment, TransferState.Uploading)

            assertThat(work.isCompleted).isTrue()
            assertThat(service.findTransferTask(attachment)?.state).isEqualTo(TransferState.PauseUpload)
            assertThat(service.findTransferTask(attachment)?.onCompletionListeners).isEmpty()
            assertThat(transport.uploadCalls.last().cancelled).isEqualTo(!nativePause)
        }
        runBlocking {
            verify(messageLogic, never()).sendMessageWithUploadedAttachments(any(), any())
        }
    }

    @Test
    fun `pausing one shared upload finishes only its worker`() {
        val owner = localAttachment().copy(messageTid = 52L)
        val follower = owner.copy(messageTid = 53L)
        val ownerWork = testScope.async { worker(owner, shared = true).doWork() }
        val followerWork = testScope.async { worker(follower, shared = true).doWork() }

        service.pause(owner.messageTid, owner, TransferState.Uploading)

        assertThat(ownerWork.isCompleted).isTrue()
        assertThat(followerWork.isCompleted).isFalse()
        assertThat(transport.uploadCalls).hasSize(1)
        assertThat(transport.uploadCalls.single().cancelled).isFalse()
        transport.uploadCalls.single().succeed("shared-url")
        assertThat(followerWork.isCompleted).isTrue()
        assertSentUrl(follower, "shared-url")
        assertThat(service.findTransferTask(owner)?.state).isEqualTo(TransferState.PauseUpload)
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
    fun `clearing a channel stops its upload and releases the next queued upload`() = runBlocking<Unit> {
        val attachment = localAttachment()
        val work = testScope.async { worker(attachment).doWork() }
        val call = transport.uploadCalls.single()
        val next = attachment.copy(messageTid = 11L)
        service.upload(next)
        val messageDao = mock<MessageDao>()
        val repository = mock<ChannelsRepository>()
        val pending = checkNotNull(messageLogic.getMessageFromDbByTid(attachment.messageTid))
        whenever(messageDao.getPendingMessagesByChannelIds(listOf(0L))).thenReturn(listOf(pending.toMessageDb(false)))
        whenever(repository.clearHistory(0L, false))
            .thenReturn(SceytResponse.Success(createChannel(0L, 0L, 0L)))

        channelLogic(messageDao, repository).clearHistory(0L, false)

        assertThat(call.cancelled).isTrue()
        assertThat(work.isCompleted).isTrue()
        assertThat(service.findTransferTask(attachment)).isNull()
        assertThat(transport.uploadCalls).hasSize(2)
        assertThat(transport.uploadCalls.last().request.operationId).isEqualTo("upload:11")
        verify(messageLogic, never()).sendMessageWithUploadedAttachments(any(), any())
    }

    @Test
    fun `deleting a channel leaves another channels shared upload running`() = runBlocking {
        val owner = localAttachment()
        val follower = owner.copy(messageTid = 11L)
        val ownerWork = testScope.async { worker(owner, shared = true).doWork() }
        val followerWork = testScope.async { worker(follower, shared = true, channelId = 1L).doWork() }
        val call = transport.uploadCalls.single()
        val messageDao = mock<MessageDao>()
        val repository = mock<ChannelsRepository>()
        val pending = checkNotNull(messageLogic.getMessageFromDbByTid(owner.messageTid))
        whenever(messageDao.getPendingMessagesByChannelIds(listOf(0L))).thenReturn(listOf(pending.toMessageDb(false)))
        whenever(repository.deleteChannel(0L)).thenReturn(SceytResponse.Success(0L))

        channelLogic(messageDao, repository).deleteChannel(0L)

        verify(workManager).cancelAllWorkByTag(owner.messageTid.toString())
        verify(workManager, never()).cancelAllWorkByTag(follower.messageTid.toString())
        assertThat(ownerWork.isCompleted).isTrue()
        assertThat(service.findTransferTask(owner)).isNull()
        assertThat(call.cancelled).isFalse()
        assertThat(followerWork.isCompleted).isFalse()
        call.succeed("shared-after-channel-deletion")
        assertThat(followerWork.isCompleted).isTrue()
        assertSentUrl(follower, "shared-after-channel-deletion", channelId = 1L)
    }

    @Test
    fun `leaving a channel cancels a natively paused upload after its worker finished`() = runBlocking<Unit> {
        transport.pauseResult = true
        val attachment = localAttachment()
        val work = testScope.async { worker(attachment).doWork() }
        service.pause(attachment.messageTid, attachment, TransferState.Uploading)
        val call = transport.uploadCalls.single()
        assertThat(work.isCompleted).isTrue()
        assertThat(call.cancelled).isFalse()
        val messageDao = mock<MessageDao>()
        val repository = mock<ChannelsRepository>()
        val pending = checkNotNull(messageLogic.getMessageFromDbByTid(attachment.messageTid))
        whenever(messageDao.getPendingMessagesByChannelIds(listOf(0L))).thenReturn(listOf(pending.toMessageDb(false)))
        whenever(repository.leaveChannel(0L)).thenReturn(SceytResponse.Success(0L))

        channelLogic(messageDao, repository).leaveChannel(0L)

        assertThat(call.cancelled).isTrue()
        assertThat(service.findTransferTask(attachment)).isNull()
        verify(messageLogic, never()).sendMessageWithUploadedAttachments(any(), any())
    }

    @Test
    fun `failed clear history leaves its upload running`() = runBlocking {
        val attachment = localAttachment()
        val work = testScope.async { worker(attachment).doWork() }
        val call = transport.uploadCalls.single()
        val messageDao = mock<MessageDao>()
        val repository = mock<ChannelsRepository>()
        val pending = checkNotNull(messageLogic.getMessageFromDbByTid(attachment.messageTid))
        whenever(messageDao.getPendingMessagesByChannelIds(listOf(0L))).thenReturn(listOf(pending.toMessageDb(false)))
        whenever(repository.clearHistory(0L, false))
            .thenReturn(SceytResponse.Error(SceytException(1, "clear failed")))

        channelLogic(messageDao, repository).clearHistory(0L, false)

        assertThat(call.cancelled).isFalse()
        assertThat(work.isCompleted).isFalse()
        call.succeed("upload-after-failed-clear")
        assertThat(work.isCompleted).isTrue()
        assertSentUrl(attachment, "upload-after-failed-clear")
    }

    @Test
    fun `sync removing a channel cancels its pending upload`() = runBlocking<Unit> {
        val attachment = localAttachment()
        val work = testScope.async { worker(attachment).doWork() }
        val call = transport.uploadCalls.single()
        val messageDao = mock<MessageDao>()
        val channelDao = mock<ChannelDao>()
        val repository = mock<ChannelsRepository>()
        val pending = checkNotNull(messageLogic.getMessageFromDbByTid(attachment.messageTid))
        whenever(messageDao.getPendingMessagesByChannelIds(listOf(0L))).thenReturn(listOf(pending.toMessageDb(false)))
        whenever(channelDao.getAllChannelIdsByTypes(emptyList(), true)).thenReturn(listOf(0L))
        whenever(repository.getAllChannels(any())).thenReturn(flowOf(SyncResult.SuccessfullyFinished))

        channelLogic(messageDao, repository, channelDao).syncChannels(ChannelListConfig.default).toList()

        assertThat(call.cancelled).isTrue()
        assertThat(work.isCompleted).isTrue()
        assertThat(service.findTransferTask(attachment)).isNull()
        verify(messageLogic, never()).sendMessageWithUploadedAttachments(any(), any())
    }

    @Test
    fun `sync looks up pending messages once for all removed channels`() = runBlocking<Unit> {
        val channelIds = (0L until 200L).toList()
        val messageDao = mock<MessageDao>()
        val channelDao = mock<ChannelDao>()
        val repository = mock<ChannelsRepository>()
        whenever(messageDao.getPendingMessages(any())).thenReturn(emptyList())
        whenever(messageDao.getPendingMessagesByChannelIds(channelIds)).thenReturn(emptyList())
        whenever(channelDao.getAllChannelIdsByTypes(emptyList(), true)).thenReturn(channelIds)
        whenever(repository.getAllChannels(any())).thenReturn(flowOf(SyncResult.SuccessfullyFinished))

        channelLogic(messageDao, repository, channelDao).syncChannels(ChannelListConfig.default).toList()

        verify(messageDao).getPendingMessagesByChannelIds(channelIds)
        verify(messageDao, never()).getPendingMessages(any())
        verify(workManager, never()).cancelAllWorkByTag(any())
    }

    @Test
    fun `server clear prevents a worker with an already loaded message from sending`() = runBlocking<Unit> {
        val updateStarted = CompletableDeferred<Unit>()
        val releaseUpdate = CompletableDeferred<Unit>()
        whenever(attachmentLogic.updateAttachmentWithTransferData(any())).doSuspendableAnswer {
            updateStarted.complete(Unit)
            releaseUpdate.await()
        }
        val attachment = localAttachment()
        val work = testScope.async { worker(attachment).doWork() }
        assertThat(updateStarted.isCompleted).isTrue()
        assertThat(service.findTransferTask(attachment)).isNull()
        val messageDao = mock<MessageDao>()
        val repository = mock<ChannelsRepository>()
        val pending = checkNotNull(messageLogic.getMessageFromDbByTid(attachment.messageTid))
        whenever(messageDao.getPendingMessagesByChannelIds(listOf(0L))).thenReturn(listOf(pending.toMessageDb(false)))
        val cancellation = mock<Operation>()
        whenever(cancellation.result).thenReturn(Futures.immediateFuture(Operation.SUCCESS))
        whenever(workManager.cancelAllWorkByTag(attachment.messageTid.toString())).thenAnswer {
            work.cancel()
            cancellation
        }

        try {
            channelLogic(messageDao, repository).onChannelEvent(
                ChannelActionEvent.ClearedHistory(createChannel(0L, 0L, 0L)),
            )
            whenever(messageLogic.getMessageFromDbByTid(attachment.messageTid)).thenReturn(null)
            releaseUpdate.complete(Unit)
            transport.uploadCalls.singleOrNull()?.succeed("uploaded-after-clear")

            assertThat(work.isCompleted).isTrue()
            verify(messageLogic, never()).sendMessageWithUploadedAttachments(any(), any())
            assertThat(transport.uploadCalls).isEmpty()
        } finally {
            releaseUpdate.complete(Unit)
            work.cancelAndJoin()
        }
    }

    @Test
    fun `server cleanup waits for worker cancellation before deleting message rows`() = runBlocking<Unit> {
        val attachment = attachment()
        val pending = createMessage(createdAt = 1L, tid = attachment.messageTid).copy(
            deliveryStatus = MessageDeliveryStatus.Pending,
            attachments = listOf(attachment),
        )
        val messageDao = mock<MessageDao>()
        whenever(messageDao.getPendingMessagesByChannelIds(listOf(0L))).thenReturn(listOf(pending.toMessageDb(false)))
        val stopped = SettableFuture.create<Operation.State.SUCCESS>()
        val cancellation = mock<Operation>()
        whenever(cancellation.result).thenReturn(stopped)
        whenever(workManager.cancelAllWorkByTag(attachment.messageTid.toString())).thenReturn(cancellation)
        val cleanup = testScope.async {
            channelLogic(messageDao, mock()).onChannelEvent(
                ChannelActionEvent.ClearedHistory(createChannel(0L, 0L, 0L)),
            )
        }

        try {
            assertThat(cleanup.isCompleted).isFalse()
            verify(messageDao, never()).deleteAllMessagesByChannel(0L)

            stopped.set(Operation.SUCCESS)
            cleanup.await()

            verify(messageDao).deleteAllMessagesByChannel(0L)
            assertThat(service.getTasks()).isEmpty()
        } finally {
            stopped.set(Operation.SUCCESS)
            cleanup.cancelAndJoin()
        }
    }

    @Test
    fun `worker cancellation failure still cancels the upload and clears message rows`() = runBlocking<Unit> {
        val attachment = localAttachment()
        val work = testScope.async { worker(attachment).doWork() }
        val call = transport.uploadCalls.single()
        val messageDao = mock<MessageDao>()
        val pending = checkNotNull(messageLogic.getMessageFromDbByTid(attachment.messageTid))
        whenever(messageDao.getPendingMessagesByChannelIds(listOf(0L))).thenReturn(listOf(pending.toMessageDb(false)))
        val cancellation = mock<Operation>()
        whenever(cancellation.result).thenReturn(Futures.immediateFailedFuture(IllegalStateException("cancel failed")))
        whenever(workManager.cancelAllWorkByTag(attachment.messageTid.toString())).thenReturn(cancellation)

        channelLogic(messageDao, mock()).onChannelEvent(
            ChannelActionEvent.ClearedHistory(createChannel(0L, 0L, 0L)),
        )

        assertThat(call.cancelled).isTrue()
        assertThat(work.isCompleted).isTrue()
        assertThat(service.findTransferTask(attachment)).isNull()
        verify(messageDao).deleteAllMessagesByChannel(0L)
        verify(messageLogic, never()).sendMessageWithUploadedAttachments(any(), any())
    }

    private fun channelLogic(
        messageDao: MessageDao,
        repository: ChannelsRepository,
        channelDao: ChannelDao = mock(),
    ) =
        PersistenceChannelsLogicImpl(
            context = context,
            channelsRepository = repository,
            channelDao = channelDao,
            globalSearchDao = mock(),
            usersDao = mock(),
            messageDao = messageDao,
            rangeDao = mock(),
            draftMessageDao = mock(),
            chatUserReactionDao = mock(),
            pendingReactionDao = mock(),
            channelsCache = ChannelsCache(),
            channelSyncStateStore = mock(),
            pendingChannelCoordinator = mock(),
            insertChannelWithMembersUseCase = mock(),
        )

    @Test
    fun `explicit resume after process restart starts a fresh shared upload`() {
        val attachment = localAttachment().copy(transferState = TransferState.PauseUpload)
        val worker = worker(attachment, resume = true, shared = true)
        val work = testScope.async { worker.doWork() }

        assertThat(transport.uploadCalls).hasSize(1)
        val context = transport.uploadCalls.single().request.context as AttachmentTransferContext
        assertThat(context.isSharedUpload).isTrue()
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
        runAttemptCount: Int = 0,
        channelId: Long = 0L,
    ): UploadAndSendAttachmentWorker {
        val message = createMessage(createdAt = 1L, tid = attachment.messageTid).copy(
            channelId = channelId,
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
        whenever(params.runAttemptCount).thenReturn(runAttemptCount)
        return UploadAndSendAttachmentWorker(context, params)
    }

    private fun assertSentUrl(attachment: SceytAttachment, url: String, channelId: Long = 0L) = runBlocking {
        val message = argumentCaptor<Message>()
        verify(messageLogic).sendMessageWithUploadedAttachments(eq(channelId), message.capture())
        assertThat(message.firstValue.tid).isEqualTo(attachment.messageTid)
        assertThat(message.firstValue.attachments.single().url).isEqualTo(url)
    }
}
