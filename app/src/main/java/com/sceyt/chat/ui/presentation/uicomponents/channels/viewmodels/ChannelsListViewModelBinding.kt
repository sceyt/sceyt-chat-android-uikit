package com.sceyt.chat.ui.presentation.uicomponents.channels.viewmodels

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.extensions.customToastSnackBar
import com.sceyt.chat.ui.presentation.uicomponents.searchinput.SearchInputView
import com.sceyt.chat.ui.presentation.uicomponents.channels.ChannelsListView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

fun ChannelsViewModel.bindChannelsView(channelsListView: ChannelsListView, lifecycleOwner: LifecycleOwner) {

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

fun ChannelsViewModel.bindSearchView(searchView: SearchInputView) {
    searchView.setDebouncedTextChangeListener {
        loadChannels(0, query = it)
    }

    searchView.setOnQuerySubmitListener {
        loadChannels(0, query = it)
    }
}


fun bindViewFromJava(viewModel: ChannelsViewModel, channelsListView: ChannelsListView, lifecycleOwner: LifecycleOwner) {
    viewModel.bindChannelsView(channelsListView, lifecycleOwner)
}

fun bindSearchViewFromJava(viewModel: ChannelsViewModel, searchView: SearchInputView) {
    viewModel.bindSearchView(searchView)
}