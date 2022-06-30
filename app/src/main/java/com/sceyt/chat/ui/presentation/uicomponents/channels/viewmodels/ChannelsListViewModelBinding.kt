package com.sceyt.chat.ui.presentation.uicomponents.channels.viewmodels

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sceyt.chat.Types
import com.sceyt.chat.ui.SceytUiKitApp
import com.sceyt.chat.ui.data.channeleventobserverservice.ChannelEventEnum.*
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.toSceytUiChannel
import com.sceyt.chat.ui.data.toSceytUiMessage
import com.sceyt.chat.ui.extensions.asAppCompatActivity
import com.sceyt.chat.ui.extensions.awaitAnimationEnd
import com.sceyt.chat.ui.extensions.customToastSnackBar
import com.sceyt.chat.ui.presentation.uicomponents.channels.ChannelsListView
import com.sceyt.chat.ui.presentation.uicomponents.searchinput.SearchInputView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

fun ChannelsViewModel.bindView(channelsListView: ChannelsListView, lifecycleOwner: LifecycleOwner) {
    val connectionStatusLiveData = (channelsListView.context.asAppCompatActivity().application as? SceytUiKitApp)?.sceytConnectionStatus

    lifecycleOwner.lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (connectionStatusLiveData?.value == Types.ConnectState.StateConnected)
                channelsListView.getChannelsRv().awaitAnimationEnd {
                    getChannels(query = searchQuery)
                }
            else
                (channelsListView.context.asAppCompatActivity().application as? SceytUiKitApp)?.sceytConnectionStatus?.observe(lifecycleOwner) {
                    if (it == Types.ConnectState.StateConnected)
                        getChannels(query = searchQuery)
                }
        }
    }

    lifecycleOwner.lifecycleScope.launch {
        channelsFlow.collect {
            if (it is SceytResponse.Success) {
                it.data?.let { data -> channelsListView.setChannelsList(data) }
            } else if (it is SceytResponse.Error) {
                customToastSnackBar(channelsListView, it.message ?: "")
            }
        }
    }

    lifecycleOwner.lifecycleScope.launch {
        loadMoreChannelsFlow.collect {
            if (it is SceytResponse.Success && it.data != null)
                channelsListView.addNewChannels(it.data)
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

    onNewMessageLiveData.observe(lifecycleOwner) {
        if (!channelsListView.updateLastMessage(it.second.toSceytUiMessage(), it.first.unreadMessageCount)) {
            getChannels(query = searchQuery)
        }
    }

    onMessageEditedOrDeletedLiveData.observe(lifecycleOwner) {
        if (!channelsListView.updateLastMessage(it)) {
            getChannels(query = searchQuery)
        }
    }

    onMessageStatusLiveData.observe(lifecycleOwner) {
        channelsListView.updateLastMessageStatus(it)
    }

    onChannelEventLiveData.observe(lifecycleOwner) {
        when (it.eventType) {
            Created -> getChannels(query = searchQuery)
            Deleted, Left -> channelsListView.deleteChannel(it.channelId)
            ClearedHistory -> channelsListView.channelCleared(it.channelId ?: return@observe)
            Updated -> channelsListView.channelUpdated(it.channel?.toSceytUiChannel())
            Muted -> channelsListView.updateMuteState(true, it.channelId)
            UnMuted -> channelsListView.updateMuteState(false, it.channelId)
            else -> return@observe
        }
    }

    pageStateLiveData.observe(lifecycleOwner) {
        channelsListView.updateStateView(it)
    }

    channelsListView.setChannelEvenListener {
        onChannelEvent(it)
    }

    channelsListView.setReachToEndListener {
        if (!loadingItems && hasNext) {
            loadingItems = true
            loadMoreChannels()
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