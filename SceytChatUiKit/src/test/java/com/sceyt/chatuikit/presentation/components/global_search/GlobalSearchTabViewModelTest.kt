package com.sceyt.chatuikit.presentation.components.global_search

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.presentation.components.global_search.chats.ChatsSearchViewModel
import kotlinx.coroutines.CoroutineDispatcher
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

@OptIn(ExperimentalCoroutinesApi::class)
class GlobalSearchTabViewModelTest {
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
    fun `revisiting chats tab does not reload already loaded results`() = runTest(dispatcher) {
        val dataSource = FakeGlobalSearchDataSource()
        val session = GlobalSearchSessionStore(GlobalSearchSessionState(activeTab = GlobalSearchTab.Chats))
        TestChatsSearchViewModel(session, dataSource, dispatcher)
        MediaSearchViewModel(session, dataSource, dispatcher)
        advanceUntilIdle()

        assertThat(dataSource.recentChatsCalls).isEqualTo(1)

        session.update { it.copy(query = "design") }
        advanceTimeBy(299)
        assertThat(dataSource.searchChatsCalls).isEqualTo(0)

        advanceTimeBy(1)
        advanceUntilIdle()
        assertThat(dataSource.searchChatsCalls).isEqualTo(1)
        assertThat(dataSource.searchMessagesCalls.count { it.query == "design" && it.senderId == null }).isEqualTo(1)

        session.update { it.copy(activeTab = GlobalSearchTab.Media) }
        advanceUntilIdle()
        assertThat(dataSource.attachmentCalls.count { it.tab == GlobalSearchTab.Media && it.query == "design" }).isEqualTo(1)

        session.update { it.copy(activeTab = GlobalSearchTab.Chats) }
        advanceUntilIdle()
        assertThat(dataSource.searchChatsCalls).isEqualTo(1)
        assertThat(dataSource.searchMessagesCalls.count { it.query == "design" && it.senderId == null }).isEqualTo(1)
    }

    @Test
    fun `chats tab loads when query changes but not when only tab changes`() = runTest(dispatcher) {
        val dataSource = FakeGlobalSearchDataSource()
        val session = GlobalSearchSessionStore(GlobalSearchSessionState(activeTab = GlobalSearchTab.Chats))
        TestChatsSearchViewModel(session, dataSource, dispatcher)
        MediaSearchViewModel(session, dataSource, dispatcher)
        advanceUntilIdle()

        session.update { it.copy(query = "alpha") }
        advanceTimeBy(300)
        advanceUntilIdle()
        session.update { it.copy(activeTab = GlobalSearchTab.Media) }
        advanceUntilIdle()

        assertThat(dataSource.searchChatsCalls).isEqualTo(1)
        assertThat(dataSource.attachmentCalls.count { it.tab == GlobalSearchTab.Media && it.query == "alpha" }).isEqualTo(1)

        session.update { it.copy(activeTab = GlobalSearchTab.Chats) }
        advanceUntilIdle()
        assertThat(dataSource.searchChatsCalls).isEqualTo(1)

        session.update { it.copy(query = "beta") }
        advanceTimeBy(300)
        advanceUntilIdle()
        assertThat(dataSource.searchChatsCalls).isEqualTo(2)

        session.update { it.copy(activeTab = GlobalSearchTab.Media) }
        advanceUntilIdle()
        assertThat(dataSource.attachmentCalls.count { it.tab == GlobalSearchTab.Media && it.query == "beta" }).isEqualTo(1)

        session.update { it.copy(activeTab = GlobalSearchTab.Chats) }
        session.update { it.copy(activeTab = GlobalSearchTab.Media) }
        advanceUntilIdle()
        assertThat(dataSource.attachmentCalls.count { it.tab == GlobalSearchTab.Media && it.query == "beta" }).isEqualTo(1)
    }

    @Test
    fun `channels ignore selected member in cache key and search loading`() = runTest(dispatcher) {
        val dataSource = FakeGlobalSearchDataSource()
        val session = GlobalSearchSessionStore(GlobalSearchSessionState(activeTab = GlobalSearchTab.Channels))
        ChannelsSearchViewModel(session, dataSource, dispatcher)
        advanceUntilIdle()

        assertThat(dataSource.recentChannelsCalls).isEqualTo(1)

        session.update { it.copy(selectedMember = SceytUser("member-1")) }
        advanceUntilIdle()
        assertThat(dataSource.recentChannelsCalls).isEqualTo(1)

        session.update { it.copy(query = "engineering") }
        advanceTimeBy(300)
        advanceUntilIdle()
        assertThat(dataSource.searchChannelsCalls).isEqualTo(1)

        session.update { it.copy(selectedMember = SceytUser("member-2")) }
        advanceUntilIdle()
        assertThat(dataSource.searchChannelsCalls).isEqualTo(1)
    }

    @Test
    fun `typed queries debounce while blank query criteria load immediately`() = runTest(dispatcher) {
        val dataSource = FakeGlobalSearchDataSource()
        val session = GlobalSearchSessionStore(GlobalSearchSessionState(activeTab = GlobalSearchTab.Media))
        MediaSearchViewModel(session, dataSource, dispatcher)
        advanceUntilIdle()

        assertThat(dataSource.attachmentCalls.count { it.tab == GlobalSearchTab.Media && it.query.isBlank() && it.senderId == null }).isEqualTo(1)

        session.update { it.copy(query = "voice") }
        advanceTimeBy(299)
        assertThat(dataSource.attachmentCalls.count { it.tab == GlobalSearchTab.Media && it.query == "voice" }).isEqualTo(0)

        advanceTimeBy(1)
        advanceUntilIdle()
        assertThat(dataSource.attachmentCalls.count { it.tab == GlobalSearchTab.Media && it.query == "voice" }).isEqualTo(1)

        session.update { it.copy(query = "") }
        advanceUntilIdle()
        session.update { it.copy(selectedMember = SceytUser("member-42")) }
        advanceUntilIdle()
        assertThat(
            dataSource.attachmentCalls.count {
                it.tab == GlobalSearchTab.Media && it.query.isBlank() && it.senderId == "member-42"
            }
        ).isEqualTo(1)
    }

    @Test
    fun `selecting a tab with stale session state triggers a first page load`() = runTest(dispatcher) {
        val dataSource = FakeGlobalSearchDataSource()
        val session = GlobalSearchSessionStore(GlobalSearchSessionState(activeTab = GlobalSearchTab.Chats))
        MediaSearchViewModel(session, dataSource, dispatcher)
        advanceUntilIdle()

        session.update { it.copy(query = "beta") }
        advanceTimeBy(300)
        advanceUntilIdle()

        assertThat(dataSource.attachmentCalls.count { it.tab == GlobalSearchTab.Media && it.query == "beta" }).isEqualTo(0)

        session.update { it.copy(activeTab = GlobalSearchTab.Media) }
        advanceUntilIdle()

        assertThat(dataSource.attachmentCalls.count { it.tab == GlobalSearchTab.Media && it.query == "beta" }).isEqualTo(1)
    }

    @Test
    fun `media tab preserves list and grid presentation state`() = runTest(dispatcher) {
        val dataSource = FakeGlobalSearchDataSource()
        val session = GlobalSearchSessionStore(GlobalSearchSessionState(activeTab = GlobalSearchTab.Media))
        val viewModel = MediaSearchViewModel(session, dataSource, dispatcher)
        advanceUntilIdle()

        assertThat(viewModel.state.value.showMediaGrid).isTrue()
        assertThat(viewModel.state.value.mediaGridItems).isNotEmpty()
        assertThat(viewModel.state.value.query).isEmpty()

        session.update { it.copy(query = "cat") }
        advanceTimeBy(300)
        advanceUntilIdle()

        assertThat(viewModel.state.value.showMediaGrid).isFalse()
        assertThat(viewModel.state.value.listItems).isNotEmpty()
        assertThat(viewModel.state.value.query).isEqualTo("cat")
    }

    @Test
    fun `custom chats viewmodel subclass can observe session and publish custom state`() = runTest(dispatcher) {
        val session = GlobalSearchSessionStore(GlobalSearchSessionState(activeTab = GlobalSearchTab.Chats))
        val viewModel = object : ChatsSearchViewModel(session, dispatcher) {
            override suspend fun performLoad(
                criteria: SearchCriteria,
                offset: Int,
                pageSize: Int,
            ): SearchResultPage {
                return SearchResultPage(
                    listItems = if (criteria.query == "override") {
                        listOf(GlobalSearchListItem.SectionHeader(R.string.sceyt_chats))
                    } else {
                        emptyList()
                    },
                    loadedCount = if (criteria.query == "override") 1 else 0,
                )
            }
        }
        advanceUntilIdle()

        session.update { it.copy(query = "override") }
        advanceTimeBy(300)
        advanceUntilIdle()

        assertThat(viewModel.state.value.listItems)
            .containsExactly(GlobalSearchListItem.SectionHeader(R.string.sceyt_chats))
    }
}

private class TestChatsSearchViewModel(
    session: GlobalSearchSession,
    private val dataSource: FakeGlobalSearchDataSource,
    ioDispatcher: CoroutineDispatcher,
) : ChatsSearchViewModel(session, ioDispatcher) {
    override suspend fun performLoad(
        criteria: SearchCriteria,
        offset: Int,
        pageSize: Int,
    ): SearchResultPage {
        return when {
            criteria.selectedMemberId != null -> {
                val page = dataSource.searchMessages(criteria.query, criteria.selectedMemberId, offset, pageSize)
                SearchResultPage(
                    listItems = page.data.map { GlobalSearchListItem.MessageItem(it) },
                    hasMore = page.hasMore,
                    loadedCount = page.data.size
                )
            }

            criteria.query.isBlank() -> {
                val page = dataSource.getRecentChats(offset, pageSize)
                SearchResultPage(
                    listItems = page.data.map { GlobalSearchListItem.ChannelItem(it) },
                    hasMore = page.hasMore,
                    loadedCount = page.data.size
                )
            }

            else -> {
                val chatsPage = dataSource.searchChats(criteria.query, pageSize)
                val messagesPage = dataSource.searchMessages(criteria.query, null, 0, pageSize)
                SearchResultPage(
                    listItems = buildList {
                        if (chatsPage.data.isNotEmpty()) {
                            add(GlobalSearchListItem.SectionHeader(R.string.sceyt_chats))
                            addAll(chatsPage.data.map { GlobalSearchListItem.ChannelItem(it) })
                        }
                        if (messagesPage.data.isNotEmpty()) {
                            add(GlobalSearchListItem.SectionHeader(R.string.sceyt_messages))
                            addAll(messagesPage.data.map { GlobalSearchListItem.MessageItem(it) })
                        }
                    },
                    loadedCount = chatsPage.data.size + messagesPage.data.size
                )
            }
        }
    }
}
