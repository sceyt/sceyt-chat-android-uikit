package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.viewmodels

import androidx.lifecycle.*
import com.sceyt.chat.Types
import com.sceyt.chat.models.channel.GroupChannel
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.Message
import com.sceyt.sceytchatuikit.SceytSyncManager
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventEnum.*
import com.sceyt.sceytchatuikit.data.connectionobserver.ConnectionEventsObserver
import com.sceyt.sceytchatuikit.data.models.LoadKeyData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.PaginationResponse.LoadType.*
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytDirectChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytGroupChannel
import com.sceyt.sceytchatuikit.data.models.getLoadKey
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.models.messages.SelfMarkerTypeEnum
import com.sceyt.sceytchatuikit.extensions.asActivity
import com.sceyt.sceytchatuikit.extensions.customToastSnackBar
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.ChannelsCash
import com.sceyt.sceytchatuikit.persistence.logics.messageslogic.MessagesCash
import com.sceyt.sceytchatuikit.persistence.mappers.toMessage
import com.sceyt.sceytchatuikit.presentation.common.checkIsMemberInChannel
import com.sceyt.sceytchatuikit.presentation.root.PageState
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.MessagesListView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.ConversationHeaderView
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.MessageInputView
import com.sceyt.sceytchatuikit.services.SceytPresenceChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


fun MessageListViewModel.bind(messagesListView: MessagesListView, lifecycleOwner: LifecycleOwner) {
    val pendingDisplayMsgIds by lazy { mutableSetOf<Long>() }

    /** Send pending markers and pending messages when lifecycle come back onResume state,
     * Also set update current chat Id in ChannelsCash*/
    lifecycleOwner.lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            ChannelsCash.currentChannelId = channel.id
            if (ConnectionEventsObserver.connectionState == Types.ConnectState.StateConnected) {
                if (pendingDisplayMsgIds.isNotEmpty()) {
                    markMessageAsRead(*pendingDisplayMsgIds.toLongArray())
                    pendingDisplayMsgIds.clear()
                }
                sendPendingMessages()
            }
        }
    }

    if (channel.lastReadMessageId == 0L || channel.lastMessage?.deliveryStatus == DeliveryStatus.Pending
            || channel.lastReadMessageId == channel.lastMessage?.id)
        loadPrevMessages(0, 0)
    else {
        pinnedLastReadMessageId = channel.lastReadMessageId
        loadNearMessages(pinnedLastReadMessageId, LoadKeyData(key = LoadKeyType.ScrollToUnreadMessage.longValue))
    }

    messagesListView.setUnreadCount(channel.unreadMessageCount.toInt())

    messagesListView.setNeedDownloadListener {
        needDownload(it)
    }

    fun checkEnableDisableActions(channel: SceytChannel) {
        messagesListView.enableDisableClickActions(!replyInThread && channel.checkIsMemberInChannel(myId)
                && (channel.isGroup || (channel as? SceytDirectChannel)?.peer?.user?.blocked != true))
    }

    checkEnableDisableActions(channel)

    fun getCompareMessage(loadType: PaginationResponse.LoadType, offset: Int): SceytMessage? {
        if (offset == 0) return null
        return if (loadType == LoadNext)
            messagesListView.getLastMessage()?.message
        else messagesListView.getFirstMessage()?.message
    }

    fun checkToHildeLoadingMoreItemByLoadType(loadType: PaginationResponse.LoadType) {
        when {
            loadType == LoadPrev && !hasPrevDb -> messagesListView.hideLoadingPrev()
            loadType == LoadNext && !hasNextDb -> messagesListView.hideLoadingNext()
            loadType == LoadNear -> {
                if (!hasPrevDb)
                    messagesListView.hideLoadingPrev()
                if (!hasNextDb)
                    messagesListView.hideLoadingNext()
            }
        }
    }

    fun checkToScrollAfterResponse(response: PaginationResponse<SceytMessage>) {
        val loadKey = response.getLoadKey() ?: return
        when (loadKey.key) {
            LoadKeyType.ScrollToUnreadMessage.longValue -> {
                messagesListView.scrollToUnReadMessage()
            }
            LoadKeyType.ScrollToLastMessage.longValue -> {
                messagesListView.scrollToLastMessage()
            }
            LoadKeyType.ScrollToMessageById.longValue -> {
                messagesListView.scrollToMessage(loadKey.value, true)
            }
        }
    }

    suspend fun initPaginationDbResponse(response: PaginationResponse.DBResponse<SceytMessage>) {
        if (response.offset == 0) {
            messagesListView.setMessagesList(mapToMessageListItem(data = response.data,
                hasNext = response.hasNext, hasPrev = response.hasPrev))
        } else {
            val compareMessage = getCompareMessage(response.loadType, response.offset)
            when (response.loadType) {
                LoadPrev -> {
                    messagesListView.addPrevPageMessages(mapToMessageListItem(data = response.data,
                        hasNext = response.hasNext, hasPrev = response.hasPrev))
                }
                LoadNext -> {
                    messagesListView.addNextPageMessages(mapToMessageListItem(data = response.data,
                        hasNext = response.hasNext, hasPrev = response.hasPrev, compareMessage))
                }
                LoadNear, LoadNewest -> {
                    messagesListView.setMessagesList(mapToMessageListItem(data = response.data, hasNext = response.hasNext,
                        hasPrev = response.hasPrev), true)
                }
            }
        }
        checkToScrollAfterResponse(response)
    }

    suspend fun initPaginationServerResponse(response: PaginationResponse.ServerResponse<SceytMessage>) {
        when (response.data) {
            is SceytResponse.Success -> {
                if (response.hasDiff) {
                    val newMessages = mapToMessageListItem(data = response.cashData,
                        hasNext = response.hasNext,
                        hasPrev = response.hasPrev)
                    messagesListView.setMessagesList(newMessages, response.loadKey?.key == LoadKeyType.ScrollToLastMessage.longValue)
                } else
                    checkToHildeLoadingMoreItemByLoadType(response.loadType)

                checkToScrollAfterResponse(response)
            }
            is SceytResponse.Error -> checkToHildeLoadingMoreItemByLoadType(response.loadType)
        }
    }

    suspend fun initMessagesResponse(response: PaginationResponse<SceytMessage>) {
        when (response) {
            is PaginationResponse.DBResponse -> initPaginationDbResponse(response)
            is PaginationResponse.ServerResponse -> initPaginationServerResponse(response)
            else -> return
        }
    }

    ChannelsCash.channelDeletedFlow
        .filter { it == channel.id }
        .onEach {
            messagesListView.context.asActivity().finish()
        }.launchIn(lifecycleOwner.lifecycleScope)

    SceytSyncManager.syncChannelMessagesFinished.observe(lifecycleOwner, Observer {
        if (it.first.id == channel.id) {
            channel = it.first

            if (pinnedLastReadMessageId == 0L && channel.lastReadMessageId != 0L && channel.lastReadMessageId != channel.lastMessage?.id)
                pinnedLastReadMessageId = channel.lastReadMessageId

            lifecycleOwner.lifecycleScope.launch {
                val isLastDisplaying = messagesListView.isLastCompletelyItemDisplaying()
                messagesListView.addNextPageMessages(mapToMessageListItem(data = it.second,
                    hasNext = false, hasPrev = false, messagesListView.getLastMessage()?.message))
                if (isLastDisplaying)
                    messagesListView.scrollToLastMessage()

                messagesListView.sortMessages()
            }
        }
    })

    ConnectionEventsObserver.onChangedConnectStatusFlow.onEach { stateData ->
        if (stateData.state == Types.ConnectState.StateConnected) {
            val message = messagesListView.getLastMessageBy {
                // First tying to get last read message
                it is MessageListItem.MessageItem && it.message.deliveryStatus == DeliveryStatus.Read
            } ?: messagesListView.getFirstMessageBy {
                // Next tying to get fist sent message
                it is MessageListItem.MessageItem && it.message.deliveryStatus == DeliveryStatus.Sent
            } ?: messagesListView.getFirstMessageBy {
                // Next tying to get fist delivered message
                it is MessageListItem.MessageItem && it.message.deliveryStatus == DeliveryStatus.Delivered
            }
            (message as? MessageListItem.MessageItem)?.let {
                syncConversationMessagesAfter(it.message.id)
            }
        }
    }.launchIn(lifecycleOwner.lifecycleScope)

    MessagesCash.messageUpdatedFlow.onEach { messages ->
        messages.forEach {
            messagesListView.updateMessage(it)
        }
    }.launchIn(lifecycleOwner.lifecycleScope)

    loadMessagesFlow.onEach(::initMessagesResponse).launchIn(lifecycleOwner.lifecycleScope)

    onChannelUpdatedEventFlow.onEach {
        channel = it
        messagesListView.setUnreadCount(it.unreadMessageCount.toInt())
        checkEnableDisableActions(channel)
    }.launchIn(lifecycleOwner.lifecycleScope)

    onScrollToLastMessageLiveData.observe(lifecycleOwner, Observer {
        viewModelScope.launch(Dispatchers.Default) {
            channel.lastMessage?.id?.let { lastMsgId ->
                messagesListView.getMessageIndexedById(lastMsgId)?.let {
                    withContext(Dispatchers.Main) {
                        messagesListView.scrollToLastMessage()
                    }
                } ?: run {
                    loadNewestMessages(LoadKeyData(key = LoadKeyType.ScrollToLastMessage.longValue))
                    markChannelAsRead(channel.id)
                }
            }
        }
    })

    onScrollToReplyMessageLiveData.observe(lifecycleOwner, Observer {
        it.parent?.id?.let { parentId ->
            viewModelScope.launch(Dispatchers.Default) {
                messagesListView.getMessageIndexedById(parentId)?.let {
                    withContext(Dispatchers.Main) {
                        it.second.highlighted = true
                        messagesListView.scrollToPositionAndHighlight(it.first, true)
                    }
                } /*?: run {
                    loadNearMessages(parentId, LoadKeyData(
                        key = LoadKeyType.ScrollToMessageById.longValue,
                        value = parentId))
                }*/
            }
        }
    })

    markAsReadLiveData.observe(lifecycleOwner, Observer {
        if (it is SceytResponse.Success) {
            val data = it.data ?: return@Observer
            viewModelScope.launch(Dispatchers.Default) {
                messagesListView.getData()?.forEach { listItem ->
                    (listItem as? MessageListItem.MessageItem)?.message?.let { message ->
                        if (data.messageIds.contains(message.id)) {
                            message.selfMarkers = message.selfMarkers?.toMutableSet()?.apply {
                                add(SelfMarkerTypeEnum.Displayed.value())
                            }?.toTypedArray()
                        }
                    }
                }
            }
        }
    })

    messageEditedDeletedLiveData.observe(lifecycleOwner, Observer {
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
    })

    addDeleteReactionLiveData.observe(lifecycleOwner, Observer {
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
    })

    onNewOutGoingMessageFlow.onEach {
        if (hasNext || hasNextDb) return@onEach
        viewModelScope.launch {
            val initMessage = mapToMessageListItem(
                data = arrayListOf(it),
                hasNext = false,
                hasPrev = false,
                compareMessage = messagesListView.getLastMessage()?.message)

            messagesListView.addNewMessages(*initMessage.toTypedArray())
            messagesListView.updateViewState(PageState.Nothing)
        }
    }.launchIn(lifecycleOwner.lifecycleScope)

    onChannelMemberAddedOrKickedLiveData.observe(lifecycleOwner, Observer {
        checkEnableDisableActions(it)
    })

    fun checkStateAndMarkAsRead(messageItem: MessageListItem) {
        (messageItem as? MessageListItem.MessageItem)?.message?.let { message ->
            if (!message.incoming || message.selfMarkers?.contains(SelfMarkerTypeEnum.Displayed.value()) == true)
                return

            if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.RESUMED) {
                pendingDisplayMsgIds.add(message.id)
                sendDisplayedHelper.submit {
                    markMessageAsRead(*(pendingDisplayMsgIds).toLongArray())
                    pendingDisplayMsgIds.clear()
                }
            } else pendingDisplayMsgIds.add(message.id)
        }
    }

    onNewMessageFlow.onEach {
        if (hasNext || hasNextDb) return@onEach
        val initMessage = mapToMessageListItem(
            data = arrayListOf(it),
            hasNext = false,
            hasPrev = false,
            compareMessage = messagesListView.getLastMessage()?.message)

        messagesListView.addNewMessages(*initMessage.toTypedArray())
        messagesListView.updateViewState(PageState.Nothing)
    }.launchIn(lifecycleOwner.lifecycleScope)

    onNewThreadMessageFlow.onEach {
        messagesListView.updateReplyCount(it)
    }.launchIn(lifecycleOwner.lifecycleScope)

    onOutGoingThreadMessageFlow.onEach {
        messagesListView.newReplyMessage(it.parent?.id)
    }.launchIn(lifecycleOwner.lifecycleScope)

    onMessageStatusFlow.onEach {
        messagesListView.updateMessagesStatus(it.status, it.messageIds)
    }.launchIn(lifecycleOwner.lifecycleScope)

    onTransferUpdatedFlow.onEach {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            messagesListView.updateProgress(it)
        }
    }.launchIn(lifecycleOwner.lifecycleScope)

    onMessageReactionUpdatedFlow.onEach {
        messagesListView.updateReaction(it)
    }.launchIn(lifecycleOwner.lifecycleScope)

    onMessageEditedOrDeletedFlow.onEach {
        messagesListView.messageEditedOrDeleted(it)
    }.launchIn(lifecycleOwner.lifecycleScope)

    onChannelEventFlow.onEach {
        when (it.eventType) {
            ClearedHistory -> messagesListView.clearData()
            Left -> {
                val leftUser = (it.channel as? GroupChannel)?.members?.getOrNull(0)?.id
                if (leftUser == myId && (channel.channelType == ChannelTypeEnum.Direct || channel.channelType == ChannelTypeEnum.Private))
                    messagesListView.context.asActivity().finish()
            }
            Deleted -> messagesListView.context.asActivity().finish()
            else -> return@onEach
        }
    }.launchIn(lifecycleOwner.lifecycleScope)

    onOutGoingMessageStatusFlow.onEach {
        val sceytUiMessage = it.second
        sceytUiMessage.canShowAvatarAndName = shouldShowAvatarAndName(sceytUiMessage, messagesListView.getLastMessage()?.message)
        messagesListView.updateMessage(sceytUiMessage)
        messagesListView.sortMessages()
    }.launchIn(lifecycleOwner.lifecycleScope)

    joinLiveData.observe(lifecycleOwner, Observer {
        if (it is SceytResponse.Success) {
            it.data?.let { channel ->
                checkEnableDisableActions(channel)
            }
        }
    })

    channelLiveData.observe(lifecycleOwner, Observer {
        if (it is SceytResponse.Success) {
            it.data?.let { channel ->
                checkEnableDisableActions(channel)
                messagesListView.setUnreadCount(channel.unreadMessageCount.toInt())
            }
        }
    })

    pageStateLiveData.observe(lifecycleOwner, Observer {
        messagesListView.updateViewState(it, false)
    })

    messagesListView.setMessageCommandEventListener {
        onMessageCommandEvent(it)
    }

    messagesListView.setMessageReactionsEventListener {
        onReactionEvent(it)
    }

    messagesListView.setNeedLoadPrevMessagesListener { offset, message ->
        if (canLoadPrev()) {
            val messageId = (message as? MessageListItem.MessageItem)?.message?.id ?: 0
            loadPrevMessages(messageId, offset)
        }
    }

    messagesListView.setNeedLoadNextMessagesListener { offset, message ->
        if (canLoadNext()) {
            val messageId = (message as? MessageListItem.MessageItem)?.message?.id ?: 0
            loadNextMessages(messageId, offset)
        }
    }

    messagesListView.setMessageDisplayedListener {
        checkStateAndMarkAsRead(it)
    }
}

fun MessageListViewModel.bind(messageInputView: MessageInputView,
                              replyInThreadMessage: SceytMessage?,
                              lifecycleOwner: LifecycleOwner) {

    messageInputView.setReplyInThreadMessageId(replyInThreadMessage?.id)
    messageInputView.checkIsParticipant(channel)
    getChannel(channel.id)

    onChannelUpdatedEventFlow.onEach {
        messageInputView.checkIsParticipant(it)
    }.launchIn(lifecycleOwner.lifecycleScope)

    channelLiveData.observe(lifecycleOwner, Observer {
        if (it is SceytResponse.Success) {
            channel = it.data ?: return@Observer
            messageInputView.checkIsParticipant(channel)
        }
    })

    joinLiveData.observe(lifecycleOwner, Observer {
        if (it is SceytResponse.Success) {
            messageInputView.joinSuccess()
            (channel as SceytGroupChannel).members = (it.data as SceytGroupChannel).members
        }

        notifyPageStateWithResponse(it)
    })

    onEditMessageCommandLiveData.observe(lifecycleOwner, Observer {
        messageInputView.editMessage(it.toMessage())
    })

    onReplyMessageCommandLiveData.observe(lifecycleOwner, Observer {
        messageInputView.replyMessage(it.toMessage())
    })

    onChannelMemberAddedOrKickedLiveData.observe(lifecycleOwner, Observer {
        messageInputView.checkIsParticipant(channel)
    })

    onChannelEventFlow.onEach {
        when (it.eventType) {
            Left -> {
                if (channel.channelType == ChannelTypeEnum.Public) {
                    val leftUser = (it.channel as? GroupChannel)?.members?.getOrNull(0)?.id
                    if (leftUser == myId)
                        messageInputView.onChannelLeft()
                }
            }
            Joined -> {
                if (channel.channelType == ChannelTypeEnum.Public) {
                    val leftUser = (it.channel as? GroupChannel)?.members?.getOrNull(0)?.id
                    if (leftUser == myId)
                        messageInputView.joinSuccess()
                }
            }
            else -> return@onEach
        }
    }.launchIn(lifecycleOwner.lifecycleScope)

    messageInputView.messageInputActionCallback = object : MessageInputView.MessageInputActionCallback {
        override fun sendMessage(message: Message) {
            this@bind.sendMessage(message)
        }

        override fun sendReplyMessage(message: Message, parent: Message?) {
            this@bind.sendMessage(message)
        }

        override fun sendEditMessage(message: SceytMessage) {
            this@bind.editMessage(message)
        }

        override fun typing(typing: Boolean) {
            sendTypingEvent(typing)
        }

        override fun join() {
            this@bind.join()
        }
    }
}

fun MessageListViewModel.bind(headerView: ConversationHeaderView,
                              replyInThreadMessage: SceytMessage?,
                              lifecycleOwner: LifecycleOwner) {

    if (replyInThread)
        headerView.setReplyMessage(channel, replyInThreadMessage)
    else
        headerView.setChannel(channel)

    if (channel is SceytDirectChannel)
        SceytPresenceChecker.addNewUserToPresenceCheck((channel as SceytDirectChannel).peer?.id)

    SceytPresenceChecker.onPresenceCheckUsersFlow.onEach {
        headerView.onPresenceUpdate(it.map { presenceUser -> presenceUser.user })
    }.launchIn(lifecycleOwner.lifecycleScope)

    onChannelTypingEventFlow.onEach {
        headerView.onTyping(it)
    }.launchIn(lifecycleOwner.lifecycleScope)

    onChannelMemberAddedOrKickedLiveData.observe(lifecycleOwner, Observer {
        if (!replyInThread)
            headerView.setChannel(channel)
    })

    joinLiveData.observe(lifecycleOwner, Observer {
        if (!replyInThread)
            getChannel(channel.id)
    })

    channelLiveData.observe(lifecycleOwner, Observer {
        if (it is SceytResponse.Success) {
            channel = it.data ?: return@Observer
            if (!replyInThread)
                headerView.setChannel(it.data)
        }
    })
}

@Suppress("unused")
fun bindViewFromJava(viewModel: MessageListViewModel, messagesListView: MessagesListView, lifecycleOwner: LifecycleOwner) {
    viewModel.bind(messagesListView, lifecycleOwner)
}
