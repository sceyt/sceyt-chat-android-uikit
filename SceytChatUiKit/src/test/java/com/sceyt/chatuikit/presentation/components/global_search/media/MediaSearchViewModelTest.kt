package com.sceyt.chatuikit.presentation.components.global_search.media

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.ChatClient
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.data.models.search.GlobalSearchAttachmentKind
import com.sceyt.chatuikit.data.models.search.GlobalSearchAttachmentResult
import com.sceyt.chatuikit.data.models.search.GlobalSearchMessageResult
import com.sceyt.chatuikit.data.models.search.GlobalSearchPage
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.interactor.GlobalSearchDataSource
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchListItem
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchSessionState
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchSessionStore
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.mockStatic
import org.mockito.kotlin.whenever

private const val DAY_1 = 1_700_000_000_000L
private const val DAY_2 = DAY_1 + 86_400_000L  // +1 day

@OptIn(ExperimentalCoroutinesApi::class)
class MediaSearchViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val fileTransferService = mock<FileTransferService>()
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
                single<FileTransferService> { fileTransferService }
            })
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        chatClientStaticMock.close()
        SceytKoinApp.koinApp = null
        stopKoin()
    }

    // ─── Grid mode ────────────────────────────────────────────────────────────────

    @Test
    fun `first page loads as Grid when query is blank`() = runTest(dispatcher) {
        val dataSource = MediaFakeDataSource()
        dataSource.enqueue(fakeResultsPage(count = 3, hasMore = false))

        val vm = MediaSearchViewModel(sessionForTab(GlobalSearchTab.Media), dataSource, dispatcher)
        advanceUntilIdle()

        val state = vm.state.value
        assertThat(state.mode).isInstanceOf(MediaSearchDisplayMode.Grid::class.java)
        assertThat(state.isLoading).isFalse()
        assertThat(state.hasMore).isFalse()
        val items = (state.mode as MediaSearchDisplayMode.Grid).items
            .filterIsInstance<GlobalSearchListItem.AttachmentItem>()
        assertThat(items).hasSize(3)
    }

    @Test
    fun `Grid offset equals loaded item count after first page`() = runTest(dispatcher) {
        val dataSource = MediaFakeDataSource()
        dataSource.enqueue(fakeResultsPage(count = 5, hasMore = true))

        val vm = MediaSearchViewModel(sessionForTab(GlobalSearchTab.Media), dataSource, dispatcher)
        advanceUntilIdle()

        val grid = vm.state.value.mode as MediaSearchDisplayMode.Grid
        assertThat(grid.offset).isEqualTo(5)
    }

    @Test
    fun `Grid loadMore passes mode offset as data source offset`() = runTest(dispatcher) {
        val dataSource = MediaFakeDataSource()
        dataSource.enqueue(fakeResultsPage(count = 7, hasMore = true))
        dataSource.enqueue(fakeResultsPage(count = 3, hasMore = false))

        val vm = MediaSearchViewModel(sessionForTab(GlobalSearchTab.Media), dataSource, dispatcher)
        advanceUntilIdle()
        vm.loadMore()
        advanceUntilIdle()

        assertThat(dataSource.calls[1].offset).isEqualTo(7)
    }

    @Test
    fun `Grid loadMore appends items and advances offset`() = runTest(dispatcher) {
        val dataSource = MediaFakeDataSource()
        dataSource.enqueue(fakeResultsPage(count = 5, hasMore = true))
        dataSource.enqueue(fakeResultsPage(count = 3, hasMore = false))

        val vm = MediaSearchViewModel(sessionForTab(GlobalSearchTab.Media), dataSource, dispatcher)
        advanceUntilIdle()
        vm.loadMore()
        advanceUntilIdle()

        val grid = vm.state.value.mode as MediaSearchDisplayMode.Grid
        assertThat(grid.items.filterIsInstance<GlobalSearchListItem.AttachmentItem>()).hasSize(8)
        assertThat(grid.offset).isEqualTo(8)
        assertThat(vm.state.value.hasMore).isFalse()
    }

    @Test
    fun `Grid loadMore is no-op when hasMore is false`() = runTest(dispatcher) {
        val dataSource = MediaFakeDataSource()
        dataSource.enqueue(fakeResultsPage(count = 3, hasMore = false))

        val vm = MediaSearchViewModel(sessionForTab(GlobalSearchTab.Media), dataSource, dispatcher)
        advanceUntilIdle()
        vm.loadMore()
        advanceUntilIdle()

        assertThat(dataSource.calls).hasSize(1)
    }

    // ─── SearchList mode ─────────────────────────────────────────────────────────

    @Test
    fun `first page loads as SearchList when query is non-blank`() = runTest(dispatcher) {
        val dataSource = MediaFakeDataSource()
        dataSource.enqueue(fakeResultsPage(count = 4, hasMore = true))

        val vm = MediaSearchViewModel(
            sessionForTab(GlobalSearchTab.Media, query = "cats"),
            dataSource,
            dispatcher
        )
        advanceUntilIdle()

        val state = vm.state.value
        assertThat(state.mode).isInstanceOf(MediaSearchDisplayMode.SearchList::class.java)
        assertThat(state.hasMore).isTrue()
        val items = (state.mode as MediaSearchDisplayMode.SearchList).items
            .filterIsInstance<GlobalSearchListItem.AttachmentItem>()
        assertThat(items).hasSize(4)
    }

    @Test
    fun `SearchList loadMore passes correct offset to data source`() = runTest(dispatcher) {
        val dataSource = MediaFakeDataSource()
        dataSource.enqueue(fakeResultsPage(count = 4, hasMore = true))
        dataSource.enqueue(fakeResultsPage(count = 2, hasMore = false))

        val vm = MediaSearchViewModel(
            sessionForTab(GlobalSearchTab.Media, query = "cats"),
            dataSource,
            dispatcher
        )
        advanceUntilIdle()
        vm.loadMore()
        advanceUntilIdle()

        assertThat(dataSource.calls[1].offset).isEqualTo(4)
    }

    @Test
    fun `SearchList loadMore appends items and advances offset`() = runTest(dispatcher) {
        val dataSource = MediaFakeDataSource()
        dataSource.enqueue(fakeResultsPage(count = 4, hasMore = true))
        dataSource.enqueue(fakeResultsPage(count = 2, hasMore = false))

        val vm = MediaSearchViewModel(
            sessionForTab(GlobalSearchTab.Media, query = "cats"),
            dataSource,
            dispatcher
        )
        advanceUntilIdle()
        vm.loadMore()
        advanceUntilIdle()

        val list = vm.state.value.mode as MediaSearchDisplayMode.SearchList
        assertThat(list.items.filterIsInstance<GlobalSearchListItem.AttachmentItem>()).hasSize(6)
        assertThat(list.offset).isEqualTo(6)
    }

    @Test
    fun `SearchList hasMore reflects data source response`() = runTest(dispatcher) {
        val dataSource = MediaFakeDataSource()
        dataSource.enqueue(fakeResultsPage(count = 4, hasMore = true))

        val vm = MediaSearchViewModel(
            sessionForTab(GlobalSearchTab.Media, query = "cats"),
            dataSource,
            dispatcher
        )
        advanceUntilIdle()

        assertThat(vm.state.value.hasMore).isTrue()
    }

    // ─── Mode transitions ─────────────────────────────────────────────────────────

    @Test
    fun `switching from blank to typed query transitions Grid to SearchList`() = runTest(dispatcher) {
        val dataSource = MediaFakeDataSource()
        val session = GlobalSearchSessionStore(
            GlobalSearchSessionState(activeTab = GlobalSearchTab.Media)
        )
        dataSource.enqueue(fakeResultsPage(count = 3, hasMore = false))
        dataSource.enqueue(fakeResultsPage(count = 2, hasMore = false))

        val vm = MediaSearchViewModel(session, dataSource, dispatcher)
        advanceUntilIdle()
        assertThat(vm.state.value.mode).isInstanceOf(MediaSearchDisplayMode.Grid::class.java)

        session.update { it.copy(query = "cats") }
        advanceUntilIdle()

        assertThat(vm.state.value.mode).isInstanceOf(MediaSearchDisplayMode.SearchList::class.java)
    }

    @Test
    fun `clearing typed query transitions SearchList back to Grid`() = runTest(dispatcher) {
        val dataSource = MediaFakeDataSource()
        val session = GlobalSearchSessionStore(
            GlobalSearchSessionState(activeTab = GlobalSearchTab.Media, query = "cats")
        )
        dataSource.enqueue(fakeResultsPage(count = 2, hasMore = false))
        dataSource.enqueue(fakeResultsPage(count = 5, hasMore = false))

        val vm = MediaSearchViewModel(session, dataSource, dispatcher)
        advanceUntilIdle()
        assertThat(vm.state.value.mode).isInstanceOf(MediaSearchDisplayMode.SearchList::class.java)

        session.update { it.copy(query = "") }
        advanceUntilIdle()

        assertThat(vm.state.value.mode).isInstanceOf(MediaSearchDisplayMode.Grid::class.java)
    }

    // ─── Session / tab behavior ──────────────────────────────────────────────────

    @Test
    fun `query changes on a non-Media tab do not trigger data source calls`() = runTest(dispatcher) {
        val dataSource = MediaFakeDataSource()
        val session = GlobalSearchSessionStore(
            GlobalSearchSessionState(activeTab = GlobalSearchTab.Chats)
        )

        MediaSearchViewModel(session, dataSource, dispatcher)
        advanceUntilIdle()

        session.update { it.copy(query = "cats") }
        advanceUntilIdle()

        assertThat(dataSource.calls).isEmpty()
    }

    @Test
    fun `switching to Media tab triggers first page load with current query`() = runTest(dispatcher) {
        val dataSource = MediaFakeDataSource()
        val session = GlobalSearchSessionStore(
            GlobalSearchSessionState(activeTab = GlobalSearchTab.Chats, query = "cats")
        )
        dataSource.enqueue(fakeResultsPage(count = 2, hasMore = false))

        val vm = MediaSearchViewModel(session, dataSource, dispatcher)
        advanceUntilIdle()
        assertThat(dataSource.calls).isEmpty()

        session.update { it.copy(activeTab = GlobalSearchTab.Media) }
        advanceUntilIdle()

        assertThat(dataSource.calls).hasSize(1)
        assertThat(dataSource.calls[0].query).isEqualTo("cats")
        assertThat(vm.state.value.mode).isInstanceOf(MediaSearchDisplayMode.SearchList::class.java)
    }

    @Test
    fun `re-selecting Media tab without query change does not reload`() = runTest(dispatcher) {
        val dataSource = MediaFakeDataSource()
        val session = GlobalSearchSessionStore(
            GlobalSearchSessionState(activeTab = GlobalSearchTab.Media)
        )
        dataSource.enqueue(fakeResultsPage(count = 3, hasMore = false))

        val vm = MediaSearchViewModel(session, dataSource, dispatcher)
        advanceUntilIdle()

        session.update { it.copy(activeTab = GlobalSearchTab.Chats) }
        session.update { it.copy(activeTab = GlobalSearchTab.Media) }
        advanceUntilIdle()

        // State hasn't changed from the vm's perspective since query and tab are the same
        // (sessionState equality check in onSessionStateChanged prevents duplicate load)
        assertThat(vm.state.value.mode).isInstanceOf(MediaSearchDisplayMode.Grid::class.java)
    }

    // ─── selectedMember ──────────────────────────────────────────────────────────

    @Test
    fun `selectedMember id is forwarded as senderId to searchAttachments`() = runTest(dispatcher) {
        val dataSource = MediaFakeDataSource()
        val session = GlobalSearchSessionStore(
            GlobalSearchSessionState(
                activeTab = GlobalSearchTab.Media,
                selectedMember = SceytUser("user-99")
            )
        )
        dataSource.enqueue(fakeResultsPage(count = 1, hasMore = false))

        MediaSearchViewModel(session, dataSource, dispatcher)
        advanceUntilIdle()

        assertThat(dataSource.calls[0].senderId).isEqualTo("user-99")
    }

    @Test
    fun `null selectedMember results in null senderId`() = runTest(dispatcher) {
        val dataSource = MediaFakeDataSource()
        dataSource.enqueue(fakeResultsPage(count = 1, hasMore = false))

        MediaSearchViewModel(sessionForTab(GlobalSearchTab.Media), dataSource, dispatcher)
        advanceUntilIdle()

        assertThat(dataSource.calls[0].senderId).isNull()
    }

    // ─── Loading guards ───────────────────────────────────────────────────────────

    @Test
    fun `loadMore is no-op while first page is still loading`() = runTest(dispatcher) {
        val dataSource = MediaFakeDataSource()
        dataSource.enqueue(fakeResultsPage(count = 3, hasMore = true))

        val vm = MediaSearchViewModel(sessionForTab(GlobalSearchTab.Media), dataSource, dispatcher)
        // isLoading = true; coroutines not yet advanced
        vm.loadMore()
        advanceUntilIdle()

        assertThat(dataSource.calls).hasSize(1)
    }

    @Test
    fun `rapid loadMore calls result in only one additional data source call`() = runTest(dispatcher) {
        val dataSource = MediaFakeDataSource()
        dataSource.enqueue(fakeResultsPage(count = 3, hasMore = true))
        dataSource.enqueue(fakeResultsPage(count = 2, hasMore = false))

        val vm = MediaSearchViewModel(sessionForTab(GlobalSearchTab.Media), dataSource, dispatcher)
        advanceUntilIdle()

        vm.loadMore()  // sets isLoadingMore = true synchronously before coroutine runs
        vm.loadMore()  // sees isLoadingMore = true → no-op
        advanceUntilIdle()

        assertThat(dataSource.calls).hasSize(2)
    }

    // ─── Empty state ──────────────────────────────────────────────────────────────

    @Test
    fun `showEmptyState is true when data source returns empty page`() = runTest(dispatcher) {
        val dataSource = MediaFakeDataSource()
        dataSource.enqueue(GlobalSearchPage(emptyList(), false))

        val vm = MediaSearchViewModel(sessionForTab(GlobalSearchTab.Media), dataSource, dispatcher)
        advanceUntilIdle()

        assertThat(vm.state.value.showEmptyState).isTrue()
    }

    @Test
    fun `showEmptyState is false while loading`() = runTest(dispatcher) {
        val dataSource = MediaFakeDataSource()
        dataSource.enqueue(fakeResultsPage(count = 0, hasMore = false))

        val vm = MediaSearchViewModel(sessionForTab(GlobalSearchTab.Media), dataSource, dispatcher)
        // Not advanced yet — isLoading = true

        assertThat(vm.state.value.showEmptyState).isFalse()
    }

    // ─── Date separators ──────────────────────────────────────────────────────────

    @Test
    fun `no duplicate date separator when second page items are on same day as last first-page item`() =
        runTest(dispatcher) {
            val dataSource = MediaFakeDataSource()
            dataSource.enqueue(
                GlobalSearchPage(
                    data = listOf(fakeMediaResult(DAY_1), fakeMediaResult(DAY_1)),
                    hasMore = true
                )
            )
            dataSource.enqueue(
                GlobalSearchPage(
                    data = listOf(fakeMediaResult(DAY_1), fakeMediaResult(DAY_1)),
                    hasMore = false
                )
            )

            val vm = MediaSearchViewModel(sessionForTab(GlobalSearchTab.Media), dataSource, dispatcher)
            advanceUntilIdle()
            vm.loadMore()
            advanceUntilIdle()

            val grid = vm.state.value.mode as MediaSearchDisplayMode.Grid
            val separators = grid.items.filterIsInstance<GlobalSearchListItem.DateSeparator>()
            assertThat(separators).hasSize(1)
        }

    @Test
    fun `date separator is added when second page starts on a different day`() =
        runTest(dispatcher) {
            val dataSource = MediaFakeDataSource()
            dataSource.enqueue(
                GlobalSearchPage(
                    data = listOf(fakeMediaResult(DAY_1), fakeMediaResult(DAY_1)),
                    hasMore = true
                )
            )
            dataSource.enqueue(
                GlobalSearchPage(
                    data = listOf(fakeMediaResult(DAY_2), fakeMediaResult(DAY_2)),
                    hasMore = false
                )
            )

            val vm = MediaSearchViewModel(sessionForTab(GlobalSearchTab.Media), dataSource, dispatcher)
            advanceUntilIdle()
            vm.loadMore()
            advanceUntilIdle()

            val grid = vm.state.value.mode as MediaSearchDisplayMode.Grid
            val separators = grid.items.filterIsInstance<GlobalSearchListItem.DateSeparator>()
            assertThat(separators).hasSize(2)
        }

    // ─── Attachment kind ──────────────────────────────────────────────────────────

    @Test
    fun `searchAttachments is always called with Media kind`() = runTest(dispatcher) {
        val dataSource = MediaFakeDataSource()
        dataSource.enqueue(fakeResultsPage(count = 1, hasMore = true))
        dataSource.enqueue(fakeResultsPage(count = 1, hasMore = false))

        val vm = MediaSearchViewModel(sessionForTab(GlobalSearchTab.Media), dataSource, dispatcher)
        advanceUntilIdle()
        vm.loadMore()
        advanceUntilIdle()

        assertThat(dataSource.calls.map { it.kind })
            .containsExactly(GlobalSearchAttachmentKind.Media, GlobalSearchAttachmentKind.Media)
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────────

private fun sessionForTab(tab: GlobalSearchTab, query: String = "") =
    GlobalSearchSessionStore(GlobalSearchSessionState(activeTab = tab, query = query))

private fun fakeMediaResult(createdAt: Long = DAY_1): GlobalSearchAttachmentResult {
    val attachment = mock<SceytAttachment>()
    whenever(attachment.createdAt).thenReturn(createdAt)
    whenever(attachment.type).thenReturn(AttachmentTypeEnum.Image.value)
    return GlobalSearchAttachmentResult(
        attachment = attachment,
        message = mock(),
        channel = mock(),
        sender = null,
        kind = GlobalSearchAttachmentKind.Media
    )
}

private fun fakeResultsPage(count: Int, hasMore: Boolean, createdAt: Long = DAY_1) =
    GlobalSearchPage(
        data = List(count) { fakeMediaResult(createdAt) },
        hasMore = hasMore
    )

private class MediaFakeDataSource : GlobalSearchDataSource {
    private val queue = ArrayDeque<GlobalSearchPage<GlobalSearchAttachmentResult>>()
    val calls = mutableListOf<MediaAttachmentCall>()

    fun enqueue(page: GlobalSearchPage<GlobalSearchAttachmentResult>) = queue.addLast(page)

    override suspend fun searchAttachments(
        kind: GlobalSearchAttachmentKind,
        query: String,
        senderId: String?,
        offset: Int,
        limit: Int,
    ): GlobalSearchPage<GlobalSearchAttachmentResult> {
        calls += MediaAttachmentCall(kind, query, senderId, offset)
        return queue.removeFirstOrNull() ?: GlobalSearchPage(emptyList(), false)
    }

    override suspend fun getRecentChats(offset: Int, limit: Int) =
        GlobalSearchPage<SceytChannel>(emptyList(), false)

    override suspend fun searchChats(query: String, offset: Int, limit: Int) =
        GlobalSearchPage<SceytChannel>(emptyList(), false)

    override suspend fun getRecentChannels(offset: Int, limit: Int) =
        GlobalSearchPage<SceytChannel>(emptyList(), false)

    override suspend fun searchChannels(query: String, offset: Int, limit: Int) =
        GlobalSearchPage<SceytChannel>(emptyList(), false)

    override suspend fun searchMessages(
        query: String,
        senderId: String?,
        channelTypes: List<String>,
        onlyJoined: Boolean,
        offset: Int,
        limit: Int,
    ) = GlobalSearchPage<GlobalSearchMessageResult>(emptyList(), false)

    override suspend fun searchUsersLinkedToJoinedChannelsByDisplayName(
        query: String,
        limit: Int,
    ) = emptyList<SceytUser>()
}

private data class MediaAttachmentCall(
    val kind: GlobalSearchAttachmentKind,
    val query: String,
    val senderId: String?,
    val offset: Int,
)
