package com.sceyt.sceytchatuikit.presentation.uicomponents.channels.viewmodels

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
import com.sceyt.sceytchatuikit.persistence.extensions.safeResume
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
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.inject

class ChannelsViewModel : BaseViewModel(), SceytKoinComponent {

    private val channelMiddleWare: PersistenceChanelMiddleWare by inject()
    private val membersMiddleWare: PersistenceMembersMiddleWare by inject()
    private var getChannelsJog: Job? = null

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
        setPagingLoadingStarted(PaginationResponse.LoadType.LoadNext)

        notifyPageLoadingState(false)

        getChannelsJog?.cancel()
        getChannelsJog = viewModelScope.launch(Dispatchers.IO) {
            channelMiddleWare.loadChannels(offset, query, loadKey, ignoreDb).collect {
                initPaginationResponse(it)
            }
        }
    }

    fun searchChannels(offset: Int, limit: Int, query: List<String>, loadKey: LoadKeyData? = null,
                       notifyFlow: NotifyFlow, onlyMine: Boolean, ignoreDb: Boolean = false) {
        if (notifyFlow == NotifyFlow.LOAD) {
            setPagingLoadingStarted(PaginationResponse.LoadType.LoadNext)

            notifyPageLoadingState(false)
        }

        getChannelsJog?.cancel()
        getChannelsJog = viewModelScope.launch(Dispatchers.IO) {
            channelMiddleWare.searchChannels(offset, limit, query, loadKey, onlyMine, ignoreDb).collect {
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
                    notifyPageStateWithResponse(SceytResponse.Success(null), response.offset > 0, response.data.isEmpty())
                }
            }

            is PaginationResponse.ServerResponse -> {
                _loadChannelsFlow.value = response
                notifyPageStateWithResponse(response.data, response.offset > 0, response.cacheData.isEmpty())
            }

            else -> return
        }
        pagingResponseReceived(response)
    }

    internal suspend fun mapToChannelItem(data: List<SceytChannel>?, hasNext: Boolean,
                                          includeDirectChannelsWithDeletedPeers: Boolean = true): List<ChannelListItem> {
        return suspendCancellableCoroutine {

            val filteredChannels = if (includeDirectChannelsWithDeletedPeers) data ?: emptyList()
            else data?.filter { channel -> !channel.isPeerDeleted() }
                    ?: emptyList()

            if (filteredChannels.isEmpty())
                it.safeResume(emptyList())

            val channelItems: List<ChannelListItem>
            channelItems = filteredChannels.map { item -> ChannelListItem.ChannelItem(item) }

            if (hasNext)
                (channelItems as ArrayList).add(ChannelListItem.LoadingMoreItem)

            it.safeResume(channelItems)
        }
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

    fun clearHistory(channelId: Long, forEveryone: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            channelMiddleWare.clearHistory(channelId, forEveryone)
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

    fun muteChannel(channelId: Long, muteUntil: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            channelMiddleWare.muteChannel(channelId, muteUntil)
        }
    }

    fun unMuteChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            channelMiddleWare.unMuteChannel(channelId)
        }
    }

    fun hideChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            channelMiddleWare.hideChannel(channelId)
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
        }
    }
}
