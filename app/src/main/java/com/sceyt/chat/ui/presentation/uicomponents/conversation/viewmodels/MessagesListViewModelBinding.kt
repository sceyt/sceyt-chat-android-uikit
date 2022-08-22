package com.sceyt.chat.ui.presentation.uicomponents.conversation.viewmodels

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sceyt.chat.ClientWrapper
import com.sceyt.chat.models.channel.GroupChannel
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.ui.data.channeleventobserver.ChannelEventEnum
import com.sceyt.chat.ui.data.models.PaginationResponse
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.models.channels.ChannelTypeEnum
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.data.toMessage
import com.sceyt.chat.ui.extensions.asAppCompatActivity
import com.sceyt.chat.ui.extensions.customToastSnackBar
import com.sceyt.chat.ui.presentation.common.checkIsMemberInChannel
import com.sceyt.chat.ui.presentation.root.PageState
import com.sceyt.chat.ui.presentation.uicomponents.conversation.MessagesListView
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversationheader.ConversationHeaderView
import com.sceyt.chat.ui.presentation.uicomponents.messageinput.MessageInputView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


fun MessageListViewModel.bindView(messagesListView: MessagesListView, lifecycleOwner: LifecycleOwner) {
    val pendingDisplayMsgIds by lazy { arrayListOf<Long>() }
    val myId = preferences.getUsername()

    lifecycleOwner.lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (pendingDisplayMsgIds.isNotEmpty()) {
                markMessageAsDisplayed(*pendingDisplayMsgIds.toLongArray())
                pendingDisplayMsgIds.clear()
            }
        }
    }

    messagesListView.enableDisableClickActions(!replayInThread && channel.checkIsMemberInChannel(myId))

    lifecycleOwner.lifecycleScope.launch {
        messagesFlow.collect {
            when (it) {
                is SceytResponse.Success -> {
                    it.data?.let { data -> messagesListView.setMessagesList(data) }
                }
                is SceytResponse.Error -> {
                    customToastSnackBar(messagesListView, it.message ?: "")
                }
            }
        }
    }

    lifecycleOwner.lifecycleScope.launch {
        loadMessagesFlow.collect {
            when (it) {
                is PaginationResponse.DBResponse -> {
                    if (it.offset == 0) {
                        messagesListView.setMessagesList(it.data)
                    } else messagesListView.addNextPageMessages(it.data)
                }
                is PaginationResponse.ServerResponse -> {
                    if (it.data is SceytResponse.Success) {
                        it.data.data?.let { data ->
                            messagesListView.updateMessagesWithServerData(data, it.offset, lifecycleOwner)
                        }
                    }
                    notifyPageStateWithResponse(it.data, it.offset > 0, it.data.data.isNullOrEmpty())
                }
                is PaginationResponse.Nothing -> return@collect
            }
        }
    }

    messageEditedDeletedLiveData.observe(lifecycleOwner) {
        when (it) {
            is SceytResponse.Success -> {
                it.data?.let { data -> messagesListView.messageEditedOrDeleted(data) }
            }
            is SceytResponse.Error -> {
                if (it.data?.deliveryStatus == DeliveryStatus.Pending ||
                        it.data?.deliveryStatus == DeliveryStatus.Failed) {
                    messagesListView.messageEditedOrDeleted(it.data)
                } else
                    customToastSnackBar(messagesListView, it.message ?: "")
            }
        }
    }

    addDeleteReactionLiveData.observe(lifecycleOwner) {
        when (it) {
            is SceytResponse.Success -> {
                it.data?.let { data ->
                    messagesListView.updateReaction(data)
                }
            }
            is SceytResponse.Error -> {
                customToastSnackBar(messagesListView, it.message ?: "")
            }
        }
    }

    onNewOutgoingMessageLiveData.observe(lifecycleOwner) {
        val initMessage = mapToMessageListItem(
            data = arrayListOf(it),
            hasNext = false,
            lastMessage = messagesListView.getLastMessage())

        messagesListView.addNewMessages(*initMessage.toTypedArray())
        messagesListView.updateViewState(PageState.Nothing)

        markMessageAsDisplayed(it.id)
    }

    onChannelMemberAddedOrKickedLiveData.observe(lifecycleOwner) {
        messagesListView.enableDisableClickActions(!replayInThread && it.checkIsMemberInChannel(myId))
    }

    lifecycleOwner.lifecycleScope.launch {
        onNewMessageFlow.collect {
            val initMessage = mapToMessageListItem(
                data = arrayListOf(it),
                hasNext = false,
                lastMessage = messagesListView.getLastMessage())

            messagesListView.addNewMessages(*initMessage.toTypedArray())
            messagesListView.updateViewState(PageState.Nothing)

            if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.RESUMED)
                markMessageAsDisplayed(it.id)
            else pendingDisplayMsgIds.add(it.id)
        }
    }

    lifecycleOwner.lifecycleScope.launch {
        onNewThreadMessageFlow.collect {
            messagesListView.updateReplayCount(it)
        }
    }

    lifecycleOwner.lifecycleScope.launch {
        onOutGoingThreadMessageFlow.collect {
            messagesListView.newReplayMessage(it.parent?.id)
        }
    }

    lifecycleOwner.lifecycleScope.launch {
        onMessageStatusFlow.collect {
            messagesListView.updateMessagesStatus(it.status, it.messageIds)
        }
    }

    lifecycleOwner.lifecycleScope.launch {
        onMessageReactionUpdatedFlow.collect {
            messagesListView.updateReaction(it)
        }
    }

    lifecycleOwner.lifecycleScope.launch {
        onMessageEditedOrDeletedFlow.collect {
            messagesListView.messageEditedOrDeleted(it)
        }
    }

    lifecycleOwner.lifecycleScope.launch {
        onChannelEventFlow.collect {
            when (it.eventType) {
                ChannelEventEnum.ClearedHistory -> messagesListView.clearData()
                ChannelEventEnum.Left -> {
                    val leftUser = (it.channel as? GroupChannel)?.members?.getOrNull(0)?.id
                    if (leftUser == ClientWrapper.currentUser.id &&
                            (channel.channelType == ChannelTypeEnum.Direct || channel.channelType == ChannelTypeEnum.Private))
                        messagesListView.context.asAppCompatActivity().finish()
                }
                ChannelEventEnum.Deleted -> messagesListView.context.asAppCompatActivity().finish()
                else -> return@collect
            }
        }
    }

    messageSentLiveData.observe(lifecycleOwner) {
        when (it) {
            is SceytResponse.Success -> {
                it.data?.let { sceytUiMessage ->
                    sceytUiMessage.canShowAvatarAndName = shouldShowAvatarAndName(sceytUiMessage, messagesListView.getLastMessage()?.message)
                    messagesListView.updateMessage(sceytUiMessage)
                }
            }
            is SceytResponse.Error -> {
                it.data?.let { msg ->
                    messagesListView.messageSendFailed(msg.tid)
                }
                customToastSnackBar(messagesListView, it.message ?: "")
            }
        }
    }

    joinLiveData.observe(lifecycleOwner) {
        if (it is SceytResponse.Success) {
            it.data?.let { channel ->
                messagesListView.enableDisableClickActions(!replayInThread && channel.checkIsMemberInChannel(myId))
            }
        }
    }

    channelLiveData.observe(lifecycleOwner) {
        if (it is SceytResponse.Success) {
            it.data?.let { channel ->
                messagesListView.enableDisableClickActions(!replayInThread && channel.checkIsMemberInChannel(myId))
            }
        }
    }

    pageStateLiveData.observe(lifecycleOwner) {
        messagesListView.updateViewState(it)
    }

    messagesListView.setMessageEventListener {
        onMessageEvent(it)
    }

    messagesListView.setMessageReactionsEventListener {
        onReactionEvent(it)
    }

    messagesListView.setNeedLoadMoreMessagesListener { offset, message ->
        if (!loadingItems.get() && hasNext) {
            loadingItems.set(true)
            val lastMessageId = (message as? MessageListItem.MessageItem)?.message?.id ?: 0
            loadMessages(lastMessageId, offset)
        }
    }
}

fun MessageListViewModel.bindView(messageInputView: MessageInputView,
                                  replayInThreadMessage: SceytMessage?,
                                  lifecycleOwner: LifecycleOwner) {

    messageInputView.setReplayInThreadMessageId(replayInThreadMessage?.id)
    messageInputView.checkIsParticipant(channel)


    pageStateLiveData.observe(lifecycleOwner) {
        if (it is PageState.StateError)
            customToastSnackBar(messageInputView, it.errorMessage.toString())
    }

    channelLiveData.observe(lifecycleOwner) {
        if (it is SceytResponse.Success) {
            channel = it.data ?: return@observe
            messageInputView.checkIsParticipant(channel)
        }
    }

    joinLiveData.observe(lifecycleOwner) {
        if (it is SceytResponse.Success)
            messageInputView.joinSuccess()

        notifyPageStateWithResponse(it)
    }

    onEditMessageCommandLiveData.observe(lifecycleOwner) {
        messageInputView.message = it.toMessage()
    }

    onReplayMessageCommandLiveData.observe(lifecycleOwner) {
        messageInputView.replayMessage(it.toMessage())
    }

    onChannelMemberAddedOrKickedLiveData.observe(lifecycleOwner) {
        messageInputView.checkIsParticipant(channel)
    }

    lifecycleOwner.lifecycleScope.launch {
        onChannelEventFlow.collect {
            when (it.eventType) {
                ChannelEventEnum.Left -> {
                    if (channel.channelType == ChannelTypeEnum.Public) {
                        val leftUser = (it.channel as? GroupChannel)?.members?.getOrNull(0)?.id
                        if (leftUser == ClientWrapper.currentUser.id)
                            messageInputView.onChannelLeft()
                    }
                }
                ChannelEventEnum.Joined -> {
                    if (channel.channelType == ChannelTypeEnum.Public) {
                        val leftUser = (it.channel as? GroupChannel)?.members?.getOrNull(0)?.id
                        if (leftUser == ClientWrapper.currentUser.id)
                            messageInputView.joinSuccess()
                    }
                }
                else -> return@collect
            }
        }
    }

    messageInputView.messageInputActionCallback = object : MessageInputView.MessageInputActionCallback {
        override fun sendMessage(message: Message) {
            messageInputView.cancelReplay {
                this@bindView.sendMessage(message)
            }
        }

        override fun sendReplayMessage(message: Message, parent: Message?) {
            messageInputView.cancelReplay {
                this@bindView.sendReplayMessage(message, parent)
            }
        }

        override fun sendEditMessage(message: Message) {
            this@bindView.editMessage(message)
            messageInputView.cancelReplay()
        }

        override fun typing(typing: Boolean) {
            sendTypingEvent(typing)
        }

        override fun join() {
            this@bindView.join()
        }
    }
}

fun MessageListViewModel.bindView(headerView: ConversationHeaderView,
                                  replayInThreadMessage: SceytMessage?,
                                  lifecycleOwner: LifecycleOwner) {


    if (replayInThread)
        headerView.setReplayMessage(replayInThreadMessage)
    else
        headerView.setChannel(channel)

    lifecycleOwner.lifecycleScope.launch {
        onChannelTypingEventFlow.collectLatest {
            headerView.onTyping(it)
        }
    }

    onChannelMemberAddedOrKickedLiveData.observe(lifecycleOwner) {
        if (!replayInThread)
            headerView.setChannel(channel)
    }

    joinLiveData.observe(lifecycleOwner) {
        if (!replayInThread)
            getChannel(channel.id)
    }

    channelLiveData.observe(lifecycleOwner) {
        if (it is SceytResponse.Success) {
            channel = it.data ?: return@observe
            if (!replayInThread)
                headerView.setChannel(it.data)
        }
    }
}


fun bindViewFromJava(viewModel: MessageListViewModel, messagesListView: MessagesListView, lifecycleOwner: LifecycleOwner) {
    viewModel.bindView(messagesListView, lifecycleOwner)
}
