package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.bindings

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.sceyt.chatuikit.data.managers.connection.ConnectionEventManager
import com.sceyt.chatuikit.data.managers.connection.event.ConnectionStateData
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.extensions.getPeer
import com.sceyt.chatuikit.persistence.extensions.isDirect
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelUpdatedType
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelsCache
import com.sceyt.chatuikit.presentation.components.channel.header.MessagesListHeaderView
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.MessageListViewModel
import com.sceyt.chatuikit.services.SceytPresenceChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

@JvmName("bind")
fun MessageListViewModel.bind(
        headerView: MessagesListHeaderView,
        replyInThreadMessage: SceytMessage?,
        lifecycleOwner: LifecycleOwner
) {

    messageActionBridge.setHeaderView(headerView)

    headerView.setSearchQueryChangeListener {
        searchMessages(it)
    }

    if (replyInThread)
        headerView.setReplyMessage(channel, replyInThreadMessage)
    else
        headerView.setChannel(channel)

    val peerId = channel.getPeer()?.id
    if (channel.isDirect()) {
        SceytPresenceChecker.addNewUserToPresenceCheck(peerId)
        SceytPresenceChecker.onPresenceCheckUsersFlow.distinctUntilChanged()
            .onEach {
                it.find { user -> user.user.id == peerId }?.let { presenceUser ->
                    headerView.onPresenceUpdate(presenceUser.user)
                }
            }.launchIn(lifecycleOwner.lifecycleScope)
    }

    ConnectionEventManager.onChangedConnectStatusFlow
        .stateIn(
            lifecycleOwner.lifecycleScope,
            started = SharingStarted.Lazily,
            initialValue = ConnectionStateData(ConnectionEventManager.connectionState)
        )
        .onEach { state ->
            state.state?.let { headerView.onConnectionStateUpdate(it) }
        }
        .launchIn(lifecycleOwner.lifecycleScope)

    ChannelsCache.channelUpdatedFlow
        .filter { it.channel.id == channel.id }
        .onEach {
            // We handling presence update with SceytPresenceChecker
            if (it.eventType != ChannelUpdatedType.Presence)
                headerView.setChannel(it.channel)
        }
        .launchIn(lifecycleOwner.lifecycleScope)

    onChannelTypingEventFlow
        .onEach {
            headerView.handleTypingEvent(it)
        }
        .flowOn(Dispatchers.Main)
        .launchIn(lifecycleOwner.lifecycleScope)

    onChannelMemberAddedOrKickedLiveData.observe(lifecycleOwner) {
        if (!replyInThread)
            headerView.setChannel(channel)
    }

    joinLiveData.observe(lifecycleOwner) {
        if (!replyInThread && it is SceytResponse.Success) {
            headerView.setChannel(it.data ?: return@observe)
        }
    }

    channelLiveData.observe(lifecycleOwner) {
        if (!replyInThread && it is SceytResponse.Success) {
            headerView.setChannel(it.data ?: return@observe)
        }
    }
}