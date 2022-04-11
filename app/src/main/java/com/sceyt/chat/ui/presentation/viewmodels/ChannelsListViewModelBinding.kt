package com.sceyt.chat.ui.presentation.viewmodels

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.ui.data.SceytResponse
import com.sceyt.chat.ui.presentation.ChannelListView
import kotlinx.coroutines.flow.collectLatest

fun ChannelsViewModel.bindView(channelListView: ChannelListView, lifecycleOwner: LifecycleOwner) {
    getChannels()

    lifecycleOwner.lifecycleScope.launchWhenResumed {
        uiState.collectLatest {
            if (it is SceytResponse.Success && it.data != null)
                channelListView.setChannelsList(it.data)
        }
    }
}