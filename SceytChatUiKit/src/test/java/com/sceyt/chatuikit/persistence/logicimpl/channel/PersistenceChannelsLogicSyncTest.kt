package com.sceyt.chatuikit.persistence.logicimpl.channel

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.channel.ChannelListQuery.ChannelListOrder
import com.sceyt.chat.models.channel.ChannelQueryParam
import com.sceyt.chatuikit.SceytChatUIFacade
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.config.ChannelListConfig
import com.sceyt.chatuikit.config.SceytChatUIKitConfig
import com.sceyt.chatuikit.createChannel
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.SyncResult
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.logger.SceytLogLevel
import com.sceyt.chatuikit.logger.SceytLoggerImpl
import com.sceyt.chatuikit.persistence.database.dao.ChannelDao
import com.sceyt.chatuikit.persistence.database.dao.ChatUserReactionDao
import com.sceyt.chatuikit.persistence.database.dao.PendingReactionDao
import com.sceyt.chatuikit.persistence.logic.PersistenceMessagesLogic
import com.sceyt.chatuikit.persistence.repositories.ChannelsRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.mockingDetails
import org.mockito.kotlin.never
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import kotlin.time.Duration.Companion.milliseconds

internal class PersistenceChannelsLogicSyncTest {
    private val channelsRepository = mock<ChannelsRepository>()
    private val channelDao = mock<ChannelDao>()
    private val channelsCache = mock<ChannelsCache>()
    private val chatUserReactionDao = mock<ChatUserReactionDao>()
    private val pendingReactionDao = mock<PendingReactionDao>()
    private val pendingChannelCoordinator = mock<PendingChannelCoordinator>()
    private val messageLogic = mock<PersistenceMessagesLogic>()
    private val chatUIFacade = mock<SceytChatUIFacade>()
    private val config = ChannelListConfig(
        types = emptyList(),
        order = ChannelListOrder.ListQueryChannelOrderLastMessage,
        queryLimit = 2,
        queryParam = ChannelQueryParam(0, 0, 0, false)
    )
    private val serverPage = SyncResult.Proportion(listOf(myChannel(1), myChannel(2)))

    @Before
    fun setUp() {
        stopKoin()
        SceytLog.setLogger(SceytLogLevel.None) { _, _, _, _ -> }
        SceytChatUIKit.config = SceytChatUIKitConfig()
        whenever(chatUIFacade.myId).thenReturn("me")
        SceytKoinApp.koinApp = startKoin {
            modules(
                module {
                    single { messageLogic }
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

    private fun logic() = PersistenceChannelsLogicImpl(
        context = mock(),
        channelsRepository = channelsRepository,
        channelDao = channelDao,
        globalSearchDao = mock(),
        usersDao = mock(),
        messageDao = mock(),
        rangeDao = mock(),
        draftMessageDao = mock(),
        chatUserReactionDao = chatUserReactionDao,
        pendingReactionDao = pendingReactionDao,
        channelsCache = channelsCache,
        channelSyncStateStore = mock(),
        pendingChannelCoordinator = pendingChannelCoordinator,
        insertChannelWithMembersUseCase = mock(),
    )

    private fun myChannel(
        id: Long,
        hidden: Boolean = false,
        archived: Boolean = false,
        userRole: String? = "owner",
    ): SceytChannel = createChannel(id = id, pinnedAt = 0, createdAt = id)
        .copy(userRole = userRole, hidden = hidden, archived = archived)

    private fun serverError(type: String?): SceytException {
        val exception = mock<SceytException>()
        whenever(exception.type).thenReturn(type)
        return exception
    }

    private suspend fun givenSyncMisses(missing: Map<Long, SceytResponse<SceytChannel>>) {
        whenever(channelsRepository.getAllChannels(config.queryLimit))
            .thenReturn(flowOf(serverPage, SyncResult.SuccessfullyFinished))
        whenever(
            channelDao.getNotExistingChannelIdsByIdsAndTypes(
                eq(listOf(1L, 2L)), any(), eq(true), any()
            )
        ).thenReturn(missing.keys.toList())
        missing.forEach { (id, response) ->
            whenever(channelsRepository.getChannel(id)).thenReturn(response)
        }
        whenever(chatUserReactionDao.getChannelUserReactions(any())).thenReturn(emptyList())
        whenever(pendingReactionDao.getAllByChannelId(any())).thenReturn(emptyList())
        whenever(channelDao.getChannelsLastMessageTIds(any())).thenReturn(emptyList())
        whenever(
            pendingChannelCoordinator.persistAndMergeFetchedChannels(
                any(),
                any(),
                anyOrNull()
            )
        )
            .thenAnswer { it.getArgument<List<SceytChannel>>(0) }
    }

    private suspend fun givenSyncMisses(id: Long, response: SceytResponse<SceytChannel>) =
        givenSyncMisses(mapOf(id to response))

    private fun persistedChannelIds(): List<List<Long>> {
        val captor = argumentCaptor<List<SceytChannel>>()
        verifyBlocking(pendingChannelCoordinator, atLeastOnce()) {
            persistAndMergeFetchedChannels(captor.capture(), any(), anyOrNull())
        }
        return captor.allValues.map { channels -> channels.map { it.id } }
    }

    private fun syncedToMessageLogicIds(): List<List<Long>> {
        val captor = argumentCaptor<List<SceytChannel>>()
        verifyBlocking(messageLogic, atLeastOnce()) {
            onSyncedChannels(captor.capture())
        }
        return captor.allValues.map { channels -> channels.map { it.id } }
    }

    private fun cacheUpdatedChannelIds(): List<List<Long>> =
        mockingDetails(channelsCache).invocations
            .filter { it.method.name == "updateChannel" }
            .map { invocation ->
                (invocation.rawArguments[1] as Array<*>).map { (it as SceytChannel).id }
            }

    @Test
    fun `channel skipped by offset shift but still on server is kept and persisted`() = runTest {
        givenSyncMisses(3, SceytResponse.Success(myChannel(3)))

        logic().syncChannels(config).toList()

        verifyBlocking(channelDao, never()) { deleteAllChannelsAndLinksById(any()) }
        assertThat(persistedChannelIds()).containsExactly(listOf(1L, 2L), listOf(3L)).inOrder()
        assertThat(syncedToMessageLogicIds()).containsExactly(listOf(1L, 2L), listOf(3L)).inOrder()
        assertThat(cacheUpdatedChannelIds()).containsExactly(listOf(1L, 2L), listOf(3L)).inOrder()
    }

    @Test
    fun `recovered channel does not produce an extra proportion`() = runTest {
        givenSyncMisses(3, SceytResponse.Success(myChannel(3)))

        val results = logic().syncChannels(config).toList()

        assertThat(results).containsExactly(serverPage, SyncResult.SuccessfullyFinished).inOrder()
    }

    @Test
    fun `channel not found on server is deleted and not persisted`() = runTest {
        givenSyncMisses(3, SceytResponse.Error(serverError("NotFound")))

        val results = logic().syncChannels(config).toList()

        verifyBlocking(channelDao) { deleteAllChannelsAndLinksById(listOf(3L)) }
        assertThat(persistedChannelIds()).containsExactly(listOf(1L, 2L))
        assertThat(results).containsExactly(serverPage, SyncResult.SuccessfullyFinished).inOrder()
    }

    @Test
    fun `channel not allowed on server is deleted`() = runTest {
        givenSyncMisses(3, SceytResponse.Error(serverError("NotAllowed")))

        logic().syncChannels(config).toList()

        verifyBlocking(channelDao) { deleteAllChannelsAndLinksById(listOf(3L)) }
    }

    @Test
    fun `channel is kept when confirmation fails with resendable error`() = runTest {
        givenSyncMisses(3, SceytResponse.Error(serverError("TooManyRequests")))

        logic().syncChannels(config).toList()

        verifyBlocking(channelDao, never()) { deleteAllChannelsAndLinksById(any()) }
        assertThat(persistedChannelIds()).containsExactly(listOf(1L, 2L))
    }

    @Test
    fun `channel is kept when confirmation fails without error type`() = runTest {
        givenSyncMisses(3, SceytResponse.Error(serverError(null)))

        logic().syncChannels(config).toList()

        verifyBlocking(channelDao, never()) { deleteAllChannelsAndLinksById(any()) }
        assertThat(persistedChannelIds()).containsExactly(listOf(1L, 2L))
    }

    @Test
    fun `channel hidden on server is deleted`() = runTest {
        givenSyncMisses(3, SceytResponse.Success(myChannel(3, hidden = true)))

        logic().syncChannels(config).toList()

        verifyBlocking(channelDao) { deleteAllChannelsAndLinksById(listOf(3L)) }
        assertThat(persistedChannelIds()).containsExactly(listOf(1L, 2L))
    }

    @Test
    fun `channel archived on server is deleted`() = runTest {
        givenSyncMisses(3, SceytResponse.Success(myChannel(3, archived = true)))

        logic().syncChannels(config).toList()

        verifyBlocking(channelDao) { deleteAllChannelsAndLinksById(listOf(3L)) }
        assertThat(persistedChannelIds()).containsExactly(listOf(1L, 2L))
    }

    @Test
    fun `channel where user has no role anymore is deleted`() = runTest {
        givenSyncMisses(3, SceytResponse.Success(myChannel(3, userRole = "")))

        logic().syncChannels(config).toList()

        verifyBlocking(channelDao) { deleteAllChannelsAndLinksById(listOf(3L)) }
        assertThat(persistedChannelIds()).containsExactly(listOf(1L, 2L))
    }

    @Test
    fun `each missing channel is resolved independently`() = runTest {
        givenSyncMisses(
            mapOf(
                3L to SceytResponse.Success(myChannel(3)),
                4L to SceytResponse.Error(serverError("NotFound")),
                5L to SceytResponse.Error(serverError(null)),
                6L to SceytResponse.Success(myChannel(6, hidden = true)),
            )
        )

        logic().syncChannels(config).toList()

        verifyBlocking(channelDao) { deleteAllChannelsAndLinksById(listOf(4L, 6L)) }
        assertThat(persistedChannelIds()).containsExactly(listOf(1L, 2L), listOf(3L)).inOrder()
    }

    @Test
    fun `missing channels are confirmed concurrently`() = runTest {
        givenSyncMisses(
            mapOf(
                3L to SceytResponse.Success(myChannel(3)),
                4L to SceytResponse.Error(serverError("NotFound")),
            )
        )
        val secondConfirmationStarted = CompletableDeferred<Unit>()
        whenever(channelsRepository.getChannel(3)).doSuspendableAnswer {
            secondConfirmationStarted.await()
            SceytResponse.Success(myChannel(3))
        }
        whenever(channelsRepository.getChannel(4)).doSuspendableAnswer {
            secondConfirmationStarted.complete(Unit)
            SceytResponse.Error(serverError("NotFound"))
        }

        withTimeout(1_000.milliseconds) { logic().syncChannels(config).toList() }

        verifyBlocking(channelDao) { deleteAllChannelsAndLinksById(listOf(4L)) }
        assertThat(persistedChannelIds()).containsExactly(listOf(1L, 2L), listOf(3L)).inOrder()
    }

    @Test
    fun `no missing channels skips server confirmation`() = runTest {
        givenSyncMisses(emptyMap())

        val results = logic().syncChannels(config).toList()

        verifyBlocking(channelsRepository, never()) { getChannel(any()) }
        verifyBlocking(channelDao, never()) { deleteAllChannelsAndLinksById(any()) }
        assertThat(results).containsExactly(serverPage, SyncResult.SuccessfullyFinished).inOrder()
    }
}
