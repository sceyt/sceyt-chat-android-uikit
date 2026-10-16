package com.sceyt.chatuikit.persistence.logicimpl.channel

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.channel.ChannelListQuery.ChannelListOrder
import com.sceyt.chat.models.channel.ChannelQueryParam
import com.sceyt.chatuikit.SceytChatUIFacade
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.config.ChannelListConfig
import com.sceyt.chatuikit.config.SceytChatUIKitConfig
import com.sceyt.chatuikit.config.SearchChannelParams
import com.sceyt.chatuikit.createChannel
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.persistence.database.dao.ChannelDao
import com.sceyt.chatuikit.persistence.database.dao.ChatUserReactionDao
import com.sceyt.chatuikit.persistence.database.dao.PendingReactionDao
import com.sceyt.chatuikit.persistence.logic.PersistenceMessagesLogic
import com.sceyt.chatuikit.persistence.repositories.ChannelsRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

internal class PersistenceChannelsLogicImplSearchTest {
    private val repository = mock<ChannelsRepository>()
    private val channelDao = mock<ChannelDao>()
    private val chatUserReactionDao = mock<ChatUserReactionDao>()
    private val pendingReactionDao = mock<PendingReactionDao>()
    private val pendingChannelCoordinator = mock<PendingChannelCoordinator>()
    private val messageLogic = mock<PersistenceMessagesLogic>()
    private val facade = mock<SceytChatUIFacade>()
    private val cache = ChannelsCache()
    private val config = ChannelListConfig(
        types = listOf("broadcast"),
        order = ChannelListOrder.ListQueryChannelOrderLastMessage,
        queryLimit = 15,
        queryParam = ChannelQueryParam(1, 10, 1, true),
    )

    @Before
    fun setUp() = runTest {
        stopKoin()
        SceytChatUIKit.config = SceytChatUIKitConfig()
        whenever(facade.myId).thenReturn("me")
        SceytKoinApp.koinApp = startKoin {
            modules(module {
                single { messageLogic }
                single { facade }
            })
        }
        whenever(chatUserReactionDao.getChannelUserReactions(any())).thenReturn(emptyList())
        whenever(pendingReactionDao.getAllByChannelId(any())).thenReturn(emptyList())
        whenever(channelDao.getChannelsLastMessageTIds(any())).thenReturn(emptyList())
        whenever(pendingChannelCoordinator.persistAndMergeFetchedChannels(any(), any(), anyOrNull()))
            .thenAnswer { it.getArgument<List<SceytChannel>>(0) }
        cache.addAll(config, listOf(channel(1), channel(2)), checkDifference = false)
    }

    @After
    fun tearDown() {
        SceytKoinApp.koinApp = null
        stopKoin()
        SceytChatUIKit.config = SceytChatUIKitConfig()
    }

    @Test
    fun `search saves server results without replacing or updating the channel list cache`() = runTest {
        val serverChannels = listOf(channel(2).copy(subject = "updated"), channel(3))
        whenever(repository.searchChannels("hello", config, SearchChannelParams.default))
            .thenReturn(SceytResponse.Success(serverChannels))

        val response = logic().searchChannels("hello", config)

        assertThat(response.data?.map { it.id }).containsExactly(2L, 3L)
        assertThat(response.data?.first()?.subject).isEqualTo("updated")
        assertCacheUnchanged()
        verifyBlocking(pendingChannelCoordinator) {
            persistAndMergeFetchedChannels(any(), any(), eq("me"))
        }
        verifyBlocking(repository, never()) { getChannels(any(), any(), any()) }
        verifyBlocking(repository, never()) { loadMoreChannels(any(), any(), any()) }
    }

    @Test
    fun `empty search results do not clear the channel list cache`() = runTest {
        whenever(repository.searchChannels("hello", config, SearchChannelParams.default))
            .thenReturn(SceytResponse.Success(emptyList()))

        val response = logic().searchChannels("hello", config)

        assertThat(response.data).isEmpty()
        assertCacheUnchanged()
        verifyNoInteractions(pendingChannelCoordinator)
    }

    @Test
    fun `failed search preserves the channel list cache and does not persist results`() = runTest {
        whenever(repository.searchChannels("hello", config, SearchChannelParams.default))
            .thenReturn(SceytResponse.Error())

        val response = logic().searchChannels("hello", config)

        assertThat(response).isInstanceOf(SceytResponse.Error::class.java)
        assertCacheUnchanged()
        verifyNoInteractions(channelDao, pendingChannelCoordinator, messageLogic)
    }

    private suspend fun assertCacheUnchanged() {
        assertThat(cache.getData(config).map { it.id }).containsExactly(1L, 2L)
        assertThat(cache.getOneOf(2, config)?.subject).isEqualTo("channel 2")
    }

    private fun channel(id: Long) = createChannel(id = id, pinnedAt = 0, createdAt = id)
        .copy(type = "broadcast", subject = "channel $id")

    private fun logic() = PersistenceChannelsLogicImpl(
        context = mock(),
        channelsRepository = repository,
        channelDao = channelDao,
        globalSearchDao = mock(),
        usersDao = mock(),
        messageDao = mock(),
        rangeDao = mock(),
        draftMessageDao = mock(),
        chatUserReactionDao = chatUserReactionDao,
        pendingReactionDao = pendingReactionDao,
        channelsCache = cache,
        channelSyncStateStore = mock(),
        pendingChannelCoordinator = pendingChannelCoordinator,
        insertChannelWithMembersUseCase = mock(),
    )
}
