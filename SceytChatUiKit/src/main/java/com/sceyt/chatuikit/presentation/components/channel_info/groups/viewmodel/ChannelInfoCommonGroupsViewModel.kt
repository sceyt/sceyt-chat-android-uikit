package com.sceyt.chatuikit.presentation.components.channel_info.groups.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sceyt.chatuikit.data.models.SceytPagingResponse
import com.sceyt.chatuikit.persistence.logic.PersistenceChannelsLogic
import com.sceyt.chatuikit.presentation.components.channel_info.groups.adapter.CommonGroupListItem
import com.sceyt.chatuikit.presentation.root.PageState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class ChannelInfoCommonGroupsViewModel(
    private val userId: String,
    private val persistenceChannelsLogic: PersistenceChannelsLogic
) : ViewModel() {

    private val _groupsFlow = MutableSharedFlow<List<CommonGroupListItem>>(replay = 1)
    val groupsFlow = _groupsFlow.asSharedFlow()

    private val _loadMoreFlow = MutableSharedFlow<List<CommonGroupListItem>>(replay = 0)
    val loadMoreFlow = _loadMoreFlow.asSharedFlow()
    private val _pageStateLiveData = MutableLiveData<PageState>()
    val pageStateLiveData: LiveData<PageState> = _pageStateLiveData

    private var hasMoreData = false
    private var isLoadingMore = false

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch(Dispatchers.IO) {
            _pageStateLiveData.postValue(PageState.StateLoading())

            hasMoreData = false
            isLoadingMore = false

            val response = persistenceChannelsLogic.getCommonGroups(
                userId = userId,
            )

            when (response) {
                is SceytPagingResponse.Success -> {
                    hasMoreData = response.hasNext

                    val items = buildList {
                        addAll(response.data.map { CommonGroupListItem.GroupItem(it) })
                        if (response.hasNext) {
                            add(CommonGroupListItem.LoadingMore)
                        }
                    }

                    _groupsFlow.emit(items)

                    if (items.isEmpty()) {
                        _pageStateLiveData.postValue(PageState.StateEmpty())
                    } else {
                        _pageStateLiveData.postValue(PageState.Nothing)
                    }
                }

                is SceytPagingResponse.Error -> {
                    _pageStateLiveData.postValue(
                        PageState.StateError(
                            code = response.code,
                            errorMessage = response.message
                        )
                    )
                }
            }
        }
    }

    fun loadMore() {
        if (!hasMoreData || isLoadingMore) return

        viewModelScope.launch(Dispatchers.IO) {
            isLoadingMore = true

            when (val response = persistenceChannelsLogic.loadMore()) {
                is SceytPagingResponse.Success -> {
                    hasMoreData = response.hasNext

                    val items = buildList {
                        addAll(response.data.map { CommonGroupListItem.GroupItem(it) })
                        if (response.hasNext) {
                            add(CommonGroupListItem.LoadingMore)
                        }
                    }

                    _loadMoreFlow.emit(items)
                }

                is SceytPagingResponse.Error -> {
                    _loadMoreFlow.emit(emptyList())
                }
            }

            isLoadingMore = false
        }
    }

    fun canLoadMore(): Boolean = hasMoreData && !isLoadingMore
}