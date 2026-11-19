package com.sceyt.chatuikit.persistence.logicimpl.message

import android.os.Looper
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.logic.PersistenceChannelsLogic
import com.sceyt.chatuikit.persistence.logicimpl.usecases.SendPendingMessagesUseCase
import com.sceyt.chatuikit.persistence.mappers.toMessage
import com.sceyt.chatuikit.persistence.repositories.MessagesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class PersistenceMessagesLogicImplSendThreadTest {
    private val messageDao = mock<MessageDao>()
    private val repository = mock<MessagesRepository>()
    private val channels = mock<PersistenceChannelsLogic>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        stopKoin()
        SceytKoinApp.koinApp = startKoin {
            modules(module { single { channels } })
        }
    }

    @After
    fun tearDown() {
        SceytKoinApp.koinApp = null
        stopKoin()
        Dispatchers.resetMain()
    }

    @Test
    fun `messages sent from the main thread are prepared and sent off the main thread`() = runBlocking<Unit> {
        val mainLooper = Looper.getMainLooper()
        var sentOnMainThread: Boolean? = null
        whenever(repository.sendMessage(any(), any())).thenAnswer {
            sentOnMainThread = mainLooper.isCurrentThread
            SceytResponse.Error<SceytMessage>()
        }
        val message = createMessage(createdAt = 1L, tid = TID).copy(channelId = CHANNEL_ID).toMessage()

        assertThat(mainLooper.isCurrentThread).isTrue()
        logic().sendMessages(CHANNEL_ID, listOf(message))

        assertThat(sentOnMainThread).isFalse()
    }

    private fun logic() = PersistenceMessagesLogicImpl(
        context = RuntimeEnvironment.getApplication(),
        messageDao = messageDao,
        rangeDao = mock(),
        attachmentDao = mock(),
        pendingMarkerDao = mock(),
        reactionDao = mock(),
        userDao = mock(),
        pendingMessageStateDao = mock(),
        pendingMessageDeleteByTidDao = mock(),
        pollDao = mock(),
        pinnedMessageDao = mock(),
        pendingPinDao = mock(),
        pendingPollVoteDao = mock(),
        fileTransferService = mock(),
        messagesRepository = repository,
        preference = mock(),
        messagesCache = mock(),
        channelCache = mock(),
        messageLoadRangeUpdater = mock(),
        checkDeletedMessagesUseCase = mock(),
        channelSyncStateStore = mock(),
        sendPendingMessagesUseCase = SendPendingMessagesUseCase(
            messageDao = messageDao,
            registry = PendingSendRegistry(),
            isConnected = { true },
        ),
    )

    private companion object {
        const val CHANNEL_ID = 7L
        const val TID = 42L
    }
}
