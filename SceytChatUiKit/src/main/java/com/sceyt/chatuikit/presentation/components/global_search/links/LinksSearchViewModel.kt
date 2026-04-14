package com.sceyt.chatuikit.presentation.components.global_search.links

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sceyt.chatuikit.data.models.search.GlobalSearchAttachmentKind
import com.sceyt.chatuikit.data.models.search.GlobalSearchAttachmentResult
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.persistence.interactor.GlobalSearchDataSource
import com.sceyt.chatuikit.presentation.components.global_search.DefaultGlobalSearchLocalInteractor
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchListItem
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchSession
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchSessionState
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchTab
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import java.util.Calendar

private const val LINKS_DEFAULT_PAGE_SIZE = 30

data class LinksSearchState(
    val sessionState: GlobalSearchSessionState? = null,
    val items: List<GlobalSearchListItem> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
) {
    val showEmptyState: Boolean
        get() = !isLoading && !isLoadingMore && items.isEmpty()

    val query: String
        get() = sessionState?.query.orEmpty()

    val settled: Boolean
        get() = !isLoading && !isLoadingMore
}

open class LinksSearchViewModel(
    protected val session: GlobalSearchSession,
    protected val dataSource: GlobalSearchDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel(), SceytKoinComponent {

    private val fileTransferService: FileTransferService by inject()

    private val _state = MutableStateFlow(LinksSearchState(isLoading = true))
    val state: StateFlow<LinksSearchState> = _state.asStateFlow()

    private var loadJob: Job? = null
    var lastResultsRequestKeyForUI: GlobalSearchSessionState? = null

    init {
        viewModelScope.launch {
            session.state.collectLatest(::onSessionStateChanged)
        }
    }

    private suspend fun onSessionStateChanged(sessionState: GlobalSearchSessionState) {
        val current = _state.value
        if (current.sessionState == sessionState) return

        if (sessionState.isCurrent(GlobalSearchTab.Links)) {
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
                kind = GlobalSearchAttachmentKind.Link,
                query = sessionState.query,
                senderId = sessionState.selectedMember?.id,
                offset = 0,
                limit = LINKS_DEFAULT_PAGE_SIZE,
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
        if (key.query.isNotBlank() || current.isLoading || current.isLoadingMore || !current.hasMore)
            return
        loadNextPage(key)
    }

    protected open fun loadNextPage(sessionState: GlobalSearchSessionState) {
        loadJob?.cancel()
        _state.update { it.copy(isLoadingMore = true) }

        loadJob = viewModelScope.launch(ioDispatcher) {
            val offset = currentLinkItemCount()
            val page = dataSource.searchAttachments(
                kind = GlobalSearchAttachmentKind.Link,
                query = "",
                senderId = sessionState.selectedMember?.id,
                offset = offset,
                limit = LINKS_DEFAULT_PAGE_SIZE,
            )
            val newItems = buildListItems(page.data)
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

    private fun buildListItems(results: List<GlobalSearchAttachmentResult>): List<GlobalSearchListItem> {
        if (results.isEmpty()) return emptyList()
        return buildList {
            var prevYearMonth = -1
            for (result in results) {
                val cal =
                    Calendar.getInstance().apply { timeInMillis = result.attachment.createdAt }
                val yearMonth = cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.MONTH)
                if (yearMonth != prevYearMonth) {
                    add(GlobalSearchListItem.DateSeparator(result.attachment.createdAt))
                    prevYearMonth = yearMonth
                }
                add(GlobalSearchListItem.AttachmentItem(result, query = ""))
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

    private fun currentLinkItemCount(): Int {
        return _state.value.items.count { it is GlobalSearchListItem.AttachmentItem }
    }
}

class LinksSearchViewModelFactory(
    private val session: GlobalSearchSession,
    private val dataSource: GlobalSearchDataSource = DefaultGlobalSearchLocalInteractor(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LinksSearchViewModel::class.java)) {
            return LinksSearchViewModel(session, dataSource, ioDispatcher) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
