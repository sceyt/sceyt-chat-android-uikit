package com.sceyt.chatuikit.persistence.logicimpl.channel

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.channel.ChannelListQuery.ChannelListOrder
import com.sceyt.chat.models.channel.ChannelQueryParam
import com.sceyt.chatuikit.SceytChatUIFacade
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.config.ChannelListConfig
import com.sceyt.chatuikit.config.SceytChatUIKitConfig
import com.sceyt.chatuikit.config.SearchChannelParams
import com.sceyt.chatuikit.createChannel
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.persistence.database.dao.ChannelDao
import com.sceyt.chatuikit.persistence.database.dao.ChatUserReactionDao
import com.sceyt.chatuikit.persistence.database.dao.GlobalSearchDao
import com.sceyt.chatuikit.persistence.database.dao.PendingReactionDao
import com.sceyt.chatuikit.persistence.database.dao.UserDao
import com.sceyt.chatuikit.persistence.database.entity.channel.ChannelDb
import com.sceyt.chatuikit.persistence.logic.PersistenceMessagesLogic
import com.sceyt.chatuikit.persistence.mappers.toChannelEntity
import com.sceyt.chatuikit.persistence.repositories.ChannelsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.MockedStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.mockStatic
import org.mockito.kotlin.never
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import kotlin.time.Duration.Companion.milliseconds

internal class PersistenceChannelsLogicImplSearchTest {
    private val repository = mock<ChannelsRepository>()
    private val channelDao = mock<ChannelDao>()
    private val globalSearchDao = mock<GlobalSearchDao>()
    private val usersDao = mock<UserDao>()
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

    @Test
    fun `taking only user ids DB search results leaves the list cache untouched`() = runTest {
        givenUserIdsDbPage(listOf(channel(3)))

        val response = searchByUserIds().first { it is PaginationResponse.DBResponse }

        assertThat((response as PaginationResponse.DBResponse).data.map { it.id }).containsExactly(3L)
        assertCacheUnchanged()
        verifyNoInteractions(repository)
    }

    @Test
    fun `user ids server search returns its saved page without changing the list cache`() = runTest {
        givenUserIdsDbPage(listOf(channel(3)))
        val serverChannels = listOf(channel(2).copy(subject = "updated"), channel(4))
        whenever(repository.searchChannels("hello", config, SearchChannelParams.default, 0))
            .thenReturn(SceytResponse.Success(serverChannels))

        val responses = searchByUserIds().toList()
        val response = responses.last() as PaginationResponse.ServerResponse

        assertThat(responses).hasSize(2)
        assertThat(response.data.data?.map { it.id }).containsExactly(2L, 4L)
        assertThat(response.cacheData.map { it.id }).containsExactly(2L, 4L)
        assertCacheUnchanged()
        verifyBlocking(pendingChannelCoordinator) {
            persistAndMergeFetchedChannels(any(), any(), eq("me"))
        }
        verifyBlocking(repository, never()) { getChannels(any(), any(), any()) }
        verifyBlocking(repository, never()) { loadMoreChannels(any(), any(), any()) }
    }

    @Test
    fun `user ids search requests the server page at its own offset`() = runTest {
        whenever(usersDao.getUserIdsByDisplayName("hello")).thenReturn(emptyList())
        whenever(repository.searchChannels("hello", config, SearchChannelParams.default, 15))
            .thenReturn(SceytResponse.Success(emptyList()))

        searchByUserIds(ignoreDb = true, offset = 15).toList()

        verifyBlocking(repository) { searchChannels("hello", config, SearchChannelParams.default, 15) }
        verifyBlocking(repository, never()) { loadMoreChannels(any(), any(), any()) }
    }

    @Test
    fun `empty and failed user ids searches preserve the list cache`() = runTest {
        givenUserIdsDbPage(emptyList())
        val emptyDb = searchByUserIds().first { it is PaginationResponse.DBResponse }
        assertThat((emptyDb as PaginationResponse.DBResponse).data).isEmpty()
        assertCacheUnchanged()

        whenever(repository.searchChannels("hello", config, SearchChannelParams.default, 0))
            .thenReturn(SceytResponse.Success(emptyList()), SceytResponse.Error())
        repeat(2) {
            searchByUserIds(ignoreDb = true).toList()
            assertCacheUnchanged()
        }
        verifyNoInteractions(pendingChannelCoordinator)
    }

    @Test
    fun `skip cache load leaves the list cache intact and never moves the list cursor`() = runTest {
        givenSkipCacheDbPage(listOf(channel(3)))
        val serverChannels = listOf(channel(2).copy(subject = "updated"), channel(4))
        whenever(repository.searchChannels("", config, SearchChannelParams.default, 0))
            .thenReturn(SceytResponse.Success(serverChannels))

        val responses = loadSkipCache().toList()
        val db = responses.first() as PaginationResponse.DBResponse
        val server = responses.last() as PaginationResponse.ServerResponse

        assertThat(responses).hasSize(2)
        assertThat(db.data.map { it.id }).containsExactly(3L)
        assertThat(server.cacheData.map { it.id }).containsExactly(2L, 4L)
        assertCacheUnchanged()
        verifyBlocking(repository, never()) { getChannels(any(), any(), any()) }
        verifyBlocking(repository, never()) { loadMoreChannels(any(), any(), any()) }
    }

    @Test
    fun `skip cache load requests the server page at its own offset`() = runTest {
        givenSkipCacheDbPage(emptyList(), offset = 15)
        whenever(repository.searchChannels("", config, SearchChannelParams.default, 15))
            .thenReturn(SceytResponse.Success(emptyList()))

        loadSkipCache(offset = 15).toList()

        verifyBlocking(repository) { searchChannels("", config, SearchChannelParams.default, 15) }
        verifyBlocking(repository, never()) { loadMoreChannels(any(), any(), any()) }
    }

    @Test
    fun `failed skip cache server page keeps the list cache and reports the error`() = runTest {
        givenSkipCacheDbPage(listOf(channel(3)))
        whenever(repository.searchChannels("", config, SearchChannelParams.default, 0))
            .thenReturn(SceytResponse.Error())

        val server = loadSkipCache().toList().last() as PaginationResponse.ServerResponse

        assertThat(server.data).isInstanceOf(SceytResponse.Error::class.java)
        assertThat(server.cacheData).isEmpty()
        assertCacheUnchanged()
        verifyNoInteractions(pendingChannelCoordinator)
    }

    @Test
    fun `skip cache load waits for connection before requesting the server page`() = runTest {
        givenSkipCacheDbPage(listOf(channel(3)))

        disconnectedChatClient().use {
            val responses = mutableListOf<PaginationResponse<SceytChannel>>()
            val completed = withTimeoutOrNull(1_000.milliseconds) {
                loadSkipCache(awaitForConnection = true).toList(responses)
            }

            assertThat(completed).isNull()
            assertThat(responses.single()).isInstanceOf(PaginationResponse.DBResponse::class.java)
            verifyBlocking(repository, never()) { searchChannels(any(), any(), any(), any()) }
        }
    }

    private suspend fun givenSkipCacheDbPage(channels: List<SceytChannel>, offset: Int = 0) {
        whenever(channelDao.getChannels(
            limit = config.queryLimit,
            offset = offset,
            types = config.types,
            orderByLastMessage = true,
            onlyMine = true,
        )).thenReturn(channels.map(::channelDb))
    }

    private fun loadSkipCache(offset: Int = 0, awaitForConnection: Boolean = false) =
        logic().loadChannelsSkipCache(
            offset = offset,
            searchQuery = "",
            onlyMine = true,
            awaitForConnection = awaitForConnection,
            config = config,
        )

    private fun disconnectedChatClient(): MockedStatic<ChatClient> = mockStatic<ChatClient>().also {
        it.`when`<ChatClient> { ChatClient.getClient() }.thenReturn(mock())
        it.`when`<Boolean> { ChatClient.isInitialized() }.thenReturn(false)
    }

    private suspend fun givenUserIdsDbPage(channels: List<SceytChannel>) {
        whenever(usersDao.getUserIdsByDisplayName("hello")).thenReturn(listOf("display-name-match"))
        whenever(globalSearchDao.searchChannelsByUserIds(
            query = "hello",
            userIds = listOf("contact-match", "display-name-match"),
            limit = config.queryLimit,
            offset = 0,
            onlyMine = false,
            types = config.types,
            orderByLastMessage = true,
            directType = "direct",
        )).thenReturn(channels.map(::channelDb))
    }

    private fun channelDb(channel: SceytChannel) = ChannelDb(
        channelEntity = channel.toChannelEntity(),
        members = null,
        lastMessage = null,
        createdBy = null,
        newReactions = null,
        draftMessage = null,
        pendingReactions = null,
    )

    private suspend fun searchByUserIds(
        ignoreDb: Boolean = false,
        offset: Int = 0,
    ) = logic().searchChannelsWithUserIds(
        offset = offset,
        searchQuery = "hello",
        userIds = listOf("contact-match"),
        config = config,
        includeSearchByUserDisplayName = true,
        onlyMine = false,
        ignoreDb = ignoreDb,
        loadKey = null,
        directChatType = "direct",
    )

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
        globalSearchDao = globalSearchDao,
        usersDao = usersDao,
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
