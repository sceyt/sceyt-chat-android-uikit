package com.sceyt.chatuikit.presentation.components.global_search

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.data.models.messages.SceytUser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class GlobalSearchHeaderViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `query updates shared session state`() = runTest(dispatcher) {
        val viewModel = TestGlobalSearchHeaderViewModel(ioDispatcher = dispatcher)
        advanceUntilIdle()

        viewModel.onQueryChanged("jam")
        advanceUntilIdle()

        assertThat(viewModel.headerState.value.query).isEqualTo("jam")
        assertThat(viewModel.requireSession().state.value.query).isEqualTo("jam")
    }

    @Test
    fun `tab selection updates shared session state`() = runTest(dispatcher) {
        val viewModel = TestGlobalSearchHeaderViewModel(ioDispatcher = dispatcher)

        viewModel.onTabSelected(GlobalSearchTab.Media)

        assertThat(viewModel.headerState.value.activeTab).isEqualTo(GlobalSearchTab.Media)
        assertThat(viewModel.requireSession().state.value.activeTab).isEqualTo(GlobalSearchTab.Media)
    }

    @Test
    fun `selecting a member preserves the current query and updates session state`() = runTest(dispatcher) {
        val viewModel = TestGlobalSearchHeaderViewModel(ioDispatcher = dispatcher)
        advanceUntilIdle()

        viewModel.onQueryChanged("jam")
        advanceUntilIdle()
        viewModel.onMemberSelected(SceytUser("member-7"))

        assertThat(viewModel.headerState.value.query).isEqualTo("jam")
        assertThat(viewModel.headerState.value.selectedMember?.id).isEqualTo("member-7")
        assertThat(viewModel.requireSession().state.value.query).isEqualTo("jam")
        assertThat(viewModel.requireSession().state.value.selectedMember?.id).isEqualTo("member-7")
    }

    @Test
    fun `empty input delete arms selected member removal before removing it`() = runTest(dispatcher) {
        val viewModel = TestGlobalSearchHeaderViewModel(ioDispatcher = dispatcher)
        advanceUntilIdle()

        viewModel.onMemberSelected(SceytUser("member-42"))
        viewModel.onQueryChanged("media")
        advanceUntilIdle()

        viewModel.onEmptyQueryDeleteRequested()

        assertThat(viewModel.headerState.value.isSelectedMemberRemovalPending).isTrue()
        assertThat(viewModel.headerState.value.selectedMember?.id).isEqualTo("member-42")
        assertThat(viewModel.headerState.value.query).isEqualTo("media")

        viewModel.onEmptyQueryDeleteRequested()

        assertThat(viewModel.headerState.value.selectedMember).isNull()
        assertThat(viewModel.headerState.value.isSelectedMemberRemovalPending).isFalse()
        assertThat(viewModel.requireSession().state.value.selectedMember).isNull()
    }

    @Test
    fun `typing again clears the pending selected member removal state`() = runTest(dispatcher) {
        val viewModel = TestGlobalSearchHeaderViewModel(ioDispatcher = dispatcher)
        advanceUntilIdle()

        viewModel.onMemberSelected(SceytUser("member-1"))
        viewModel.onEmptyQueryDeleteRequested()

        assertThat(viewModel.headerState.value.isSelectedMemberRemovalPending).isTrue()

        viewModel.onQueryChanged("a")

        assertThat(viewModel.headerState.value.isSelectedMemberRemovalPending).isFalse()
    }

    @Test
    fun `clear button keeps previous selected member behavior`() = runTest(dispatcher) {
        val viewModel = TestGlobalSearchHeaderViewModel(ioDispatcher = dispatcher)
        advanceUntilIdle()

        viewModel.onMemberSelected(SceytUser("member-9"))
        viewModel.onClearRequested()

        assertThat(viewModel.headerState.value.selectedMember).isNull()
        assertThat(viewModel.headerState.value.isSelectedMemberRemovalPending).isFalse()
    }

    @Test
    fun `custom suggestions provider uses configured limit and updates header suggestions`() = runTest(dispatcher) {
        val provider = FakeSuggestionsProvider(
            suggestionsByQuery = mapOf("mar" to listOf(SceytUser("member-1")))
        )
        val viewModel = TestGlobalSearchHeaderViewModel(
            memberSuggestionsProvider = provider,
            memberSuggestionsLimit = 3,
            ioDispatcher = dispatcher
        )
        advanceUntilIdle()

        viewModel.onQueryChanged("mar")
        advanceUntilIdle()

        assertThat(provider.calls).containsExactly(SuggestionCall(query = "mar", limit = 3))
        assertThat(viewModel.headerState.value.memberSuggestions.map { it.id })
            .containsExactly("member-1")
    }

    @Test
    fun `suggestions debounce waits before invoking provider`() = runTest(dispatcher) {
        val provider = FakeSuggestionsProvider(
            suggestionsByQuery = mapOf("ali" to listOf(SceytUser("member-1")))
        )
        val viewModel = TestGlobalSearchHeaderViewModel(
            memberSuggestionsProvider = provider,
            memberSuggestionsDebounceMs = 200L,
            ioDispatcher = dispatcher
        )
        advanceUntilIdle()

        viewModel.onQueryChanged("ali")
        advanceTimeBy(199)

        assertThat(provider.calls).isEmpty()

        advanceTimeBy(1)
        advanceUntilIdle()

        assertThat(provider.calls).containsExactly(SuggestionCall(query = "ali", limit = 8))
        assertThat(viewModel.headerState.value.memberSuggestions.map { it.id })
            .containsExactly("member-1")
    }

    @Test
    fun `stale suggestion results are ignored after query changes`() = runTest(dispatcher) {
        val provider = DelayedSuggestionsProvider(
            delayMs = 100L,
            suggestionsByQuery = mapOf(
                "a" to listOf(SceytUser("member-a")),
                "ab" to listOf(SceytUser("member-ab"))
            )
        )
        val viewModel = TestGlobalSearchHeaderViewModel(
            memberSuggestionsProvider = provider,
            ioDispatcher = dispatcher
        )
        advanceUntilIdle()

        viewModel.onQueryChanged("a")
        advanceTimeBy(50)
        viewModel.onQueryChanged("ab")
        advanceUntilIdle()

        assertThat(provider.calls.map { it.query }).containsExactly("a", "ab").inOrder()
        assertThat(viewModel.headerState.value.memberSuggestions.map { it.id })
            .containsExactly("member-ab")
    }

    @Test
    fun `suggestions provider failures fall back to empty suggestions`() = runTest(dispatcher) {
        val provider = GlobalSearchMemberSuggestionsProvider { _, _ ->
            error("boom")
        }
        val viewModel = TestGlobalSearchHeaderViewModel(
            memberSuggestionsProvider = provider,
            ioDispatcher = dispatcher
        )
        advanceUntilIdle()

        viewModel.onQueryChanged("ops")
        advanceUntilIdle()

        assertThat(viewModel.headerState.value.memberSuggestions).isEmpty()
    }

    @Test
    fun `session registry entry is removed when header viewmodel is cleared`() = runTest(dispatcher) {
        val viewModel = TestGlobalSearchHeaderViewModel(ioDispatcher = dispatcher)

        assertThat(GlobalSearchSessionRegistry.contains(viewModel.sessionId)).isTrue()

        viewModel.clearForTest()

        assertThat(GlobalSearchSessionRegistry.contains(viewModel.sessionId)).isFalse()
    }
}

private class TestGlobalSearchHeaderViewModel(
    initialTab: GlobalSearchTab = GlobalSearchTab.Chats,
    memberSuggestionsProvider: GlobalSearchMemberSuggestionsProvider = GlobalSearchMemberSuggestionsProvider { _, _ ->
        emptyList()
    },
    memberSuggestionsLimit: Int = DEFAULT_MEMBER_SUGGESTIONS_LIMIT,
    memberSuggestionsDebounceMs: Long = DEFAULT_MEMBER_SUGGESTIONS_DEBOUNCE_MS,
    ioDispatcher: CoroutineDispatcher,
) : GlobalSearchHeaderViewModel(
    initialTab = initialTab,
    memberSuggestionsProvider = memberSuggestionsProvider,
    memberSuggestionsLimit = memberSuggestionsLimit,
    memberSuggestionsDebounceMs = memberSuggestionsDebounceMs,
    ioDispatcher = ioDispatcher
) {
    fun requireSession(): GlobalSearchSession {
        return GlobalSearchSessionRegistry.require(sessionId)
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

    override suspend fun searchMemberSuggestions(query: String, limit: Int): List<SceytUser> {
        return emptyList()
    }

    override suspend fun getRecentChats(offset: Int, limit: Int): GlobalSearchPage<com.sceyt.chatuikit.data.models.channels.SceytChannel> {
        recentChatsCalls++
        return GlobalSearchPage(data = listOf(mock()), hasMore = false)
    }

    override suspend fun searchChats(query: String, limit: Int): GlobalSearchPage<com.sceyt.chatuikit.data.models.channels.SceytChannel> {
        searchChatsCalls++
        return GlobalSearchPage(data = listOf(mock()), hasMore = false)
    }

    override suspend fun getRecentChannels(offset: Int, limit: Int): GlobalSearchPage<com.sceyt.chatuikit.data.models.channels.SceytChannel> {
        recentChannelsCalls++
        return GlobalSearchPage(data = listOf(mock()), hasMore = false)
    }

    override suspend fun searchChannels(query: String, limit: Int): GlobalSearchPage<com.sceyt.chatuikit.data.models.channels.SceytChannel> {
        searchChannelsCalls++
        return GlobalSearchPage(data = listOf(mock()), hasMore = false)
    }

    override suspend fun searchMessages(
        query: String,
        senderId: String?,
        offset: Int,
        limit: Int,
    ): GlobalSearchPage<GlobalSearchMessageResult> {
        searchMessagesCalls += MessageCall(query = query, senderId = senderId, offset = offset)
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
        tab: GlobalSearchTab,
        query: String,
        senderId: String?,
        offset: Int,
        limit: Int,
    ): GlobalSearchPage<GlobalSearchAttachmentResult> {
        attachmentCalls += AttachmentCall(tab = tab, query = query, senderId = senderId, offset = offset)
        return GlobalSearchPage(
            data = listOf(
                GlobalSearchAttachmentResult(
                    attachment = mock(),
                    message = mock(),
                    channel = mock(),
                    sender = null,
                    kind = when (tab) {
                        GlobalSearchTab.Media -> GlobalSearchAttachmentKind.Media
                        GlobalSearchTab.Files -> GlobalSearchAttachmentKind.File
                        GlobalSearchTab.Voice -> GlobalSearchAttachmentKind.Voice
                        GlobalSearchTab.Links -> GlobalSearchAttachmentKind.Link
                        else -> GlobalSearchAttachmentKind.Media
                    }
                )
            ),
            hasMore = false
        )
    }
}

internal data class MessageCall(
    val query: String,
    val senderId: String?,
    val offset: Int,
)

internal data class AttachmentCall(
    val tab: GlobalSearchTab,
    val query: String,
    val senderId: String?,
    val offset: Int,
)

internal class FakeSuggestionsProvider(
    private val suggestionsByQuery: Map<String, List<SceytUser>> = emptyMap(),
) : GlobalSearchMemberSuggestionsProvider {
    val calls = mutableListOf<SuggestionCall>()

    override suspend fun provideSuggestions(query: String, limit: Int): List<SceytUser> {
        calls += SuggestionCall(query = query, limit = limit)
        return suggestionsByQuery[query].orEmpty()
    }
}

internal class DelayedSuggestionsProvider(
    private val delayMs: Long,
    private val suggestionsByQuery: Map<String, List<SceytUser>>,
) : GlobalSearchMemberSuggestionsProvider {
    val calls = mutableListOf<SuggestionCall>()

    override suspend fun provideSuggestions(query: String, limit: Int): List<SceytUser> {
        calls += SuggestionCall(query = query, limit = limit)
        delay(delayMs)
        return suggestionsByQuery[query].orEmpty()
    }
}

internal data class SuggestionCall(
    val query: String,
    val limit: Int,
)
