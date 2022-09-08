package com.sceyt.sceytchatuikit.presentation.uicomponents.channels.viewmodels

import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.ClientWrapper
import com.sceyt.chat.models.channel.GroupChannel
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.ChannelsListView
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.toSceytUiChannel
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.searchinput.SearchInputView
import kotlinx.coroutines.launch

fun ChannelsViewModel.bind(channelsListView: ChannelsListView, lifecycleOwner: LifecycleOwner) {

    getChannels(0, query = searchQuery)

    lifecycleOwner.lifecycleScope.launch {
        /*(channelsListView.context.asAppCompatActivity().application as? SceytUiKitApp)?.sceytConnectionStatus?.observe(lifecycleOwner) {
            if (it == Types.ConnectState.StateConnected)
                channelsListView.getChannelsRv().awaitAnimationEnd {
                    //getChannels(0, query = searchQuery)
                }
        }*/
    }

    lifecycleOwner.lifecycleScope.launch {
        loadChannelsFlow.collect {
            when (it) {
                is PaginationResponse.DBResponse -> {
                    if (it.offset == 0) {
                        channelsListView.setChannelsList(it.data)
                        Log.i("responceee", "setChannelsList DB ->  data= ${
                            it.data.map { channelListItem ->
                                if (channelListItem is ChannelListItem.ChannelItem)
                                    channelListItem.channel.channelSubject
                                else "Loading item"
                            }
                        }")
                    } else {
                        channelsListView.addNewChannels(it.data)
                        Log.i("responceee", "addNewChannels DB->  data= ${
                            it.data.map { channelListItem ->
                                if (channelListItem is ChannelListItem.ChannelItem)
                                    channelListItem.channel.channelSubject
                                else "Loading item"
                            }
                        }")
                    }
                }
                is PaginationResponse.ServerResponse -> {
                    if (it.data is SceytResponse.Success) {
                        it.data.data?.let { data ->
                            channelsListView.updateChannelsWithServerData(data, it.offset, lifecycleOwner)
                            Log.i("responceee", "addOrUpdateChannel ->  data= ${
                                it.data.data.map { channelListItem ->
                                    if (channelListItem is ChannelListItem.ChannelItem)
                                        channelListItem.channel.channelSubject
                                    else "Loading item"
                                }
                            }")
                        }
                    } else if (it.data is SceytResponse.Error)
                        com.sceyt.sceytchatuikit.extensions.customToastSnackBar(channelsListView, it.data.message
                                ?: "")
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
            channelsListView.updateOutgoingLastMessageStatus(it)
        }
    }

    lifecycleOwner.lifecycleScope.launch {
        onChannelEventFlow.collect {
            when (it.eventType) {
                com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventEnum.Created -> getChannels(0, query = searchQuery)
                com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventEnum.Deleted -> channelsListView.deleteChannel(it.channelId)
                com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventEnum.Left -> {
                    val leftUser = (it.channel as? GroupChannel)?.members?.getOrNull(0)?.id
                    if (leftUser == ClientWrapper.currentUser.id)
                        channelsListView.deleteChannel(it.channelId)
                }
                com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventEnum.ClearedHistory -> channelsListView.channelCleared(it.channelId
                        ?: return@collect)
                com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventEnum.Updated -> channelsListView.channelUpdated(it.channel?.toSceytUiChannel())
                com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventEnum.Muted -> channelsListView.updateMuteState(true, it.channelId)
                com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventEnum.UnMuted -> channelsListView.updateMuteState(false, it.channelId)
                else -> return@collect
            }
        }
    }

    markAsReadLiveData.observe(lifecycleOwner) {
        when (it) {
            is SceytResponse.Success -> {
                channelsListView.markedUsRead(it.data)
            }
            is SceytResponse.Error -> com.sceyt.sceytchatuikit.extensions.customToastSnackBar(channelsListView, it.message
                    ?: "")
        }
    }

    blockChannelLiveData.observe(lifecycleOwner) {
        when (it) {
            is SceytResponse.Success -> {
                channelsListView.deleteChannel(it.data)
            }
            is SceytResponse.Error -> com.sceyt.sceytchatuikit.extensions.customToastSnackBar(channelsListView, it.message
                    ?: "")
        }
    }

    clearHistoryLiveData.observe(lifecycleOwner) {
        when (it) {
            is SceytResponse.Success -> {
                channelsListView.channelCleared(it.data)
            }
            is SceytResponse.Error -> com.sceyt.sceytchatuikit.extensions.customToastSnackBar(channelsListView, it.message
                    ?: "")
        }
    }

    leaveChannelLiveData.observe(lifecycleOwner) {
        when (it) {
            is SceytResponse.Success -> {
                channelsListView.deleteChannel(it.data)
            }
            is SceytResponse.Error -> com.sceyt.sceytchatuikit.extensions.customToastSnackBar(channelsListView, it.message
                    ?: "")
        }
    }

    blockUserLiveData.observe(lifecycleOwner) {
        when (it) {
            is SceytResponse.Success -> {
                channelsListView.userBlocked(it.data)
            }
            is SceytResponse.Error -> com.sceyt.sceytchatuikit.extensions.customToastSnackBar(channelsListView, it.message
                    ?: "")
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