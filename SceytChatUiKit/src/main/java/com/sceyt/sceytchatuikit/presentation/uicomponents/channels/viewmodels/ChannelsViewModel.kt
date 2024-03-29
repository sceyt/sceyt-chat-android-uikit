package com.sceyt.sceytchatuikit.presentation.uicomponents.channels.viewmodels

import androidx.annotation.IntRange
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.models.LoadKeyData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.persistence.PersistenceChanelMiddleWare
import com.sceyt.sceytchatuikit.persistence.PersistenceMembersMiddleWare
import com.sceyt.sceytchatuikit.persistence.extensions.asLiveData
import com.sceyt.sceytchatuikit.presentation.common.getFirstMember
import com.sceyt.sceytchatuikit.presentation.common.isDirect
import com.sceyt.sceytchatuikit.presentation.common.isPeerDeleted
import com.sceyt.sceytchatuikit.presentation.common.isPublic
import com.sceyt.sceytchatuikit.presentation.root.BaseViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.events.ChannelEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class ChannelsViewModel : BaseViewModel(), SceytKoinComponent {

    private val channelMiddleWare: PersistenceChanelMiddleWare by inject()
    private val membersMiddleWare: PersistenceMembersMiddleWare by inject()
    private var getChannelsJog: Job? = null
    val selectedChannels = mutableSetOf<Long>()

    var searchQuery = ""
        private set

    private val _loadChannelsFlow = MutableStateFlow<PaginationResponse<SceytChannel>>(PaginationResponse.Nothing())
    val loadChannelsFlow: StateFlow<PaginationResponse<SceytChannel>> = _loadChannelsFlow

    private val _searchChannelsFlow = MutableStateFlow<PaginationResponse<SceytChannel>>(PaginationResponse.Nothing())
    val searchChannelsFlow: StateFlow<PaginationResponse<SceytChannel>> = _searchChannelsFlow

    private val _blockUserLiveData = MutableLiveData<SceytResponse<List<User>>>()
    val blockUserLiveData = _blockUserLiveData.asLiveData()

    enum class NotifyFlow {
        LOAD, SEARCH
    }

    fun getChannels(offset: Int, query: String = searchQuery, loadKey: LoadKeyData? = null, ignoreDb: Boolean = false) {
        //Reset search if any
        searchQuery = query
        setPagingLoadingStarted(PaginationResponse.LoadType.LoadNext, ignoreDb = ignoreDb)

        notifyPageLoadingState(false)

        getChannelsJog?.cancel()
        getChannelsJog = viewModelScope.launch(Dispatchers.IO) {
            channelMiddleWare.loadChannels(offset, query, loadKey, ignoreDb).collect {
                initPaginationResponse(it)
            }
        }
    }

    fun searchChannelsWithUserIds(offset: Int, @IntRange(0, 50) limit: Int, searchQuery: String,
                                  userIds: List<String>, includeUserNames: Boolean,
                                  notifyFlow: NotifyFlow, onlyMine: Boolean, ignoreDb: Boolean = false,
                                  loadKey: LoadKeyData? = null) {
        if (notifyFlow == NotifyFlow.LOAD) {
            setPagingLoadingStarted(PaginationResponse.LoadType.LoadNext, ignoreDb = ignoreDb)

            notifyPageLoadingState(false)
        }

        getChannelsJog?.cancel()
        getChannelsJog = viewModelScope.launch(Dispatchers.IO) {
            channelMiddleWare.searchChannelsWithUserIds(offset, limit, searchQuery, userIds,
                includeUserNames, loadKey, onlyMine, ignoreDb).collect {
                when (notifyFlow) {
                    // Notifies chanel list like getChannels
                    NotifyFlow.LOAD -> initPaginationResponse(it)
                    // Just notifies search flow
                    NotifyFlow.SEARCH -> {
                        _loadChannelsFlow.value = PaginationResponse.Nothing()
                        _searchChannelsFlow.value = it
                    }
                }
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
            val response = channelMiddleWare.markChannelAsRead(channelId)
            if (response is SceytResponse.Error)
                notifyPageStateWithResponse(response)
        }
    }

    fun markChannelAsUnRead(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelMiddleWare.markChannelAsUnRead(channelId)
            if (response is SceytResponse.Error)
                notifyPageStateWithResponse(response)
        }
    }

    fun blockAndLeaveChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelMiddleWare.blockAndLeaveChannel(channelId)
            if (response is SceytResponse.Error)
                notifyPageStateWithResponse(response)
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

    fun clearHistory(channelId: Long, forEveryone: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelMiddleWare.clearHistory(channelId, forEveryone)
            if (response is SceytResponse.Error)
                notifyPageStateWithResponse(response)
        }
    }

    fun deleteChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelMiddleWare.deleteChannel(channelId)
            if (response is SceytResponse.Error)
                notifyPageStateWithResponse(response)
        }
    }

    fun leaveChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelMiddleWare.leaveChannel(channelId)
            if (response is SceytResponse.Error)
                notifyPageStateWithResponse(response)
        }
    }

    fun muteChannel(channelId: Long, muteUntil: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelMiddleWare.muteChannel(channelId, muteUntil)
            if (response is SceytResponse.Error)
                notifyPageStateWithResponse(response)
        }
    }

    fun unMuteChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelMiddleWare.unMuteChannel(channelId)
            if (response is SceytResponse.Error)
                notifyPageStateWithResponse(response)
        }
    }

    fun hideChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelMiddleWare.hideChannel(channelId)
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
                    blockUser((event.channel.getFirstMember() ?: return).id)
            }

            is ChannelEvent.UnBlockUser -> {
                if (event.channel.isDirect())
                    unBlockUser((event.channel.getFirstMember() ?: return).id)
            }

            is ChannelEvent.DeleteChannel -> deleteChannel(event.channel.id)
            is ChannelEvent.Mute -> muteChannel(event.channel.id, 0)
            is ChannelEvent.UnMute -> unMuteChannel(event.channel.id)
            is ChannelEvent.Pin -> {}
            is ChannelEvent.UnPin -> {}
        }
    }
}
