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
                is SceytResponse.Loading -> {


                }
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
        if (!isLoadingMore && hasNext) {
            isLoadingMore = true
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
    viewModel.bindView(channelsListView, lifecycleOwner)
}

fun bindSearchViewFromJava(viewModel: ChannelsViewModel, searchView: SearchInputView) {
    viewModel.bindSearchView(searchView)
}