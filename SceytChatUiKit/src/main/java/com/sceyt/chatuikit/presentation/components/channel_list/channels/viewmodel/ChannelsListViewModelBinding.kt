package com.sceyt.chatuikit.presentation.components.channel_list.channels.viewmodel

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.managers.channel.ChannelEventManager
import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.persistence.extensions.getPeer
import com.sceyt.chatuikit.presentation.components.channel.header.helpers.ChannelEventChangeHelper
import com.sceyt.chatuikit.presentation.components.channel_list.channels.ChannelListView
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItem.ChannelItem
import com.sceyt.chatuikit.presentation.components.channel_list.search.SearchChannelInputView
import com.sceyt.chatuikit.services.SceytPresenceChecker
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.ConcurrentHashMap

@JvmName("bind")
fun ChannelsViewModel.bind(channelListView: ChannelListView, lifecycleOwner: LifecycleOwner) {
    val lifecycleScope = lifecycleOwner.lifecycleScope
    val channelEventHelpersMap by lazy { ConcurrentHashMap<Long, ChannelEventChangeHelper>() }

    state
        .onEach { channelListView.setChannelsList(lifecycleScope, it.channelItems) }
        .launchIn(lifecycleScope)

    pageStateLiveData.observe(lifecycleOwner) { channelListView.updateStateView(it) }

    ChannelEventManager.onChannelMemberActivityEventFlow
        .filter { it.userId != SceytChatUIKit.chatUIFacade.myId }
        .onEach { event ->
            channelEventHelpersMap.computeIfAbsent(event.channelId) { id ->
                ChannelEventChangeHelper(
                    scope = lifecycleScope,
                    activeUsersUpdated = { events -> channelListView.onChannelEvents(id, events) },
                    showChannelEventsInSequence = false
                )
            }.onActivityEvent(event)
        }.launchIn(lifecycleScope)

    channelListView.setReachToEndListener { offset, lastChannel ->
        if (canLoadNext())
            getChannels(offset, searchQuery, LoadKeyData(value = lastChannel?.id ?: 0))
    }

    channelListView.setChannelCommandEvenListener(::onChannelCommandEvent)

    channelListView.setChannelAttachDetachListener { item, attached ->
        if (item is ChannelItem && !item.channel.isGroup) {
            item.channel.getPeer()?.let {
                if (attached) SceytPresenceChecker.addNewUserToPresenceCheck(it.id)
                else SceytPresenceChecker.removeFromPresenceCheck(it.id)
            }
        }
    }
}

@JvmName("bind")
fun ChannelsViewModel.bind(searchView: SearchChannelInputView) {
    searchView.setDebouncedTextChangeListener {
        if (searchQuery == it) return@setDebouncedTextChangeListener
        getChannels(0, query = it)
    }

    searchView.setOnQuerySubmitListener {
        if (searchQuery == it) return@setOnQuerySubmitListener
        getChannels(0, query = it)
    }
}