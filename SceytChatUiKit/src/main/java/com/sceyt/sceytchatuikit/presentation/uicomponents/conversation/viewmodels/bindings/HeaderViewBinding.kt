package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.viewmodels.bindings

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.ChannelUpdatedType
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.ChannelsCache
import com.sceyt.sceytchatuikit.presentation.common.getFirstMember
import com.sceyt.sceytchatuikit.presentation.common.isDirect
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.viewmodels.MessageListViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.ConversationHeaderView
import com.sceyt.sceytchatuikit.services.SceytPresenceChecker
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

fun MessageListViewModel.bind(headerView: ConversationHeaderView,
                              replyInThreadMessage: SceytMessage?,
                              lifecycleOwner: LifecycleOwner) {

    messageActionBridge.setHeaderView(headerView)

    if (replyInThread)
        headerView.setReplyMessage(channel, replyInThreadMessage)
    else
        headerView.setChannel(channel)

    val peer = channel.getFirstMember()
    if (channel.isDirect()) {
        SceytPresenceChecker.addNewUserToPresenceCheck(peer?.id)
        SceytPresenceChecker.onPresenceCheckUsersFlow.distinctUntilChanged()
            .onEach {
                it.find { user -> user.user.id == peer?.id }?.let { presenceUser ->
                    headerView.onPresenceUpdate(presenceUser.user)
                }
            }.launchIn(lifecycleOwner.lifecycleScope)
    }

    ChannelsCache.channelUpdatedFlow
        .filter { it.channel.id == channel.id }
        .onEach {
            if (it.eventType == ChannelUpdatedType.Presence) {
                peer?.user?.let { user ->
                    headerView.onPresenceUpdate(user)
                } ?: headerView.setChannel(it.channel)
            } else headerView.setChannel(it.channel)
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