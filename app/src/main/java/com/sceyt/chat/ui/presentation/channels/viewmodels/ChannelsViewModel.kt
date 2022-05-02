package com.sceyt.chat.ui.presentation.channels.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.ui.data.ChannelsRepositoryImpl
import com.sceyt.chat.ui.data.SceytResponse
import com.sceyt.chat.ui.data.models.SceytUiChannel
import com.sceyt.chat.ui.presentation.channels.adapter.ChannelListItem
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ChannelsViewModel : ViewModel() {
    private var searchQuery = ""
    var isLoadingMore = false
    var hasNext = false

    // todo di
    private val repo = ChannelsRepositoryImpl()

    private val _channelsFlow = MutableStateFlow<SceytResponse<List<ChannelListItem>>>(SceytResponse.Loading())
    val channelsFlow: StateFlow<SceytResponse<List<ChannelListItem>>> = _channelsFlow

    private val _loadMoreChannelsLiveData = MutableStateFlow<SceytResponse<List<ChannelListItem>>>(SceytResponse.Loading())
    val loadMoreChannelsLiveData: StateFlow<SceytResponse<List<ChannelListItem>>> = _loadMoreChannelsLiveData


    fun loadChannels(offset: Int, query: String = searchQuery, loadingMore: Boolean = false) {
        searchQuery = query
        viewModelScope.launch(Dispatchers.IO) {
            if (searchQuery.isBlank()) {
                collectChannels(offset, loadingMore)
            } else collectSearchChannels(offset, query, loadingMore)
        }
    }

    private suspend fun collectChannels(offset: Int, loadingMore: Boolean) {
        repo.getChannels(offset).collect {
            initResponse(it, loadingMore)
        }
    }

    private suspend fun collectSearchChannels(offset: Int, query: String, loadingMore: Boolean) {
        repo.searchChannels(offset, query).collect {
            initResponse(it, loadingMore)
        }
    }

    private fun initResponse(it: SceytResponse<List<SceytUiChannel>>, loadingMore: Boolean) {
        when (it) {
            is SceytResponse.Success -> {
                hasNext = it.data?.size == SceytUIKitConfig.CHANNELS_LOAD_SIZE
                emitResponse(SceytResponse.Success(mapToChannelItem(it.data, hasNext)), loadingMore)
            }
            is SceytResponse.Error -> emitResponse(SceytResponse.Error(it.message), loadingMore)
            is SceytResponse.Loading -> emitResponse(SceytResponse.Loading(it.isLoading), loadingMore)
        }
        isLoadingMore = it is SceytResponse.Loading && it.isLoading
    }

    private fun emitResponse(response: SceytResponse<List<ChannelListItem>>, loadingMore: Boolean) {
        if (loadingMore)
            _loadMoreChannelsLiveData.value = response
        else _channelsFlow.value = response
    }

    private fun mapToChannelItem(data: List<SceytUiChannel>?, hasNext: Boolean): List<ChannelListItem> {
        if (data.isNullOrEmpty()) return emptyList()

        val channelItems: List<ChannelListItem> = data.map { item -> ChannelListItem.ChannelItem(item) }
        if (hasNext)
            (channelItems as ArrayList).add(ChannelListItem.LoadingMoreItem)
        return channelItems
    }
}
