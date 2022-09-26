package com.sceyt.sceytchatuikit.presentation.uicomponents.channels.viewmodels

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.Types
import com.sceyt.chat.models.channel.GroupChannel
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventEnum.*
import com.sceyt.sceytchatuikit.data.connectionobserver.ConnectionObserver
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.toSceytUiChannel
import com.sceyt.sceytchatuikit.extensions.awaitAnimationEnd
import com.sceyt.sceytchatuikit.extensions.customToastSnackBar
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.ChannelsListView
import com.sceyt.sceytchatuikit.presentation.uicomponents.searchinput.SearchInputView
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

fun ChannelsViewModel.bind(channelsListView: ChannelsListView, lifecycleOwner: LifecycleOwner) {
    var connectAnlLoadInitialChannelsJob: Job? = null

    if (ConnectionObserver.connectionState == Types.ConnectState.StateConnected) {
        getChannels(0, query = searchQuery)
    } else {
        /** Await to connect, and load channels **/
        connectAnlLoadInitialChannelsJob = lifecycleOwner.lifecycleScope.launch {
            ConnectionObserver.onChangedConnectStatusFlow.collect {
                if (it.first == Types.ConnectState.StateConnected)
                    channelsListView.getChannelsRv().awaitAnimationEnd {
                        getChannels(0, query = searchQuery)
                        connectAnlLoadInitialChannelsJob?.cancel()
                    }
            }
        }
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
                            channelsListView.updateChannelsWithServerData(data, it.offset, lifecycleOwner)
                        }
                    } else if (it.data is SceytResponse.Error)
                        customToastSnackBar(channelsListView, it.data.message ?: "")
                }
                is PaginationResponse.Nothing -> return@collect
            }
        }
    }

    lifecycleOwner.lifecycleScope.launch {
        onNewMessageFlow.collect {
            if (!channelsListView.updateLastMessage(it.second, false, it.first.unreadMessageCount)) {
                getChannels(0, query = searchQuery)
            }
        }
    }

    lifecycleOwner.lifecycleScope.launch {
        onOutGoingMessageFlow.collect {
            if (!channelsListView.updateLastMessage(it, edited = false)) {
                getChannels(0, query = searchQuery)
            }
        }
    }

    lifecycleOwner.lifecycleScope.launch {
        onMessageEditedOrDeletedFlow.collect {
            if (!channelsListView.updateLastMessage(it, edited = true)) {
                getChannels(0, query = searchQuery)
            }
        }
    }

    lifecycleOwner.lifecycleScope.launch {
        onMessageStatusFlow.collect {
            channelsListView.updateLastMessageStatus(it)
        }
    }

    lifecycleOwner.lifecycleScope.launch {
        onOutGoingMessageStatusFlow.collect {
            channelsListView.updateOutgoingLastMessageStatus(it.first, it.second)
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
                ClearedHistory -> channelsListView.channelCleared(it.channelId
                        ?: return@collect)
                Updated -> channelsListView.channelUpdated(it.channel?.toSceytUiChannel())
                Muted -> channelsListView.updateMuteState(true, it.channelId)
                UnMuted -> channelsListView.updateMuteState(false, it.channelId)
                else -> return@collect
            }
        }
    }

    markAsReadLiveData.observe(lifecycleOwner) {
        when (it) {
            is SceytResponse.Success -> {
                channelsListView.markedChannelAsRead(it.data?.id)
            }
            is SceytResponse.Error -> customToastSnackBar(channelsListView, it.message
                    ?: "")
        }
    }

    blockChannelLiveData.observe(lifecycleOwner) {
        when (it) {
            is SceytResponse.Success -> {
                channelsListView.deleteChannel(it.data)
            }
            is SceytResponse.Error -> customToastSnackBar(channelsListView, it.message
                    ?: "")
        }
    }

    clearHistoryLiveData.observe(lifecycleOwner) {
        when (it) {
            is SceytResponse.Success -> {
                channelsListView.channelCleared(it.data)
            }
            is SceytResponse.Error -> customToastSnackBar(channelsListView, it.message
                    ?: "")
        }
    }

    leaveChannelLiveData.observe(lifecycleOwner) {
        when (it) {
            is SceytResponse.Success -> {
                channelsListView.deleteChannel(it.data)
            }
            is SceytResponse.Error -> customToastSnackBar(channelsListView, it.message
                    ?: "")
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
        if (!loadingItems.get() && hasNext) {
            loadingItems.set(true)
            getChannels(offset, searchQuery)
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