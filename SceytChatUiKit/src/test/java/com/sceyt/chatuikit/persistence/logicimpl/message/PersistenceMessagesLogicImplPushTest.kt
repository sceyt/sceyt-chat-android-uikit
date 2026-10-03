package com.sceyt.chatuikit.persistence.logicimpl.message

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.SceytChatUIFacade
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.config.SceytChatUIKitConfig
import com.sceyt.chatuikit.createChannel
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.messages.SceytMessage
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
import com.sceyt.chatuikit.persistence.database.dao.PendingMessageDeleteByTidDao
import com.sceyt.chatuikit.persistence.database.dao.PendingMessageStateDao
import com.sceyt.chatuikit.persistence.database.dao.PendingPollVoteDao
import com.sceyt.chatuikit.persistence.database.dao.PendingPinDao
import com.sceyt.chatuikit.persistence.database.dao.PinnedMessageDao
import com.sceyt.chatuikit.persistence.database.dao.PollDao
import com.sceyt.chatuikit.persistence.database.dao.ReactionDao
import com.sceyt.chatuikit.persistence.database.dao.UserDao
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageDb
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingMessageStateEntity
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceChannelsLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceReactionsLogic
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelsCache
import com.sceyt.chatuikit.persistence.logicimpl.sync.ChannelSyncStateStore
import com.sceyt.chatuikit.persistence.logicimpl.usecases.CheckDeletedMessagesUseCase
import com.sceyt.chatuikit.persistence.mappers.toParentMessageEntity
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
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
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
        val firstInsertStarted = CompletableDeferred<Unit>()
        val releaseFirstInsert = CompletableDeferred<Unit>()
        val insertedMessageIds = mutableListOf<Long>()
        val logic = logic()
        stubPersistenceForPushes(first, second)
        doSuspendableAnswer { invocation ->
            val messageId = invocation.getArgument<MessageDb>(0).messageEntity.id!!
            synchronized(insertedMessageIds) {
                insertedMessageIds += messageId
            }
            if (messageId == first.message.id) {
                firstInsertStarted.complete(Unit)
                releaseFirstInsert.await()
            }
            true
        }.whenever(messageDao) { insertMessageIgnored(any()) }

        val firstJob = async(Dispatchers.Default) { logic.handlePush(first) }
        withTimeout(1_000.milliseconds) { firstInsertStarted.await() }

        val secondJob = async(Dispatchers.Default) { logic.handlePush(second) }

        assertThat(snapshot(insertedMessageIds)).containsExactly(1L)

        releaseFirstInsert.complete(Unit)

        assertThat(firstJob.await()).isTrue()
        assertThat(secondJob.await()).isTrue()
        assertThat(snapshot(insertedMessageIds)).containsExactly(1L, 2L).inOrder()
        verify(persistenceChannelsLogic).handlePush(first)
        verify(persistenceChannelsLogic).handlePush(second)
    }

    @Test
    fun `handlePush emits reply with parent from db when parent exists`() = runBlocking {
        val parent = createMessage(createdAt = 1, id = PARENT_ID, tid = PARENT_ID)
            .copy(channelId = CHANNEL_ID, body = "parent body", user = SceytUser("parent-author"))
        val data = pushData(messageId = 2, parentId = PARENT_ID)
        stubPersistenceForPushes(data)
        whenever(messageDao.insertMessageIgnored(any())).thenReturn(true)
        whenever(messageDao.getParentMessageById(PARENT_ID)).thenReturn(parent.toParentMessageEntity())

        assertThat(logic().handlePush(data)).isTrue()

        val emitted = argumentCaptor<SceytMessage>()
        verify(messagesCache).add(eq(CHANNEL_ID), emitted.capture())
        assertThat(emitted.firstValue.parentMessage?.id).isEqualTo(PARENT_ID)
        assertThat(emitted.firstValue.parentMessage?.body).isEqualTo("parent body")
        assertThat(emitted.firstValue.parentMessage?.user?.id).isEqualTo("parent-author")
    }

    @Test
    fun `handlePush emits reply without parent when parent missing in db but keeps parentId`() =
        runBlocking {
            val data = pushData(messageId = 2, parentId = PARENT_ID)
            stubPersistenceForPushes(data)
            whenever(messageDao.insertMessageIgnored(any())).thenReturn(true)
            whenever(messageDao.getParentMessageById(PARENT_ID)).thenReturn(null)

            assertThat(logic().handlePush(data)).isTrue()

            val inserted = argumentCaptor<MessageDb>()
            verify(messageDao).insertMessageIgnored(inserted.capture())
            assertThat(inserted.firstValue.messageEntity.parentId).isEqualTo(PARENT_ID)

            val emitted = argumentCaptor<SceytMessage>()
            verify(messagesCache).add(eq(CHANNEL_ID), emitted.capture())
            assertThat(emitted.firstValue.parentMessage).isNull()
        }

    @Test
    fun `handlePush applies pending hard delete before insert and emit`() = runBlocking {
        val data = pushData(messageId = 2)
        stubPersistenceForPushes(data)
        val pendingDelete = PendingMessageStateEntity(
            messageId = 2,
            channelId = CHANNEL_ID,
            state = MessageState.DeletedHard,
            editBody = null,
            deleteOnlyForMe = false
        )
        whenever(pendingMessageStateDao.getAll()).thenReturn(listOf(pendingDelete))
        whenever(messageDao.insertMessageIgnored(any())).thenReturn(true)

        assertThat(logic().handlePush(data)).isTrue()

        val inserted = argumentCaptor<MessageDb>()
        verify(messageDao).insertMessageIgnored(inserted.capture())
        assertThat(inserted.firstValue.messageEntity.state).isEqualTo(MessageState.DeletedHard)

        val emitted = argumentCaptor<SceytMessage>()
        verify(messagesCache).add(eq(CHANNEL_ID), emitted.capture())
        assertThat(emitted.firstValue.state).isEqualTo(MessageState.DeletedHard)
    }

    @Test
    fun `handlePush skips emit when message already exists in db`() = runBlocking {
        val data = pushData(messageId = 2, parentId = PARENT_ID)
        stubPersistenceForPushes(data)
        whenever(messageDao.insertMessageIgnored(any())).thenReturn(false)

        assertThat(logic().handlePush(data)).isTrue()

        verify(messageDao, never()).getParentMessageById(any())
        verify(messagesCache, never()).add(any(), any())
        verify(persistenceChannelsLogic, never()).handlePush(any())
    }

    private suspend fun stubPersistenceForPushes(vararg data: PushData) {
        data.forEach {
            whenever(persistenceChannelsLogic.getChannelFromDb(it.channel.id)).thenReturn(it.channel)
        }
        whenever(pendingMessageStateDao.getAll()).thenReturn(emptyList())
        whenever(pendingPollVoteDao.getAllPendingVotesDb()).thenReturn(emptyList())
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
        pinnedMessageDao = mock<PinnedMessageDao>(),
        pendingPinDao = mock<PendingPinDao>(),
        pendingPollVoteDao = pendingPollVoteDao,
        fileTransferService = mock<FileTransferService>(),
        messagesRepository = mock<MessagesRepository>(),
        preference = mock<SceytSharedPreference>(),
        messagesCache = messagesCache,
        channelCache = mock<ChannelsCache>(),
        messageLoadRangeUpdater = MessageLoadRangeUpdater(rangeDao),
        checkDeletedMessagesUseCase = mock<CheckDeletedMessagesUseCase>(),
        channelSyncStateStore = mock<ChannelSyncStateStore>(),
        pendingMessageDeleteByTidDao = mock<PendingMessageDeleteByTidDao>(),
    )

    private fun pushData(messageId: Long, parentId: Long? = null): PushData {
        val user = SceytUser("sender")
        val parentMessage = parentId?.let {
            createMessage(createdAt = 0, id = it).copy(channelId = CHANNEL_ID)
        }
        val message = createMessage(createdAt = messageId, id = messageId, tid = messageId)
            .copy(channelId = CHANNEL_ID, user = user, incoming = true, parentMessage = parentMessage)
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

    private fun snapshot(ids: MutableList<Long>): List<Long> {
        return synchronized(ids) { ids.toList() }
    }

    private companion object {
        const val CHANNEL_ID = 99L
        const val PARENT_ID = 50L
    }
}
