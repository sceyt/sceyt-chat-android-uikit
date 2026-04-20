package com.sceyt.chatuikit.presentation.components.global_search.channels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.ChatClient
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.data.models.search.GlobalSearchAttachmentKind
import com.sceyt.chatuikit.data.models.search.GlobalSearchAttachmentResult
import com.sceyt.chatuikit.data.models.search.GlobalSearchMessageResult
import com.sceyt.chatuikit.data.models.search.GlobalSearchPage
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.persistence.database.dao.FileChecksumDao
import com.sceyt.chatuikit.persistence.di.CoroutineContextType
import com.sceyt.chatuikit.persistence.interactor.ChannelInteractor
import com.sceyt.chatuikit.persistence.interactor.GlobalSearchDataSource
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchListItem
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchSessionState
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchSessionStore
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.mockStatic
import org.mockito.kotlin.whenever
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
class ChannelsSearchViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()
    private val channelInteractor = mock<ChannelInteractor>()
    private val fileTransferService = mock<FileTransferService>()
    private val attachmentLogic = mock<PersistenceAttachmentLogic>()
    private val fileChecksumDao = mock<FileChecksumDao>()
    private lateinit var chatClientStaticMock: MockedStatic<ChatClient>

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        chatClientStaticMock = mockStatic<ChatClient>()
        val mockClient = mock<ChatClient>()
        chatClientStaticMock.`when`<ChatClient> { ChatClient.getClient() }.thenReturn(mockClient)
        Mockito.doNothing().`when`(mockClient).addMessageListener(Mockito.anyString(), Mockito.any())

        stopKoin()
        SceytKoinApp.koinApp = startKoin {
            modules(module {
                single<ChannelInteractor> { channelInteractor }
                single<FileTransferService> { fileTransferService }
                single<PersistenceAttachmentLogic> { attachmentLogic }
                single<FileChecksumDao> { fileChecksumDao }
                single<CoroutineContext>(named(CoroutineContextType.SingleThreaded)) { dispatcher }
            })
        }
        runBlocking {
            whenever(
                channelInteractor.loadChannels(any(), any(), anyOrNull(), any(), any(), any(), any())
            ).thenReturn(emptyFlow())
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        chatClientStaticMock.close()
        SceytKoinApp.koinApp = null
        stopKoin()
    }

    // ─── Blank query (recent channels) ───────────────────────────────────────────

    @Test
    fun `blank query first page loads recent channels`() = runTest(dispatcher) {
        val dataSource = ChannelsFakeDataSource()
        dataSource.enqueueChannels(fakeChannelPage(count = 5, hasMore = false))

        val vm = vmForTab(GlobalSearchTab.Channels, dataSource)
        advanceUntilIdle()

        val state = vm.state.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.listItems.filterIsInstance<GlobalSearchListItem.ChannelItem>()).hasSize(5)
        assertThat(state.hasMore).isFalse()
    }

    @Test
    fun `blank query offset equals loaded channel count, not listItems size`() = runTest(dispatcher) {
        val dataSource = ChannelsFakeDataSource()
        dataSource.enqueueChannels(fakeChannelPage(count = 5, hasMore = true))

        val vm = vmForTab(GlobalSearchTab.Channels, dataSource)
        advanceUntilIdle()

        // listItems has header + 5 channels = 6, but offset must be 5 (data count only)
        assertThat(vm.state.value.offset).isEqualTo(5)
    }

    @Test
    fun `blank query loadMore passes state offset as data source offset`() = runTest(dispatcher) {
        val dataSource = ChannelsFakeDataSource()
        dataSource.enqueueChannels(fakeChannelPage(count = 5, hasMore = true))
        dataSource.enqueueChannels(fakeChannelPage(count = 3, hasMore = false))

        val vm = vmForTab(GlobalSearchTab.Channels, dataSource)
        advanceUntilIdle()
        vm.loadMore()
        advanceUntilIdle()

        assertThat(dataSource.channelCalls[1].offset).isEqualTo(5)
    }

    @Test
    fun `blank query loadMore appends channels and advances offset`() = runTest(dispatcher) {
        val dataSource = ChannelsFakeDataSource()
        dataSource.enqueueChannels(fakeChannelPage(count = 5, hasMore = true))
        dataSource.enqueueChannels(fakeChannelPage(count = 3, hasMore = false))

        val vm = vmForTab(GlobalSearchTab.Channels, dataSource)
        advanceUntilIdle()
        vm.loadMore()
        advanceUntilIdle()

        val state = vm.state.value
        assertThat(state.listItems.filterIsInstance<GlobalSearchListItem.ChannelItem>()).hasSize(8)
        assertThat(state.offset).isEqualTo(8)
        assertThat(state.hasMore).isFalse()
    }

    @Test
    fun `blank query loadMore is no-op when hasMore is false`() = runTest(dispatcher) {
        val dataSource = ChannelsFakeDataSource()
        dataSource.enqueueChannels(fakeChannelPage(count = 3, hasMore = false))

        val vm = vmForTab(GlobalSearchTab.Channels, dataSource)
        advanceUntilIdle()
        vm.loadMore()
        advanceUntilIdle()

        assertThat(dataSource.channelCalls).hasSize(1)
    }

    @Test
    fun `blank query loadMore is no-op while first page is still loading`() = runTest(dispatcher) {
        val dataSource = ChannelsFakeDataSource()
        dataSource.enqueueChannels(fakeChannelPage(count = 5, hasMore = true))

        val vm = vmForTab(GlobalSearchTab.Channels, dataSource)
        vm.loadMore() // isLoading = true, coroutines not yet advanced
        advanceUntilIdle()

        assertThat(dataSource.channelCalls).hasSize(1)
    }

    // ─── Selected member (messages only) ─────────────────────────────────────────

    @Test
    fun `selected member first page fetches messages with correct senderId`() = runTest(dispatcher) {
        val dataSource = ChannelsFakeDataSource()
        dataSource.enqueueMessages(fakeMessagePage(count = 3, hasMore = false))

        val vm = vmForState(
            GlobalSearchSessionState(
                activeTab = GlobalSearchTab.Channels,
                selectedMember = SceytUser("user-42")
            ),
            dataSource
        )
        advanceUntilIdle()

        assertThat(dataSource.messageCalls[0].senderId).isEqualTo("user-42")
        assertThat(vm.state.value.listItems.filterIsInstance<GlobalSearchListItem.MessageItem>()).hasSize(3)
    }

    @Test
    fun `selected member offset equals message count, not inflated by header`() = runTest(dispatcher) {
        val dataSource = ChannelsFakeDataSource()
        dataSource.enqueueMessages(fakeMessagePage(count = 4, hasMore = true))

        val vm = vmForState(
            GlobalSearchSessionState(
                activeTab = GlobalSearchTab.Channels,
                selectedMember = SceytUser("user-1")
            ),
            dataSource
        )
        advanceUntilIdle()

        // listItems has header + 4 messages = 5, but offset must be 4
        assertThat(vm.state.value.offset).isEqualTo(4)
    }

    @Test
    fun `selected member loadMore passes correct offset to data source`() = runTest(dispatcher) {
        val dataSource = ChannelsFakeDataSource()
        dataSource.enqueueMessages(fakeMessagePage(count = 4, hasMore = true))
        dataSource.enqueueMessages(fakeMessagePage(count = 2, hasMore = false))

        val vm = vmForState(
            GlobalSearchSessionState(
                activeTab = GlobalSearchTab.Channels,
                selectedMember = SceytUser("user-1")
            ),
            dataSource
        )
        advanceUntilIdle()
        vm.loadMore()
        advanceUntilIdle()

        assertThat(dataSource.messageCalls[1].offset).isEqualTo(4)
    }

    @Test
    fun `selected member loadMore appends messages`() = runTest(dispatcher) {
        val dataSource = ChannelsFakeDataSource()
        dataSource.enqueueMessages(fakeMessagePage(count = 4, hasMore = true))
        dataSource.enqueueMessages(fakeMessagePage(count = 2, hasMore = false))

        val vm = vmForState(
            GlobalSearchSessionState(
                activeTab = GlobalSearchTab.Channels,
                selectedMember = SceytUser("user-1")
            ),
            dataSource
        )
        advanceUntilIdle()
        vm.loadMore()
        advanceUntilIdle()

        val state = vm.state.value
        assertThat(state.listItems.filterIsInstance<GlobalSearchListItem.MessageItem>()).hasSize(6)
        assertThat(state.offset).isEqualTo(6)
        assertThat(state.hasMore).isFalse()
    }

    // ─── Typed query (channels + paginated messages) ──────────────────────────────

    @Test
    fun `typed query first page loads both channels and messages`() = runTest(dispatcher) {
        val dataSource = ChannelsFakeDataSource()
        dataSource.enqueueChannels(fakeChannelPage(count = 2, hasMore = false))
        dataSource.enqueueMessages(fakeMessagePage(count = 3, hasMore = true))

        val vm = vmForState(
            GlobalSearchSessionState(activeTab = GlobalSearchTab.Channels, query = "hello"),
            dataSource
        )
        advanceUntilIdle()

        val state = vm.state.value
        assertThat(state.listItems.filterIsInstance<GlobalSearchListItem.ChannelItem>()).hasSize(2)
        assertThat(state.listItems.filterIsInstance<GlobalSearchListItem.MessageItem>()).hasSize(3)
        assertThat(state.hasMore).isTrue()
    }

    @Test
    fun `typed query hasMore reflects messages hasMore, not false`() = runTest(dispatcher) {
        val dataSource = ChannelsFakeDataSource()
        dataSource.enqueueChannels(fakeChannelPage(count = 1, hasMore = false))
        dataSource.enqueueMessages(fakeMessagePage(count = 5, hasMore = true))

        val vm = vmForState(
            GlobalSearchSessionState(activeTab = GlobalSearchTab.Channels, query = "hello"),
            dataSource
        )
        advanceUntilIdle()

        assertThat(vm.state.value.hasMore).isTrue()
    }

    @Test
    fun `typed query offset equals message count only, channels excluded`() = runTest(dispatcher) {
        val dataSource = ChannelsFakeDataSource()
        dataSource.enqueueChannels(fakeChannelPage(count = 3, hasMore = false))
        dataSource.enqueueMessages(fakeMessagePage(count = 5, hasMore = true))

        val vm = vmForState(
            GlobalSearchSessionState(activeTab = GlobalSearchTab.Channels, query = "hello"),
            dataSource
        )
        advanceUntilIdle()

        // channels (3) are not counted; offset = messages loaded (5)
        assertThat(vm.state.value.offset).isEqualTo(5)
    }

    @Test
    fun `typed query loadMore fetches messages only, no second channels call`() = runTest(dispatcher) {
        val dataSource = ChannelsFakeDataSource()
        dataSource.enqueueChannels(fakeChannelPage(count = 2, hasMore = false))
        dataSource.enqueueMessages(fakeMessagePage(count = 5, hasMore = true))
        dataSource.enqueueMessages(fakeMessagePage(count = 3, hasMore = false))

        val vm = vmForState(
            GlobalSearchSessionState(activeTab = GlobalSearchTab.Channels, query = "hello"),
            dataSource
        )
        advanceUntilIdle()
        vm.loadMore()
        advanceUntilIdle()

        assertThat(dataSource.channelCalls).hasSize(1)
        assertThat(dataSource.messageCalls).hasSize(2)
    }

    @Test
    fun `typed query loadMore passes message offset to data source`() = runTest(dispatcher) {
        val dataSource = ChannelsFakeDataSource()
        dataSource.enqueueChannels(fakeChannelPage(count = 2, hasMore = false))
        dataSource.enqueueMessages(fakeMessagePage(count = 5, hasMore = true))
        dataSource.enqueueMessages(fakeMessagePage(count = 3, hasMore = false))

        val vm = vmForState(
            GlobalSearchSessionState(activeTab = GlobalSearchTab.Channels, query = "hello"),
            dataSource
        )
        advanceUntilIdle()
        vm.loadMore()
        advanceUntilIdle()

        assertThat(dataSource.messageCalls[1].offset).isEqualTo(5)
    }

    @Test
    fun `typed query loadMore appends message items below existing channels`() = runTest(dispatcher) {
        val dataSource = ChannelsFakeDataSource()
        dataSource.enqueueChannels(fakeChannelPage(count = 2, hasMore = false))
        dataSource.enqueueMessages(fakeMessagePage(count = 5, hasMore = true))
        dataSource.enqueueMessages(fakeMessagePage(count = 3, hasMore = false))

        val vm = vmForState(
            GlobalSearchSessionState(activeTab = GlobalSearchTab.Channels, query = "hello"),
            dataSource
        )
        advanceUntilIdle()
        vm.loadMore()
        advanceUntilIdle()

        val state = vm.state.value
        assertThat(state.listItems.filterIsInstance<GlobalSearchListItem.ChannelItem>()).hasSize(2)
        assertThat(state.listItems.filterIsInstance<GlobalSearchListItem.MessageItem>()).hasSize(8)
        assertThat(state.offset).isEqualTo(8)
        assertThat(state.hasMore).isFalse()
    }

    @Test
    fun `typed query new query resets offset and reloads from zero`() = runTest(dispatcher) {
        val dataSource = ChannelsFakeDataSource()
        dataSource.enqueueChannels(fakeChannelPage(count = 1, hasMore = false))
        dataSource.enqueueMessages(fakeMessagePage(count = 5, hasMore = true))
        // second query
        dataSource.enqueueChannels(fakeChannelPage(count = 2, hasMore = false))
        dataSource.enqueueMessages(fakeMessagePage(count = 4, hasMore = false))

        val session = GlobalSearchSessionStore(
            GlobalSearchSessionState(activeTab = GlobalSearchTab.Channels, query = "first")
        )
        ChannelsSearchViewModel(session, dataSource, dispatcher)
        advanceUntilIdle()

        session.update { it.copy(query = "second") }
        advanceUntilIdle()

        assertThat(dataSource.messageCalls[0].offset).isEqualTo(0)
        assertThat(dataSource.messageCalls[1].offset).isEqualTo(0)
    }

    @Test
    fun `typed query shorter than min length does not fetch messages`() = runTest(dispatcher) {
        val dataSource = ChannelsFakeDataSource()
        dataSource.enqueueChannels(fakeChannelPage(count = 3, hasMore = false))

        val vm = vmForState(
            GlobalSearchSessionState(activeTab = GlobalSearchTab.Channels, query = "h"), // length 1 < min 2
            dataSource
        )
        advanceUntilIdle()

        assertThat(dataSource.messageCalls).isEmpty()
        assertThat(vm.state.value.hasMore).isFalse()
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private fun vmForTab(tab: GlobalSearchTab, dataSource: ChannelsFakeDataSource) =
        vmForState(GlobalSearchSessionState(activeTab = tab), dataSource)

    private fun vmForState(state: GlobalSearchSessionState, dataSource: ChannelsFakeDataSource) =
        ChannelsSearchViewModel(GlobalSearchSessionStore(state), dataSource, dispatcher)
}

// ─── Fake data source ─────────────────────────────────────────────────────────

private class ChannelsFakeDataSource : GlobalSearchDataSource {
    private val channelQueue = ArrayDeque<GlobalSearchPage<SceytChannel>>()
    private val messageQueue = ArrayDeque<GlobalSearchPage<GlobalSearchMessageResult>>()

    val channelCalls = mutableListOf<ChannelsCall>()
    val messageCalls = mutableListOf<MessagesCall>()

    fun enqueueChannels(page: GlobalSearchPage<SceytChannel>) = channelQueue.addLast(page)
    fun enqueueMessages(page: GlobalSearchPage<GlobalSearchMessageResult>) = messageQueue.addLast(page)

    override suspend fun getRecentChannels(offset: Int, limit: Int): GlobalSearchPage<SceytChannel> {
        channelCalls += ChannelsCall(query = null, offset = offset)
        return channelQueue.removeFirstOrNull() ?: GlobalSearchPage(emptyList(), false)
    }

    override suspend fun searchChannels(query: String, offset: Int, limit: Int): GlobalSearchPage<SceytChannel> {
        channelCalls += ChannelsCall(query = query, offset = offset)
        return channelQueue.removeFirstOrNull() ?: GlobalSearchPage(emptyList(), false)
    }

    override suspend fun searchMessages(
        query: String,
        senderId: String?,
        channelTypes: List<String>,
        onlyJoined: Boolean,
        offset: Int,
        limit: Int,
    ): GlobalSearchPage<GlobalSearchMessageResult> {
        messageCalls += MessagesCall(query = query, senderId = senderId, offset = offset)
        return messageQueue.removeFirstOrNull() ?: GlobalSearchPage(emptyList(), false)
    }

    override suspend fun getRecentChats(offset: Int, limit: Int) =
        GlobalSearchPage<SceytChannel>(emptyList(), false)

    override suspend fun searchChats(query: String, offset: Int, limit: Int) =
        GlobalSearchPage<SceytChannel>(emptyList(), false)

    override suspend fun searchAttachments(
        kind: GlobalSearchAttachmentKind,
        query: String,
        senderId: String?,
        offset: Int,
        limit: Int,
    ) = GlobalSearchPage<GlobalSearchAttachmentResult>(emptyList(), false)

    override suspend fun searchUsersLinkedToJoinedChannelsByDisplayName(query: String, limit: Int) =
        emptyList<SceytUser>()
}

private data class ChannelsCall(val query: String?, val offset: Int)
private data class MessagesCall(val query: String, val senderId: String?, val offset: Int)

// ─── Factories ────────────────────────────────────────────────────────────────

private fun fakeChannelPage(count: Int, hasMore: Boolean) =
    GlobalSearchPage(data = List(count) { mock<SceytChannel>() }, hasMore = hasMore)

private fun fakeMessagePage(count: Int, hasMore: Boolean) =
    GlobalSearchPage(
        data = List(count) { GlobalSearchMessageResult(message = mock<SceytMessage>(), channel = mock<SceytChannel>()) },
        hasMore = hasMore
    )
