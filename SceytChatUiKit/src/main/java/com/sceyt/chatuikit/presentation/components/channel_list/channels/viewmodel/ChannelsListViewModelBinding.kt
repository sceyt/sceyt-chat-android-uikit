package com.sceyt.chatuikit.presentation.components.channel_list.channels.viewmodel

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.managers.channel.ChannelEventManager
import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.extensions.isResumed
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.differs.ChannelDiff
import com.sceyt.chatuikit.persistence.extensions.getPeer
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelsCache
import com.sceyt.chatuikit.presentation.components.channel.header.helpers.ChannelEventCancelHelper
import com.sceyt.chatuikit.presentation.components.channel_list.channels.ChannelListView
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItem
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItem.ChannelItem
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelsItemComparatorBy
import com.sceyt.chatuikit.presentation.components.channel_list.search.SearchChannelInputView
import com.sceyt.chatuikit.services.SceytPresenceChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@JvmName("bind")
fun ChannelsViewModel.bind(channelListView: ChannelListView, lifecycleOwner: LifecycleOwner) {

    val channelEventCancelHelper by lazy { ChannelEventCancelHelper() }
    var needSubmitOnResume: List<ChannelListItem>? = null
    val mutexUpdateList = Mutex()
    val lifecycleScope = lifecycleOwner.lifecycleScope

    fun getUpdateAfterOnResumeData(): List<ChannelListItem> {
        return (needSubmitOnResume.takeIf { !it.isNullOrEmpty() }
                ?: channelListView.getData()) ?: mutableListOf()
    }

    getChannels(0, query = searchQuery)

    viewModelScope.launch {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (needSubmitOnResume.isNullOrEmpty())
                return@repeatOnLifecycle

            lifecycleScope.launch(Dispatchers.Default) {
                mutexUpdateList.withLock {
                    val newData = needSubmitOnResume?.sortedWith(ChannelsItemComparatorBy(config.order))
                    needSubmitOnResume = null
                    if (newData != null) {
                        withContext(Dispatchers.Main) {
                            channelListView.setChannelsList(lifecycleScope, newData)
                        }
                    }
                }
            }
        }
    }

    fun initPaginationDbResponse(response: PaginationResponse.DBResponse<SceytChannel>) {
        if (response.offset == 0) {
            channelListView.setChannelsList(
                scope = lifecycleScope,
                channels = mapToChannelItem(data = response.data, hasNext = response.hasNext)
            )
        } else {
            channelListView.addNewChannels(
                scope = lifecycleScope,
                channels = mapToChannelItem(data = response.data, hasNext = response.hasNext)
            )
        }
    }

    fun initPaginationServerResponse(response: PaginationResponse.ServerResponse<SceytChannel>) {
        when (response.data) {
            is SceytResponse.Success -> {
                if (response.hasDiff) {
                    val newChannels = mapToChannelItem(data = response.cacheData, hasNext = response.hasNext)
                    channelListView.setChannelsList(lifecycleScope, newChannels)
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

    loadChannelsFlow.onEach(::initChannelsResponse).launchIn(lifecycleScope)

    ChannelsCache.channelsDeletedFlow.onEach { channelIds ->
        if (!lifecycleOwner.isResumed()) {
            lifecycleScope.launch(Dispatchers.Default) {
                mutexUpdateList.withLock {
                    needSubmitOnResume = getUpdateAfterOnResumeData().filter {
                        (it as? ChannelItem)?.channel?.id !in channelIds
                    }
                }
            }
        } else {
            channelIds.forEach { channelId ->
                channelListView.deleteChannel(channelId, searchQuery)
            }
        }
    }.launchIn(viewModelScope)

    ChannelsCache.channelUpdatedFlow.onEach { data ->
        if (!lifecycleOwner.isResumed()) {
            lifecycleScope.launch(Dispatchers.Default) {
                mutexUpdateList.withLock {
                    needSubmitOnResume = getUpdateAfterOnResumeData().map {
                        if (it is ChannelItem && it.channel.id == data.channel.id) {
                            it.copy(channel = data.channel)
                        } else it
                    }
                }
            }
        } else {
            val isCanceled = channelListView.cancelLastSort()
            channelListView.channelUpdated(data.channel) {
                if (data.diff.lastMessageChanged || data.needSorting || isCanceled)
                    channelListView.sortChannelsBy(lifecycleScope, SceytChatUIKit.config.channelListOrder)

                SceytLog.i("ChannelsCache", "viewModel: id: ${data.channel.id}  body: ${data.channel.lastMessage?.body} draft:${data.channel.draftMessage?.body}  unreadCount ${data.channel.newMessageCount}" +
                        " isResumed ${lifecycleOwner.isResumed()} hasDifference: ${data.diff.hasDifference()} lastMessageChanged: ${data.diff.lastMessageChanged} needSorting: ${data.needSorting}")
            }
        }
    }.launchIn(viewModelScope)

    ChannelsCache.channelReactionMsgLoadedFlow.onEach { data ->
        lifecycleScope.launch {
            channelListView.channelUpdatedWithDiff(data, ChannelDiff.DEFAULT_FALSE.copy(lastMessageChanged = true))
        }
    }.launchIn(viewModelScope)

    ChannelsCache.channelAddedFlow
        .filter { config.isValidForConfig(it) }
        .onEach {
            if (!lifecycleOwner.isResumed()) {
                lifecycleScope.launch(Dispatchers.Default) {
                    mutexUpdateList.withLock {
                        val newData = getUpdateAfterOnResumeData().toMutableList()
                        if (newData.none { item -> (item as? ChannelItem)?.channel?.id == it.id }) {
                            newData.add(ChannelItem(it))
                            needSubmitOnResume = newData
                        }
                    }
                }
            } else {
                channelListView.addNewChannelAndSort(lifecycleScope, config.order, ChannelItem(it))
            }
        }
        .launchIn(viewModelScope)

    ChannelsCache.pendingChannelCreatedFlow
        .filter { config.isValidForConfig(it.second) }
        .onEach { (pendingChannelId, newChannel) ->
            if (!lifecycleOwner.isResumed()) {
                lifecycleScope.launch(Dispatchers.Default) {
                    mutexUpdateList.withLock {
                        val newData = getUpdateAfterOnResumeData().toMutableList()
                        newData.removeAll { (it as? ChannelItem)?.channel?.id == pendingChannelId }
                        if (newData.none { (it as? ChannelItem)?.channel?.id == newChannel.id }) {
                            newData.add(ChannelItem(newChannel))
                            needSubmitOnResume = newData
                        }
                    }
                }
            } else
                channelListView.replaceChannel(pendingChannelId, newChannel)
        }.launchIn(viewModelScope)

    ChannelsCache.channelDraftMessageChangesFlow.onEach { channel ->
        if (!lifecycleOwner.isResumed()) {
            lifecycleScope.launch {
                mutexUpdateList.withLock {
                    needSubmitOnResume = getUpdateAfterOnResumeData().map {
                        if (it is ChannelItem && it.channel.id == channel.id) {
                            it.copy(channel = channel)
                        } else it
                    }
                }
            }
        } else {
            channelListView.channelUpdatedWithDiff(channel, ChannelDiff.DEFAULT_FALSE.copy(lastMessageChanged = true))
        }
    }.launchIn(viewModelScope)

    ChannelsCache.newChannelsOnSync
        .onEach { (_, channels) ->
            lifecycleScope.launch {
                if (lifecycleOwner.isResumed()) {
                    val loadedChannels = channelListView.getData()?.mapNotNull {
                        (it as? ChannelItem)?.channel
                    } ?: emptyList()
                    val dataToSubmit = initDataOnNewChannelsOnSync(loadedChannels, channels)
                            ?: return@launch
                    channelListView.setChannelsList(lifecycleScope, dataToSubmit)
                } else {
                    mutexUpdateList.withLock {
                        val data = getUpdateAfterOnResumeData().mapNotNull {
                            (it as? ChannelItem)?.channel
                        }
                        needSubmitOnResume = initDataOnNewChannelsOnSync(data, channels)
                    }
                }
            }
        }.launchIn(viewModelScope)

    ChannelEventManager.onChannelMemberActivityEventFlow
        .filter { it.userId != SceytChatUIKit.chatUIFacade.myId }
        .onEach {
            channelEventCancelHelper.await(it) { event ->
                channelListView.onChannelEvent(event)
            }
            channelListView.onChannelEvent(it)
        }.launchIn(lifecycleScope)

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

@JvmName("bind")
fun ChannelsViewModel.bind(searchView: SearchChannelInputView) {
    searchView.setDebouncedTextChangeListener {
        getChannels(0, query = it)
    }

    searchView.setOnQuerySubmitListener {
        getChannels(0, query = it)
    }
}