package com.sceyt.chatuikit.presentation.uicomponents.channels.viewmodels

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.withResumed
import com.sceyt.chatuikit.SceytKitClient
import com.sceyt.chatuikit.data.channeleventobserver.ChannelEventsObserver
import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.extensions.isResumed
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.differs.ChannelDiff
import com.sceyt.chatuikit.persistence.logicimpl.channelslogic.ChannelUpdateData
import com.sceyt.chatuikit.persistence.logicimpl.channelslogic.ChannelsCache
import com.sceyt.chatuikit.presentation.common.getPeer
import com.sceyt.chatuikit.presentation.uicomponents.channels.ChannelsListView
import com.sceyt.chatuikit.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.chatuikit.presentation.uicomponents.conversationheader.TypingCancelHelper
import com.sceyt.chatuikit.presentation.uicomponents.searchinput.SearchChannelInputView
import com.sceyt.chatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.chatuikit.services.SceytPresenceChecker
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

    fun initPaginationDbResponse(response: PaginationResponse.DBResponse<SceytChannel>) {
        if (response.offset == 0) {
            channelsListView.setChannelsList(mapToChannelItem(data = response.data, hasNext = response.hasNext))
        } else
            channelsListView.addNewChannels(mapToChannelItem(data = response.data, hasNext = response.hasNext))
    }

    fun initPaginationServerResponse(response: PaginationResponse.ServerResponse<SceytChannel>) {
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

    fun initChannelsResponse(response: PaginationResponse<SceytChannel>) {
        when (response) {
            is PaginationResponse.DBResponse -> initPaginationDbResponse(response)
            is PaginationResponse.ServerResponse -> initPaginationServerResponse(response)
            else -> return
        }
    }

    loadChannelsFlow.onEach(::initChannelsResponse).launchIn(viewModelScope)

    ChannelsCache.channelDeletedFlow.onEach { channelId ->
        lifecycleOwner.lifecycleScope.launch {
            newAddedChannelJobs[channelId]?.apply {
                cancel()
                newAddedChannelJobs.remove(channelId)
            }
            needToUpdateChannelsAfterResume.remove(channelId)
            lifecycleOwner.withResumed {
                channelsListView.deleteChannel(channelId, searchQuery)
            }
        }
    }.launchIn(viewModelScope)

    ChannelsCache.channelUpdatedFlow.onEach { data ->
        if (!lifecycleOwner.isResumed())
            needToUpdateChannelsAfterResume[data.channel.id] = data

        lifecycleOwner.lifecycleScope.launch {
            val isCanceled = channelsListView.cancelLastSort()
            val diff = channelsListView.channelUpdated(data.channel)
            if (diff != null) {
                if (diff.lastMessageChanged || data.needSorting || isCanceled)
                    channelsListView.sortChannelsBy(SceytKitConfig.sortChannelsBy)
                SceytLog.i("ChannelsCache", "viewModel: id: ${data.channel.id}  body: ${data.channel.lastMessage?.body} draft:${data.channel.draftMessage?.message}  unreadCount ${data.channel.newMessageCount}" +
                        " isResumed ${lifecycleOwner.isResumed()} hasDifference: ${diff.hasDifference()} lastMessageChanged: ${diff.lastMessageChanged} needSorting: ${data.needSorting}")
            } else {
                SceytLog.i("ChannelsCache", "viewModel: id: ${data.channel.id}  body: ${data.channel.lastMessage?.body}  unreadCount ${data.channel.newMessageCount}" +
                        " isResumed ${lifecycleOwner.isResumed()} but started getChannels ")
                getChannels(0, query = searchQuery)
            }
        }
    }.launchIn(viewModelScope)

    ChannelsCache.channelReactionMsgLoadedFlow.onEach { data ->
        lifecycleOwner.lifecycleScope.launch {
            channelsListView.channelUpdatedWithDiff(data, ChannelDiff.DEFAULT_FALSE.copy(lastMessageChanged = true))
        }
    }.launchIn(viewModelScope)

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

    ChannelsCache.channelAddedFlow
        .onEach(::createJobToAddNewChannelWithOnResumed)
        .launchIn(viewModelScope)

    ChannelsCache.pendingChannelCreatedFlow.onEach { data ->
        channelsListView.replaceChannel(data.first, data.second)
        if (!lifecycleOwner.isResumed()) {
            newAddedChannelJobs[data.first]?.let {
                it.cancel()
                newAddedChannelJobs.remove(data.first)
            }
            createJobToAddNewChannelWithOnResumed(data.second)
        }
    }.launchIn(viewModelScope)

    ChannelsCache.channelDraftMessageChangesFlow.onEach { channel ->
        channelsListView.channelUpdatedWithDiff(channel, ChannelDiff.DEFAULT_FALSE.copy(lastMessageChanged = true))
        if (!lifecycleOwner.isResumed()) {
            val pendingUpdate = needToUpdateChannelsAfterResume[channel.id]
            if (pendingUpdate != null) {
                pendingUpdate.channel = channel
            } else
                needToUpdateChannelsAfterResume[channel.id] = ChannelUpdateData(channel, false)
        }
    }.launchIn(viewModelScope)

    ChannelEventsObserver.onChannelTypingEventFlow
        .filter { it.member.id != SceytKitClient.myId }
        .onEach {
            typingCancelHelper.await(it) { data ->
                channelsListView.onTyping(data)
                needToUpdateChannelsAfterResume[it.channel.id]?.channel?.typingData = data
            }
            channelsListView.onTyping(it)
            needToUpdateChannelsAfterResume[it.channel.id]?.channel?.typingData = it
        }.launchIn(viewModelScope)

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
            val peer = item.channel.getPeer()
            peer?.let {
                if (attached)
                    SceytPresenceChecker.addNewUserToPresenceCheck(it.id)
                else SceytPresenceChecker.removeFromPresenceCheck(it.id)
            }
        }
    }
}

fun ChannelsViewModel.bind(searchView: SearchChannelInputView) {
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
fun bindSearchViewFromJava(viewModel: ChannelsViewModel, searchView: SearchChannelInputView) {
    viewModel.bind(searchView)
}