package com.sceyt.chatuikit.persistence.logicimpl.message

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.SceytChatUIFacade
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.config.SceytChatUIKitConfig
import com.sceyt.chatuikit.createChannel
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.logger.SceytLogLevel
import com.sceyt.chatuikit.logger.SceytLoggerImpl
import com.sceyt.chatuikit.notifications.NotificationType
import com.sceyt.chatuikit.persistence.database.dao.AttachmentDao
import com.sceyt.chatuikit.persistence.database.dao.LoadRangeDao
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.PendingMarkerDao
import com.sceyt.chatuikit.persistence.database.dao.PendingMessageStateDao
import com.sceyt.chatuikit.persistence.database.dao.PendingPollVoteDao
import com.sceyt.chatuikit.persistence.database.dao.PollDao
import com.sceyt.chatuikit.persistence.database.dao.ReactionDao
import com.sceyt.chatuikit.persistence.database.dao.UserDao
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceChannelsLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceReactionsLogic
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelsCache
import com.sceyt.chatuikit.persistence.logicimpl.sync.ChannelSyncStateStore
import com.sceyt.chatuikit.persistence.logicimpl.usecases.CheckDeletedMessagesUseCase
import com.sceyt.chatuikit.persistence.repositories.MessagesRepository
import com.sceyt.chatuikit.persistence.repositories.SceytSharedPreference
import com.sceyt.chatuikit.push.PushData
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.time.Duration.Companion.milliseconds

internal class PersistenceMessagesLogicImplPushTest {
    private val context = mock<Context>()
    private val messageDao = mock<MessageDao>()
    private val rangeDao = mock<LoadRangeDao>()
    private val pendingMessageStateDao = mock<PendingMessageStateDao>()
    private val pendingPollVoteDao = mock<PendingPollVoteDao>()
    private val userDao = mock<UserDao>()
    private val messagesCache = mock<MessagesCache>()
    private val persistenceChannelsLogic = mock<PersistenceChannelsLogic>()
    private val persistenceAttachmentLogic = mock<PersistenceAttachmentLogic>()
    private val persistenceReactionLogic = mock<PersistenceReactionsLogic>()
    private val chatUIFacade = mock<SceytChatUIFacade>()

    @Before
    fun setUp() {
        stopKoin()
        SceytLog.setLogger(SceytLogLevel.None) { _, _, _, _ -> }
        SceytChatUIKit.config = SceytChatUIKitConfig()
        whenever(chatUIFacade.myId).thenReturn("me")
        SceytKoinApp.koinApp = startKoin {
            modules(
                module {
                    single { persistenceChannelsLogic }
                    single { persistenceAttachmentLogic }
                    single { persistenceReactionLogic }
                    single { chatUIFacade }
                }
            )
        }
    }

    @After
    fun tearDown() {
        SceytKoinApp.koinApp = null
        stopKoin()
        SceytLog.setLogger(SceytLogLevel.Verbose, SceytLoggerImpl())
        SceytChatUIKit.config = SceytChatUIKitConfig()
    }

    @Test
    fun `handlePush serializes incoming message persistence`() = runBlocking {
        val first = pushData(messageId = 1)
        val second = pushData(messageId = 2)
        val firstReadStarted = CompletableDeferred<Unit>()
        val releaseFirstRead = CompletableDeferred<Unit>()
        val readMessageIds = mutableListOf<Long>()
        val logic = logic()
        stubPersistenceForPushes(first, second)
        doSuspendableAnswer { invocation ->
            val messageId = invocation.getArgument<Long>(0)
            synchronized(readMessageIds) {
                readMessageIds += messageId
            }
            if (messageId == first.message.id) {
                firstReadStarted.complete(Unit)
                releaseFirstRead.await()
            }
            null
        }.whenever(messageDao) { getMessageById(any()) }

        val firstJob = async(Dispatchers.Default) { logic.handlePush(first) }
        withTimeout(1_000.milliseconds) { firstReadStarted.await() }

        val secondJob = async(Dispatchers.Default) { logic.handlePush(second) }

        assertThat(readMessageIdsSnapshot(readMessageIds)).containsExactly(1L)

        releaseFirstRead.complete(Unit)

        assertThat(firstJob.await()).isTrue()
        assertThat(secondJob.await()).isTrue()
        assertThat(readMessageIdsSnapshot(readMessageIds)).containsExactly(1L, 2L).inOrder()
        verify(persistenceChannelsLogic).handlePush(first)
        verify(persistenceChannelsLogic).handlePush(second)
    }

    private suspend fun stubPersistenceForPushes(vararg data: PushData) {
        data.forEach {
            whenever(persistenceChannelsLogic.getChannelFromDb(it.channel.id)).thenReturn(it.channel)
        }
        whenever(pendingMessageStateDao.getAll()).thenReturn(emptyList())
        whenever(pendingPollVoteDao.getAllPendingVotesDb()).thenReturn(emptyList())
        whenever(messageDao.upsertMessages(any())).thenReturn(emptyList())
        whenever(rangeDao.getLoadRanges(any(), any(), any(), any())).thenReturn(emptyList())
    }

    private fun logic() = PersistenceMessagesLogicImpl(
        context = context,
        messageDao = messageDao,
        rangeDao = rangeDao,
        attachmentDao = mock<AttachmentDao>(),
        pendingMarkerDao = mock<PendingMarkerDao>(),
        reactionDao = mock<ReactionDao>(),
        userDao = userDao,
        pendingMessageStateDao = pendingMessageStateDao,
        pollDao = mock<PollDao>(),
        pendingPollVoteDao = pendingPollVoteDao,
        fileTransferService = mock<FileTransferService>(),
        messagesRepository = mock<MessagesRepository>(),
        preference = mock<SceytSharedPreference>(),
        messagesCache = messagesCache,
        channelCache = mock<ChannelsCache>(),
        messageLoadRangeUpdater = MessageLoadRangeUpdater(rangeDao),
        checkDeletedMessagesUseCase = mock<CheckDeletedMessagesUseCase>(),
        channelSyncStateStore = mock<ChannelSyncStateStore>(),
    )

    private fun pushData(messageId: Long): PushData {
        val user = SceytUser("sender")
        val message = createMessage(createdAt = messageId, id = messageId, tid = messageId)
            .copy(channelId = CHANNEL_ID, user = user)
        val channel = createChannel(
            id = CHANNEL_ID,
            pinnedAt = 0,
            createdAt = 1,
            lastMessage = createMessage(createdAt = 0, id = 0, tid = 0)
        )
        return PushData(
            type = NotificationType.ChannelMessage,
            channel = channel,
            message = message,
            user = user,
            reaction = null
        )
    }

    private fun readMessageIdsSnapshot(readMessageIds: MutableList<Long>): List<Long> {
        return synchronized(readMessageIds) { readMessageIds.toList() }
    }

    private companion object {
        const val CHANNEL_ID = 99L
    }
}
