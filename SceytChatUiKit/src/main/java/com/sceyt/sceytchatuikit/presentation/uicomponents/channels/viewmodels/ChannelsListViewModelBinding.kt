package com.sceyt.sceytchatuikit.presentation.uicomponents.channels.viewmodels

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.models.channel.GroupChannel
import com.sceyt.sceytchatuikit.SceytKitClient
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventEnum.*
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.SceytDirectChannel
import com.sceyt.sceytchatuikit.data.toSceytUiChannel
import com.sceyt.sceytchatuikit.extensions.customToastSnackBar
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.ChannelsCash
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.ChannelsListView
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.searchinput.SearchInputView
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.services.SceytPresenceChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

fun ChannelsViewModel.bind(channelsListView: ChannelsListView, lifecycleOwner: LifecycleOwner) {

    getChannels(0, query = searchQuery)

    /** Await to connect, and load channels **/
    /* lifecycleOwner.lifecycleScope.launch {
         ConnectionObserver.onChangedConnectStatusFlow.collect {
             if (it.first == Types.ConnectState.StateConnected)
                 channelsListView.getChannelsRv().awaitAnimationEnd {
                     getChannels(0, query = searchQuery)
                 }
         }
     }*/
    SceytKitClient.getConnectionService().getOnAvailableLiveData().observe(lifecycleOwner) {
        getChannels(0, query = searchQuery)
    }

    lifecycleOwner.lifecycleScope.launch {
        loadChannelsFlow.collect {
            when (it) {
                is PaginationResponse.DBResponse -> {
                    if (it.offset == 0) {
                        channelsListView.setChannelsList(it.data)
                    } else
                        channelsListView.addNewChannels(it.data)
                }
                is PaginationResponse.ServerResponse -> {
                    if (it.data is SceytResponse.Success) {
                        it.data.data?.let { data ->
                            channelsListView.updateChannelsWithServerData(data, it.offset, it.hasNext, lifecycleOwner)
                        }
                    } else if (it.data is SceytResponse.Error)
                        customToastSnackBar(channelsListView, it.data.message ?: "")
                }
                is PaginationResponse.Nothing -> return@collect
                else -> {}
            }
        }
    }

    lifecycleOwner.lifecycleScope.launch {
        onOutGoingMessageStatusFlow.collect {
            channelsListView.updateOutgoingLastMessageStatus(it.first, it.second)
        }
    }

    lifecycleOwner.lifecycleScope.launch {
        onMessageEditedOrDeletedFlow.collect {
            if (!channelsListView.updateLastMessage(it, checkId = true)) {
                getChannels(0, query = searchQuery)
            }
        }
    }

    lifecycleOwner.lifecycleScope.launch {
        onChannelEventFlow.collect {
            when (it.eventType) {
                Created -> getChannels(0, query = searchQuery)
                Deleted -> channelsListView.deleteChannel(it.channelId)
                Left -> {
                    val leftUser = (it.channel as? GroupChannel)?.members?.getOrNull(0)?.id
                    if (leftUser == preference.getUserId())
                        channelsListView.deleteChannel(it.channelId)
                }
                ClearedHistory -> channelsListView.channelCleared(it.channelId ?: return@collect)
                Updated -> channelsListView.channelUpdated(it.channel?.toSceytUiChannel())
                Muted -> channelsListView.updateMuteState(true, it.channelId)
                UnMuted -> channelsListView.updateMuteState(false, it.channelId)
                else -> return@collect
            }
        }
    }

    lifecycleOwner.lifecycleScope.launch {
        ChannelsCash.channelUpdatedFlow.collect { sceytChannel ->
            if (channelsListView.channelUpdated(sceytChannel)) {
                channelsListView.sortChannelsBy(SceytKitConfig.sortChannelsBy)
            } else
                getChannels(0, query = searchQuery)
        }
    }

    lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
        SceytPresenceChecker.onPresenceCheckUsersFlow.distinctUntilChanged().collect {
            channelsListView.updateUsersPresenceIfNeeded(it.map { presenceUser -> presenceUser.user })
        }
    }

    blockChannelLiveData.observe(lifecycleOwner) {
        when (it) {
            is SceytResponse.Success -> {
                channelsListView.deleteChannel(it.data)
            }
            is SceytResponse.Error -> customToastSnackBar(channelsListView, it.message ?: "")
        }
    }

    leaveChannelLiveData.observe(lifecycleOwner) {
        when (it) {
            is SceytResponse.Success -> {
                channelsListView.deleteChannel(it.data)
            }
            is SceytResponse.Error -> customToastSnackBar(channelsListView, it.message ?: "")
        }
    }

    deleteChannelLiveData.observe(lifecycleOwner) {
        when (it) {
            is SceytResponse.Success -> {
                channelsListView.deleteChannel(it.data)
            }
            is SceytResponse.Error -> customToastSnackBar(channelsListView, it.message ?: "")
        }
    }

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

    channelsListView.setReachToEndListener { offset, _ ->
        if (canLoadPrev())
            getChannels(offset, searchQuery)
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


fun bindViewFromJava(viewModel: ChannelsViewModel, channelsListView: ChannelsListView, lifecycleOwner: LifecycleOwner) {
    viewModel.bind(channelsListView, lifecycleOwner)
}

fun bindSearchViewFromJava(viewModel: ChannelsViewModel, searchView: SearchInputView) {
    viewModel.bind(searchView)
}