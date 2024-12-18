package com.sceyt.chatuikit.presentation.components.channel_list.channels.viewmodel

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.withResumed
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.managers.channel.ChannelEventManager
import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.extensions.isResumed
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.differs.ChannelDiff
import com.sceyt.chatuikit.persistence.extensions.getPeer
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelUpdateData
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelsCache
import com.sceyt.chatuikit.presentation.components.channel.header.helpers.TypingCancelHelper
import com.sceyt.chatuikit.presentation.components.channel_list.channels.ChannelListView
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItem
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItem.ChannelItem
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelsComparatorDescBy
import com.sceyt.chatuikit.presentation.components.channel_list.search.SearchChannelInputView
import com.sceyt.chatuikit.services.SceytPresenceChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

fun ChannelsViewModel.bind(channelListView: ChannelListView, lifecycleOwner: LifecycleOwner) {

    val typingCancelHelper by lazy { TypingCancelHelper() }
    val needToUpdateChannelsAfterResume = ConcurrentHashMap<Long, ChannelUpdateData>()
    val newAddedChannelJobs = ConcurrentHashMap<Long, Job>()

    getChannels(0, query = searchQuery)

    viewModelScope.launch {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            channelListView.post {
                if (needToUpdateChannelsAfterResume.isNotEmpty()) {
                    val isCanceled = channelListView.cancelLastSort()
                    var needSort = isCanceled
                    needToUpdateChannelsAfterResume.values.forEach { data ->
                        val diff = channelListView.channelUpdated(data.channel)
                        if (diff != null && !needSort) {
                            if (diff.lastMessageChanged || data.needSorting)
                                needSort = true
                        }
                    }
                    needToUpdateChannelsAfterResume.clear()
                    if (needSort)
                        channelListView.sortChannelsBy(config.order)
                }
            }
        }
    }

    fun initPaginationDbResponse(response: PaginationResponse.DBResponse<SceytChannel>) {
        if (response.offset == 0) {
            channelListView.setChannelsList(mapToChannelItem(data = response.data, hasNext = response.hasNext))
        } else
            channelListView.addNewChannels(mapToChannelItem(data = response.data, hasNext = response.hasNext))
    }

    fun initPaginationServerResponse(response: PaginationResponse.ServerResponse<SceytChannel>) {
        when (response.data) {
            is SceytResponse.Success -> {
                if (response.hasDiff) {
                    val newChannels = mapToChannelItem(data = response.cacheData, hasNext = response.hasNext)
                    channelListView.setChannelsList(newChannels)
                } else {
                    if (!hasNextDb) channelListView.hideLoadingMore()
                }
            }

            is SceytResponse.Error -> if (!hasNextDb) channelListView.hideLoadingMore()
        }
    }

    fun initChannelsResponse(response: PaginationResponse<SceytChannel>) {
        when (response) {
            is PaginationResponse.DBResponse -> initPaginationDbResponse(response)
            is PaginationResponse.ServerResponse -> initPaginationServerResponse(response)
            else -> return
        }
    }

    loadChannelsFlow.onEach(::initChannelsResponse).launchIn(lifecycleOwner.lifecycleScope)

    ChannelsCache.channelsDeletedFlow.onEach { channelIds ->
        lifecycleOwner.lifecycleScope.launch {
            channelIds.forEach { channelId ->
                newAddedChannelJobs[channelId]?.apply {
                    cancel()
                    newAddedChannelJobs.remove(channelId)
                }
                needToUpdateChannelsAfterResume.remove(channelId)
                lifecycleOwner.withResumed {
                    channelListView.deleteChannel(channelId, searchQuery)
                }
            }
        }
    }.launchIn(viewModelScope)

    ChannelsCache.channelUpdatedFlow.onEach { data ->
        if (!lifecycleOwner.isResumed())
            needToUpdateChannelsAfterResume[data.channel.id] = data
        else {
            lifecycleOwner.lifecycleScope.launch {
                val isCanceled = channelListView.cancelLastSort()
                val diff = channelListView.channelUpdated(data.channel)
                if (diff != null) {
                    if (diff.lastMessageChanged || data.needSorting || isCanceled)
                        channelListView.sortChannelsBy(SceytChatUIKit.config.channelListOrder)
                    SceytLog.i("ChannelsCache", "viewModel: id: ${data.channel.id}  body: ${data.channel.lastMessage?.body} draft:${data.channel.draftMessage?.body}  unreadCount ${data.channel.newMessageCount}" +
                            " isResumed ${lifecycleOwner.isResumed()} hasDifference: ${diff.hasDifference()} lastMessageChanged: ${diff.lastMessageChanged} needSorting: ${data.needSorting}")
                } else {
                    SceytLog.i("ChannelsCache", "viewModel: id: ${data.channel.id}  body: ${data.channel.lastMessage?.body}  unreadCount ${data.channel.newMessageCount}" +
                            " isResumed ${lifecycleOwner.isResumed()} but started getChannels ")
                }
            }
        }
    }.launchIn(viewModelScope)

    ChannelsCache.channelReactionMsgLoadedFlow.onEach { data ->
        lifecycleOwner.lifecycleScope.launch {
            channelListView.channelUpdatedWithDiff(data, ChannelDiff.DEFAULT_FALSE.copy(lastMessageChanged = true))
        }
    }.launchIn(viewModelScope)

    fun createJobToAddNewChannelWithOnResumed(sceytChannel: SceytChannel) {
        val job = viewModelScope.launch {
            lifecycleOwner.withResumed {
                val updatedChannel = needToUpdateChannelsAfterResume[sceytChannel.id]?.channel
                        ?: sceytChannel
                channelListView.cancelLastSort()
                channelListView.addNewChannelAndSort(config.order, ChannelItem(updatedChannel))
                newAddedChannelJobs.remove(sceytChannel.id)
            }
        }
        newAddedChannelJobs[sceytChannel.id] = job
    }

    ChannelsCache.channelAddedFlow
        .filter { config.isValidForConfig(it) }
        .onEach(::createJobToAddNewChannelWithOnResumed)
        .launchIn(viewModelScope)

    ChannelsCache.pendingChannelCreatedFlow
        .filter { config.isValidForConfig(it.second) }
        .onEach { (pendingChannelId, newChannel) ->
            channelListView.replaceChannel(pendingChannelId, newChannel)
            if (!lifecycleOwner.isResumed()) {
                newAddedChannelJobs[pendingChannelId]?.let {
                    it.cancel()
                    newAddedChannelJobs.remove(pendingChannelId)
                }
                createJobToAddNewChannelWithOnResumed(newChannel)
            }
        }.launchIn(viewModelScope)

    ChannelsCache.channelDraftMessageChangesFlow.onEach { channel ->
        channelListView.channelUpdatedWithDiff(channel, ChannelDiff.DEFAULT_FALSE.copy(lastMessageChanged = true))
        if (!lifecycleOwner.isResumed()) {
            val pendingUpdate = needToUpdateChannelsAfterResume[channel.id]
            if (pendingUpdate != null) {
                pendingUpdate.channel = channel
            } else
                needToUpdateChannelsAfterResume[channel.id] = ChannelUpdateData(channel, false)
        }
    }.launchIn(viewModelScope)

    ChannelsCache.newChannelsOnSync
        .onEach { (_, channels) ->
            lifecycleOwner.withResumed {
                lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                    // Filter channels by config
                    val filtered = channels.filter { config.isValidForConfig(it) }
                    if (filtered.isNotEmpty()) {
                        val loadedChannels = channelListView.getData()?.mapNotNull {
                            (it as? ChannelItem)?.channel
                        }?.toMutableSet() ?: mutableSetOf()

                        // If loadedChannels are empty and not loading data from server, it means we can setData,
                        // otherwise we filter only channels which are between loaded channels and
                        // insert them to the list.
                        if (loadedChannels.isEmpty()) {
                            if (loadingFromServer || loadingFromDb) return@launch
                            val sorted = filtered.sortedWith(ChannelsComparatorDescBy(config.order))
                            val date = mapToChannelItem(data = sorted, hasNext = false)
                            SceytLog.i("syncResultUpdate", "loaded channels are empty, set data : ${sorted.map { it.channelSubject }}")
                            withContext(Dispatchers.Main) {
                                channelListView.setChannelsList(date)
                            }
                        } else {
                            // Get last channel to understand where to insert new channels
                            val lastChannel = loadedChannels.last()
                            val sorted = filtered.toSet().plus(lastChannel).sortedWith(ChannelsComparatorDescBy(config.order))
                            val index = sorted.indexOf(lastChannel)

                            // If index is last and we have more channels, we don't need to insert them,
                            // because they will be inserted by next page loading
                            if (index == loadedChannels.size - 1 && (hasNext || hasNextDb)) {
                                return@launch
                            }
                            // Get channels which need to be inserted
                            sorted.subList(0, index).forEach {
                                loadedChannels.add(it)
                            }
                            var newData: List<ChannelListItem> = loadedChannels.sortedWith(ChannelsComparatorDescBy(config.order)).map {
                                ChannelItem(it)
                            }

                            if (hasNext || hasNextDb)
                                newData = newData.plus(ChannelListItem.LoadingMoreItem)

                            SceytLog.i("syncResultUpdate", "apply synced channels : ${
                                newData.map {
                                    (it as? ChannelItem)?.channel?.channelSubject ?: it.toString()
                                }
                            }")
                            withContext(Dispatchers.Main) {
                                channelListView.setChannelsList(newData)
                            }
                        }
                    }
                }
            }
        }
        .launchIn(viewModelScope)

    ChannelEventManager.onChannelTypingEventFlow
        .filter { it.member.id != SceytChatUIKit.chatUIFacade.myId }
        .onEach {
            typingCancelHelper.await(it) { data ->
                channelListView.onTyping(data)
            }
            channelListView.onTyping(it)
        }.launchIn(lifecycleOwner.lifecycleScope)

    blockUserLiveData.observe(lifecycleOwner) {
        when (it) {
            is SceytResponse.Success -> {
                channelListView.userBlocked(it.data)
            }

            is SceytResponse.Error -> customToastSnackBar(channelListView, it.message ?: "")
        }
    }

    pageStateLiveData.observe(lifecycleOwner) {
        channelListView.updateStateView(it)
    }

    channelListView.setChannelCommandEvenListener {
        onChannelCommandEvent(it)
    }

    channelListView.setReachToEndListener { offset, lastChannel ->
        if (canLoadNext())
            getChannels(offset, searchQuery, LoadKeyData(value = lastChannel?.id ?: 0))
    }

    channelListView.setChannelAttachDetachListener { item, attached ->
        if (item is ChannelItem && !item.channel.isGroup) {
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
fun bindViewFromJava(viewModel: ChannelsViewModel, channelListView: ChannelListView, lifecycleOwner: LifecycleOwner) {
    viewModel.bind(channelListView, lifecycleOwner)
}

@Suppress("unused")
fun bindSearchViewFromJava(viewModel: ChannelsViewModel, searchView: SearchChannelInputView) {
    viewModel.bind(searchView)
}