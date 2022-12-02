package com.sceyt.sceytchatuikit.presentation.uicomponents.channels.viewmodels

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytDirectChannel
import com.sceyt.sceytchatuikit.extensions.customToastSnackBar
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.ChannelsCash
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.ChannelsListView
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.searchinput.SearchInputView
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.services.SceytPresenceChecker
import kotlinx.coroutines.flow.*

fun ChannelsViewModel.bind(channelsListView: ChannelsListView, lifecycleOwner: LifecycleOwner) {

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
                    val newChannels = mapToChannelItem(data = response.cashData, hasNext = response.hasNext)
                    channelsListView.setChannelsList(newChannels)
                } else {
                    if (!hasNextDb) channelsListView.hideLoadingMore()
                }
            }
            is SceytResponse.Error -> if (!hasNextDb) channelsListView.hideLoadingMore()
        }
    }

    suspend fun initChannelsResponse(response: PaginationResponse<SceytChannel>) {
        when (response) {
            is PaginationResponse.DBResponse -> initPaginationDbResponse(response)
            is PaginationResponse.ServerResponse -> initPaginationServerResponse(response)
            else -> return
        }
    }

    loadChannelsFlow.onEach(::initChannelsResponse).launchIn(lifecycleOwner.lifecycleScope)

    ChannelsCash.channelDeletedFlow.onEach {
        channelsListView.deleteChannel(it)
    }.launchIn(lifecycleOwner.lifecycleScope)

    lifecycleOwner.lifecycleScope.launchWhenResumed {
        ChannelsCash.channelUpdatedFlow.collect { data ->
            val diff = channelsListView.channelUpdated(data.channel)
            if (diff != null) {
                if (diff.lastMessageChanged || data.needSorting)
                    channelsListView.sortChannelsBy(SceytKitConfig.sortChannelsBy)
            } else
                getChannels(0, query = searchQuery)
        }
    }

    ChannelsCash.channelAddedFlow.onEach { sceytChannel ->
        channelsListView.addNewChannelAndSort(ChannelListItem.ChannelItem(sceytChannel))
    }.launchIn(lifecycleOwner.lifecycleScope)

    SceytPresenceChecker.onPresenceCheckUsersFlow.distinctUntilChanged().onEach {
        channelsListView.updateUsersPresenceIfNeeded(it.map { presenceUser -> presenceUser.user })
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

    channelsListView.setChannelEvenListener {
        onChannelEvent(it)
    }

    channelsListView.setReachToEndListener { offset, lastChannel ->
        if (canLoadNext())
            getChannels(offset, searchQuery, lastChannel?.id ?: 0)
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