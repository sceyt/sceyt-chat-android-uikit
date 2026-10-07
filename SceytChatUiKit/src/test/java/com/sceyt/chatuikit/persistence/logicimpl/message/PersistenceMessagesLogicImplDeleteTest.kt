package com.sceyt.chatuikit.persistence.logicimpl.message

import androidx.work.impl.WorkManagerImpl
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.message.DeleteMessageType.DeleteHard
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.logic.PersistenceChannelsLogic
import com.sceyt.chatuikit.persistence.logicimpl.attachment
import com.sceyt.chatuikit.persistence.repositories.MessagesRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
internal class PersistenceMessagesLogicImplDeleteTest {
    private val service = mock<FileTransferService>()
    private val repository = mock<MessagesRepository>()
    private val cache = mock<MessagesCache>()
    private val channels = mock<PersistenceChannelsLogic>()
    private val workManager = mock<WorkManagerImpl>()

    @Before
    fun setUp() {
        WorkManagerImpl.setDelegate(workManager)
        stopKoin()
        SceytKoinApp.koinApp = startKoin {
            modules(module { single { channels } })
        }
    }

    @After
    fun tearDown() {
        WorkManagerImpl.setDelegate(null)
        SceytKoinApp.koinApp = null
        stopKoin()
    }

    @Test
    fun `deleting a pending message cancels rather than pauses its upload`() = runBlocking {
        val attachment = attachment()
        val message = createMessage(createdAt = 1L, tid = attachment.messageTid).copy(
            channelId = 1L,
            deliveryStatus = MessageDeliveryStatus.Pending,
            attachments = listOf(attachment),
        )
        whenever(cache.get(message.channelId, message.tid)).thenReturn(message)
        whenever(repository.deleteMessageByTid(message.channelId, message.tid, DeleteHard))
            .thenReturn(SceytResponse.Success(null))

        val result = logic().deleteMessage(message.channelId, message, DeleteHard)

        assertThat((result as SceytResponse.Success).data?.state).isEqualTo(MessageState.Deleted)
        verify(workManager).cancelAllWorkByTag(message.tid.toString())
        verify(service).cancelUpload(attachment)
        verify(service, never()).pause(any(), any(), any())
    }

    @Test
    fun `deleting a pending message cancels every attachment including unstarted uploads`() = runBlocking<Unit> {
        val first = attachment(state = TransferState.WaitingToUpload)
        val second = first.copy(id = 11L, url = "second-url", transferState = TransferState.PauseUpload)
        val message = createMessage(createdAt = 1L, tid = first.messageTid).copy(
            channelId = 1L,
            deliveryStatus = MessageDeliveryStatus.Pending,
            attachments = listOf(first, second),
        )
        whenever(cache.get(message.channelId, message.tid)).thenReturn(message)
        whenever(repository.deleteMessageByTid(message.channelId, message.tid, DeleteHard))
            .thenReturn(SceytResponse.Success(null))

        logic().deleteMessage(message.channelId, message, DeleteHard)

        verify(service).cancelUpload(first)
        verify(service).cancelUpload(second)
        verify(service, never()).findOrCreateTransferTask(any())
        verify(service, never()).pause(any(), any(), any())
        verify(repository).deleteMessageByTid(message.channelId, message.tid, DeleteHard)
    }

    private fun logic() = PersistenceMessagesLogicImpl(
        context = RuntimeEnvironment.getApplication(),
        messageDao = mock(),
        rangeDao = mock(),
        attachmentDao = mock(),
        pendingMarkerDao = mock(),
        reactionDao = mock(),
        userDao = mock(),
        pendingMessageStateDao = mock(),
        pendingMessageDeleteByTidDao = mock(),
        pollDao = mock(),
        pendingPollVoteDao = mock(),
        fileTransferService = service,
        messagesRepository = repository,
        preference = mock(),
        messagesCache = cache,
        channelCache = mock(),
        messageLoadRangeUpdater = mock(),
        checkDeletedMessagesUseCase = mock(),
        channelSyncStateStore = mock(),
    )
}
