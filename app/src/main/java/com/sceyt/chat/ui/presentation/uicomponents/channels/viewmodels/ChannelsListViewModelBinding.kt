package com.sceyt.chat.ui.presentation.uicomponents.channels.viewmodels

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.ui.data.channeleventobserverservice.ChannelEventEnum.*
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.toSceytUiMessage
import com.sceyt.chat.ui.extensions.customToastSnackBar
import com.sceyt.chat.ui.presentation.uicomponents.channels.ChannelsListView
import com.sceyt.chat.ui.presentation.uicomponents.searchinput.SearchInputView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

fun ChannelsViewModel.bindView(channelsListView: ChannelsListView, lifecycleOwner: LifecycleOwner) {

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

    onNewMessageLiveData.observe(lifecycleOwner) {
        if (!channelsListView.updateLastMessage(it.second.toSceytUiMessage(), it.first.unreadMessageCount)) {
            loadChannels(0, channelsListView.getChannelsSizeFromUpdate)
        }
    }

    onMessageEditedOrDeletedLiveData.observe(lifecycleOwner) {
        if (!channelsListView.updateLastMessage(it)) {
            loadChannels(0, channelsListView.getChannelsSizeFromUpdate)
        }
    }

    onMessageStatusLiveData.observe(lifecycleOwner) {
        channelsListView.updateLastMessageStatus(it)
    }

    onChannelEventLiveData.observe(lifecycleOwner) {
        when (it.eventType) {
            Created -> loadChannels(0, channelsListView.getChannelsSizeFromUpdate + 1)
            Deleted -> channelsListView.deleteChannel(it.channelId)
            ClearedHistory -> channelsListView.channelCleared(it.channelId ?: return@observe)
            Updated -> {/*TODO)*/
            }
            Muted -> {/*TODO)*/
            }
            UnMuted -> {/*TODO)*/
            }
        }
    }

    pageStateLiveData.observe(lifecycleOwner) {
        channelsListView.updateState(it)
    }

    channelsListView.setReachToEndListener { offset ->
        if (!isLoadingChannels && hasNext) {
            isLoadingChannels = true
            loadChannels(offset)
        }
    }
}

fun ChannelsViewModel.bindView(searchView: SearchInputView) {
    searchView.setDebouncedTextChangeListener {
        loadChannels(0, query = it)
    }

    searchView.setOnQuerySubmitListener {
        loadChannels(0, query = it)
    }
}


fun bindViewFromJava(viewModel: ChannelsViewModel, channelsListView: ChannelsListView, lifecycleOwner: LifecycleOwner) {
    viewModel.bindView(channelsListView, lifecycleOwner)
}

fun bindSearchViewFromJava(viewModel: ChannelsViewModel, searchView: SearchInputView) {
    viewModel.bindView(searchView)
}