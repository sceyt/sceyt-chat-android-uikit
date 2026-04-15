package com.sceyt.chatuikit.presentation.components.global_search

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.data.models.search.GlobalSearchAttachmentKind
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.persistence.interactor.ChannelInteractor
import com.sceyt.chatuikit.presentation.components.global_search.channels.ChannelsSearchViewModel
import com.sceyt.chatuikit.presentation.components.global_search.chats.ChatsSearchViewModel
import com.sceyt.chatuikit.presentation.components.global_search.media.MediaSearchViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.dsl.module
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GlobalSearchTabViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val channelInteractor = mock<ChannelInteractor>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        stopKoin()
        SceytKoinApp.koinApp = startKoin {
            modules(
                module {
                    single<ChannelInteractor> { channelInteractor }
                }
            )
        }
        runBlocking {
            whenever(
                channelInteractor.loadChannels(
                    any(),
                    any(),
                    anyOrNull(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(emptyFlow())
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        SceytKoinApp.koinApp = null
        stopKoin()
    }

    @Test
    fun `chats tab loads when query changes but not when only tab changes`() = runTest(dispatcher) {
        val dataSource = FakeGlobalSearchDataSource()
        val session =
            GlobalSearchSessionStore(GlobalSearchSessionState(activeTab = GlobalSearchTab.Chats))
        TestChatsSearchViewModel(session, dataSource, dispatcher)
        MediaSearchViewModel(session, dataSource, dispatcher)
        advanceUntilIdle()

        session.update { it.copy(query = "alpha") }
        advanceTimeBy(300)
        advanceUntilIdle()
        session.update { it.copy(activeTab = GlobalSearchTab.Media) }
        advanceUntilIdle()

        assertThat(dataSource.searchChatsCalls).isEqualTo(1)
        assertThat(
            dataSource.attachmentCalls.count {
                it.kind == GlobalSearchAttachmentKind.Media && it.query == "alpha"
            }
        ).isEqualTo(
            1
        )

        session.update { it.copy(activeTab = GlobalSearchTab.Chats) }
        advanceUntilIdle()
        assertThat(dataSource.searchChatsCalls).isEqualTo(1)

        session.update { it.copy(query = "beta") }
        advanceTimeBy(300)
        advanceUntilIdle()
        assertThat(dataSource.searchChatsCalls).isEqualTo(2)

        session.update { it.copy(activeTab = GlobalSearchTab.Media) }
        advanceUntilIdle()
        assertThat(
            dataSource.attachmentCalls.count {
                it.kind == GlobalSearchAttachmentKind.Media && it.query == "beta"
            }
        ).isEqualTo(
            1
        )

        session.update { it.copy(activeTab = GlobalSearchTab.Chats) }
        session.update { it.copy(activeTab = GlobalSearchTab.Media) }
        advanceUntilIdle()
        assertThat(
            dataSource.attachmentCalls.count {
                it.kind == GlobalSearchAttachmentKind.Media && it.query == "beta"
            }
        ).isEqualTo(
            1
        )
    }

    @Test
    fun `channels tab reloads selected-member message search when member or query changes`() = runTest(dispatcher) {
        val dataSource = FakeGlobalSearchDataSource()
        val session =
            GlobalSearchSessionStore(GlobalSearchSessionState(activeTab = GlobalSearchTab.Channels))
        ChannelsSearchViewModel(session, dataSource, dispatcher)
        advanceUntilIdle()

        assertThat(dataSource.recentChannelsCalls).isEqualTo(1)

        session.update { it.copy(selectedMember = SceytUser("member-1")) }
        advanceUntilIdle()
        assertThat(dataSource.recentChannelsCalls).isEqualTo(1)
        assertThat(dataSource.searchMessagesCalls).hasSize(1)
        assertThat(dataSource.searchMessagesCalls.last().senderId).isEqualTo("member-1")
        assertThat(dataSource.searchMessagesCalls.last().query).isEmpty()

        session.update { it.copy(query = "engineering") }
        advanceTimeBy(300)
        advanceUntilIdle()
        assertThat(dataSource.searchChannelsCalls).isEqualTo(0)
        assertThat(dataSource.searchMessagesCalls).hasSize(2)
        assertThat(dataSource.searchMessagesCalls.last().senderId).isEqualTo("member-1")
        assertThat(dataSource.searchMessagesCalls.last().query).isEqualTo("engineering")

        session.update { it.copy(selectedMember = SceytUser("member-2")) }
        advanceUntilIdle()
        assertThat(dataSource.searchChannelsCalls).isEqualTo(0)
        assertThat(dataSource.searchMessagesCalls).hasSize(3)
        assertThat(dataSource.searchMessagesCalls.last().senderId).isEqualTo("member-2")
        assertThat(dataSource.searchMessagesCalls.last().query).isEqualTo("engineering")
    }

    @Test
    fun `selecting a tab with stale session state triggers a first page load`() =
        runTest(dispatcher) {
            val dataSource = FakeGlobalSearchDataSource()
            val session =
                GlobalSearchSessionStore(GlobalSearchSessionState(activeTab = GlobalSearchTab.Chats))
            MediaSearchViewModel(session, dataSource, dispatcher)
            advanceUntilIdle()

            session.update { it.copy(query = "beta") }
            advanceTimeBy(300)
            advanceUntilIdle()

            assertThat(
                dataSource.attachmentCalls.count {
                    it.kind == GlobalSearchAttachmentKind.Media && it.query == "beta"
                }
            ).isEqualTo(
                0
            )

            session.update { it.copy(activeTab = GlobalSearchTab.Media) }
            advanceUntilIdle()

            assertThat(
                dataSource.attachmentCalls.count {
                    it.kind == GlobalSearchAttachmentKind.Media && it.query == "beta"
                }
            ).isEqualTo(
                1
            )
        }

    @Test
    fun `media tab preserves list and grid presentation state`() = runTest(dispatcher) {
        val dataSource = FakeGlobalSearchDataSource()
        val session =
            GlobalSearchSessionStore(GlobalSearchSessionState(activeTab = GlobalSearchTab.Media))
        val viewModel = MediaSearchViewModel(session, dataSource, dispatcher)
        advanceUntilIdle()

        assertThat(viewModel.state.value.query).isEmpty()

        session.update { it.copy(query = "cat") }
        advanceTimeBy(300)
        advanceUntilIdle()

        assertThat(viewModel.state.value.query).isEqualTo("cat")
    }

    @Test
    fun `custom chats viewmodel subclass can observe session and publish custom state`() =
        runTest(dispatcher) {
            val dataSource = FakeGlobalSearchDataSource()
            val session =
                GlobalSearchSessionStore(GlobalSearchSessionState(activeTab = GlobalSearchTab.Chats))
            val viewModel = object : ChatsSearchViewModel(session, dataSource, dispatcher) {
                override suspend fun performLoad(
                    state: GlobalSearchSessionState,
                    offset: Int,
                    pageSize: Int,
                ): SearchResultPage {
                    return SearchResultPage(
                        listItems = if (state.query == "override") {
                            listOf(GlobalSearchListItem.SectionHeader(R.string.sceyt_chats))
                        } else {
                            emptyList()
                        },
                        loadedCount = if (state.query == "override") 1 else 0,
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
    private val fakeDataSource: FakeGlobalSearchDataSource,
    ioDispatcher: CoroutineDispatcher,
) : ChatsSearchViewModel(session, fakeDataSource, ioDispatcher) {
    override suspend fun performLoad(
        state: GlobalSearchSessionState,
        offset: Int,
        pageSize: Int,
    ): SearchResultPage {
        return when {
            state.selectedMember != null -> {
                val page = fakeDataSource.searchMessages(
                    query = state.query,
                    senderId = state.selectedMember.id,
                    channelTypes = emptyList(),
                    onlyJoined = false,
                    offset = offset,
                    limit = pageSize,
                )
                SearchResultPage(
                    listItems = page.data.map { GlobalSearchListItem.MessageItem(it, state.query) },
                    hasMore = page.hasMore,
                    loadedCount = page.data.size
                )
            }

            state.query.isBlank() -> {
                val page = fakeDataSource.getRecentChats(offset, pageSize)
                SearchResultPage(
                    listItems = page.data.map { GlobalSearchListItem.ChannelItem(it) },
                    hasMore = page.hasMore,
                    loadedCount = page.data.size
                )
            }

            else -> {
                val chatsPage = fakeDataSource.searchChats(
                    query = state.query,
                    offset = 0,
                    limit = pageSize,
                )
                val messagesPage = fakeDataSource.searchMessages(
                    query = state.query,
                    senderId = null,
                    channelTypes = emptyList(),
                    onlyJoined = false,
                    offset = 0,
                    limit = pageSize,
                )
                SearchResultPage(
                    listItems = buildList {
                        if (chatsPage.data.isNotEmpty()) {
                            add(GlobalSearchListItem.SectionHeader(R.string.sceyt_chats))
                            addAll(chatsPage.data.map { GlobalSearchListItem.ChannelItem(it) })
                        }
                        if (messagesPage.data.isNotEmpty()) {
                            add(GlobalSearchListItem.SectionHeader(R.string.sceyt_messages))
                            addAll(messagesPage.data.map {
                                GlobalSearchListItem.MessageItem(
                                    it,
                                    state.query
                                )
                            })
                        }
                    },
                    loadedCount = chatsPage.data.size + messagesPage.data.size
                )
            }
        }
    }
}
