package com.sceyt.sceytchatuikit.presentation.root

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.PaginationResponse.LoadType.LoadNear
import com.sceyt.sceytchatuikit.data.models.PaginationResponse.LoadType.LoadNewest
import com.sceyt.sceytchatuikit.data.models.PaginationResponse.LoadType.LoadNext
import com.sceyt.sceytchatuikit.data.models.PaginationResponse.LoadType.LoadPrev
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.persistence.extensions.asLiveData
import com.sceyt.sceytchatuikit.persistence.shared.LiveEvent
import java.util.concurrent.atomic.AtomicBoolean

open class BaseViewModel : ViewModel() {
    protected val pageStateLiveDataInternal = LiveEvent<PageState>()
    val pageStateLiveData = pageStateLiveDataInternal.asLiveData()
    var hasPrevDb: Boolean = false
    var hasPrev: Boolean = false
    var hasNextDb: Boolean = false
    var hasNext: Boolean = false

    var loadingPrevItems = AtomicBoolean(false)
    var loadingPrevItemsDb = AtomicBoolean(false)
    var loadingNextItems = AtomicBoolean(false)
    var loadingNextItemsDb = AtomicBoolean(false)

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

    val isLoadingFromServer get() = loadingNextItems.get() || loadingPrevItems.get()

    protected fun setPagingLoadingStarted(loadType: PaginationResponse.LoadType,
                                          ignoreDb: Boolean = false,
                                          ignoreServer: Boolean = false) {
        fun initPrev() {
            loadingPrevItemsDb.set(ignoreDb.not())
            loadingPrevItems.set(ignoreServer.not())
        }

        fun initNext() {
            loadingNextItemsDb.set(ignoreDb.not())
            loadingNextItems.set(ignoreServer.not())
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
            is PaginationResponse.ServerResponse -> onPaginationSeverResponse(response)
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

    private fun onPaginationSeverResponse(response: PaginationResponse.ServerResponse<*>) {
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
        // When db data is empty, check  maybe loading data from server
        return when (response.loadType) {
            LoadPrev -> hasPrev || loadingPrevItems.get()
            LoadNext -> hasNext || loadingNextItems.get()
            LoadNear, LoadNewest -> true
        }
    }

    protected fun notifyPageLoadingState(isLoadingMore: Boolean) {
        if (isLoadingMore) {
            pageStateLiveDataInternal.postValue(PageState.StateLoadingMore())
        } else
            pageStateLiveDataInternal.postValue(PageState.StateLoading())
    }

    fun <T> notifyPageStateWithResponse(response: SceytResponse<T>,
                                        wasLoadingMore: Boolean = false,
                                        isEmpty: Boolean = false,
                                        searchQuery: String? = null,
                                        showError: Boolean = true) {
        val state = when {
            response is SceytResponse.Error -> PageState.StateError(response.code, response.message,
                wasLoadingMore, searchQuery, showError)

            isEmpty -> PageState.StateEmpty(searchQuery, wasLoadingMore)
            wasLoadingMore -> PageState.StateLoadingMore(false)
            else -> PageState.StateLoading(false)
        }
        pageStateLiveDataInternal.postValue(state)
    }

    fun <T> notifyResponseAndPageState(liveData: MutableLiveData<T>?,
                                       response: SceytResponse<T>,
                                       wasLoadingMore: Boolean = false,
                                       isEmpty: Boolean = false,
                                       searchQuery: String? = null,
                                       showError: Boolean = true) {
        if (response is SceytResponse.Success) {
            liveData?.postValue(response.data)
        }
        notifyPageStateWithResponse(response, wasLoadingMore, isEmpty, searchQuery, showError)
    }
}