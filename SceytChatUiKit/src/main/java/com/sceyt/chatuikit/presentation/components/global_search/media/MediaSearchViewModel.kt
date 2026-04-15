package com.sceyt.chatuikit.presentation.components.global_search.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
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
import com.sceyt.chatuikit.presentation.components.global_search.defaults.DefaultGlobalSearchLocalInteractor
import com.sceyt.chatuikit.presentation.components.global_search.media.MediaSearchDisplayMode.Grid
import com.sceyt.chatuikit.presentation.components.global_search.media.MediaSearchDisplayMode.SearchList
import com.sceyt.chatuikit.shared.utils.DateTimeUtil
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

private const val MEDIA_DEFAULT_PAGE_SIZE = 30

sealed interface MediaSearchDisplayMode {
    data class Grid(
        val items: List<GlobalSearchListItem>,
        val offset: Int
    ) : MediaSearchDisplayMode

    data class SearchList(
        val items: List<GlobalSearchListItem>,
        val offset: Int
    ) : MediaSearchDisplayMode

    fun isEmpty(): Boolean = when (this) {
        is Grid -> items.isEmpty()
        is SearchList -> items.isEmpty()
    }

    companion object {
        val EMPTY_GRID = Grid(emptyList(), offset = 0)
        val EMPTY_SEARCH_LIST = SearchList(emptyList(), offset = 0)
    }
}

data class MediaSearchState(
    val sessionState: GlobalSearchSessionState? = null,
    val mode: MediaSearchDisplayMode = MediaSearchDisplayMode.EMPTY_GRID,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
) {
    val showEmptyState: Boolean
        get() = !isLoading && !isLoadingMore && mode.isEmpty()

    val query: String
        get() = sessionState?.query.orEmpty()

    val settled: Boolean
        get() = !isLoading && !isLoadingMore
}

open class MediaSearchViewModel(
    protected val session: GlobalSearchSession,
    protected val dataSource: GlobalSearchDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel(), SceytKoinComponent {

    private val fileTransferService: FileTransferService by inject()

    private val _state = MutableStateFlow(MediaSearchState(isLoading = true))
    val state: StateFlow<MediaSearchState> = _state.asStateFlow()

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

        if (sessionState.isCurrent(GlobalSearchTab.Media)) {
            loadFirstPage(sessionState)
        } else {
            val queryChanged = current.sessionState?.isQueryChanged(sessionState.query) == true
            if (queryChanged) {
                val newMode = if (sessionState.query.isBlank()) {
                    MediaSearchDisplayMode.EMPTY_GRID
                } else MediaSearchDisplayMode.EMPTY_SEARCH_LIST

                _state.update {
                    it.copy(
                        sessionState = sessionState,
                        mode = newMode,
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
                pageSize = MEDIA_DEFAULT_PAGE_SIZE
            )
            withContext(Dispatchers.Main) {
                _state.update {
                    it.copy(
                        sessionState = sessionState,
                        mode = result.mode,
                        isLoading = false,
                        isLoadingMore = false,
                        hasMore = result.hasMore,
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
        val currentMode = _state.value.mode
        val (currentItems, currentOffset) = when (currentMode) {
            is Grid -> currentMode.items to currentMode.offset
            is SearchList -> currentMode.items to currentMode.offset
        }
        val lastCreatedAt = currentItems
            .lastOrNull { it is GlobalSearchListItem.AttachmentItem }
            ?.let { (it as GlobalSearchListItem.AttachmentItem).result.attachment.createdAt }
            ?: 0L
        _state.update { it.copy(isLoadingMore = true) }

        loadJob = viewModelScope.launch(ioDispatcher) {
            val result = performLoad(
                state = sessionState,
                offset = currentOffset,
                pageSize = MEDIA_DEFAULT_PAGE_SIZE,
                prevCreatedAt = lastCreatedAt,
            )
            withContext(Dispatchers.Main) {
                _state.update { state ->
                    val newMode = when (val mode = result.mode) {
                        is Grid -> {
                            val existingItems = (state.mode as? Grid)?.items.orEmpty()
                            Grid(
                                items = existingItems + mode.items,
                                offset = mode.offset
                            )
                        }

                        is SearchList -> {
                            val existingItems = (state.mode as? SearchList)?.items.orEmpty()
                            SearchList(
                                items = existingItems + mode.items,
                                offset = mode.offset
                            )
                        }
                    }
                    state.copy(
                        mode = newMode,
                        isLoadingMore = false,
                        hasMore = result.hasMore,
                    )
                }
            }
        }
    }

    protected open suspend fun performLoad(
        state: GlobalSearchSessionState,
        offset: Int,
        pageSize: Int,
        prevCreatedAt: Long = 0L,
    ): LoadResult {
        val page = dataSource.searchAttachments(
            kind = GlobalSearchAttachmentKind.Media,
            query = state.query,
            senderId = state.selectedMember?.id,
            offset = offset,
            limit = pageSize,
        )
        return if (state.query.isBlank()) {
            LoadResult(
                mode = Grid(
                    items = page.data.toGridItems(initialPrevCreatedAt = prevCreatedAt),
                    offset = offset + page.data.size
                ),
                hasMore = page.hasMore,
            )
        } else {
            LoadResult(
                mode = SearchList(
                    items = buildListItems(
                        results = page.data,
                        query = state.query,
                        initialPrevTimestamp = prevCreatedAt
                    ),
                    offset = offset + page.data.size
                ),
                hasMore = page.hasMore,
            )
        }
    }

    private fun List<GlobalSearchAttachmentResult>.toGridItems(
        initialPrevCreatedAt: Long = 0L,
    ): List<GlobalSearchListItem> {
        val fileItems = mutableListOf<GlobalSearchListItem>()
        var prevCreatedAt = initialPrevCreatedAt
        for (result in this) {
            val attachment = result.attachment
            if (prevCreatedAt == 0L || !DateTimeUtil.isSameDay(
                    prevCreatedAt,
                    attachment.createdAt
                )
            ) {
                fileItems.add(GlobalSearchListItem.DateSeparator(attachment.createdAt))
            }
            val type = attachment.type
            if (type == AttachmentTypeEnum.Image.value || type == AttachmentTypeEnum.Video.value) {
                fileItems.add(GlobalSearchListItem.AttachmentItem(result, query = ""))
            }
            prevCreatedAt = attachment.createdAt
        }
        return fileItems
    }

    private fun buildListItems(
        results: List<GlobalSearchAttachmentResult>,
        query: String,
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
                add(GlobalSearchListItem.AttachmentItem(result, query))
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

            is NeedMediaInfoData.NeedThumb -> {
                viewModelScope.launch(Dispatchers.IO) {
                    fileTransferService.getThumb(data.item.messageTid, data.item, data.thumbData)
                }
            }
        }
    }

    protected data class LoadResult(
        val mode: MediaSearchDisplayMode,
        val hasMore: Boolean,
    )
}

class MediaSearchViewModelFactory(
    private val session: GlobalSearchSession,
    private val dataSource: GlobalSearchDataSource = DefaultGlobalSearchLocalInteractor(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MediaSearchViewModel::class.java)) {
            return MediaSearchViewModel(session, dataSource, ioDispatcher) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
