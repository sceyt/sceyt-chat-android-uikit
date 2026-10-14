package com.sceyt.chatuikit.presentation.components.global_search

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.ChatClient
import com.sceyt.chatuikit.SceytChatUIFacade
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.config.ChannelListConfig
import com.sceyt.chatuikit.createChannel
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.SyncResult
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.data.models.search.GlobalSearchPage
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.persistence.database.SceytDatabase
import com.sceyt.chatuikit.persistence.database.dao.FileChecksumDao
import com.sceyt.chatuikit.persistence.di.CoroutineContextType
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.interactor.ChannelInteractor
import com.sceyt.chatuikit.persistence.interactor.GlobalSearchDataSource
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.persistence.logic.GlobalSearchLocalInteractor
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelsCache
import com.sceyt.chatuikit.persistence.mappers.toChannelEntity
import com.sceyt.chatuikit.presentation.components.global_search.channels.ChannelsSearchViewModel
import com.sceyt.chatuikit.presentation.components.global_search.chats.ChatsSearchViewModel
import com.sceyt.chatuikit.services.sync.SceytSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.mockito.MockedStatic
import org.mockito.kotlin.mock
import org.mockito.kotlin.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.coroutines.CoroutineContext

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class GlobalSearchChannelPaginationTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()
    private val viewModelStore = ViewModelStore()
    private val channelsCache = ChannelsCache()
    private val channelInteractor = mock<ChannelInteractor>()
    private lateinit var database: SceytDatabase
    private lateinit var chatClientStaticMock: MockedStatic<ChatClient>

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        chatClientStaticMock = mockStatic<ChatClient>()
        chatClientStaticMock.`when`<ChatClient> { ChatClient.getClient() }
            .thenReturn(mock<ChatClient>())

        stopKoin()
        SceytKoinApp.koinApp = startKoin {
            modules(module {
                single<ChannelInteractor> { channelInteractor }
                single<SceytChatUIFacade> { mock() }
                single<FileTransferService> { mock() }
                single<PersistenceAttachmentLogic> { mock() }
                single<FileChecksumDao> { mock() }
                single<CoroutineContext>(named(CoroutineContextType.SingleThreaded)) { dispatcher }
            })
        }
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            SceytDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        viewModelStore.clear()
        database.close()
        chatClientStaticMock.close()
        SceytKoinApp.koinApp = null
        stopKoin()
        Dispatchers.resetMain()
    }

    @Test
    fun `blank channels query loads every channel once when ordering is unchanged`() = runTest(dispatcher) {
        val ids = loadRecentChannels(GlobalSearchTab.Channels, updateLastMessage = false)

        assertThat(ids).containsExactlyElementsIn(1L..32L).inOrder()
    }

    @Test
    fun `blank chats query loads every channel once when ordering is unchanged`() = runTest(dispatcher) {
        val ids = loadRecentChannels(GlobalSearchTab.Chats, updateLastMessage = false)

        assertThat(ids).containsExactlyElementsIn(1L..32L).inOrder()
    }

    @Test
    fun `blank channels query does not duplicate channels after an unloaded channel receives a message`() = runTest(dispatcher) {
        val ids = loadRecentChannels(GlobalSearchTab.Channels, updateLastMessage = true)

        assertThat(ids).containsNoDuplicates()
    }

    @Test
    fun `blank chats query does not duplicate channels after an unloaded channel receives a message`() = runTest(dispatcher) {
        val ids = loadRecentChannels(GlobalSearchTab.Chats, updateLastMessage = true)

        assertThat(ids).containsNoDuplicates()
    }

    @Test
    fun `channel and sync events do not replace typed search results`() = runTest(dispatcher) {
        for (tab in listOf(GlobalSearchTab.Channels, GlobalSearchTab.Chats)) {
            val search = openRecentSearch(tab, GlobalSearchSessionState(query = "missing", activeTab = tab))
            assertThat(search.pages.first().ids).isEmpty()

            moveChannel(tab, id = 20, lastMessageAt = 2_000, emitEvent = true)
            SceytSyncManager.syncChannelsResult_.tryEmit(SyncResult.SuccessfullyFinished)
            runCurrent()

            assertThat(search.pages.first().ids).isEmpty()
            assertThat(search.dataSource.recentCalls).isEmpty()
        }
    }

    @Test
    fun `channel and sync events do not replace selected member results`() = runTest(dispatcher) {
        for (tab in listOf(GlobalSearchTab.Channels, GlobalSearchTab.Chats)) {
            val search = openRecentSearch(
                tab,
                GlobalSearchSessionState(selectedMember = SceytUser("member"), activeTab = tab),
            )
            assertThat(search.pages.first().ids).isEmpty()

            moveChannel(tab, id = 20, lastMessageAt = 2_000, emitEvent = true)
            SceytSyncManager.syncChannelsResult_.tryEmit(SyncResult.SuccessfullyFinished)
            runCurrent()

            assertThat(search.pages.first().ids).isEmpty()
            assertThat(search.dataSource.recentCalls).isEmpty()
        }
    }

    private suspend fun loadRecentChannels(
        tab: GlobalSearchTab,
        updateLastMessage: Boolean,
    ): List<Long> {
        val search = openRecentSearch(tab)
        assertThat(search.pages.first().ids).containsExactlyElementsIn(1L..15L).inOrder()
        if (updateLastMessage) {
            // Channel 20 moves ahead of page one, shifting channel 15 into the next OFFSET page.
            moveChannel(tab, id = 20, lastMessageAt = 2_000, emitEvent = false)
        }
        return loadRemaining(search)
    }

    private suspend fun loadRemaining(search: RecentSearch): List<Long> {
        search.loadMore()
        search.pages.first()
        search.loadMore()
        return search.pages.first().ids
    }

    private fun channel(tab: GlobalSearchTab, id: Long, lastMessageAt: Long = 1_000 - id): SceytChannel {
        val channelTypes = SceytChatUIKit.config.channelTypesConfig
        val type = if (tab == GlobalSearchTab.Channels) channelTypes.broadcast else channelTypes.direct
        return createChannel(id = id, pinnedAt = 0, createdAt = 1_000 - id)
            .copy(
                type = type,
                uri = "channel://$id",
                userRole = "member",
                lastMessage = createMessage(createdAt = lastMessageAt, id = id, tid = id),
            )
    }

    private suspend fun moveChannel(tab: GlobalSearchTab, id: Long, lastMessageAt: Long, emitEvent: Boolean) {
        database.channelDao().updateLastMessage(channelId = id, lastMessageTid = id, lastMessageAt = lastMessageAt)
        if (emitEvent) channelsCache.upsertChannel(channel(tab, id, lastMessageAt))
    }

    private suspend fun openRecentSearch(
        tab: GlobalSearchTab,
        initialState: GlobalSearchSessionState = GlobalSearchSessionState(activeTab = tab),
    ): RecentSearch {
        val channelDao = database.channelDao()
        val channels = (1L..32L).map { channel(tab, it) }
        channelDao.insertChannelsAndLinks(
            channels = channels.map { it.toChannelEntity() },
            userChatLinks = emptyList(),
        )
        channelsCache.addAll(ChannelListConfig.default, channels, checkDifference = false)
        whenever(channelInteractor.loadChannels(any(), any(), anyOrNull(), any(), any(), any(), any()))
            .thenReturn(emptyFlow())
        val dataSource = ControlledDataSource(GlobalSearchLocalInteractor(
            channelDao = channelDao,
            messageDao = database.messageDao(),
            globalSearchDao = database.globalSearchDao(),
            userDao = database.userDao(),
        ))
        val session = GlobalSearchSessionStore(initialState)
        val pages: Flow<RecentPage>
        val loadMore: () -> Unit
        when (tab) {
            GlobalSearchTab.Channels -> {
                val viewModel = ChannelsSearchViewModel(session, dataSource, dispatcher)
                viewModelStore.put("search", viewModel)
                pages = viewModel.state.filter { !it.isLoading && !it.isLoadingMore }
                    .map { state ->
                        RecentPage(state.listItems.channelIds())
                    }
                loadMore = viewModel::loadMore
            }

            GlobalSearchTab.Chats -> {
                val viewModel = ChatsSearchViewModel(session, dataSource, dispatcher)
                viewModelStore.put("search", viewModel)
                pages = viewModel.state.filter { !it.isLoading && !it.isLoadingMore }
                    .map { state ->
                        RecentPage(state.listItems.channelIds())
                    }
                loadMore = viewModel::loadMore
            }

            else -> error("Expected a tab containing recent channels")
        }

        return RecentSearch(pages, loadMore, dataSource)
    }

    private fun List<GlobalSearchListItem>.channelIds() =
        filterIsInstance<GlobalSearchListItem.ChannelItem>().map { it.channel.id }

    private data class RecentPage(val ids: List<Long>)

    private data class RecentSearch(
        val pages: Flow<RecentPage>,
        val loadMore: () -> Unit,
        val dataSource: ControlledDataSource,
    )

    private class ControlledDataSource(private val delegate: GlobalSearchDataSource) : GlobalSearchDataSource by delegate {
        val recentCalls = mutableListOf<Pair<Int, Int>>()

        override suspend fun getRecentChannels(offset: Int, limit: Int): GlobalSearchPage<SceytChannel> =
            readRecent(offset, limit) { delegate.getRecentChannels(offset, limit) }

        override suspend fun getRecentChats(offset: Int, limit: Int): GlobalSearchPage<SceytChannel> =
            readRecent(offset, limit) { delegate.getRecentChats(offset, limit) }

        private suspend fun readRecent(
            offset: Int,
            limit: Int,
            read: suspend () -> GlobalSearchPage<SceytChannel>,
        ): GlobalSearchPage<SceytChannel> {
            recentCalls.add(offset to limit)
            return read()
        }
    }
}
