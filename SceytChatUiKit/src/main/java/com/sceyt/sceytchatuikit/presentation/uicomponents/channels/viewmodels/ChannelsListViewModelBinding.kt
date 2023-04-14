package com.sceyt.sceytchatuikit.presentation.uicomponents.channels.viewmodels

import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.sceyt.sceytchatuikit.SceytKitClient
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventsObserver
import com.sceyt.sceytchatuikit.data.models.LoadKeyData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytDirectChannel
import com.sceyt.sceytchatuikit.extensions.customToastSnackBar
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.ChannelsCache
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.ChannelsListView
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.TypingCancelHelper
import com.sceyt.sceytchatuikit.presentation.uicomponents.searchinput.SearchInputView
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.services.SceytPresenceChecker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

fun ChannelsViewModel.bind(channelsListView: ChannelsListView, lifecycleOwner: LifecycleOwner) {

    val typingCancelHelper by lazy { TypingCancelHelper() }

    getChannels(0, query = searchQuery)

    suspend fun initPaginationDbResponse(response: PaginationResponse.DBResponse<SceytChannel>) {
        if (response.offset == 0) {
            channelsListView.setChannelsList(mapToChannelItem(data = response.data, hasNext = response.hasNext))
        } else
            channelsListView.addNewChannels(mapToChannelItem(data = response.data, hasNext = response.hasNext))
    }

    suspend fun initPaginationServerResponse(response: PaginationResponse.ServerResponse<SceytChannel>) {
        when (response.data) {
            is SceytResponse.Success -> {
                if (response.hasDiff) {
                    val newChannels = mapToChannelItem(data = response.cacheData, hasNext = response.hasNext)
                    channelsListView.setChannelsList(newChannels)
                } else {
                    if (!hasNextDb) channelsListView.hideLoadingMore()
                }
            }
            is SceytResponse.Error -> if (!hasNextDb) channelsListView.hideLoadingMore()
        }
    }

    suspend fun initChannelsResponse(response: PaginationResponse<SceytChannel>) {
        viewModelScope.launch {
            when (response) {
                is PaginationResponse.DBResponse -> initPaginationDbResponse(response)
                is PaginationResponse.ServerResponse -> initPaginationServerResponse(response)
                else -> return@launch
            }
        }
    }

    loadChannelsFlow.onEach(::initChannelsResponse).launchIn(lifecycleOwner.lifecycleScope)

    ChannelsCache.channelDeletedFlow.onEach {
        channelsListView.deleteChannel(it)
    }.launchIn(lifecycleOwner.lifecycleScope)

    ChannelsCache.channelUpdatedFlow.onEach { data ->
        viewModelScope.launch {
            val isCanceled = channelsListView.cancelLastSort()
            val diff = channelsListView.channelUpdated(data.channel)
            Log.i("diffChannel", data.channel.lastMessage?.body.toString() + "  " + data.channel.unreadMessageCount)
            if (diff != null) {
                if (diff.lastMessageChanged || data.needSorting || isCanceled)
                    channelsListView.sortChannelsBy(SceytKitConfig.sortChannelsBy)
            } else
                getChannels(0, query = searchQuery)
        }
    }.launchIn(lifecycleOwner.lifecycleScope)

    ChannelsCache.channelAddedFlow.onEach { sceytChannel ->
        Log.i("diffChannel", "add " + sceytChannel.lastMessage?.body.toString() + "  " + sceytChannel.unreadMessageCount)
        channelsListView.cancelLastSort()
        channelsListView.addNewChannelAndSort(ChannelListItem.ChannelItem(sceytChannel))
    }.launchIn(lifecycleOwner.lifecycleScope)

    ChannelsCache.channelDraftMessageChangesFlow.onEach { sceytChannel ->
        channelsListView.channelUpdated(sceytChannel)
    }.launchIn(lifecycleOwner.lifecycleScope)

    ChannelEventsObserver.onChannelTypingEventFlow
        .filter { it.member.id != SceytKitClient.myId }
        .onEach {
            typingCancelHelper.await(it) { data ->
                channelsListView.onTyping(data)
            }
            channelsListView.onTyping(it)
        }.launchIn(lifecycleOwner.lifecycleScope)

    blockUserLiveData.observe(lifecycleOwner) {
        when (it) {
            is SceytResponse.Success -> {
                channelsListView.userBlocked(it.data)
            }
            is SceytResponse.Error -> customToastSnackBar(channelsListView, it.message ?: "")
        }
    }

    pageStateLiveData.observe(lifecycleOwner) {
        channelsListView.updateStateView(it)
    }

    channelsListView.setChannelCommandEvenListener {
        onChannelCommandEvent(it)
    }

    channelsListView.setReachToEndListener { offset, lastChannel ->
        if (canLoadNext())
            getChannels(offset, searchQuery, LoadKeyData(value = lastChannel?.id ?: 0))
    }

    channelsListView.setChannelAttachDetachListener { item, attached ->
        if (item is ChannelListItem.ChannelItem && !item.channel.isGroup) {
            val peer = (item.channel as SceytDirectChannel).peer
            peer?.let {
                if (attached)
                    SceytPresenceChecker.addNewUserToPresenceCheck(it.id)
                else SceytPresenceChecker.removeFromPresenceCheck(it.id)
            }
        }
    }
}

fun ChannelsViewModel.bind(searchView: SearchInputView) {
    searchView.setDebouncedTextChangeListener {
        getChannels(0, query = it)
    }

    searchView.setOnQuerySubmitListener {
        getChannels(0, query = it)
    }
}

@Suppress("unused")
fun bindViewFromJava(viewModel: ChannelsViewModel, channelsListView: ChannelsListView, lifecycleOwner: LifecycleOwner) {
    viewModel.bind(channelsListView, lifecycleOwner)
}

@Suppress("unused")
fun bindSearchViewFromJava(viewModel: ChannelsViewModel, searchView: SearchInputView) {
    viewModel.bind(searchView)
}