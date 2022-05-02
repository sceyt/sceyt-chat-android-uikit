package com.sceyt.chat.ui.presentation.channels.viewmodels

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.ui.data.SceytResponse
import com.sceyt.chat.ui.extencions.customToastSnackBar
import com.sceyt.chat.ui.presentation.channels.components.SearchInputView
import com.sceyt.chat.ui.presentation.channels.components.channels.ChannelsListView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

fun ChannelsViewModel.bindView(channelsListView: ChannelsListView, lifecycleOwner: LifecycleOwner) {

    lifecycleOwner.lifecycleScope.launch {
        channelsFlow.collect {
            when (it) {
                is SceytResponse.Success -> {
                    it.data?.let { data -> channelsListView.setChannelsList(data) }
                }
                is SceytResponse.Error -> {
                    customToastSnackBar(channelsListView, it.message ?: "")
                }
                is SceytResponse.Loading -> channelsListView.showHideLoading(it.isLoading)
            }
        }
    }

    lifecycleOwner.lifecycleScope.launch {
        loadMoreChannelsLiveData.collect {
            if (it is SceytResponse.Success && it.data != null)
                channelsListView.addNewChannels(it.data)
        }
    }

    channelsListView.setReachToEndListener { offset ->
        if (!isLoadingMore && hasNext) {
            isLoadingMore = true
            loadChannels(offset, loadingMore = true)
        }
    }
}

fun ChannelsViewModel.bindSearchView(searchView: SearchInputView) {
    searchView.setDebouncedTextChangeListener {
        loadChannels(0, query = it.toString(), false)
    }
}