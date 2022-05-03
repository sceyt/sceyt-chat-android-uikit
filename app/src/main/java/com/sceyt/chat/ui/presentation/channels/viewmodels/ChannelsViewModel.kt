package com.sceyt.chat.ui.presentation.channels.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.ui.data.ChannelsRepositoryImpl
import com.sceyt.chat.ui.data.SceytResponse
import com.sceyt.chat.ui.data.models.SceytUiChannel
import com.sceyt.chat.ui.presentation.channels.adapter.ChannelListItem
import com.sceyt.chat.ui.presentation.channels.components.PageState
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChannelsViewModel : ViewModel() {
    private var searchQuery = ""
    var isLoadingMore = false
    var hasNext = false

    // todo di
    private val repo = ChannelsRepositoryImpl()

    private val _channelsFlow = MutableStateFlow<SceytResponse<List<ChannelListItem>>>(SceytResponse.Loading())
    val channelsFlow: StateFlow<SceytResponse<List<ChannelListItem>>> = _channelsFlow

    private val _loadMoreChannelsFlow = MutableStateFlow<SceytResponse<List<ChannelListItem>>>(SceytResponse.Loading())
    val loadMoreChannelsFlow: StateFlow<SceytResponse<List<ChannelListItem>>> = _loadMoreChannelsFlow

    private val _pageStateLiveData = MutableLiveData<PageState>()
    val pageStateLiveData: LiveData<PageState> = _pageStateLiveData


    fun loadChannels(offset: Int, query: String = searchQuery) {
        searchQuery = query
        viewModelScope.launch(Dispatchers.IO) {
            if (searchQuery.isBlank()) {
                getChannelsList(offset, offset > 0)
            } else searchChannels(offset, query, offset > 0)
        }
    }

    private suspend fun getChannelsList(offset: Int, loadingMoreType: Boolean) {
        repo.getChannels(offset).collect {
            initResponse(it, loadingMoreType)
        }
    }

    private suspend fun searchChannels(offset: Int, query: String, loadingMoreType: Boolean) {
        repo.searchChannels(offset, query).collect {
            initResponse(it, loadingMoreType)
        }
    }

    private suspend fun initResponse(it: SceytResponse<List<SceytUiChannel>>, loadingNext: Boolean) {
        when (it) {
            is SceytResponse.Success -> {
                hasNext = it.data?.size == SceytUIKitConfig.CHANNELS_LOAD_SIZE
                emitResponse(SceytResponse.Success(mapToChannelItem(it.data, hasNext)), loadingNext)
            }
            is SceytResponse.Error -> emitResponse(SceytResponse.Error(it.message), loadingNext)
            is SceytResponse.Loading -> emitResponse(SceytResponse.Loading(), loadingNext)
        }
        isLoadingMore = it is SceytResponse.Loading
    }

    private suspend fun emitResponse(response: SceytResponse<List<ChannelListItem>>, loadingNext: Boolean) {
        if (loadingNext)
            _loadMoreChannelsFlow.value = response
        else _channelsFlow.value = response

        withContext(Dispatchers.Main) {
            val isLoading = response is SceytResponse.Loading
            _pageStateLiveData.value = PageState(
                query = searchQuery,
                isLoading = isLoading,
                isLoadingMore = loadingNext,
                isEmpty = !isLoading && response.data.isNullOrEmpty())
        }
    }

    private fun mapToChannelItem(data: List<SceytUiChannel>?, hasNext: Boolean): List<ChannelListItem> {
        if (data.isNullOrEmpty()) return arrayListOf()

        val channelItems: List<ChannelListItem> = data.map { item -> ChannelListItem.ChannelItem(item) }
        if (hasNext)
            (channelItems as ArrayList).add(ChannelListItem.LoadingMoreItem)
        return channelItems
    }
}
