package com.sceyt.chat.ui.presentation.root

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

open class BaseViewModel : ViewModel() {
    private val _pageStateLiveData = MutableLiveData<PageState>()
    val pageStateLiveData: LiveData<PageState> = _pageStateLiveData

    fun notifyPageLoadingState(isLoadingMore: Boolean = false, searchQuery: String? = null) {
        _pageStateLiveData.postValue(PageState(
            isLoading = !isLoadingMore,
            isLoadingMore = isLoadingMore,
            query = searchQuery,
            isEmpty = false))
    }

    fun notifyPageStateWithResponse(loadingNext: Boolean = false, isEmpty: Boolean, searchQuery: String? = null) {
        _pageStateLiveData.postValue(PageState(
            isLoading = false,
            isLoadingMore = loadingNext,
            query = searchQuery,
            isEmpty = isEmpty))
    }

    data class PageState(
            val isLoading: Boolean,
            val isLoadingMore: Boolean,
            val isEmpty: Boolean,
            val query: String?,
    ) {
        val isSearch get() = !query.isNullOrBlank()
    }
}