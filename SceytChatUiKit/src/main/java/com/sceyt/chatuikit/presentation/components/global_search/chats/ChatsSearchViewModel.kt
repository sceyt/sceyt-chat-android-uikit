package com.sceyt.chatuikit.presentation.components.global_search.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.search.GlobalSearchMessageResult
import com.sceyt.chatuikit.data.models.search.GlobalSearchPage
import com.sceyt.chatuikit.persistence.interactor.GlobalSearchDataSource
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchListItem
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchSession
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchSessionState
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchTab
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchUpdateEventSource
import com.sceyt.chatuikit.presentation.components.global_search.applyChannelMessageUpdateEvent
import com.sceyt.chatuikit.presentation.components.global_search.defaults.DefaultGlobalSearchLocalInteractor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val CHATS_DEFAULT_PAGE_SIZE = 15
private const val CHATS_MIN_QUERY_LENGTH_FOR_MESSAGES = 2

data class ChatsSearchState(
    val sessionState: GlobalSearchSessionState? = null,
    val listItems: List<GlobalSearchListItem> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val offset: Int = 0,
    val refreshKey: Long = 0,
) {
    val showEmptyState: Boolean
        get() = !isLoading && !isLoadingMore && listItems.isEmpty()

    val query: String
        get() = sessionState?.query.orEmpty()

    val canLoadMore: Boolean
        get() = hasMore && !isLoading && !isLoadingMore
}

open class ChatsSearchViewModel(
    protected val session: GlobalSearchSession,
    protected val dataSource: GlobalSearchDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val globalSearchUpdateEventSource = GlobalSearchUpdateEventSource(viewModelScope)

    private val _state = MutableStateFlow(ChatsSearchState(isLoading = true))
    val state: StateFlow<ChatsSearchState> = _state.asStateFlow()

    private var loadJob: Job? = null
    var lastResultsRequestKeyForUI: GlobalSearchSessionState? = null
        private set

    fun onResultsRendered(key: GlobalSearchSessionState?) {
        lastResultsRequestKeyForUI = key
    }

    init {
        viewModelScope.launch {
            session.state.collectLatest(::onSessionStateChanged)
        }

        globalSearchUpdateEventSource.updatesFlow.onEach { event ->
            _state.update {
                it.copy(
                    listItems = it.listItems.applyChannelMessageUpdateEvent(event),
                    refreshKey = if (event.shouldUpdateRefreshKey) it.refreshKey + 1 else it.refreshKey
                )
            }
        }.launchIn(viewModelScope)
    }

    private suspend fun onSessionStateChanged(sessionState: GlobalSearchSessionState) {
        val current = _state.value
        if (current.sessionState == sessionState) return

        if (sessionState.isCurrent(GlobalSearchTab.Chats)) {
            loadFirstPage(sessionState)
        } else {
            val queryChanged = current.sessionState?.isQueryChanged(sessionState.query) == true
            if (queryChanged) {
                _state.update {
                    it.copy(
                        sessionState = sessionState,
                        listItems = emptyList(),
                        isLoading = true
                    )
                }
            }
        }
    }

    protected open fun loadFirstPage(sessionState: GlobalSearchSessionState) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch(ioDispatcher) {
            val result = performLoad(
                state = sessionState,
                offset = 0,
                pageSize = CHATS_DEFAULT_PAGE_SIZE
            )
            withContext(Dispatchers.Main) {
                _state.update {
                    it.copy(
                        sessionState = sessionState,
                        listItems = result.listItems,
                        isLoading = false,
                        isLoadingMore = false,
                        hasMore = result.hasMore,
                        offset = result.loadedCount,
                    )
                }
            }
        }
    }

    open fun loadMore() {
        val current = _state.value
        val key = current.sessionState ?: return
        if (!current.canLoadMore) return
        loadNextPage(key)
    }

    protected open fun loadNextPage(sessionState: GlobalSearchSessionState) {
        loadJob?.cancel()
        val offset = _state.value.offset
        _state.update { it.copy(isLoadingMore = true) }

        loadJob = viewModelScope.launch(ioDispatcher) {
            val result = performLoad(
                state = sessionState,
                offset = offset,
                pageSize = CHATS_DEFAULT_PAGE_SIZE
            )
            withContext(Dispatchers.Main) {
                _state.update {
                    it.copy(
                        listItems = it.listItems + result.listItems,
                        isLoadingMore = false,
                        hasMore = result.hasMore,
                        offset = it.offset + result.loadedCount,
                    )
                }
            }
        }
    }

    protected open suspend fun performLoad(
        state: GlobalSearchSessionState,
        offset: Int,
        pageSize: Int,
    ): SearchResultPage = when {
        state.selectedMember != null -> loadSelectedMemberPage(state, offset, pageSize)
        state.query.isBlank() -> loadRecentPage(state, offset, pageSize)
        else -> loadTypedQueryPage(state, offset, pageSize)
    }

    private suspend fun loadSelectedMemberPage(
        state: GlobalSearchSessionState,
        offset: Int,
        pageSize: Int,
    ): SearchResultPage {
        val page = loadSelectedUserMessagesPage(state, offset, pageSize)
        return SearchResultPage(
            listItems = buildListItems(
                chatsPage = GlobalSearchPage.empty(),
                messagesPage = page,
                query = state.query,
                includeHeader = offset == 0,
            ),
            hasMore = page.hasMore,
            loadedCount = page.data.size,
        )
    }

    private suspend fun loadRecentPage(
        state: GlobalSearchSessionState,
        offset: Int,
        pageSize: Int,
    ): SearchResultPage {
        val page = loadRecentChatsPage(offset, pageSize)
        return SearchResultPage(
            listItems = buildListItems(
                chatsPage = page,
                messagesPage = GlobalSearchPage.empty(),
                query = state.query,
                includeHeader = offset == 0,
            ),
            hasMore = page.hasMore,
            loadedCount = page.data.size,
        )
    }

    private suspend fun loadTypedQueryPage(
        state: GlobalSearchSessionState,
        offset: Int,
        pageSize: Int,
    ): SearchResultPage {
        if (offset > 0) {
            return loadTypedQueryMoreMessagesPage(state, offset, pageSize)
        }
        return coroutineScope {
            val chatsPage = async { loadTypedQueryChatsPage(state, 0, pageSize) }
            val messagesPage = if (state.query.length >= CHATS_MIN_QUERY_LENGTH_FOR_MESSAGES) {
                async { loadTypedQueryMessagesPage(state, 0, pageSize) }
            } else {
                CompletableDeferred(GlobalSearchPage(emptyList(), false))
            }
            val chatsResult = chatsPage.await()
            val messagesResult = messagesPage.await()
            SearchResultPage(
                listItems = buildListItems(
                    chatsResult,
                    messagesResult,
                    state.query,
                    includeHeader = true
                ),
                hasMore = messagesResult.hasMore,
                loadedCount = messagesResult.data.size,
            )
        }
    }

    private suspend fun loadTypedQueryMoreMessagesPage(
        state: GlobalSearchSessionState,
        offset: Int,
        pageSize: Int,
    ): SearchResultPage {
        val page = if (state.query.length >= CHATS_MIN_QUERY_LENGTH_FOR_MESSAGES) {
            loadTypedQueryMessagesPage(state, offset, pageSize)
        } else {
            GlobalSearchPage(emptyList(), false)
        }
        return SearchResultPage(
            listItems = page.data.map { GlobalSearchListItem.MessageItem(it, state.query) },
            hasMore = page.hasMore,
            loadedCount = page.data.size,
        )
    }

    private suspend fun loadRecentChatsPage(
        offset: Int,
        pageSize: Int,
    ): GlobalSearchPage<SceytChannel> {
        return dataSource.getRecentChats(offset = offset, limit = pageSize)
    }

    private suspend fun loadSelectedUserMessagesPage(
        state: GlobalSearchSessionState,
        offset: Int,
        pageSize: Int,
    ): GlobalSearchPage<GlobalSearchMessageResult> {
        val selectedMemberId = requireNotNull(state.selectedMember?.id)
        val types = SceytChatUIKit.config.channelTypesConfig.getPrivateTypes()
        return dataSource.searchMessages(
            query = state.query,
            senderId = selectedMemberId,
            channelTypes = types,
            onlyJoined = true,
            offset = offset,
            limit = pageSize
        )
    }

    private suspend fun loadTypedQueryChatsPage(
        state: GlobalSearchSessionState,
        offset: Int,
        pageSize: Int,
    ): GlobalSearchPage<SceytChannel> {
        return dataSource.searchChats(state.query, offset = offset, pageSize)
    }

    private suspend fun loadTypedQueryMessagesPage(
        state: GlobalSearchSessionState,
        offset: Int,
        pageSize: Int,
    ): GlobalSearchPage<GlobalSearchMessageResult> {
        val types = SceytChatUIKit.config.channelTypesConfig.getPrivateTypes()
        return dataSource.searchMessages(
            query = state.query,
            senderId = null,
            channelTypes = types,
            onlyJoined = true,
            offset = offset,
            limit = pageSize
        )
    }

    private fun buildListItems(
        chatsPage: GlobalSearchPage<SceytChannel>,
        messagesPage: GlobalSearchPage<GlobalSearchMessageResult>,
        query: String,
        includeHeader: Boolean,
    ): List<GlobalSearchListItem> {
        return buildList {
            if (chatsPage.data.isNotEmpty()) {
                if (includeHeader) add(GlobalSearchListItem.SectionHeader(R.string.sceyt_chats))
                addAll(chatsPage.data.map { GlobalSearchListItem.ChannelItem(it) })
            }
            if (messagesPage.data.isNotEmpty()) {
                if (includeHeader) add(GlobalSearchListItem.SectionHeader(R.string.sceyt_messages))
                addAll(messagesPage.data.map { GlobalSearchListItem.MessageItem(it, query) })
            }
        }
    }

    protected data class SearchResultPage(
        val listItems: List<GlobalSearchListItem> = emptyList(),
        val hasMore: Boolean = false,
        val loadedCount: Int = 0,
    )
}

class ChatsSearchViewModelFactory(
    private val session: GlobalSearchSession,
    private val dataSource: GlobalSearchDataSource = DefaultGlobalSearchLocalInteractor(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatsSearchViewModel::class.java)) {
            return ChatsSearchViewModel(session, dataSource, ioDispatcher) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
