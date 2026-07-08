package com.sceyt.chatuikit

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.config.ChannelListConfig
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.SyncResult
import com.sceyt.chatuikit.persistence.interactor.ChannelInteractor
import com.sceyt.chatuikit.persistence.interactor.MessageInteractor
import com.sceyt.chatuikit.persistence.logicimpl.sync.ChannelSyncStateStore
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.logger.SceytLogLevel
import com.sceyt.chatuikit.logger.SceytLoggerImpl
import com.sceyt.chatuikit.services.sync.SceytSyncManager
import com.sceyt.chatuikit.services.sync.SceytSyncManagerImpl
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class SceytSyncManagerImplTest {

    private val channelInteractor = mock<ChannelInteractor>()
    private val messageInteractor = mock<MessageInteractor>()
    private val channelSyncStateStore = mock<ChannelSyncStateStore>()
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        SceytLog.setLogger(SceytLogLevel.Verbose) { _, _, _, _ -> }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        SceytLog.setLogger(SceytLogLevel.Verbose, SceytLoggerImpl())
    }

    @Test
    fun `conversation sync returns collected messages once after finish`() = runTest {
        val messages = listOf(
            createMessage(createdAt = 1, id = 1, tid = 1),
            createMessage(createdAt = 2, id = 2, tid = 2),
            createMessage(createdAt = 3, id = 3, tid = 3),
        )
        val channel = createChannel(
            id = 10,
            pinnedAt = 0,
            createdAt = 0,
            lastMessage = messages.last()
        )

        whenever(channelInteractor.getChannelFromServer(channel.id))
            .thenReturn(SceytResponse.Success(channel))
        whenever(
            messageInteractor.syncMessagesAfterMessageId(
                conversationId = channel.id,
                replyInThread = false,
                messageId = 0
            )
        ).thenReturn(
            flowOf(
                SyncResult.Proportion(messages.take(1)),
                SyncResult.Proportion(messages.drop(1)),
                SyncResult.SuccessfullyFinished
            )
        )

        val result = syncManager().syncConversationMessagesAfter(channel.id, fromMessageId = 0)

        assertThat(result?.channel?.id).isEqualTo(channel.id)
        assertThat(result?.fromMessageId).isEqualTo(0)
        assertThat(result?.messages?.map { it.id }).containsExactlyElementsIn(messages.map { it.id })
    }

    @Test
    fun `conversation sync does not update channel sync result data`() = runTest {
        val config = ChannelListConfig.default
        val manager = syncManager()
        val messages = listOf(
            createMessage(createdAt = 1, id = 1, tid = 1),
            createMessage(createdAt = 2, id = 2, tid = 2),
        )
        val channel = createChannel(
            id = 10,
            pinnedAt = 0,
            createdAt = 0,
            lastMessage = messages.last()
        )
        val channelSyncStarted = CompletableDeferred<Unit>()
        val finishChannelSync = CompletableDeferred<Unit>()
        val callbacks = mutableListOf<Result<SceytSyncManager.SyncResultData>>()
        whenever(channelInteractor.syncChannels(config))
            .thenReturn(
                flow {
                    channelSyncStarted.complete(Unit)
                    finishChannelSync.await()
                    emit(SyncResult.SuccessfullyFinished)
                }
            )
        whenever(channelInteractor.getChannelFromServer(channel.id))
            .thenReturn(SceytResponse.Success(channel))
        whenever(
            messageInteractor.syncMessagesAfterMessageId(
                conversationId = channel.id,
                replyInThread = false,
                messageId = 0
            )
        ).thenReturn(
            flowOf(
                SyncResult.Proportion(messages),
                SyncResult.SuccessfullyFinished
            )
        )

        val syncJob = launch {
            manager.startSync(config) { callbacks.add(it) }
        }
        channelSyncStarted.await()

        manager.syncConversationMessagesAfter(channel.id, fromMessageId = 0)
        finishChannelSync.complete(Unit)
        syncJob.join()

        assertThat(callbacks).hasSize(1)
        assertThat(callbacks.first().getOrThrow().syncedMessagesCount).isEqualTo(0)
    }

    @Test
    fun `startSync clears in-process state after channel sync error`() = runTest {
        val config = ChannelListConfig.default
        val manager = syncManager()
        val callbacks = mutableListOf<Result<SceytSyncManager.SyncResultData>>()
        whenever(channelInteractor.syncChannels(config))
            .thenReturn(flowOf(SyncResult.Error(null)))

        manager.startSync(config) { callbacks.add(it) }
        manager.startSync(config) { callbacks.add(it) }

        assertThat(callbacks).hasSize(2)
        assertThat(callbacks.all { it.isSuccess }).isTrue()
    }

    @Test
    fun `syncs messages for channels in the same proportion concurrently`() = runTest {
        val config = ChannelListConfig.default
        val manager = syncManager()
        val firstLastMessage = createMessage(createdAt = 1, id = 100, tid = 100)
        val secondLastMessage = createMessage(createdAt = 2, id = 200, tid = 200)
        val firstChannel = createChannel(
            id = 1,
            pinnedAt = 0,
            createdAt = 0,
            lastMessage = firstLastMessage
        )
        val secondChannel = createChannel(
            id = 2,
            pinnedAt = 0,
            createdAt = 0,
            lastMessage = secondLastMessage
        )
        val callbacks = mutableListOf<Result<SceytSyncManager.SyncResultData>>()
        val secondMessageSyncStarted = CompletableDeferred<Unit>()
        whenever(channelInteractor.syncChannels(config))
            .thenReturn(
                flowOf(
                    SyncResult.Proportion(listOf(firstChannel, secondChannel)),
                    SyncResult.SuccessfullyFinished
                )
            )
        whenever(
            messageInteractor.syncMessagesAfterMessageId(
                conversationId = firstChannel.id,
                replyInThread = false,
                messageId = firstChannel.lastDisplayedMessageId
            )
        ).thenReturn(
            flow {
                secondMessageSyncStarted.await()
                emit(SyncResult.SuccessfullyFinished)
            }
        )
        whenever(
            messageInteractor.syncMessagesAfterMessageId(
                conversationId = secondChannel.id,
                replyInThread = false,
                messageId = secondChannel.lastDisplayedMessageId
            )
        ).thenReturn(
            flow {
                secondMessageSyncStarted.complete(Unit)
                emit(SyncResult.SuccessfullyFinished)
            }
        )
        whenever(channelSyncStateStore.isMessagesSynced(firstChannel.id, firstLastMessage.id))
            .thenReturn(false)
        whenever(channelSyncStateStore.isMessagesSynced(secondChannel.id, secondLastMessage.id))
            .thenReturn(false)

        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(1000.milliseconds) {
                manager.startSync(config) { callbacks.add(it) }
            }
        }

        assertThat(callbacks).hasSize(1)
        assertThat(callbacks.first().isSuccess).isTrue()
    }

    @Test
    fun `cancelSync cancels in-flight channel sync and allows next sync`() = runTest {
        val config = ChannelListConfig.default
        val manager = syncManager()
        val callbacks = mutableListOf<Result<SceytSyncManager.SyncResultData>>()
        whenever(channelInteractor.syncChannels(config))
            .thenReturn(
                flow { awaitCancellation() },
                flowOf(SyncResult.SuccessfullyFinished)
            )

        val syncJob = launch {
            manager.startSync(config) { callbacks.add(it) }
        }
        runCurrent()

        manager.cancelSync()
        syncJob.join()
        manager.startSync(config) { callbacks.add(it) }

        assertThat(callbacks).hasSize(1)
        assertThat(callbacks.first().isSuccess).isTrue()
    }

    private fun syncManager() = SceytSyncManagerImpl(
        channelInteractor = channelInteractor,
        messageInteractor = messageInteractor,
        channelSyncStateStore = channelSyncStateStore,
    )
}
