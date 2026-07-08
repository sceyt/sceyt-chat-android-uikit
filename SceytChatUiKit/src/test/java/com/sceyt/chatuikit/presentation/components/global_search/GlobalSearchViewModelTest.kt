package com.sceyt.chatuikit.presentation.components.global_search

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.ChatClient
import com.sceyt.chatuikit.config.GlobalSearchCloseBehavior
import com.sceyt.chatuikit.config.GlobalSearchConfig
import com.sceyt.chatuikit.data.managers.message.MessageEventManager
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.data.models.search.GlobalSearchAttachmentKind
import com.sceyt.chatuikit.data.models.search.GlobalSearchAttachmentResult
import com.sceyt.chatuikit.data.models.search.GlobalSearchMessageResult
import com.sceyt.chatuikit.data.models.search.GlobalSearchPage
import com.sceyt.chatuikit.persistence.interactor.GlobalSearchDataSource
import com.sceyt.chatuikit.persistence.interactor.GlobalSearchUserSuggestionsProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.mockStatic
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class GlobalSearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var chatClientStaticMock: MockedStatic<ChatClient>

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        chatClientStaticMock = mockStatic<ChatClient>()
        val mockClient = mock<ChatClient>()
        chatClientStaticMock.`when`<ChatClient> { ChatClient.getClient() }.thenReturn(mockClient)
        Mockito.doNothing().`when`(mockClient)
            .addMessageListener(Mockito.anyString(), Mockito.any())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        chatClientStaticMock.close()
    }

    @Test
    fun `query updates shared session state`() = runTest(dispatcher) {
        val viewModel = TestGlobalSearchViewModel(ioDispatcher = dispatcher)
        advanceUntilIdle()

        viewModel.onQueryChanged("jam")
        advanceUntilIdle()

        assertThat(viewModel.headerState.value.query).isEqualTo("jam")
        assertThat(viewModel.requireSession().state.value.query).isEqualTo("jam")
    }

    @Test
    fun `tab selection updates shared session state`() = runTest(dispatcher) {
        val viewModel = TestGlobalSearchViewModel(ioDispatcher = dispatcher)

        viewModel.onTabSelected(GlobalSearchTab.Media)

        assertThat(viewModel.headerState.value.activeTab).isEqualTo(GlobalSearchTab.Media)
        assertThat(viewModel.requireSession().state.value.activeTab).isEqualTo(GlobalSearchTab.Media)
    }

    @Test
    fun `selecting a member clears the query and updates session state`() = runTest(dispatcher) {
        val viewModel = TestGlobalSearchViewModel(ioDispatcher = dispatcher)
        advanceUntilIdle()

        viewModel.onQueryChanged("jam")
        advanceUntilIdle()
        viewModel.onUserSelected(SceytUser("member-7"))

        assertThat(viewModel.headerState.value.query).isEmpty()
        assertThat(viewModel.headerState.value.selectedUser?.id).isEqualTo("member-7")
        assertThat(viewModel.requireSession().state.value.query).isEmpty()
        assertThat(viewModel.requireSession().state.value.selectedMember?.id).isEqualTo("member-7")
    }

    @Test
    fun `empty input delete arms selected member removal before removing it`() =
        runTest(dispatcher) {
            val viewModel = TestGlobalSearchViewModel(ioDispatcher = dispatcher)
            advanceUntilIdle()

            viewModel.onUserSelected(SceytUser("member-42"))
            viewModel.onQueryChanged("media")
            advanceUntilIdle()

            viewModel.onEmptyQueryDeleteRequested()

            assertThat(viewModel.headerState.value.isSelectedMemberRemovalPending).isTrue()
            assertThat(viewModel.headerState.value.selectedUser?.id).isEqualTo("member-42")
            assertThat(viewModel.headerState.value.query).isEqualTo("media")

            viewModel.onEmptyQueryDeleteRequested()

            assertThat(viewModel.headerState.value.selectedUser).isNull()
            assertThat(viewModel.headerState.value.isSelectedMemberRemovalPending).isFalse()
            assertThat(viewModel.requireSession().state.value.selectedMember).isNull()
        }

    @Test
    fun `typing again clears the pending selected member removal state`() = runTest(dispatcher) {
        val viewModel = TestGlobalSearchViewModel(ioDispatcher = dispatcher)
        advanceUntilIdle()

        viewModel.onUserSelected(SceytUser("member-1"))
        viewModel.onEmptyQueryDeleteRequested()

        assertThat(viewModel.headerState.value.isSelectedMemberRemovalPending).isTrue()

        viewModel.onQueryChanged("a")

        assertThat(viewModel.headerState.value.isSelectedMemberRemovalPending).isFalse()
    }

    @Test
    fun `clear button keeps previous selected member behavior`() = runTest(dispatcher) {
        val viewModel = TestGlobalSearchViewModel(ioDispatcher = dispatcher)
        advanceUntilIdle()

        viewModel.onUserSelected(SceytUser("member-9"))
        viewModel.onClearRequested()

        assertThat(viewModel.headerState.value.selectedUser).isNull()
        assertThat(viewModel.headerState.value.isSelectedMemberRemovalPending).isFalse()
    }

    @Test
    fun `custom suggestions provider uses configured limit and updates header suggestions`() =
        runTest(dispatcher) {
            val provider = FakeSuggestionsProvider(
                suggestionsByQuery = mapOf("mar" to listOf(SceytUser("member-1")))
            )
            val viewModel = TestGlobalSearchViewModel(
                userSuggestionsProvider = provider,
                config = GlobalSearchConfig().apply { userSuggestionsLimit = 3 },
                ioDispatcher = dispatcher
            )
            advanceUntilIdle()

            viewModel.onQueryChanged("mar")
            advanceUntilIdle()

            assertThat(provider.calls).containsExactly(SuggestionCall(query = "mar", limit = 3))
            assertThat(viewModel.headerState.value.userSuggestions.map { it.id })
                .containsExactly("member-1")
        }

    @Test
    fun `suggestions debounce waits before invoking provider`() = runTest(dispatcher) {
        val provider = FakeSuggestionsProvider(
            suggestionsByQuery = mapOf("ali" to listOf(SceytUser("member-1")))
        )
        val viewModel = TestGlobalSearchViewModel(
            userSuggestionsProvider = provider,
            config = GlobalSearchConfig().apply { searchInputDebounceMs = 200L },
            ioDispatcher = dispatcher
        )
        advanceUntilIdle()

        viewModel.onQueryChanged("ali")
        advanceTimeBy(199.milliseconds)

        assertThat(provider.calls).isEmpty()

        advanceTimeBy(1.milliseconds)
        advanceUntilIdle()

        assertThat(provider.calls).containsExactly(SuggestionCall(query = "ali", limit = 8))
        assertThat(viewModel.headerState.value.userSuggestions.map { it.id })
            .containsExactly("member-1")
    }

    @Test
    fun `onChannelOpened emits closeEvent when behavior is OnChannelOpen`() = runTest(dispatcher) {
        val viewModel = TestGlobalSearchViewModel(
            config = GlobalSearchConfig().apply {
                closeBehavior = GlobalSearchCloseBehavior.OnChannelOpen
            },
            ioDispatcher = dispatcher,
        )
        var count = 0
        val job = launch { viewModel.closeEvent.collect { count++ } }
        advanceUntilIdle()

        viewModel.onChannelOpened()
        advanceUntilIdle()

        assertThat(count).isEqualTo(1)
        job.cancel()
    }

    @Test
    fun `onChannelOpened does not emit closeEvent when behavior is Never`() = runTest(dispatcher) {
        val viewModel = TestGlobalSearchViewModel(
            config = GlobalSearchConfig().apply { closeBehavior = GlobalSearchCloseBehavior.Never },
            ioDispatcher = dispatcher,
        )
        var count = 0
        val job = launch { viewModel.closeEvent.collect { count++ } }
        advanceUntilIdle()

        viewModel.onChannelOpened()
        advanceUntilIdle()

        assertThat(count).isEqualTo(0)
        job.cancel()
    }

    @Test
    fun `closeEvent emits when outgoing message fires and behavior is OnMessageSent`() =
        runTest(dispatcher) {
            val viewModel = TestGlobalSearchViewModel(
                config = GlobalSearchConfig().apply {
                    closeBehavior = GlobalSearchCloseBehavior.OnMessageSent
                },
                ioDispatcher = dispatcher,
            )
            var count = 0
            val job = launch { viewModel.closeEvent.collect { count++ } }
            advanceUntilIdle()

            MessageEventManager.emitOutgoingMessage(mock())
            advanceUntilIdle()

            assertThat(count).isEqualTo(1)
            job.cancel()
        }

    @Test
    fun `closeEvent does not emit on outgoing message when behavior is Never`() =
        runTest(dispatcher) {
            val viewModel = TestGlobalSearchViewModel(
                config = GlobalSearchConfig().apply {
                    closeBehavior = GlobalSearchCloseBehavior.Never
                },
                ioDispatcher = dispatcher,
            )
            var count = 0
            val job = launch { viewModel.closeEvent.collect { count++ } }
            advanceUntilIdle()

            MessageEventManager.emitOutgoingMessage(mock())
            advanceUntilIdle()

            assertThat(count).isEqualTo(0)
            job.cancel()
        }

    @Test
    fun `session registry entry is removed when header viewmodel is cleared`() =
        runTest(dispatcher) {
            val viewModel = TestGlobalSearchViewModel(ioDispatcher = dispatcher)

            assertThat(GlobalSearchSessionRegistry.contains(viewModel.sessionId)).isTrue()

            viewModel.clearForTest()

            assertThat(GlobalSearchSessionRegistry.contains(viewModel.sessionId)).isFalse()
        }
}

private class TestGlobalSearchViewModel(
    initialTab: GlobalSearchTab = GlobalSearchTab.Chats,
    userSuggestionsProvider: GlobalSearchUserSuggestionsProvider = GlobalSearchUserSuggestionsProvider { _, _ ->
        emptyList()
    },
    config: GlobalSearchConfig = GlobalSearchConfig(),
    ioDispatcher: CoroutineDispatcher,
) : GlobalSearchViewModel(
    initialTab = initialTab,
    userSuggestionsProvider = userSuggestionsProvider,
    config = config,
    ioDispatcher = ioDispatcher,
) {
    fun requireSession(): GlobalSearchSession {
        return GlobalSearchSessionRegistry.getOrDefault(sessionId)
    }

    fun clearForTest() {
        super.onCleared()
    }
}

internal class FakeGlobalSearchDataSource : GlobalSearchDataSource {
    var recentChatsCalls = 0
    var searchChatsCalls = 0
    var recentChannelsCalls = 0
    var searchChannelsCalls = 0
    val searchMessagesCalls = mutableListOf<MessageCall>()
    val attachmentCalls = mutableListOf<AttachmentCall>()

    override suspend fun getRecentChats(offset: Int, limit: Int): GlobalSearchPage<SceytChannel> {
        recentChatsCalls++
        return GlobalSearchPage(data = listOf(mock()), hasMore = false)
    }

    override suspend fun searchChats(
        query: String,
        offset: Int,
        limit: Int
    ): GlobalSearchPage<SceytChannel> {
        searchChatsCalls++
        return GlobalSearchPage(data = listOf(mock()), hasMore = false)
    }

    override suspend fun getRecentChannels(
        offset: Int,
        limit: Int
    ): GlobalSearchPage<SceytChannel> {
        recentChannelsCalls++
        return GlobalSearchPage(data = listOf(mock()), hasMore = false)
    }

    override suspend fun searchChannels(
        query: String,
        offset: Int,
        limit: Int
    ): GlobalSearchPage<SceytChannel> {
        searchChannelsCalls++
        return GlobalSearchPage(data = listOf(mock()), hasMore = false)
    }

    override suspend fun searchMessages(
        query: String,
        senderId: String?,
        channelTypes: List<String>,
        onlyJoined: Boolean,
        offset: Int,
        limit: Int,
    ): GlobalSearchPage<GlobalSearchMessageResult> {
        searchMessagesCalls += MessageCall(
            query = query,
            senderId = senderId,
            channelTypes = channelTypes,
            onlyJoined = onlyJoined,
            offset = offset
        )
        return GlobalSearchPage(
            data = listOf(
                GlobalSearchMessageResult(
                    message = mock(),
                    channel = mock()
                )
            ),
            hasMore = false
        )
    }

    override suspend fun searchAttachments(
        kind: GlobalSearchAttachmentKind,
        query: String,
        senderId: String?,
        offset: Int,
        limit: Int,
    ): GlobalSearchPage<GlobalSearchAttachmentResult> {
        attachmentCalls += AttachmentCall(
            kind = kind,
            query = query,
            senderId = senderId,
            offset = offset
        )
        return GlobalSearchPage(
            data = listOf(
                GlobalSearchAttachmentResult(
                    attachment = mock(),
                    message = mock(),
                    channel = mock(),
                    sender = null,
                    kind = kind
                )
            ),
            hasMore = false
        )
    }

    override suspend fun searchUsersLinkedToJoinedChannelsByDisplayName(
        query: String,
        limit: Int
    ): List<SceytUser> {
        return emptyList()
    }
}

internal data class MessageCall(
    val query: String,
    val senderId: String?,
    val channelTypes: List<String>,
    val onlyJoined: Boolean,
    val offset: Int,
)

internal data class AttachmentCall(
    val kind: GlobalSearchAttachmentKind,
    val query: String,
    val senderId: String?,
    val offset: Int,
)

internal class FakeSuggestionsProvider(
    private val suggestionsByQuery: Map<String, List<SceytUser>> = emptyMap(),
) : GlobalSearchUserSuggestionsProvider {
    val calls = mutableListOf<SuggestionCall>()

    override suspend fun provideSuggestions(query: String, limit: Int): List<SceytUser> {
        calls += SuggestionCall(query = query, limit = limit)
        return suggestionsByQuery[query].orEmpty()
    }
}

internal data class SuggestionCall(
    val query: String,
    val limit: Int,
)
