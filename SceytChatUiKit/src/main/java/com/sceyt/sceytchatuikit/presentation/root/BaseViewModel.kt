package com.sceyt.sceytchatuikit.presentation.root

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hadilq.liveevent.LiveEvent
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import java.util.concurrent.atomic.AtomicBoolean

open class BaseViewModel : ViewModel() {
    private val _pageStateLiveData = LiveEvent<PageState>()
    val pageStateLiveData: LiveData<PageState> = _pageStateLiveData
    var hasPrevDb: Boolean = false
    var hasPrev: Boolean = false
    var hasNextDb: Boolean = false
    var hasNext: Boolean = false
    var loadingItemsDb: AtomicBoolean = AtomicBoolean(false)
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

    protected fun setPagingLoadingNearStarted() {
        setPagingLoadingPrevStarted()
        setPagingLoadingNextStarted()
    }


    protected fun setPagingLoadingPrevStarted() {
        loadingPrevItemsDb.set(true)
        loadingPrevItems.set(true)
    }

    protected fun setPagingLoadingNextStarted() {
        loadingNextItemsDb.set(true)
        loadingNextItems.set(true)
    }

    protected fun pagingLoadPrevResponseReceived(response: PaginationResponse<*>) {
        when (response) {
            is PaginationResponse.DBResponse -> {
                loadingItemsDb.set(false)
                loadingPrevItemsDb.set(false)
                hasPrevDb = response.hasPrev
            }
            is PaginationResponse.ServerResponse -> return
            is PaginationResponse.Nothing -> return
            is PaginationResponse.ServerResponse2 -> {
                loadingItems.set(false)
                loadingPrevItems.set(false)
                if (response.data is SceytResponse.Success) {
                    hasPrev = response.hasPrev
                }
            }
        }
    }

    protected fun pagingLoadNextResponseReceived(response: PaginationResponse<*>) {
        when (response) {
            is PaginationResponse.DBResponse -> {
                loadingItemsDb.set(false)
                loadingNextItemsDb.set(false)
                hasNextDb = response.hasNext
            }
            is PaginationResponse.ServerResponse -> {
                return
            }
            is PaginationResponse.Nothing -> return
            is PaginationResponse.ServerResponse2 -> {
                loadingItems.set(false)
                loadingNextItems.set(false)
                if (response.data is SceytResponse.Success) {
                    hasNext = response.hasNext
                }
            }
        }
    }

  protected fun pagingLoadNearResponseReceived(response: PaginationResponse<*>) {
      when (response) {
          is PaginationResponse.DBResponse -> {
              loadingPrevItemsDb.set(false)
              loadingNextItemsDb.set(false)
              hasPrevDb = response.hasPrev
              hasNextDb = response.hasNext
          }
          is PaginationResponse.ServerResponse2 -> {
              loadingPrevItemsDb.set(false)
              loadingNextItemsDb.set(false)
             /* hasPrevDb = response.hasPrev
              hasNextDb = response.hasNext*/
              loadingItems.set(false)
              loadingPrevItems.set(false)
              loadingNextItems.set(false)
              if (response.data is SceytResponse.Success) {
                  hasPrev = response.hasPrev
                  hasNext = response.hasNext
              }
          }
          else -> return
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