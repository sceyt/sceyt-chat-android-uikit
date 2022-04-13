package com.sceyt.chat.ui.presentation.channels.viewmodels

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.ui.data.SceytResponse
import com.sceyt.chat.ui.presentation.channels.components.ChannelListView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

fun ChannelsViewModel.bindView(channelListView: ChannelListView, lifecycleOwner: LifecycleOwner) {

    loadChannels(0, false)

    lifecycleOwner.lifecycleScope.launch {
        channelsFlow.collect {
            if (it is SceytResponse.Success && it.data != null)
                channelListView.setChannelsList(it.data)

            isLoadingMore = it is SceytResponse.Loading && it.isLoading
        }
    }

    lifecycleOwner.lifecycleScope.launch {
        loadMoreChannelsFlow.collect {
            if (it is SceytResponse.Success && it.data != null)
                channelListView.addNewChannels(it.data)

            isLoadingMore = it is SceytResponse.Loading && it.isLoading
        }
    }

    channelListView.setReachToEndListener { offset ->
        if (!isLoadingMore && hasNext) {
            isLoadingMore = true
            loadChannels(offset, true)
        }
    }
}