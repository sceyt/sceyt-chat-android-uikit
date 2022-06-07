package com.sceyt.chat.ui.presentation.uicomponents.channels.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.ui.data.ChannelsRepositoryImpl
import com.sceyt.chat.ui.data.channeleventobserverservice.ChannelEventData
import com.sceyt.chat.ui.data.channeleventobserverservice.MessageStatusChange
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.data.toSceytUiMessage
import com.sceyt.chat.ui.presentation.root.BaseViewModel
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ChannelsViewModel : BaseViewModel() {
    private var searchQuery = ""
    var isLoadingChannels = false
    var hasNext = false

    // todo di
    private val repo = ChannelsRepositoryImpl()

    private val _channelsFlow = MutableStateFlow<SceytResponse<List<ChannelListItem>>>(SceytResponse.Success(null))
    val channelsFlow: StateFlow<SceytResponse<List<ChannelListItem>>> = _channelsFlow

    private val _loadMoreChannelsFlow = MutableStateFlow<SceytResponse<List<ChannelListItem>>>(SceytResponse.Success(null))
    val loadMoreChannelsFlow: StateFlow<SceytResponse<List<ChannelListItem>>> = _loadMoreChannelsFlow

    val onNewMessageLiveData = MutableLiveData<Pair<Channel, Message>>()
    val onMessageStatusLiveData = MutableLiveData<MessageStatusChange>()
    val onMessageEditedOrDeletedLiveData = MutableLiveData<SceytMessage>()
    val onChannelEventLiveData = MutableLiveData<ChannelEventData>()

    init {
        addChannelListeners()
    }

    private fun addChannelListeners() {
        viewModelScope.launch {
            repo.onMessageFlow.collect {
                onNewMessageLiveData.value = it
            }
        }

        viewModelScope.launch {
            repo.onMessageStatusFlow.collect {
                onMessageStatusLiveData.value = it
            }
        }

        viewModelScope.launch {
            repo.onMessageEditedOrDeleteFlow.collect {
                onMessageEditedOrDeletedLiveData.value = it.toSceytUiMessage()
            }
        }

        viewModelScope.launch {
            repo.onChannelEvenFlow.collect {
                onChannelEventLiveData.value = it
            }
        }
    }

    fun loadChannels(offset: Int, limit: Int = SceytUIKitConfig.CHANNELS_LOAD_SIZE, query: String = searchQuery) {
        searchQuery = query
        isLoadingChannels = true
        val isLoadingMore = offset > 0

        notifyPageLoadingState(isLoadingMore, searchQuery)

        viewModelScope.launch(Dispatchers.IO) {
            if (searchQuery.isBlank()) {
                getChannelsList(offset, limit, isLoadingMore)
            } else searchChannels(offset, query, isLoadingMore)
        }
    }

    private suspend fun getChannelsList(offset: Int, limit: Int, loadingMoreType: Boolean) {
        initResponse(repo.getChannels(offset, limit), loadingMoreType)
    }

    private suspend fun searchChannels(offset: Int, query: String, loadingMoreType: Boolean) {
        initResponse(repo.searchChannels(offset, query), loadingMoreType)
    }

    private fun initResponse(it: SceytResponse<List<SceytChannel>>, loadingNext: Boolean) {
        isLoadingChannels = false
        when (it) {
            is SceytResponse.Success -> {
                hasNext = it.data?.size == SceytUIKitConfig.CHANNELS_LOAD_SIZE
                emitResponse(SceytResponse.Success(mapToChannelItem(it.data, hasNext)), loadingNext)
            }
            is SceytResponse.Error -> emitResponse(SceytResponse.Error(it.message), loadingNext)
        }
    }

    private fun emitResponse(response: SceytResponse<List<ChannelListItem>>, loadingNext: Boolean) {
        if (loadingNext)
            _loadMoreChannelsFlow.value = response
        else _channelsFlow.value = response

        notifyPageStateWithResponse(loadingNext, response.data.isNullOrEmpty(), searchQuery)
    }

    private fun mapToChannelItem(data: List<SceytChannel>?, hasNext: Boolean): List<ChannelListItem> {
        if (data.isNullOrEmpty()) return arrayListOf()

        val channelItems: List<ChannelListItem> = data.map { item -> ChannelListItem.ChannelItem(item) }
        if (hasNext)
            (channelItems as ArrayList).add(ChannelListItem.LoadingMoreItem)
        return channelItems
    }
}
