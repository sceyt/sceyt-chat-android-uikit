package com.sceyt.chatuikit.presentation.components.global_search.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sceyt.chatuikit.data.models.search.GlobalSearchAttachmentKind
import com.sceyt.chatuikit.data.models.search.GlobalSearchAttachmentResult
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.persistence.interactor.GlobalSearchDataSource
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchListItem
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchSession
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchSessionState
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchTab
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchUpdateEventSource
import com.sceyt.chatuikit.presentation.components.global_search.applyAttachmentUpdateEvent
import com.sceyt.chatuikit.presentation.components.global_search.defaults.DefaultGlobalSearchLocalInteractor
import com.sceyt.chatuikit.shared.utils.DateTimeUtil
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject

private const val VOICE_DEFAULT_PAGE_SIZE = 30

data class VoiceSearchState(
    val sessionState: GlobalSearchSessionState? = null,
    val items: List<GlobalSearchListItem> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val refreshKey: Int = 0
) {
    val showEmptyState: Boolean
        get() = !isLoading && !isLoadingMore && items.isEmpty()

    val query: String
        get() = sessionState?.query.orEmpty()

    val settled: Boolean
        get() = !isLoading && !isLoadingMore
}

open class VoiceSearchViewModel(
    protected val session: GlobalSearchSession,
    protected val dataSource: GlobalSearchDataSource = DefaultGlobalSearchLocalInteractor(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel(), SceytKoinComponent {

    private val fileTransferService: FileTransferService by inject()
    private val globalSearchUpdateEventSource = GlobalSearchUpdateEventSource(viewModelScope)

    private val _state = MutableStateFlow(VoiceSearchState(isLoading = true))
    val state: StateFlow<VoiceSearchState> = _state.asStateFlow()

    private var loadJob: Job? = null
    var lastResultsRequestKeyForUI: GlobalSearchSessionState? = null

    init {
        viewModelScope.launch {
            session.state.collectLatest(::onSessionStateChanged)
        }

        globalSearchUpdateEventSource.updatesFlow.onEach { event ->
            _state.update {
                it.copy(
                    items = it.items.applyAttachmentUpdateEvent(event),
                    refreshKey = if (event.shouldUpdateRefreshKey) it.refreshKey + 1 else it.refreshKey
                )
            }
        }.launchIn(viewModelScope)
    }

    private suspend fun onSessionStateChanged(sessionState: GlobalSearchSessionState) {
        val current = _state.value
        if (current.sessionState == sessionState) return

        if (sessionState.isCurrent(GlobalSearchTab.Voice)) {
            loadFirstPage(sessionState)
        } else {
            val queryChanged = current.sessionState?.isQueryChanged(sessionState.query) == true
            if (queryChanged) {
                _state.update {
                    it.copy(
                        sessionState = sessionState,
                        items = emptyList(),
                        isLoading = true,
                    )
                }
            }
        }
    }

    protected open fun loadFirstPage(sessionState: GlobalSearchSessionState) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch(ioDispatcher) {
            val page = dataSource.searchAttachments(
                kind = GlobalSearchAttachmentKind.Voice,
                query = sessionState.query,
                senderId = sessionState.selectedMember?.id,
                offset = 0,
                limit = VOICE_DEFAULT_PAGE_SIZE,
            )
            val items = buildListItems(page.data)
            withContext(Dispatchers.Main) {
                _state.update {
                    it.copy(
                        sessionState = sessionState,
                        items = items,
                        isLoading = false,
                        isLoadingMore = false,
                        hasMore = page.hasMore,
                    )
                }
            }
        }
    }

    open fun loadMore() {
        val current = _state.value
        val key = current.sessionState ?: return
        if (current.isLoading || current.isLoadingMore || !current.hasMore)
            return
        loadNextPage(key)
    }

    protected open fun loadNextPage(sessionState: GlobalSearchSessionState) {
        loadJob?.cancel()
        val lastCreatedAt = _state.value.items
            .lastOrNull { it is GlobalSearchListItem.AttachmentItem }
            ?.let { (it as GlobalSearchListItem.AttachmentItem).result.attachment.createdAt }
            ?: 0L
        _state.update { it.copy(isLoadingMore = true) }

        loadJob = viewModelScope.launch(ioDispatcher) {
            val offset = currentVoiceItemCount()
            val page = dataSource.searchAttachments(
                kind = GlobalSearchAttachmentKind.Voice,
                query = sessionState.query,
                senderId = sessionState.selectedMember?.id,
                offset = offset,
                limit = VOICE_DEFAULT_PAGE_SIZE,
            )
            val newItems = buildListItems(page.data, initialPrevTimestamp = lastCreatedAt)
            withContext(Dispatchers.Main) {
                _state.update { state ->
                    state.copy(
                        items = state.items + newItems,
                        isLoadingMore = false,
                        hasMore = page.hasMore,
                    )
                }
            }
        }
    }

    private fun buildListItems(
        results: List<GlobalSearchAttachmentResult>,
        initialPrevTimestamp: Long = 0L,
    ): List<GlobalSearchListItem> {
        if (results.isEmpty()) return emptyList()
        return buildList {
            var prevTimestamp = initialPrevTimestamp
            for (result in results) {
                val createdAt = result.attachment.createdAt
                if (prevTimestamp == 0L || !DateTimeUtil.isSameDay(prevTimestamp, createdAt)) {
                    add(GlobalSearchListItem.DateSeparator(createdAt))
                }
                add(GlobalSearchListItem.AttachmentItem(result, query = ""))
                prevTimestamp = createdAt
            }
        }
    }

    fun onNeedMediaInfo(data: NeedMediaInfoData) {
        when (data) {
            is NeedMediaInfoData.NeedDownload -> {
                viewModelScope.launch(Dispatchers.IO) {
                    fileTransferService.download(
                        attachment = data.item,
                        transferTask = fileTransferService.findOrCreateTransferTask(data.item)
                    )
                }
            }

            is NeedMediaInfoData.NeedThumb -> Unit
        }
    }

    private fun currentVoiceItemCount(): Int {
        return _state.value.items.count { it is GlobalSearchListItem.AttachmentItem }
    }
}

class VoiceSearchViewModelFactory(
    private val session: GlobalSearchSession,
    private val dataSource: GlobalSearchDataSource = DefaultGlobalSearchLocalInteractor(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VoiceSearchViewModel::class.java)) {
            return VoiceSearchViewModel(session, dataSource, ioDispatcher) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
