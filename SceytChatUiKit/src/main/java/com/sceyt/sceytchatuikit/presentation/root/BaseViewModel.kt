package com.sceyt.sceytchatuikit.presentation.root

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import java.util.concurrent.atomic.AtomicBoolean

open class BaseViewModel : ViewModel() {
    private val _pageStateLiveData = MutableLiveData<PageState>()
    val pageStateLiveData: LiveData<PageState> = _pageStateLiveData
    var hasNextDb: Boolean = false
    var hasNext: Boolean = false
    var loadingItemsDb: AtomicBoolean = AtomicBoolean(false)
    var loadingItems: AtomicBoolean = AtomicBoolean(false)

    fun canLoadMore(): Boolean {
        return when {
            loadingItemsDb.get() -> false
            loadingItemsDb.get().not() && hasNextDb -> true
            loadingItems.get() -> false
            loadingItems.get().not() && hasNext -> true
            else -> false
        }
    }

    fun setPagingLoadingStarted() {
        loadingItemsDb.set(true)
        loadingItems.set(true)
    }

    fun pagingResponseReceived(response: PaginationResponse<*>) {
        when (response) {
            is PaginationResponse.DBResponse -> {
                loadingItemsDb.set(false)
                hasNextDb = response.hasNext
            }
            is PaginationResponse.ServerResponse -> {
                loadingItems.set(false)
                if (response.data is SceytResponse.Success)
                    hasNext = response.hasNext
            }
            is PaginationResponse.Nothing -> return
        }
    }

    fun notifyPageLoadingState(isLoadingMore: Boolean) {
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