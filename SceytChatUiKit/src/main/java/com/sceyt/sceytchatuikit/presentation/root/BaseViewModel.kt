package com.sceyt.sceytchatuikit.presentation.root

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import java.util.concurrent.atomic.AtomicBoolean

open class BaseViewModel : ViewModel() {
    private val _pageStateLiveData = MutableLiveData<PageState>()
    val pageStateLiveData: LiveData<PageState> = _pageStateLiveData
    var hasNext: Boolean = false
    var loadingItems: AtomicBoolean = AtomicBoolean(false)

    fun notifyPageLoadingState(isLoadingMore: Boolean) {
        if (isLoadingMore) {
            _pageStateLiveData.postValue(PageState.StateLoadingMore())
        } else
            _pageStateLiveData.postValue(PageState.StateLoading())
    }

    fun notifyPageStateWithResponse(response: SceytResponse<*>,
                                    wasLoadingMore: Boolean = false,
                                    isEmpty: Boolean = false,
                                    searchQuery: String? = null) {
        val state = when {
            response is SceytResponse.Error -> PageState.StateError(response.message, searchQuery, wasLoadingMore)
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