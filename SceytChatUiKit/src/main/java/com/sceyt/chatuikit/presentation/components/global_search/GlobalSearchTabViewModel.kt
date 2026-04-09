package com.sceyt.chatuikit.presentation.components.global_search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sceyt.chatuikit.R
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val SEARCH_DEBOUNCE_MS = 300L
private const val DEFAULT_PAGE_SIZE = 24
private const val SEARCH_PAGE_SIZE = 20

abstract class GlobalSearchTabViewModel : ViewModel() {
    abstract val state: StateFlow<GlobalSearchTabState>

    abstract fun loadMore()

    open fun retry() = Unit
}

internal abstract class GlobalSearchSessionTabViewModel(
    private val tab: GlobalSearchTab,
    private val session: GlobalSearchSession,
    private val interactor: GlobalSearchDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : GlobalSearchTabViewModel() {
    private var latestSessionState: GlobalSearchSessionState = session.state.value
    private val _state = MutableStateFlow(defaultTabState(currentKey(session.state.value)))
    override val state: StateFlow<GlobalSearchTabState> = _state.asStateFlow()

    private val cacheEntries = mutableMapOf<GlobalSearchRequestKey, TabCacheEntry>()
    private var loadJob: Job? = null
    private var loadJobKey: GlobalSearchRequestKey? = null
    private var searchJob: Job? = null

    init {
        publishVisibleState(currentKey(latestSessionState))
        session.state
            .onEach(::onSessionStateChanged)
            .launchIn(viewModelScope)
    }

    override fun loadMore() {
        val key = currentKey()
        val entry = cacheEntries[key] ?: return
        if (key.query.isNotBlank() || entry.state.isLoading || entry.state.isLoadingMore || !entry.state.hasMore) {
            return
        }
        loadPage(key, reset = false)
    }

    override fun retry() {
        searchJob?.cancel()
        loadPage(currentKey(), reset = true)
    }

    private fun onSessionStateChanged(sessionState: GlobalSearchSessionState) {
        val previous = latestSessionState
        latestSessionState = sessionState
        val key = currentKey(sessionState)
        publishVisibleState(key)

        if (sessionState.activeTab != tab) {
            searchJob?.cancel()
            return
        }

        ensureLoaded(
            key = key,
            debounce = shouldDebounce(previous, sessionState)
        )
    }

    private fun shouldDebounce(
        previous: GlobalSearchSessionState,
        current: GlobalSearchSessionState,
    ): Boolean {
        return previous.activeTab == tab &&
                previous.query != current.query &&
                current.query.isNotBlank()
    }

    private fun ensureLoaded(
        key: GlobalSearchRequestKey,
        debounce: Boolean,
    ) {
        searchJob?.cancel()
        if (debounce) {
            searchJob = viewModelScope.launch {
                delay(SEARCH_DEBOUNCE_MS)
                loadIfNeeded(key)
            }
        } else {
            loadIfNeeded(key)
        }
    }

    private fun loadIfNeeded(key: GlobalSearchRequestKey) {
        val entry = cacheEntries[key]
        if (entry?.state?.isLoaded == true || entry?.state?.isLoading == true || entry?.state?.isLoadingMore == true) {
            publishVisibleState(key)
            return
        }
        loadPage(key, reset = true)
    }

    private fun loadPage(
        key: GlobalSearchRequestKey,
        reset: Boolean,
    ) {
        cancelActiveLoad()

        val currentEntry = cacheEntries[key] ?: TabCacheEntry(
            state = defaultTabState(key),
            loadedCount = 0
        )
        val baseState = currentEntry.state.copy(
            tab = tab,
            requestKey = key,
            query = key.query,
            showMessageChannel = showMessageChannel()
        )
        val loadingState = if (reset) {
            baseState.copy(
                listItems = emptyList(),
                mediaGridItems = emptyList(),
                isLoading = true,
                isLoadingMore = false,
                hasMore = false,
                emptyState = null,
                isLoaded = false
            )
        } else {
            baseState.copy(
                isLoading = false,
                isLoadingMore = true
            )
        }
        cacheEntries[key] = currentEntry.copy(
            state = loadingState,
            loadedCount = if (reset) 0 else currentEntry.loadedCount
        )
        publishVisibleState(key)

        val job = viewModelScope.launch(ioDispatcher) {
            val offset = if (reset) 0 else currentEntry.loadedCount
            val pageSize = if (key.query.isBlank()) DEFAULT_PAGE_SIZE else SEARCH_PAGE_SIZE
            val result = performLoad(key.toCriteria(), offset, pageSize)

            withContext(Dispatchers.Main) {
                val entry = cacheEntries[key] ?: return@withContext
                val updatedState = if (reset) {
                    entry.state.copy(
                        listItems = result.listItems,
                        mediaGridItems = result.mediaGridItems,
                        isLoading = false,
                        isLoadingMore = false,
                        hasMore = key.query.isBlank() && result.hasMore,
                        emptyState = result.emptyState,
                        isLoaded = true,
                        query = key.query,
                        showMessageChannel = showMessageChannel()
                    )
                } else {
                    entry.state.copy(
                        listItems = entry.state.listItems + result.listItems,
                        mediaGridItems = entry.state.mediaGridItems + result.mediaGridItems,
                        isLoading = false,
                        isLoadingMore = false,
                        hasMore = key.query.isBlank() && result.hasMore,
                        emptyState = result.emptyState,
                        isLoaded = true,
                        query = key.query,
                        showMessageChannel = showMessageChannel()
                    )
                }
                cacheEntries[key] = entry.copy(
                    state = updatedState,
                    loadedCount = if (reset) result.loadedCount else entry.loadedCount + result.loadedCount
                )
                if (key == currentKey()) {
                    publishVisibleState(key)
                }
            }
        }

        loadJob = job
        loadJobKey = key
        job.invokeOnCompletion {
            if (loadJobKey == key) {
                loadJobKey = null
                loadJob = null
            }
        }
    }

    private fun cancelActiveLoad() {
        loadJob?.cancel()
        val activeKey = loadJobKey
        loadJob = null
        loadJobKey = null
        activeKey ?: return

        val entry = cacheEntries[activeKey] ?: return
        cacheEntries[activeKey] = entry.copy(
            state = entry.state.copy(
                isLoading = false,
                isLoadingMore = false
            )
        )
        if (activeKey == currentKey()) {
            publishVisibleState(activeKey)
        }
    }

    private fun publishVisibleState(key: GlobalSearchRequestKey) {
        _state.value = visibleStateFor(key)
    }

    private fun visibleStateFor(key: GlobalSearchRequestKey): GlobalSearchTabState {
        return cacheEntries[key]?.state?.copy(
            tab = tab,
            requestKey = key,
            query = key.query,
            showMessageChannel = showMessageChannel()
        ) ?: defaultTabState(key)
    }

    private fun defaultTabState(key: GlobalSearchRequestKey): GlobalSearchTabState {
        return GlobalSearchTabState(
            tab = tab,
            requestKey = key,
            query = key.query,
            showMessageChannel = showMessageChannel()
        )
    }

    private fun currentKey(sessionState: GlobalSearchSessionState = latestSessionState): GlobalSearchRequestKey {
        return GlobalSearchRequestKey(
            tab = tab,
            query = sessionState.query,
            selectedMemberId = if (tab == GlobalSearchTab.Channels) null else sessionState.selectedMember?.id
        )
    }

    private fun showMessageChannel(): Boolean {
        return latestSessionState.selectedMember == null
    }

    protected abstract suspend fun performLoad(
        criteria: SearchCriteria,
        offset: Int,
        pageSize: Int,
    ): SearchResultPage

    protected suspend fun loadChannels(
        criteria: SearchCriteria,
        offset: Int,
        pageSize: Int,
    ): SearchResultPage {
        val page = if (criteria.query.isBlank()) {
            interactor.getRecentChannels(offset = offset, limit = pageSize)
        } else {
            interactor.searchChannels(criteria.query, SEARCH_PAGE_SIZE)
        }

        return SearchResultPage(
            listItems = page.data.map { GlobalSearchListItem.ChannelItem(it, criteria.query) },
            hasMore = criteria.query.isBlank() && page.hasMore,
            loadedCount = page.data.size,
            emptyState = page.toEmptyState(
                blankState = GlobalSearchEmptyState(
                    iconRes = R.drawable.sceyt_ic_channels_empty_state,
                    titleRes = R.string.sceyt_no_channels,
                    subtitleRes = R.string.sceyt_empty_channels_description
                )
            )
        )
    }

    protected suspend fun loadMedia(
        criteria: SearchCriteria,
        offset: Int,
        pageSize: Int,
    ): SearchResultPage {
        val page = interactor.searchAttachments(
            tab = GlobalSearchTab.Media,
            query = criteria.query,
            senderId = criteria.senderIdForActiveTab,
            offset = offset,
            limit = pageSize
        )

        return if (criteria.query.isBlank()) {
            SearchResultPage(
                mediaGridItems = page.data.map { GlobalSearchMediaGridItem(it) },
                hasMore = page.hasMore,
                loadedCount = page.data.size,
                emptyState = page.toEmptyState(
                    blankState = GlobalSearchEmptyState(
                        iconRes = R.drawable.sceyt_ic_empty_medias,
                        titleRes = R.string.sceyt_no_media_title,
                        subtitleRes = R.string.sceyt_no_media_desc
                    )
                )
            )
        } else {
            val items = buildList {
                if (page.data.isNotEmpty()) {
                    add(GlobalSearchListItem.SectionHeader(R.string.sceyt_media))
                    addAll(page.data.map {
                        GlobalSearchListItem.AttachmentItem(
                            it,
                            criteria.query
                        )
                    })
                }
            }
            SearchResultPage(
                listItems = items,
                hasMore = false,
                loadedCount = page.data.size,
                emptyState = if (items.isEmpty()) genericEmptyState() else null
            )
        }
    }

    protected suspend fun loadAttachmentList(
        criteria: SearchCriteria,
        offset: Int,
        pageSize: Int,
    ): SearchResultPage {
        val page = interactor.searchAttachments(
            tab = criteria.activeTab,
            query = criteria.query,
            senderId = criteria.senderIdForActiveTab,
            offset = offset,
            limit = pageSize
        )

        return SearchResultPage(
            listItems = page.data.map { GlobalSearchListItem.AttachmentItem(it, criteria.query) },
            hasMore = criteria.query.isBlank() && page.hasMore,
            loadedCount = page.data.size,
            emptyState = page.toEmptyState(
                blankState = when (criteria.activeTab) {
                    GlobalSearchTab.Files -> GlobalSearchEmptyState(
                        iconRes = R.drawable.sceyt_ic_empty_files,
                        titleRes = R.string.sceyt_no_files_title,
                        subtitleRes = R.string.sceyt_no_files_desc
                    )

                    GlobalSearchTab.Voice -> GlobalSearchEmptyState(
                        iconRes = R.drawable.sceyt_ic_empty_voices,
                        titleRes = R.string.sceyt_no_voices_title,
                        subtitleRes = R.string.sceyt_no_voices_desc
                    )

                    GlobalSearchTab.Links -> GlobalSearchEmptyState(
                        iconRes = R.drawable.sceyt_ic_empty_links,
                        titleRes = R.string.sceyt_no_links_title,
                        subtitleRes = R.string.sceyt_no_links_desc
                    )

                    else -> null
                }
            )
        )
    }

    protected open fun genericEmptyState() = GlobalSearchEmptyState(
        iconRes = R.drawable.sceyt_ic_search_messages_with_layers,
        titleRes = R.string.sceyt_ui_channel_list_empty,
        subtitleRes = R.string.sceyt_ui_channel_list_empty_desc
    )

    protected data class SearchCriteria(
        val activeTab: GlobalSearchTab,
        val query: String,
        val selectedMemberId: String?,
    ) {
        val senderIdForActiveTab: String?
            get() = if (activeTab == GlobalSearchTab.Channels) null else selectedMemberId
    }

    protected data class SearchResultPage(
        val listItems: List<GlobalSearchListItem> = emptyList(),
        val mediaGridItems: List<GlobalSearchMediaGridItem> = emptyList(),
        val hasMore: Boolean = false,
        val loadedCount: Int = 0,
        val emptyState: GlobalSearchEmptyState? = null,
    )

    private data class TabCacheEntry(
        val state: GlobalSearchTabState,
        val loadedCount: Int,
    )

    private fun GlobalSearchRequestKey.toCriteria() = SearchCriteria(
        activeTab = tab,
        query = query,
        selectedMemberId = selectedMemberId
    )

    private fun <T> GlobalSearchPage<T>.toEmptyState(
        blankState: GlobalSearchEmptyState?,
    ): GlobalSearchEmptyState? {
        if (data.isNotEmpty()) return null
        return blankState ?: genericEmptyState()
    }
}

internal class ChannelsSearchViewModel(
    session: GlobalSearchSession,
    interactor: GlobalSearchDataSource = GlobalSearchLocalInteractor(),
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : GlobalSearchSessionTabViewModel(
    tab = GlobalSearchTab.Channels,
    session = session,
    interactor = interactor,
    ioDispatcher = ioDispatcher
) {
    override suspend fun performLoad(
        criteria: SearchCriteria,
        offset: Int,
        pageSize: Int,
    ): SearchResultPage {
        return loadChannels(criteria, offset, pageSize)
    }
}

internal class MediaSearchViewModel(
    session: GlobalSearchSession,
    interactor: GlobalSearchDataSource = GlobalSearchLocalInteractor(),
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : GlobalSearchSessionTabViewModel(
    tab = GlobalSearchTab.Media,
    session = session,
    interactor = interactor,
    ioDispatcher = ioDispatcher
) {
    override suspend fun performLoad(
        criteria: SearchCriteria,
        offset: Int,
        pageSize: Int,
    ): SearchResultPage {
        return loadMedia(criteria, offset, pageSize)
    }
}

internal abstract class GlobalSearchAttachmentTabViewModel(
    tab: GlobalSearchTab,
    session: GlobalSearchSession,
    interactor: GlobalSearchDataSource = GlobalSearchLocalInteractor(),
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : GlobalSearchSessionTabViewModel(
    tab = tab,
    session = session,
    interactor = interactor,
    ioDispatcher = ioDispatcher
) {
    override suspend fun performLoad(
        criteria: SearchCriteria,
        offset: Int,
        pageSize: Int,
    ): SearchResultPage {
        return loadAttachmentList(criteria, offset, pageSize)
    }
}

internal class FilesSearchViewModel(
    session: GlobalSearchSession,
    interactor: GlobalSearchDataSource = GlobalSearchLocalInteractor(),
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : GlobalSearchAttachmentTabViewModel(
    tab = GlobalSearchTab.Files,
    session = session,
    interactor = interactor,
    ioDispatcher = ioDispatcher
)

internal class VoiceSearchViewModel(
    session: GlobalSearchSession,
    interactor: GlobalSearchDataSource = GlobalSearchLocalInteractor(),
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : GlobalSearchAttachmentTabViewModel(
    tab = GlobalSearchTab.Voice,
    session = session,
    interactor = interactor,
    ioDispatcher = ioDispatcher
)

internal class LinksSearchViewModel(
    session: GlobalSearchSession,
    interactor: GlobalSearchDataSource = GlobalSearchLocalInteractor(),
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : GlobalSearchAttachmentTabViewModel(
    tab = GlobalSearchTab.Links,
    session = session,
    interactor = interactor,
    ioDispatcher = ioDispatcher
)

internal class GlobalSearchTabViewModelFactory(
    private val tab: GlobalSearchTab,
    private val session: GlobalSearchSession,
    private val interactor: GlobalSearchDataSource = GlobalSearchLocalInteractor(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val viewModel = when (tab) {
            GlobalSearchTab.Chats -> throw IllegalStateException("Use ChatsSearchViewModelFactory for chats tab.")
            GlobalSearchTab.Channels -> ChannelsSearchViewModel(session, interactor, ioDispatcher)
            GlobalSearchTab.Media -> MediaSearchViewModel(session, interactor, ioDispatcher)
            GlobalSearchTab.Files -> FilesSearchViewModel(session, interactor, ioDispatcher)
            GlobalSearchTab.Voice -> VoiceSearchViewModel(session, interactor, ioDispatcher)
            GlobalSearchTab.Links -> LinksSearchViewModel(session, interactor, ioDispatcher)
        }

        if (!modelClass.isAssignableFrom(viewModel::class.java)) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
        return viewModel as T
    }
}
