package com.sceyt.sceytchatuikit.presentation.root

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hadilq.liveevent.LiveEvent
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.PaginationResponse.LoadType.*
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import java.util.concurrent.atomic.AtomicBoolean

open class BaseViewModel : ViewModel() {
    private val _pageStateLiveData = LiveEvent<PageState>()
    val pageStateLiveData: LiveData<PageState> = _pageStateLiveData
    var hasPrevDb: Boolean = false
    var hasPrev: Boolean = false
    var hasNextDb: Boolean = false
    var hasNext: Boolean = false

    @Deprecated("loadingItemsDb will remove soon")
    var loadingItemsDb: AtomicBoolean = AtomicBoolean(false)

    @Deprecated("loadingItems will remove soon")
    var loadingItems: AtomicBoolean = AtomicBoolean(false)
    var loadingPrevItems: AtomicBoolean = AtomicBoolean(false)
    var loadingPrevItemsDb: AtomicBoolean = AtomicBoolean(false)
    var loadingNextItems: AtomicBoolean = AtomicBoolean(false)
    var loadingNextItemsDb: AtomicBoolean = AtomicBoolean(false)

    fun canLoadPrev(): Boolean {
        return when {
            loadingPrevItemsDb.get() -> false
            loadingPrevItemsDb.get().not() && hasPrevDb -> true
            loadingPrevItems.get() -> false
            loadingPrevItems.get().not() && hasPrev -> true
            else -> false
        }
    }

    fun canLoadNext(): Boolean {
        return when {
            loadingNextItemsDb.get() -> false
            loadingNextItemsDb.get().not() && hasNextDb -> true
            loadingNextItems.get() -> false
            loadingNextItems.get().not() && hasNext -> true
            else -> false
        }
    }

    fun canLoadMore(): Boolean {
        return when {
            loadingItemsDb.get() -> false
            loadingItemsDb.get().not() && hasNextDb -> true
            loadingItems.get() -> false
            loadingItems.get().not() && hasNext -> true
            else -> false
        }
    }

    protected fun setPagingLoadingStarted(loadType: PaginationResponse.LoadType, ignoreDb: Boolean = false) {
        fun initPrev() {
            loadingPrevItemsDb.set(ignoreDb.not())
            loadingPrevItems.set(true)
        }

        fun initNext() {
            loadingNextItemsDb.set(ignoreDb.not())
            loadingNextItems.set(true)
        }

        when (loadType) {
            LoadPrev -> initPrev()
            LoadNext -> initNext()
            LoadNear -> {
                initPrev()
                initNext()
            }
            LoadNewest -> initPrev()
        }
    }

    protected fun pagingResponseReceived(response: PaginationResponse<*>) {
        when (response) {
            is PaginationResponse.DBResponse -> onPaginationDbResponse(response)
            is PaginationResponse.ServerResponse2 -> onPaginationSeverResponse(response)
            is PaginationResponse.ServerResponse -> {
                loadingItems.set(false)
                loadingNextItems.set(false)
                hasNext = response.hasNext
            }
            is PaginationResponse.Nothing -> return
        }
    }

    private fun onPaginationDbResponse(response: PaginationResponse.DBResponse<*>) {

        fun initPrev() {
            loadingPrevItemsDb.set(false)
            hasPrevDb = response.hasPrev
        }

        fun initNext() {
            loadingNextItemsDb.set(false)
            hasNextDb = response.hasNext
        }

        when (response.loadType) {
            LoadPrev -> initPrev()
            LoadNext -> initNext()
            LoadNear, LoadNewest -> {
                initPrev()
                initNext()
            }
        }
    }

    private fun onPaginationSeverResponse(response: PaginationResponse.ServerResponse2<*>) {
        fun initPrev() {
            loadingPrevItems.set(false)
            if (response.data is SceytResponse.Success)
                hasPrev = response.hasPrev
        }

        fun initNext() {
            loadingNextItems.set(false)
            if (response.data is SceytResponse.Success)
                hasNext = response.hasNext
        }

        when (response.loadType) {
            LoadPrev -> initPrev()
            LoadNext -> initNext()
            LoadNear -> {
                initPrev()
                initNext()
            }
            LoadNewest -> {
                initPrev()
                initNext()
                hasNextDb = false
                loadingNextItemsDb.set(false)
            }
        }
    }

    internal fun checkIgnoreDatabasePagingResponse(response: PaginationResponse.DBResponse<*>): Boolean {
        if (response.data.isNotEmpty()) return false

        return when (response.loadType) {
            LoadPrev, LoadNewest -> hasPrev && loadingPrevItems.get()
            LoadNext -> hasNext && loadingNextItems.get()
            LoadNear -> hasPrev && loadingPrevItems.get() && !hasNext && loadingNextItems.get()
        }
    }

    protected fun notifyPageLoadingState(isLoadingMore: Boolean) {
        if (isLoadingMore) {
            _pageStateLiveData.postValue(PageState.StateLoadingMore())
        } else
            _pageStateLiveData.postValue(PageState.StateLoading())
    }

    fun <T> notifyPageStateWithResponse(response: SceytResponse<T>,
                                        wasLoadingMore: Boolean = false,
                                        isEmpty: Boolean = false,
                                        searchQuery: String? = null) {
        val state = when {
            response is SceytResponse.Error -> PageState.StateError(response, searchQuery, wasLoadingMore)
            isEmpty -> PageState.StateEmpty(searchQuery, wasLoadingMore)
            wasLoadingMore -> PageState.StateLoadingMore(false)
            else -> PageState.StateLoading(false)
        }
        _pageStateLiveData.postValue(state)
    }

    fun <T> notifyResponseAndPageState(liveData: MutableLiveData<T>?,
                                       response: SceytResponse<T>,
                                       wasLoadingMore: Boolean = false,
                                       isEmpty: Boolean = false,
                                       searchQuery: String? = null) {
        if (response is SceytResponse.Success) {
            liveData?.postValue(response.data)
        }
        notifyPageStateWithResponse(response, wasLoadingMore, isEmpty, searchQuery)
    }
}