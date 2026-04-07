package com.sceyt.chatuikit.presentation.components.global_search

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.data.models.messages.SceytUser
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
    fun `revisiting a loaded tab reuses cached results`() = runTest(dispatcher) {
        val dataSource = FakeGlobalSearchDataSource()
        val session = GlobalSearchSessionStore(GlobalSearchSessionState(activeTab = GlobalSearchTab.Chats))
        ChatsSearchViewModel(session, dataSource, dispatcher)
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
    fun `new query creates a new cache key and each tab loads once for it`() = runTest(dispatcher) {
        val dataSource = FakeGlobalSearchDataSource()
        val session = GlobalSearchSessionStore(GlobalSearchSessionState(activeTab = GlobalSearchTab.Chats))
        ChatsSearchViewModel(session, dataSource, dispatcher)
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
}
