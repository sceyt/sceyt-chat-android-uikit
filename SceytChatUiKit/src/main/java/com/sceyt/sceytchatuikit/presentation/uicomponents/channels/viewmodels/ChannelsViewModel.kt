package com.sceyt.sceytchatuikit.presentation.uicomponents.channels.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.models.LoadKeyData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytDirectChannel
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.persistence.PersistenceChanelMiddleWare
import com.sceyt.sceytchatuikit.persistence.PersistenceMembersMiddleWare
import com.sceyt.sceytchatuikit.presentation.root.BaseViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.events.ChannelEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject

class ChannelsViewModel : BaseViewModel(), SceytKoinComponent {

    private val channelMiddleWare: PersistenceChanelMiddleWare by inject()
    private val membersMiddleWare: PersistenceMembersMiddleWare by inject()

    internal var searchQuery = ""
    internal var searchItems = listOf("")

    private val _loadChannelsFlow = MutableStateFlow<PaginationResponse<SceytChannel>>(PaginationResponse.Nothing())
    val loadChannelsFlow: StateFlow<PaginationResponse<SceytChannel>> = _loadChannelsFlow

    private val _blockUserLiveData = MutableLiveData<SceytResponse<List<User>>>()
    val blockUserLiveData: LiveData<SceytResponse<List<User>>> = _blockUserLiveData

    fun getChannels(offset: Int, query: String = searchQuery, loadKey: LoadKeyData? = null) {
        searchQuery = query
        setPagingLoadingStarted(PaginationResponse.LoadType.LoadNext)

        notifyPageLoadingState(false)

        viewModelScope.launch(Dispatchers.IO) {
            channelMiddleWare.loadChannels(offset, query, loadKey, false).collect {
                initPaginationResponse(it)
            }
        }
    }

    fun searchChannels(offset: Int, query: List<String> = searchItems, loadKey: LoadKeyData? = null) {
        searchItems = query
        setPagingLoadingStarted(PaginationResponse.LoadType.LoadNext)

        notifyPageLoadingState(false)

        viewModelScope.launch(Dispatchers.IO) {
            channelMiddleWare.searchChannels(offset, searchItems, loadKey, false).collect {
                initPaginationResponse(it)
            }
        }
    }

    private fun initPaginationResponse(response: PaginationResponse<SceytChannel>) {
        when (response) {
            is PaginationResponse.DBResponse -> {
                if (!checkIgnoreDatabasePagingResponse(response)) {
                    _loadChannelsFlow.value = response
                    notifyPageStateWithResponse(SceytResponse.Success(null), response.offset > 0, response.data.isEmpty())
                }
            }
            is PaginationResponse.ServerResponse -> {
                _loadChannelsFlow.value = response
                notifyPageStateWithResponse(response.data, response.offset > 0, response.cashData.isEmpty())
            }
            else -> return
        }
        pagingResponseReceived(response)
    }


    internal suspend fun mapToChannelItem(data: List<SceytChannel>?, hasNext: Boolean): List<ChannelListItem> {
        if (data.isNullOrEmpty()) return arrayListOf()
        val channelItems: List<ChannelListItem>

        withContext(Dispatchers.Default) {
            channelItems = data.map { item -> ChannelListItem.ChannelItem(item) }

            if (hasNext)
                (channelItems as ArrayList).add(ChannelListItem.LoadingMoreItem)
        }
        return channelItems
    }

    fun markChannelAsRead(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            channelMiddleWare.markChannelAsRead(channelId)
        }
    }

    fun markChannelAsUnRead(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            channelMiddleWare.markChannelAsUnRead(channelId)
        }
    }

    fun blockAndLeaveChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            channelMiddleWare.blockAndLeaveChannel(channelId)
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
            channelMiddleWare.clearHistory(channelId)
        }
    }

    fun deleteChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            channelMiddleWare.deleteChannel(channelId)
        }
    }

    fun leaveChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            channelMiddleWare.leaveChannel(channelId)
        }
    }

    fun muteChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            channelMiddleWare.muteChannel(channelId, 0)
        }
    }

    fun unMuteChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            channelMiddleWare.unMuteChannel(channelId)
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
