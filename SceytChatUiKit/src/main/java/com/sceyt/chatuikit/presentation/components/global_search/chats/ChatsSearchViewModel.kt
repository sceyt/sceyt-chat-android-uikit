package com.sceyt.chatuikit.presentation.components.global_search.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchListItem
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchLocalInteractor
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchMessageResult
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchPage
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchSession
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchSessionState
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchTab
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val CHATS_SEARCH_DEBOUNCE_MS = 200L
private const val CHATS_DEFAULT_PAGE_SIZE = 20
private const val CHATS_MIN_QUERY_LENGTH_FOR_MESSAGES = 2

data class ChatsSearchRequestKey(
    val query: String = "",
    val selectedMemberId: String? = null,
)

data class ChatsSearchState(
    val requestKey: ChatsSearchRequestKey? = null,
    val query: String = "",
    val listItems: List<GlobalSearchListItem> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val isLoaded: Boolean = false,
    val showMessageChannel: Boolean = true,
) {
    val showEmptyState: Boolean
        get() = !isLoading && isLoaded && listItems.isEmpty()
}

open class ChatsSearchViewModel(
    protected val session: GlobalSearchSession,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val defaultDataSource by lazy(LazyThreadSafetyMode.NONE) { GlobalSearchLocalInteractor() }

    private var latestSessionState: GlobalSearchSessionState = session.state.value
    private val _state = MutableStateFlow(createDefaultState(defaultRequestKey(latestSessionState)))
    val state: StateFlow<ChatsSearchState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            session.state.collectLatest(::onSessionStateChanged)
        }
    }

    private suspend fun onSessionStateChanged(sessionState: GlobalSearchSessionState) {
        val previous = latestSessionState
        latestSessionState = sessionState
        val key = currentRequestKey(sessionState)

        if (_state.value.requestKey != key) {
            // Keep the existing results if the query and selected member are the same,
            // otherwise reset the state for new search.
            _state.update { current ->
                current.copy(
                    requestKey = key,
                    query = key.query,
                    isLoading = false,
                    isLoadingMore = false,
                    hasMore = false,
                    isLoaded = false,
                    showMessageChannel = sessionState.shouldShowMessageChannel(),
                )
            }
        } else {
            _state.update { it.copy(showMessageChannel = sessionState.shouldShowMessageChannel()) }
        }

        if (!sessionState.isCurrent(GlobalSearchTab.Chats)) return

        val current = _state.value
        if (current.isLoaded || current.isLoading) return

        if (shouldDebounce(previous, sessionState))
            delay(CHATS_SEARCH_DEBOUNCE_MS)

        val s = _state.value
        if (!s.isLoaded && !s.isLoading)
            loadFirstPage(key)
    }

    open fun loadMore() {
        val key = currentRequestKey()
        val current = _state.value
        if (current.requestKey != key || key.query.isNotBlank() ||
            current.isLoading || current.isLoadingMore || !current.hasMore
        ) return
        loadNextPage(key)
    }

    private fun loadFirstPage(key: ChatsSearchRequestKey) {
        loadJob?.cancel()
        _state.update { it.copy(isLoading = true) }

        val job = viewModelScope.launch(ioDispatcher) {
            val result = performLoad(
                requestKey = key,
                offset = 0,
                pageSize = CHATS_DEFAULT_PAGE_SIZE
            )
            withContext(Dispatchers.Main) {
                if (currentRequestKey() != key || _state.value.requestKey != key) return@withContext
                _state.update {
                    it.copy(
                        listItems = result.listItems,
                        isLoading = false,
                        isLoadingMore = false,
                        hasMore = hasMore(key, result),
                        isLoaded = true,
                        query = key.query,
                        showMessageChannel = latestSessionState.shouldShowMessageChannel()
                    )
                }
            }
        }
        loadJob = job
        job.invokeOnCompletion { if (loadJob === job) loadJob = null }
    }

    private fun loadNextPage(key: ChatsSearchRequestKey) {
        loadJob?.cancel()
        _state.update { it.copy(isLoadingMore = true) }

        val job = viewModelScope.launch(ioDispatcher) {
            val result = performLoad(
                requestKey = key,
                offset = _state.value.listItems.size,
                pageSize = CHATS_DEFAULT_PAGE_SIZE
            )
            withContext(Dispatchers.Main) {
                if (currentRequestKey() != key || _state.value.requestKey != key) return@withContext
                _state.update {
                    it.copy(
                        listItems = it.listItems + result.listItems,
                        isLoading = false,
                        isLoadingMore = false,
                        hasMore = hasMore(key, result),
                        isLoaded = true,
                        query = key.query,
                        showMessageChannel = latestSessionState.shouldShowMessageChannel()
                    )
                }
            }
        }
        loadJob = job
        job.invokeOnCompletion { if (loadJob === job) loadJob = null }
    }

    private fun currentRequestKey(
        sessionState: GlobalSearchSessionState = latestSessionState,
    ) = defaultRequestKey(sessionState)

    private fun shouldDebounce(
        previous: GlobalSearchSessionState,
        current: GlobalSearchSessionState,
    ): Boolean {
        return previous.activeTab == GlobalSearchTab.Chats &&
                previous.query != current.query &&
                current.query.isNotBlank()
    }

    private fun createDefaultState(key: ChatsSearchRequestKey) = ChatsSearchState(
        requestKey = key,
        query = key.query,
        showMessageChannel = latestSessionState.shouldShowMessageChannel(),
    )

    private fun GlobalSearchSessionState.shouldShowMessageChannel(): Boolean {
        return selectedMember == null
    }

    protected open suspend fun performLoad(
        requestKey: ChatsSearchRequestKey,
        offset: Int,
        pageSize: Int,
    ): SearchResultPage = when {
        requestKey.selectedMemberId != null -> {
            val page = loadSelectedMemberMessagesPage(
                requestKey = requestKey,
                offset = offset,
                pageSize = pageSize
            )
            SearchResultPage(
                listItems = page.data.map { GlobalSearchListItem.MessageItem(it) },
                hasMore = page.hasMore,
                loadedCount = page.data.size,
            )
        }

        requestKey.query.isBlank() -> {
            val page = loadRecentChatsPage(offset, pageSize)
            SearchResultPage(
                listItems = buildListItems(page, GlobalSearchPage.Companion.empty()),
                hasMore = page.hasMore,
                loadedCount = page.data.size,
            )
        }

        else -> {
            coroutineScope {
                val chatsPage = async { loadTypedQueryChatsPage(requestKey, pageSize) }
                val messagesPage =
                    if (requestKey.query.length >= CHATS_MIN_QUERY_LENGTH_FOR_MESSAGES) {
                        async { loadTypedQueryMessagesPage(requestKey, pageSize) }
                    } else {
                        CompletableDeferred(GlobalSearchPage(emptyList(), false))
                    }
                val chatsResult = chatsPage.await()
                val messagesResult = messagesPage.await()
                val items = buildListItems(chatsResult, messagesResult)
                SearchResultPage(
                    listItems = items,
                    hasMore = false,
                    loadedCount = chatsResult.data.size + messagesResult.data.size,
                )
            }
        }
    }

    private suspend fun loadRecentChatsPage(
        offset: Int,
        pageSize: Int,
    ): GlobalSearchPage<SceytChannel> {
        return defaultDataSource.getRecentChats(offset = offset, limit = pageSize)
    }

    private suspend fun loadSelectedMemberMessagesPage(
        requestKey: ChatsSearchRequestKey,
        offset: Int,
        pageSize: Int,
    ): GlobalSearchPage<GlobalSearchMessageResult> {
        val selectedMemberId = requireNotNull(requestKey.selectedMemberId)
        return defaultDataSource.searchMessages(
            query = requestKey.query,
            senderId = selectedMemberId,
            offset = offset,
            limit = pageSize
        )
    }

    private suspend fun loadTypedQueryChatsPage(
        requestKey: ChatsSearchRequestKey,
        pageSize: Int,
    ): GlobalSearchPage<SceytChannel> {
        return defaultDataSource.searchChats(requestKey.query, pageSize)
    }

    private suspend fun loadTypedQueryMessagesPage(
        criteria: ChatsSearchRequestKey,
        pageSize: Int,
    ): GlobalSearchPage<GlobalSearchMessageResult> {
        return defaultDataSource.searchMessages(
            query = criteria.query,
            senderId = null,
            offset = 0,
            limit = pageSize
        )
    }

    private fun buildListItems(
        chatsPage: GlobalSearchPage<SceytChannel>,
        messagesPage: GlobalSearchPage<GlobalSearchMessageResult>,
    ): List<GlobalSearchListItem> {
        return buildList {
            if (chatsPage.data.isNotEmpty()) {
                add(GlobalSearchListItem.SectionHeader(R.string.sceyt_chats))
                addAll(chatsPage.data.map { GlobalSearchListItem.ChannelItem(it) })
            }
            if (messagesPage.data.isNotEmpty()) {
                add(GlobalSearchListItem.SectionHeader(R.string.sceyt_messages))
                addAll(messagesPage.data.map { GlobalSearchListItem.MessageItem(it) })
            }
        }
    }

    private fun hasMore(
        key: ChatsSearchRequestKey,
        result: SearchResultPage,
    ): Boolean {
        return key.query.isBlank() && result.hasMore
    }

    protected data class SearchResultPage(
        val listItems: List<GlobalSearchListItem> = emptyList(),
        val hasMore: Boolean = false,
        val loadedCount: Int = 0,
    )

    private fun defaultRequestKey(
        sessionState: GlobalSearchSessionState
    ) = ChatsSearchRequestKey(
        query = sessionState.query,
        selectedMemberId = sessionState.selectedMember?.id
    )
}

class ChatsSearchViewModelFactory(
    private val session: GlobalSearchSession,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatsSearchViewModel::class.java)) {
            return ChatsSearchViewModel(session, ioDispatcher) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
