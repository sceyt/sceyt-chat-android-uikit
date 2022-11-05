package com.sceyt.sceytchatuikit.presentation.uicomponents.channels.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.SceytSharedPreference
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventData
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventsObserver
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageEventsObserver
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageStatusChangeData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytDirectChannel
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.persistence.PersistenceChanelMiddleWare
import com.sceyt.sceytchatuikit.persistence.PersistenceMembersMiddleWare
import com.sceyt.sceytchatuikit.persistence.mappers.toSceytUiMessage
import com.sceyt.sceytchatuikit.presentation.root.BaseViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.events.ChannelEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class ChannelsViewModel : BaseViewModel(), SceytKoinComponent {

    private val channelMiddleWare: PersistenceChanelMiddleWare by inject()
    private val membersMiddleWare: PersistenceMembersMiddleWare by inject()
    internal val preference: SceytSharedPreference by inject()

    internal var searchQuery = ""

    private val _loadChannelsFlow = MutableStateFlow<PaginationResponse<ChannelListItem>>(PaginationResponse.Nothing())
    val loadChannelsFlow: StateFlow<PaginationResponse<ChannelListItem>> = _loadChannelsFlow

    private val _markAsReadLiveData = MutableLiveData<SceytResponse<SceytChannel>>()
    val markAsReadLiveData: LiveData<SceytResponse<SceytChannel>> = _markAsReadLiveData

    private val _markAsUnReadLiveData = MutableLiveData<SceytResponse<SceytChannel>>()
    val markAsUnReadLiveData: LiveData<SceytResponse<SceytChannel>> = _markAsUnReadLiveData

    private val _blockChannelLiveData = MutableLiveData<SceytResponse<Long>>()
    val blockChannelLiveData: LiveData<SceytResponse<Long>> = _blockChannelLiveData

    private val _clearHistoryLiveData = MutableLiveData<SceytResponse<Long>>()
    val clearHistoryLiveData: LiveData<SceytResponse<Long>> = _clearHistoryLiveData

    private val _deleteChannelLiveData = MutableLiveData<SceytResponse<Long>>()
    val deleteChannelLiveData: LiveData<SceytResponse<Long>> = _deleteChannelLiveData

    private val _leaveChannelLiveData = MutableLiveData<SceytResponse<Long>>()
    val leaveChannelLiveData: LiveData<SceytResponse<Long>> = _leaveChannelLiveData

    private val _blockUserLiveData = MutableLiveData<SceytResponse<List<User>>>()
    val blockUserLiveData: LiveData<SceytResponse<List<User>>> = _blockUserLiveData

    private val _muteUnMuteLiveData = MutableLiveData<SceytResponse<SceytChannel>>()
    val muteUnMuteLiveData: LiveData<SceytResponse<SceytChannel>> = _muteUnMuteLiveData

    val onNewMessageFlow: Flow<Pair<SceytChannel, SceytMessage>>
    val onOutGoingMessageFlow: Flow<SceytMessage>
    val onOutGoingMessageStatusFlow: Flow<Pair<Long, SceytMessage>>
    val onMessageStatusFlow: Flow<MessageStatusChangeData>
    val onMessageEditedOrDeletedFlow: Flow<SceytMessage>
    val onChannelEventFlow: Flow<ChannelEventData>

    init {
        onMessageStatusFlow = ChannelEventsObserver.onMessageStatusFlow

        onChannelEventFlow = ChannelEventsObserver.onChannelEventFlow

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
        setPagingLoadingStarted(PaginationResponse.LoadType.LoadNext)

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
                    _loadChannelsFlow.value = PaginationResponse.DBResponse(mapToChannelItem(it.data, it.hasNext), 0, it.offset)
                    notifyPageStateWithResponse(SceytResponse.Success(null), it.offset > 0, it.data.isEmpty(), searchQuery)
                }
            }
            is PaginationResponse.ServerResponse -> {
                if (it.data is SceytResponse.Success) {
                    _loadChannelsFlow.value = PaginationResponse.ServerResponse(
                        SceytResponse.Success(mapToChannelItem(it.data.data, it.hasNext)), offset = it.offset, dbData = arrayListOf())
                }
                notifyPageStateWithResponse(it.data, it.offset > 0, it.data.data.isNullOrEmpty(), searchQuery)
            }
            is PaginationResponse.Nothing -> return
            is PaginationResponse.ServerResponse2 -> TODO()
        }

        pagingResponseReceived(it)
    }

    private fun mapToChannelItem(data: List<SceytChannel>?, hasNext: Boolean): List<ChannelListItem> {
        if (data.isNullOrEmpty()) return arrayListOf()

        val channelItems: List<ChannelListItem> = data.map { item -> ChannelListItem.ChannelItem(item) }
        if (hasNext)
            (channelItems as ArrayList).add(ChannelListItem.LoadingMoreItem)
        return channelItems
    }

    fun markChannelAsRead(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelMiddleWare.markChannelAsRead(channelId)
            _markAsReadLiveData.postValue(response)
        }
    }

    fun markChannelAsUnRead(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelMiddleWare.markChannelAsUnRead(channelId)
            _markAsUnReadLiveData.postValue(response)
        }
    }

    fun blockAndLeaveChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelMiddleWare.blockAndLeaveChannel(channelId)
            _blockChannelLiveData.postValue(response)
        }
    }

    fun blockUser(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = membersMiddleWare.blockUnBlockUser(userId, true)
            _blockUserLiveData.postValue(response)
        }
    }

    fun unBlockUser(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = membersMiddleWare.blockUnBlockUser(userId, false)
            _blockUserLiveData.postValue(response)
        }
    }

    fun clearHistory(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelMiddleWare.clearHistory(channelId)
            _clearHistoryLiveData.postValue(response)
        }
    }

    fun deleteChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelMiddleWare.deleteChannel(channelId)
            _deleteChannelLiveData.postValue(response)
        }
    }

    fun leaveChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelMiddleWare.leaveChannel(channelId)
            _leaveChannelLiveData.postValue(response)
        }
    }

    fun muteChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelMiddleWare.muteChannel(channelId, 0)
            _muteUnMuteLiveData.postValue(response)
        }
    }

    fun unMuteChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelMiddleWare.unMuteChannel(channelId)
            _muteUnMuteLiveData.postValue(response)
        }
    }

    internal fun onChannelEvent(event: ChannelEvent) {
        when (event) {
            is ChannelEvent.MarkAsRead -> markChannelAsRead(event.channel.id)
            is ChannelEvent.MarkAsUnRead -> markChannelAsUnRead(event.channel.id)
            is ChannelEvent.BlockChannel -> blockAndLeaveChannel(event.channel.id)
            is ChannelEvent.ClearHistory -> clearHistory(event.channel.id)
            is ChannelEvent.LeaveChannel -> leaveChannel(event.channel.id)
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
