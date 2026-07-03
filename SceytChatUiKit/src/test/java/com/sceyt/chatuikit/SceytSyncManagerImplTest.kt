package com.sceyt.chatuikit

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.config.ChannelListConfig
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.SyncResult
import com.sceyt.chatuikit.persistence.interactor.ChannelInteractor
import com.sceyt.chatuikit.persistence.interactor.MessageInteractor
import com.sceyt.chatuikit.persistence.logicimpl.sync.ChannelSyncStateStore
import com.sceyt.chatuikit.services.sync.SceytSyncManager
import com.sceyt.chatuikit.services.sync.SceytSyncManagerImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SceytSyncManagerImplTest {

    private val channelInteractor = mock<ChannelInteractor>()
    private val messageInteractor = mock<MessageInteractor>()
    private val channelSyncStateStore = mock<ChannelSyncStateStore>()
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `conversation sync emits collected messages once after finish`() = runTest {
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
        val emissions = mutableListOf<Pair<Long, List<Long>>>()
        val collector = launch {
            SceytSyncManager.syncChannelMessagesFinished.collect { (syncedChannel, syncedMessages) ->
                emissions.add(syncedChannel.id to syncedMessages.map { it.id })
            }
        }
        runCurrent()

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

        syncManager().syncConversationMessagesAfter(channel.id, fromMessageId = 0)
        runCurrent()
        collector.cancel()

        assertThat(emissions).containsExactly(channel.id to messages.map { it.id })
    }

    @Test
    fun `startSync clears in-process state after channel sync error`() = runTest {
        val config = ChannelListConfig.default
        val callbacks = mutableListOf<Result<SceytSyncManager.SyncResultData>>()
        whenever(channelInteractor.syncChannels(config))
            .thenReturn(flowOf(SyncResult.Error(null)))

        syncManager().startSync(config) { callbacks.add(it) }
        syncManager().startSync(config) { callbacks.add(it) }

        assertThat(callbacks).hasSize(2)
        assertThat(callbacks.all { it.isSuccess }).isTrue()
    }

    private fun syncManager() = SceytSyncManagerImpl(
        channelInteractor = channelInteractor,
        messageInteractor = messageInteractor,
        channelSyncStateStore = channelSyncStateStore,
    )
}
