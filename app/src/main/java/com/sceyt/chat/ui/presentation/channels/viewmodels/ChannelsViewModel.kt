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
    var isLoadingMore = false
    var hasNext = false

    // todo di
    private val repo = ChannelsRepositoryImpl()

    private val _channelsFlow = MutableStateFlow<SceytResponse<List<ChannelListItem>>>(SceytResponse.Loading())
    val channelsFlow: StateFlow<SceytResponse<List<ChannelListItem>>> = _channelsFlow

    private val _loadMoreChannelsFlow = MutableStateFlow<SceytResponse<List<ChannelListItem>>>(SceytResponse.Loading())
    val loadMoreChannelsFlow: StateFlow<SceytResponse<List<ChannelListItem>>> = _loadMoreChannelsFlow


    fun loadChannels(offset: Int, loadingMore: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.getChannels(offset).collect {
                if (it is SceytResponse.Success)
                    hasNext = it.data?.size == SceytUIKitConfig.CHANNELS_LOAD_SIZE
                if (loadingMore)
                    _loadMoreChannelsFlow.value = mapToChannelItem(it, hasNext)
                else _channelsFlow.value = mapToChannelItem(it, hasNext)
            }
        }
    }

    private fun mapToChannelItem(sceytResponse: SceytResponse<List<SceytUiChannel>>?, hasNext: Boolean): SceytResponse<List<ChannelListItem>> {
        if (sceytResponse is SceytResponse.Loading || sceytResponse == null)
            return SceytResponse.Loading()

        val channelItems: List<ChannelListItem> = sceytResponse.data?.map { ChannelListItem.ChannelItem(it) }
                ?: arrayListOf()
        if (hasNext)
            (channelItems as ArrayList).add(ChannelListItem.LoadingMoreItem)
        return SceytResponse.Success(channelItems)
    }
}
