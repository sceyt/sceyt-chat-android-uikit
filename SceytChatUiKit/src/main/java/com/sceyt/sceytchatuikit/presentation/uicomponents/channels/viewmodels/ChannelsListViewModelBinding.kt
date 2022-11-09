package com.sceyt.sceytchatuikit.presentation.uicomponents.channels.viewmodels

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.SceytDirectChannel
import com.sceyt.sceytchatuikit.extensions.customToastSnackBar
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.ChannelsCash
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.ChannelsListView
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.searchinput.SearchInputView
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.services.SceytPresenceChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

fun ChannelsViewModel.bind(channelsListView: ChannelsListView, lifecycleOwner: LifecycleOwner) {

    getChannels(0, query = searchQuery)

    /** Load channels after reconnection **/
    /*  lifecycleOwner.lifecycleScope.launch {
          ConnectionEventsObserver.onChangedConnectStatusFlow.collect {
              if (it.first == Types.ConnectState.StateConnected)
                  channelsListView.getChannelsRv().awaitAnimationEnd {
                      getChannels(0, query = searchQuery)
                  }
          }
      }*/

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
                            channelsListView.updateChannelsWithServerData(data, it.offset, it.hasNext, lifecycleOwner)
                        }
                    } else if (it.data is SceytResponse.Error)
                        customToastSnackBar(channelsListView, it.data.message ?: "")
                }
                is PaginationResponse.Nothing -> return@collect
                else -> {}
            }
        }
    }

    lifecycleOwner.lifecycleScope.launch {
        onMessageEditedOrDeletedFlow.collect {
            if (!channelsListView.updateLastMessage(it, checkId = true)) {
                getChannels(0, query = searchQuery)
            }
        }
    }

    lifecycleOwner.lifecycleScope.launch {
        ChannelsCash.channelDeletedFlow.collect {
            channelsListView.deleteChannel(it)
        }
    }

    lifecycleOwner.lifecycleScope.launch {
        ChannelsCash.channelUpdatedFlow.collect { sceytChannel ->
            if (channelsListView.channelUpdated(sceytChannel)) {
                channelsListView.sortChannelsBy(SceytKitConfig.sortChannelsBy)
            } else
                getChannels(0, query = searchQuery)
        }
    }

    lifecycleOwner.lifecycleScope.launch {
        ChannelsCash.channelAddedFlow.collect { sceytChannel ->
            channelsListView.addNewChannelAndSort(ChannelListItem.ChannelItem(sceytChannel))
        }
    }

    lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
        SceytPresenceChecker.onPresenceCheckUsersFlow.distinctUntilChanged().collect {
            channelsListView.updateUsersPresenceIfNeeded(it.map { presenceUser -> presenceUser.user })
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
        if (canLoadNext())
            getChannels(offset, searchQuery)
    }

    channelsListView.setChannelAttachDetachListener { item, attached ->
        if (item is ChannelListItem.ChannelItem && !item.channel.isGroup) {
            val peer = (item.channel as SceytDirectChannel).peer
            peer?.let {
                if (attached)
                    SceytPresenceChecker.addNewUserToPresenceCheck(it.id)
                else SceytPresenceChecker.removeFromPresenceCheck(it.id)
            }
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