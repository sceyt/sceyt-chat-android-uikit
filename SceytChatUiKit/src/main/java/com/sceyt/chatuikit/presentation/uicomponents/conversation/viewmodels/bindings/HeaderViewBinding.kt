package com.sceyt.chatuikit.presentation.uicomponents.conversation.viewmodels.bindings

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.logicimpl.channelslogic.ChannelUpdatedType
import com.sceyt.chatuikit.persistence.logicimpl.channelslogic.ChannelsCache
import com.sceyt.chatuikit.presentation.common.getPeer
import com.sceyt.chatuikit.presentation.common.isDirect
import com.sceyt.chatuikit.presentation.uicomponents.conversation.viewmodels.MessageListViewModel
import com.sceyt.chatuikit.presentation.uicomponents.conversationheader.ConversationHeaderView
import com.sceyt.chatuikit.services.SceytPresenceChecker
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

fun MessageListViewModel.bind(headerView: ConversationHeaderView,
                              replyInThreadMessage: SceytMessage?,
                              lifecycleOwner: LifecycleOwner) {

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

    ChannelsCache.channelUpdatedFlow
        .filter { it.channel.id == channel.id }
        .onEach {
            // We handling presence update with SceytPresenceChecker
            if (it.eventType != ChannelUpdatedType.Presence)
                headerView.setChannel(it.channel)
        }
        .launchIn(lifecycleOwner.lifecycleScope)

    onChannelTypingEventFlow.onEach {
        headerView.onTyping(it)
    }.launchIn(lifecycleOwner.lifecycleScope)

    onChannelMemberAddedOrKickedLiveData.observe(lifecycleOwner) {
        if (!replyInThread)
            headerView.setChannel(channel)
    }

    joinLiveData.observe(lifecycleOwner) {
        if (!replyInThread)
            getChannel(channel.id)
    }

    channelLiveData.observe(lifecycleOwner, Observer {
        if (it is SceytResponse.Success) {
            channel = it.data ?: return@Observer
            if (!replyInThread)
                headerView.setChannel(it.data)
        }
    })
}

@Suppress("unused")
fun bindViewFromJava(viewModel: MessageListViewModel, replyInThreadMessage: SceytMessage?, headerView: ConversationHeaderView, lifecycleOwner: LifecycleOwner) {
    viewModel.bind(headerView, replyInThreadMessage, lifecycleOwner)
}