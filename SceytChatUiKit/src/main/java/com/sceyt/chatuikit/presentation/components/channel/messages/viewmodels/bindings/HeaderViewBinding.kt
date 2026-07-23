package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.bindings

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.sceyt.chatuikit.data.managers.connection.ConnectionEventManager
import com.sceyt.chatuikit.data.managers.connection.event.ConnectionStateData
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.extensions.isDirect
import com.sceyt.chatuikit.presentation.components.channel.header.MessagesListHeaderView
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.MessageListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

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
        headerView.setChannel(channel, true)

    peerPresenceUpdatedFlow.observe(lifecycleOwner, headerView::onPresenceUpdate)

    ConnectionEventManager.onChangedConnectStatusFlow
        .stateIn(
            lifecycleOwner.lifecycleScope,
            started = SharingStarted.Lazily,
            initialValue = ConnectionStateData(ConnectionEventManager.connectionState)
        )
        .onEach { state ->
            state.state?.let { headerView.onConnectionStateUpdate(it, channel) }
        }
        .launchIn(lifecycleOwner.lifecycleScope)

    onChannelUpdatedEventFlow.onEach {
        headerView.setChannel(it, true)
    }.launchIn(lifecycleOwner.lifecycleScope)

    if (channel.isDirect() && !channel.isSelf)
        lifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                delay((1000 * 60).milliseconds)

                // Relative labels like "last seen 1 min ago" change with time even without presence events.
                headerView.refreshSubTitle(channel)
            }
        }

    onChannelMemberActivityEventFlow
        .onEach(headerView::handleMemberActivityEvent)
        .flowOn(Dispatchers.Main)
        .launchIn(lifecycleOwner.lifecycleScope)
}