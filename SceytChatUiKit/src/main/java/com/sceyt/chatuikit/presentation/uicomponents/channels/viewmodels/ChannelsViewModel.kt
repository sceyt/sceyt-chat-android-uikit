package com.sceyt.chatuikit.presentation.uicomponents.channels.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.interactor.ChannelInteractor
import com.sceyt.chatuikit.persistence.interactor.ChannelMemberInteractor
import com.sceyt.chatuikit.persistence.extensions.asLiveData
import com.sceyt.chatuikit.presentation.common.getPeer
import com.sceyt.chatuikit.presentation.common.isDirect
import com.sceyt.chatuikit.presentation.common.isPeerDeleted
import com.sceyt.chatuikit.presentation.common.isPublic
import com.sceyt.chatuikit.presentation.root.BaseViewModel
import com.sceyt.chatuikit.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.chatuikit.presentation.uicomponents.channels.events.ChannelEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class ChannelsViewModel : BaseViewModel(), SceytKoinComponent {

    private val channelInteractor: ChannelInteractor by inject()
    private val channelMemberInteractor: ChannelMemberInteractor by inject()
    private var getChannelsJog: Job? = null
    val selectedChannels = mutableSetOf<Long>()

    var searchQuery = ""
        private set

    private val _loadChannelsFlow = MutableStateFlow<PaginationResponse<SceytChannel>>(PaginationResponse.Nothing())
    val loadChannelsFlow: StateFlow<PaginationResponse<SceytChannel>> = _loadChannelsFlow

    private val _blockUserLiveData = MutableLiveData<SceytResponse<List<User>>>()
    val blockUserLiveData = _blockUserLiveData.asLiveData()

    fun getChannels(offset: Int, query: String = searchQuery, loadKey: LoadKeyData? = null, ignoreDb: Boolean = false) {
        searchQuery = query
        setPagingLoadingStarted(PaginationResponse.LoadType.LoadNext, ignoreDb = ignoreDb)

        notifyPageLoadingState(false)

        getChannelsJog?.cancel()
        getChannelsJog = viewModelScope.launch(Dispatchers.IO) {
            channelInteractor.loadChannels(offset, query, loadKey, ignoreDb).collect {
                initPaginationResponse(it)
            }
        }
    }

    private fun initPaginationResponse(response: PaginationResponse<SceytChannel>) {
        when (response) {
            is PaginationResponse.DBResponse -> {
                if (!checkIgnoreDatabasePagingResponse(response)) {
                    _loadChannelsFlow.value = response
                    notifyPageStateWithResponse(SceytResponse.Success(null),
                        wasLoadingMore = response.offset > 0,
                        isEmpty = response.data.isEmpty(), searchQuery = response.query)
                }
            }

            is PaginationResponse.ServerResponse -> {
                _loadChannelsFlow.value = response
                notifyPageStateWithResponse(response.data, wasLoadingMore = response.offset > 0,
                    isEmpty = response.cacheData.isEmpty(), searchQuery = response.query)
            }

            else -> return
        }
        pagingResponseReceived(response)
    }

    internal fun mapToChannelItem(data: List<SceytChannel>?, hasNext: Boolean,
                                  includeDirectChannelsWithDeletedPeers: Boolean = true): List<ChannelListItem> {

        val filteredChannels = if (includeDirectChannelsWithDeletedPeers) data ?: emptyList()
        else data?.filter { channel -> !channel.isPeerDeleted() }
                ?: emptyList()

        if (filteredChannels.isEmpty())
            return emptyList()

        val channelItems: List<ChannelListItem>
        channelItems = filteredChannels.map { item -> ChannelListItem.ChannelItem(item) }

        if (hasNext)
            (channelItems as ArrayList).add(ChannelListItem.LoadingMoreItem)

        return channelItems
    }

    fun markChannelAsRead(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.markChannelAsRead(channelId)
            if (response is SceytResponse.Error)
                notifyPageStateWithResponse(response)
        }
    }

    fun markChannelAsUnRead(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.markChannelAsUnRead(channelId)
            if (response is SceytResponse.Error)
                notifyPageStateWithResponse(response)
        }
    }

    fun blockAndLeaveChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.blockAndLeaveChannel(channelId)
            if (response is SceytResponse.Error)
                notifyPageStateWithResponse(response)
        }
    }

    fun blockUser(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelMemberInteractor.blockUnBlockUser(userId, true)
            _blockUserLiveData.postValue(response)
        }
    }

    fun unBlockUser(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelMemberInteractor.blockUnBlockUser(userId, false)
            _blockUserLiveData.postValue(response)
        }
    }

    fun clearHistory(channelId: Long, forEveryone: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.clearHistory(channelId, forEveryone)
            if (response is SceytResponse.Error)
                notifyPageStateWithResponse(response)
        }
    }

    fun deleteChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.deleteChannel(channelId)
            if (response is SceytResponse.Error)
                notifyPageStateWithResponse(response)
        }
    }

    fun leaveChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.leaveChannel(channelId)
            if (response is SceytResponse.Error)
                notifyPageStateWithResponse(response)
        }
    }

    fun muteChannel(channelId: Long, muteUntil: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.muteChannel(channelId, muteUntil)
            if (response is SceytResponse.Error)
                notifyPageStateWithResponse(response)
        }
    }

    fun unMuteChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.unMuteChannel(channelId)
            if (response is SceytResponse.Error)
                notifyPageStateWithResponse(response)
        }
    }

    fun pinChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.pinChannel(channelId)
            if (response is SceytResponse.Error)
                notifyPageStateWithResponse(response)
        }
    }

    fun unpinChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.unpinChannel(channelId)
            if (response is SceytResponse.Error)
                notifyPageStateWithResponse(response)
        }
    }

    fun hideChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.hideChannel(channelId)
            if (response is SceytResponse.Error)
                notifyPageStateWithResponse(response)
        }
    }

    internal fun onChannelCommandEvent(event: ChannelEvent) {
        when (event) {
            is ChannelEvent.MarkAsRead -> markChannelAsRead(event.channel.id)
            is ChannelEvent.MarkAsUnRead -> markChannelAsUnRead(event.channel.id)
            is ChannelEvent.BlockChannel -> blockAndLeaveChannel(event.channel.id)
            is ChannelEvent.ClearHistory -> clearHistory(event.channel.id, event.channel.isPublic())
            is ChannelEvent.LeaveChannel -> leaveChannel(event.channel.id)
            is ChannelEvent.BlockUser -> {
                if (event.channel.isDirect())
                    blockUser((event.channel.getPeer() ?: return).id)
            }

            is ChannelEvent.UnBlockUser -> {
                if (event.channel.isDirect())
                    unBlockUser((event.channel.getPeer() ?: return).id)
            }

            is ChannelEvent.DeleteChannel -> deleteChannel(event.channel.id)
            is ChannelEvent.Mute -> muteChannel(event.channel.id, 0)
            is ChannelEvent.UnMute -> unMuteChannel(event.channel.id)
            is ChannelEvent.Pin -> pinChannel(event.channel.id)
            is ChannelEvent.UnPin -> unpinChannel(event.channel.id)
        }
    }
}
