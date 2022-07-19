package com.sceyt.chat.ui.presentation.uicomponents.channels.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.chat.models.user.User
import com.sceyt.chat.ui.data.ChannelsRepository
import com.sceyt.chat.ui.data.channeleventobserver.ChannelEventData
import com.sceyt.chat.ui.data.channeleventobserver.MessageStatusChange
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.models.channels.ChannelTypeEnum
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.models.channels.SceytDirectChannel
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.data.toChannel
import com.sceyt.chat.ui.data.toGroupChannel
import com.sceyt.chat.ui.data.toSceytUiMessage
import com.sceyt.chat.ui.persistence.PersistenceMiddleWare
import com.sceyt.chat.ui.presentation.root.BaseViewModel
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.chat.ui.presentation.uicomponents.channels.events.ChannelEvent
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChannelsViewModel(private val channelsRepository: ChannelsRepository) : BaseViewModel() {
    internal var searchQuery = ""

    private val persistentMiddleWare: PersistenceMiddleWare = PersistenceMiddleWare()

    private val _channelsFlow = MutableStateFlow<SceytResponse<List<ChannelListItem>>>(SceytResponse.Success(null))
    val channelsFlow: StateFlow<SceytResponse<List<ChannelListItem>>> = _channelsFlow

    private val _loadMoreChannelsFlow = MutableStateFlow<SceytResponse<List<ChannelListItem>>>(SceytResponse.Success(null))
    val loadMoreChannelsFlow: StateFlow<SceytResponse<List<ChannelListItem>>> = _loadMoreChannelsFlow

    private val _markAsReadLiveData = MutableLiveData<SceytResponse<MessageListMarker>>()
    val markAsReadLiveData: LiveData<SceytResponse<MessageListMarker>> = _markAsReadLiveData

    private val _blockChannelLiveData = MutableLiveData<SceytResponse<Long>>()
    val blockChannelLiveData: LiveData<SceytResponse<Long>> = _blockChannelLiveData

    private val _clearHistoryLiveData = MutableLiveData<SceytResponse<Long>>()
    val clearHistoryLiveData: LiveData<SceytResponse<Long>> = _clearHistoryLiveData

    private val _leaveChannelLiveData = MutableLiveData<SceytResponse<Long>>()
    val leaveChannelLiveData: LiveData<SceytResponse<Long>> = _leaveChannelLiveData

    private val _blockUserLiveData = MutableLiveData<SceytResponse<List<User>>>()
    val blockUserLiveData: LiveData<SceytResponse<List<User>>> = _blockUserLiveData

    val onNewMessageFlow: Flow<Pair<Channel, Message>>
    val onOutGoingMessageFlow: Flow<SceytMessage>
    val onMessageStatusFlow: Flow<MessageStatusChange>
    val onMessageEditedOrDeletedFlow: Flow<SceytMessage>
    val onChannelEventFlow: Flow<ChannelEventData>

    init {
        onMessageStatusFlow = channelsRepository.onMessageStatusFlow

        onChannelEventFlow = channelsRepository.onChannelEvenFlow

        onNewMessageFlow = channelsRepository.onMessageFlow.filter {
            !it.second.replyInThread
        }

        onOutGoingMessageFlow = channelsRepository.onOutGoingMessageFlow.filter {
            !it.replyInThread
        }

        onMessageEditedOrDeletedFlow = channelsRepository.onMessageEditedOrDeleteFlow.map {
            it.toSceytUiMessage()
        }
    }


    fun getChannels(query: String = searchQuery) {
        searchQuery = query
        loadingItems = true

        notifyPageLoadingState(false)

        viewModelScope.launch(Dispatchers.IO) {
            persistentMiddleWare.getChannels(searchQuery).collect {
                if (it is SceytResponse.Success) {
                    Log.i("asdasdasd"," data  ${it.data?.map { it.channelSubject }}")
                }
                initResponse(it, false)
            }
        }
    }


    fun loadMoreChannels(offset: Int) {
        loadingItems = true
        notifyPageLoadingState(true)

        viewModelScope.launch(Dispatchers.IO) {
            persistentMiddleWare.loadMore(offset).collect {
                if (it is SceytResponse.Success) {
                    Log.i("asdasdasd","load more ${it.data?.map { it.channelSubject }}")
                }
                initResponse(it, true)
            }
        }
    }

    private fun initResponse(it: SceytResponse<List<SceytChannel>>, loadingNext: Boolean) {
        loadingItems = false
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

        notifyPageStateWithResponse(response, loadingNext, response.data.isNullOrEmpty(), searchQuery)
    }

    private fun mapToChannelItem(data: List<SceytChannel>?, hasNext: Boolean): List<ChannelListItem> {
        if (data.isNullOrEmpty()) return arrayListOf()

        val channelItems: List<ChannelListItem> = data.map { item -> ChannelListItem.ChannelItem(item) }
        if (hasNext)
            (channelItems as ArrayList).add(ChannelListItem.LoadingMoreItem)
        return channelItems
    }

    private fun markAsRead(channel: SceytChannel) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelsRepository.markAsRead(channel.toChannel())
            _markAsReadLiveData.postValue(response)
        }
    }

    private fun blockChannel(channel: SceytChannel) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelsRepository.blockChannel(channel.toGroupChannel())
            _blockChannelLiveData.postValue(response)
        }
    }

    private fun blockUser(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelsRepository.blockUser(userId)
            _blockUserLiveData.postValue(response)
        }
    }


    private fun unBlockUser(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelsRepository.unblockUser(userId)
            _blockUserLiveData.postValue(response)
        }
    }

    private fun clearHistory(channel: SceytChannel) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelsRepository.clearHistory(channel.toChannel())
            _clearHistoryLiveData.postValue(response)
        }
    }

    private fun leaveChannel(channel: SceytChannel) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelsRepository.leaveChannel(channel.toGroupChannel())
            _leaveChannelLiveData.postValue(response)
        }
    }

    fun onChannelEvent(event: ChannelEvent) {
        when (event) {
            is ChannelEvent.MarkAsRead -> markAsRead(event.channel)
            is ChannelEvent.BlockChannel -> blockChannel(event.channel)
            is ChannelEvent.ClearHistory -> clearHistory(event.channel)
            is ChannelEvent.LeaveChannel -> leaveChannel(event.channel)
            is ChannelEvent.BlockUser -> {
                if (event.channel.channelType == ChannelTypeEnum.Direct)
                    blockUser(((event.channel as SceytDirectChannel).peer ?: return).id)
                else throw RuntimeException("Channel must be direct")
            }
            is ChannelEvent.UnBlockUser -> {
                if (event.channel.channelType == ChannelTypeEnum.Direct)
                    unBlockUser(((event.channel as SceytDirectChannel).peer ?: return).id)
                else throw RuntimeException("Channel must be direct")
            }
        }
    }
}
