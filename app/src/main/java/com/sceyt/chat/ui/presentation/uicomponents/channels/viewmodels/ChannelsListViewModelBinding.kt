package com.sceyt.chat.ui.presentation.uicomponents.channels.viewmodels

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.Types
import com.sceyt.chat.ui.SceytUiKitApp
import com.sceyt.chat.ui.data.channeleventobserver.ChannelEventEnum.*
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.toSceytUiChannel
import com.sceyt.chat.ui.data.toSceytUiMessage
import com.sceyt.chat.ui.extensions.asAppCompatActivity
import com.sceyt.chat.ui.extensions.awaitAnimationEnd
import com.sceyt.chat.ui.extensions.customToastSnackBar
import com.sceyt.chat.ui.presentation.uicomponents.channels.ChannelsListView
import com.sceyt.chat.ui.presentation.uicomponents.searchinput.SearchInputView
import kotlinx.coroutines.launch

fun ChannelsViewModel.bindView(channelsListView: ChannelsListView, lifecycleOwner: LifecycleOwner) {

    getChannels(query = searchQuery)

    lifecycleOwner.lifecycleScope.launch {
        (channelsListView.context.asAppCompatActivity().application as? SceytUiKitApp)?.sceytConnectionStatus?.observe(lifecycleOwner) {
            if (it == Types.ConnectState.StateConnected)
                channelsListView.getChannelsRv().awaitAnimationEnd {
                    getChannels(query = searchQuery)
                }
        }
    }

    lifecycleOwner.lifecycleScope.launch {
        channelsFlow.collect {
            if (it is SceytResponse.Success) {
                it.data?.let { data -> channelsListView.setChannelsList(data, true) }
            } else if (it is SceytResponse.Error) {
                customToastSnackBar(channelsListView, it.message ?: "")
            }
        }
    }

    lifecycleOwner.lifecycleScope.launch {
        loadMoreChannelsFlow.collect {
            if (it is SceytResponse.Success && it.data != null) {
                channelsListView.addNewChannels(it.data)
            }
        }
    }

    lifecycleOwner.lifecycleScope.launch {
        onNewMessageFlow.collect {
            if (!channelsListView.updateLastMessage(it.second.toSceytUiMessage(), it.first.unreadMessageCount)) {
                getChannels(query = searchQuery)
            }
        }
    }

    lifecycleOwner.lifecycleScope.launch {
        onOutGoingMessageFlow.collect {
            if (!channelsListView.updateLastMessage(it)) {
                getChannels(query = searchQuery)
            }
        }
    }

    lifecycleOwner.lifecycleScope.launch {
        onMessageEditedOrDeletedFlow.collect {
            if (!channelsListView.updateLastMessage(it)) {
                getChannels(query = searchQuery)
            }
        }
    }

    lifecycleOwner.lifecycleScope.launch {
        onMessageStatusFlow.collect {
            channelsListView.updateLastMessageStatus(it)
        }

    }

    lifecycleOwner.lifecycleScope.launch {
        onChannelEventFlow.collect {
            when (it.eventType) {
                Created -> getChannels(query = searchQuery)
                Deleted, Left -> channelsListView.deleteChannel(it.channelId)
                ClearedHistory -> channelsListView.channelCleared(it.channelId ?: return@collect)
                Updated -> channelsListView.channelUpdated(it.channel?.toSceytUiChannel())
                Muted -> channelsListView.updateMuteState(true, it.channelId)
                UnMuted -> channelsListView.updateMuteState(false, it.channelId)
                MarkedUsUnread -> channelsListView.updateMuteState(false, it.channelId)
                else -> return@collect
            }
        }
    }

    markAsReadLiveData.observe(lifecycleOwner) {
        when (it) {
            is SceytResponse.Success -> {
                channelsListView.markedUsRead(it.data)
            }
            is SceytResponse.Error -> customToastSnackBar(channelsListView, it.message ?: "")
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

    clearHistoryLiveData.observe(lifecycleOwner) {
        when (it) {
            is SceytResponse.Success -> {
                channelsListView.channelCleared(it.data)
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
        if (!loadingItems && hasNext) {
            loadingItems = true
            loadMoreChannels(offset)
        }
    }
}

fun ChannelsViewModel.bindView(searchView: SearchInputView) {
    searchView.setDebouncedTextChangeListener {
        getChannels(query = it)
    }

    searchView.setOnQuerySubmitListener {
        getChannels(query = it)
    }
}


fun bindViewFromJava(viewModel: ChannelsViewModel, channelsListView: ChannelsListView, lifecycleOwner: LifecycleOwner) {
    viewModel.bindView(channelsListView, lifecycleOwner)
}

fun bindSearchViewFromJava(viewModel: ChannelsViewModel, searchView: SearchInputView) {
    viewModel.bindView(searchView)
}