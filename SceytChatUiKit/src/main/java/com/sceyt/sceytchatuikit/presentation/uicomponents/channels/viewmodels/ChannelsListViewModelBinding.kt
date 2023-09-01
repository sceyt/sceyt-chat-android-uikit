package com.sceyt.sceytchatuikit.presentation.uicomponents.channels.viewmodels

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.withResumed
import com.sceyt.sceytchatuikit.SceytKitClient
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventsObserver
import com.sceyt.sceytchatuikit.data.models.LoadKeyData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.extensions.customToastSnackBar
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.ChannelUpdateData
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.ChannelsCache
import com.sceyt.sceytchatuikit.presentation.common.getFirstMember
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.ChannelsListView
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelItemPayloadDiff
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.TypingCancelHelper
import com.sceyt.sceytchatuikit.presentation.uicomponents.searchinput.SearchInputView
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.services.SceytPresenceChecker
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

fun ChannelsViewModel.bind(channelsListView: ChannelsListView, lifecycleOwner: LifecycleOwner) {

    val typingCancelHelper by lazy { TypingCancelHelper() }
    val needToUpdateChannelsAfterResume = ConcurrentHashMap<Long, ChannelUpdateData>()
    val newAddedChannelJobs = ConcurrentHashMap<Long, Job>()

    getChannels(0, query = searchQuery)

    viewModelScope.launch {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            channelsListView.post {
                if (needToUpdateChannelsAfterResume.isNotEmpty()) {
                    val isCanceled = channelsListView.cancelLastSort()
                    var needSort = isCanceled
                    needToUpdateChannelsAfterResume.values.forEach { data ->
                        val diff = channelsListView.channelUpdated(data.channel)
                        if (diff != null && !needSort) {
                            if (diff.lastMessageChanged || data.needSorting)
                                needSort = true
                        }
                    }
                    needToUpdateChannelsAfterResume.clear()
                    if (needSort)
                        channelsListView.sortChannelsBy(SceytKitConfig.sortChannelsBy)
                }
            }
        }
    }

    suspend fun initPaginationDbResponse(response: PaginationResponse.DBResponse<SceytChannel>) {
        if (response.offset == 0) {
            channelsListView.setChannelsList(mapToChannelItem(data = response.data, hasNext = response.hasNext))
        } else
            channelsListView.addNewChannels(mapToChannelItem(data = response.data, hasNext = response.hasNext))
    }

    suspend fun initPaginationServerResponse(response: PaginationResponse.ServerResponse<SceytChannel>) {
        when (response.data) {
            is SceytResponse.Success -> {
                if (response.hasDiff) {
                    val newChannels = mapToChannelItem(data = response.cacheData, hasNext = response.hasNext)
                    channelsListView.setChannelsList(newChannels)
                } else {
                    if (!hasNextDb) channelsListView.hideLoadingMore()
                }
            }

            is SceytResponse.Error -> if (!hasNextDb) channelsListView.hideLoadingMore()
        }
    }

    suspend fun initChannelsResponse(response: PaginationResponse<SceytChannel>) {
        viewModelScope.launch {
            when (response) {
                is PaginationResponse.DBResponse -> initPaginationDbResponse(response)
                is PaginationResponse.ServerResponse -> initPaginationServerResponse(response)
                else -> return@launch
            }
        }
    }

    loadChannelsFlow.onEach(::initChannelsResponse).launchIn(lifecycleOwner.lifecycleScope)

    ChannelsCache.channelDeletedFlow.onEach { channelId ->
        viewModelScope.launch {
            newAddedChannelJobs[channelId]?.apply {
                cancel()
                newAddedChannelJobs.remove(channelId)
            }
            needToUpdateChannelsAfterResume.remove(channelId)
            lifecycleOwner.withResumed {
                channelsListView.deleteChannel(channelId)
            }
        }
    }.launchIn(lifecycleOwner.lifecycleScope)

    ChannelsCache.channelUpdatedFlow.onEach { data ->
        if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.RESUMED) {
            viewModelScope.launch {
                val isCanceled = channelsListView.cancelLastSort()
                val diff = channelsListView.channelUpdated(data.channel)
                if (diff != null) {
                    if (diff.lastMessageChanged || data.needSorting || isCanceled)
                        channelsListView.sortChannelsBy(SceytKitConfig.sortChannelsBy)
                } else
                    getChannels(0, query = searchQuery)
            }
        } else needToUpdateChannelsAfterResume[data.channel.id] = data
    }.launchIn(lifecycleOwner.lifecycleScope)

    ChannelsCache.channelReactionMsgLoadedFlow.onEach { data ->
        viewModelScope.launch {
            channelsListView.channelUpdatedWithDiff(data, ChannelItemPayloadDiff.DEFAULT_FALSE.copy(lastMessageChanged = true))
        }
    }.launchIn(lifecycleOwner.lifecycleScope)

    fun createJobToAddNewChannelWithOnResumed(sceytChannel: SceytChannel) {
        val job = viewModelScope.launch {
            lifecycleOwner.withResumed {
                val updatedChannel = needToUpdateChannelsAfterResume[sceytChannel.id]?.channel
                        ?: sceytChannel
                channelsListView.cancelLastSort()
                channelsListView.addNewChannelAndSort(ChannelListItem.ChannelItem(updatedChannel))
                newAddedChannelJobs.remove(sceytChannel.id)
            }
        }
        newAddedChannelJobs[sceytChannel.id] = job
    }

    ChannelsCache.channelAddedFlow.onEach(::createJobToAddNewChannelWithOnResumed)
        .launchIn(lifecycleOwner.lifecycleScope)

    ChannelsCache.pendingChannelCreatedFlow.onEach { data ->
        channelsListView.replaceChannel(data.first, data.second)
        if (lifecycleOwner.lifecycle.currentState != Lifecycle.State.RESUMED) {
            newAddedChannelJobs[data.first]?.let {
                it.cancel()
                newAddedChannelJobs.remove(data.first)
            }
            createJobToAddNewChannelWithOnResumed(data.second)
        }
    }.launchIn(lifecycleOwner.lifecycleScope)

    ChannelsCache.channelDraftMessageChangesFlow.onEach { channel ->
        if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.RESUMED) {
            channelsListView.channelUpdatedWithDiff(channel, ChannelItemPayloadDiff.DEFAULT_FALSE.copy(lastMessageChanged = true))
        } else {
            val pendingUpdate = needToUpdateChannelsAfterResume[channel.id]
            if (pendingUpdate != null) {
                pendingUpdate.channel = channel
            } else
                needToUpdateChannelsAfterResume[channel.id] = ChannelUpdateData(channel, false)
        }
    }.launchIn(lifecycleOwner.lifecycleScope)

    ChannelEventsObserver.onChannelTypingEventFlow
        .filter { it.member.id != SceytKitClient.myId }
        .onEach {
            typingCancelHelper.await(it) { data ->
                channelsListView.onTyping(data)
                needToUpdateChannelsAfterResume[it.channel.id]?.channel?.typingData = data
            }
            channelsListView.onTyping(it)
            needToUpdateChannelsAfterResume[it.channel.id]?.channel?.typingData = it
        }.launchIn(lifecycleOwner.lifecycleScope)

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

    channelsListView.setChannelCommandEvenListener {
        onChannelCommandEvent(it)
    }

    channelsListView.setReachToEndListener { offset, lastChannel ->
        if (canLoadNext())
            getChannels(offset, searchQuery, LoadKeyData(value = lastChannel?.id ?: 0))
    }

    channelsListView.setChannelAttachDetachListener { item, attached ->
        if (item is ChannelListItem.ChannelItem && !item.channel.isGroup) {
            val peer = item.channel.getFirstMember()
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

@Suppress("unused")
fun bindViewFromJava(viewModel: ChannelsViewModel, channelsListView: ChannelsListView, lifecycleOwner: LifecycleOwner) {
    viewModel.bind(channelsListView, lifecycleOwner)
}

@Suppress("unused")
fun bindSearchViewFromJava(viewModel: ChannelsViewModel, searchView: SearchInputView) {
    viewModel.bind(searchView)
}