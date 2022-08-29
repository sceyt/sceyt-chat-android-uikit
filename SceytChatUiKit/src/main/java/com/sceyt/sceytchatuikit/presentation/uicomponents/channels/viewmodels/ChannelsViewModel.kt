package com.sceyt.sceytchatuikit.presentation.uicomponents.channels.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.SceytKoinComponent
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageEventsObserver
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageStatusChangeData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytDirectChannel
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.toSceytUiMessage
import com.sceyt.sceytchatuikit.persistence.PersistenceChanelMiddleWare
import com.sceyt.sceytchatuikit.persistence.PersistenceMembersMiddleWare
import com.sceyt.sceytchatuikit.presentation.root.BaseViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.events.ChannelEvent
import com.sceyt.sceytchatuikit.sceytconfigs.SceytUIKitConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class ChannelsViewModel : BaseViewModel(), SceytKoinComponent {

    private val channelMiddleWare: PersistenceChanelMiddleWare by inject()
    private val membersMiddleWare: PersistenceMembersMiddleWare by inject()

    internal var searchQuery = ""

    private val _loadChannelsFlow = MutableStateFlow<PaginationResponse<ChannelListItem>>(PaginationResponse.Nothing())
    val loadChannelsFlow: StateFlow<PaginationResponse<ChannelListItem>> = _loadChannelsFlow

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

    val onNewMessageFlow: Flow<Pair<SceytChannel, SceytMessage>>
    val onOutGoingMessageFlow: Flow<SceytMessage>
    val onOutGoingMessageStatusFlow: Flow<MessageStatusChangeData>
    val onMessageStatusFlow: Flow<MessageStatusChangeData>
    val onMessageEditedOrDeletedFlow: Flow<SceytMessage>
    val onChannelEventFlow: Flow<com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventData>

    init {
        onMessageStatusFlow = com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventsObserver.onMessageStatusFlow

        onChannelEventFlow = com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventsObserver.onChannelEventFlow

        onNewMessageFlow = MessageEventsObserver.onMessageFlow.filter {
            !it.second.replyInThread
        }

        onOutGoingMessageFlow = MessageEventsObserver.onOutgoingMessageFlow.filter {
            !it.replyInThread
        }

        onOutGoingMessageStatusFlow = MessageEventsObserver.onOutGoingMessageStatusFlow

        onMessageEditedOrDeletedFlow = MessageEventsObserver.onMessageEditedOrDeletedFlow
            .filterNotNull()
            .map { it.toSceytUiMessage() }
    }


    fun getChannels(offset: Int, query: String = searchQuery) {
        searchQuery = query
        loadingItems.set(true)

        notifyPageLoadingState(false)

        viewModelScope.launch(Dispatchers.IO) {
            channelMiddleWare.loadChannels(offset, query).collect {
                initResponse(it)
            }
        }
    }

    private fun initResponse(it: PaginationResponse<SceytChannel>) {
        when (it) {
            is PaginationResponse.DBResponse -> {
                if (it.data.isNotEmpty()) {
                    hasNext = it.data.size == SceytUIKitConfig.CHANNELS_LOAD_SIZE
                    _loadChannelsFlow.value = PaginationResponse.DBResponse(mapToChannelItem(it.data, hasNext), it.offset)
                    notifyPageStateWithResponse(SceytResponse.Success(null), it.offset > 0, it.data.isEmpty(), searchQuery)
                }
            }
            is PaginationResponse.ServerResponse -> {
                when (it.data) {
                    is SceytResponse.Success -> {
                        hasNext = it.data.data?.size == SceytUIKitConfig.CHANNELS_LOAD_SIZE

                        _loadChannelsFlow.value = PaginationResponse.ServerResponse(
                            SceytResponse.Success(mapToChannelItem(it.data.data, hasNext)), offset = it.offset, dbData = arrayListOf())

                        notifyPageStateWithResponse(it.data, it.offset > 0, it.data.data.isNullOrEmpty(), searchQuery)
                    }
                    is SceytResponse.Error -> notifyPageStateWithResponse(it.data, it.offset > 0, it.data.data.isNullOrEmpty(), searchQuery)
                }
            }
            is PaginationResponse.Nothing -> return
        }
        loadingItems.set(false)
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
            val response = channelMiddleWare.markChannelAsRead(channel)
            _markAsReadLiveData.postValue(response)
        }
    }

    private fun blockAndLeaveChannel(channel: SceytChannel) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelMiddleWare.blockAndLeaveChannel(channel)
            _blockChannelLiveData.postValue(response)
        }
    }

    private fun blockUser(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = membersMiddleWare.blockUnBlockUser(userId, true)
            _blockUserLiveData.postValue(response)
        }
    }

    private fun unBlockUser(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = membersMiddleWare.blockUnBlockUser(userId, false)
            _blockUserLiveData.postValue(response)
        }
    }

    private fun clearHistory(channel: SceytChannel) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelMiddleWare.clearHistory(channel)
            _clearHistoryLiveData.postValue(response)
        }
    }

    private fun leaveChannel(channel: SceytChannel) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelMiddleWare.leaveChannel(channel)
            _leaveChannelLiveData.postValue(response)
        }
    }

    internal fun onChannelEvent(event: ChannelEvent) {
        when (event) {
            is ChannelEvent.MarkAsRead -> markAsRead(event.channel)
            is ChannelEvent.BlockChannel -> blockAndLeaveChannel(event.channel)
            is ChannelEvent.ClearHistory -> clearHistory(event.channel)
            is ChannelEvent.LeaveChannel -> leaveChannel(event.channel)
            is ChannelEvent.BlockUser -> {
                if (event.channel.channelType == ChannelTypeEnum.Direct)
                    blockUser(((event.channel as SceytDirectChannel).peer ?: return).id)
            }
            is ChannelEvent.UnBlockUser -> {
                if (event.channel.channelType == ChannelTypeEnum.Direct)
                    unBlockUser(((event.channel as SceytDirectChannel).peer ?: return).id)
            }
        }
    }
}
