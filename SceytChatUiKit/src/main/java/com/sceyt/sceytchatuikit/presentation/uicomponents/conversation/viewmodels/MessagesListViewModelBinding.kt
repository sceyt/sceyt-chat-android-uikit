package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.viewmodels

import androidx.lifecycle.*
import com.sceyt.chat.models.channel.GroupChannel
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.Message
import com.sceyt.sceytchatuikit.SceytSyncManager
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventEnum.*
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.PaginationResponse.LoadType.*
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.SceytDirectChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytGroupChannel
import com.sceyt.sceytchatuikit.data.models.getLoadKey
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.models.messages.SelfMarkerTypeEnum
import com.sceyt.sceytchatuikit.extensions.asActivity
import com.sceyt.sceytchatuikit.extensions.customToastSnackBar
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.ChannelsCash
import com.sceyt.sceytchatuikit.persistence.mappers.toMessage
import com.sceyt.sceytchatuikit.presentation.common.checkIsMemberInChannel
import com.sceyt.sceytchatuikit.presentation.root.PageState
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.MessagesListView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.ConversationHeaderView
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.MessageInputView
import com.sceyt.sceytchatuikit.services.SceytPresenceChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


fun MessageListViewModel.bind(messagesListView: MessagesListView, lifecycleOwner: LifecycleOwner) {
    val pendingDisplayMsgIds by lazy { mutableSetOf<Long>() }

    if (channel.lastReadMessageId == 0L || channel.lastReadMessageId == channel.lastMessage?.id)
        loadPrevMessages(0, 0)
    else {
        pinnedLastReadMessageId = channel.lastReadMessageId
        loadNearMessages(pinnedLastReadMessageId, LoadKeyType.ScrollToUnreadMessage.longValue)
    }

    messagesListView.setUnreadCount(channel.unreadMessageCount.toInt())

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
        when (val loadKey = response.getLoadKey()) {
            LoadKeyType.ScrollToUnreadMessage.longValue -> {
                messagesListView.scrollToUnReadMessage()
            }
            LoadKeyType.ScrollToLastMessage.longValue -> {
                messagesListView.scrollToLastMessage()
            }
            LoadKeyType.ScrollToMessageById.longValue -> {
                messagesListView.scrollToMessage(loadKey)
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
                    messagesListView.setMessagesList(newMessages, response.loadKey == LoadKeyType.ScrollToLastMessage.longValue)
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

    lifecycleOwner.lifecycleScope.launch {
        loadMessagesFlow.collect(::initMessagesResponse)
    }

    /** Send pending markers when lifecycle come back onResume state*/
    lifecycleOwner.lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (pendingDisplayMsgIds.isNotEmpty()) {
                markMessageAsRead(*pendingDisplayMsgIds.toLongArray())
                pendingDisplayMsgIds.clear()
            }
        }
    }

    messagesListView.enableDisableClickActions(!replayInThread && channel.checkIsMemberInChannel(myId))

    lifecycleOwner.lifecycleScope.launch {
        onChannelUpdatedEventFlow.collect {
            channel = it
            messagesListView.setUnreadCount(it.unreadMessageCount.toInt())
        }
    }

    SceytSyncManager.syncChannelMessagesFinished.observe(lifecycleOwner, Observer {
        if (it.first.id == channel.id) {
            channel = it.first

            if (pinnedLastReadMessageId == 0L && channel.lastReadMessageId != 0L && channel.lastReadMessageId != channel.lastMessage?.id)
                pinnedLastReadMessageId = channel.lastReadMessageId

            lifecycleOwner.lifecycleScope.launch {
                val isLastDisplaying = messagesListView.isLastItemDisplaying()
                messagesListView.addNextPageMessages(mapToMessageListItem(data = it.second,
                    hasNext = false, hasPrev = false, messagesListView.getLastMessage()?.message))
                if (isLastDisplaying)
                    messagesListView.scrollToLastMessage()
            }
        }
    })

    onScrollToMessageLiveData.observe(lifecycleOwner, Observer {
        viewModelScope.launch(Dispatchers.Default) {
            channel.lastMessage?.id?.let { lastMsgId ->
                messagesListView.getMessageIndexedById(lastMsgId)?.let {
                    withContext(Dispatchers.Main) {
                        messagesListView.scrollToLastMessage()
                    }
                } ?: run {
                    loadNewestMessages(LoadKeyType.ScrollToLastMessage.longValue)
                    markChannelAsRead(channel.id)
                }
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

    onNewOutgoingMessageLiveData.observe(lifecycleOwner, Observer {
        if (hasNext || hasNextDb) return@Observer
        viewModelScope.launch {
            val initMessage = mapToMessageListItem(
                data = arrayListOf(it),
                hasNext = false,
                hasPrev = false,
                compareMessage = messagesListView.getLastMessage()?.message)

            messagesListView.addNewMessages(*initMessage.toTypedArray())
            messagesListView.updateViewState(PageState.Nothing)
        }
    })

    onChannelMemberAddedOrKickedLiveData.observe(lifecycleOwner, Observer {
        messagesListView.enableDisableClickActions(!replayInThread && it.checkIsMemberInChannel(myId))
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

    lifecycleOwner.lifecycleScope.launch {
        onNewMessageFlow.collect {
            if (hasNext || hasNextDb) return@collect
            val initMessage = mapToMessageListItem(
                data = arrayListOf(it),
                hasNext = false,
                hasPrev = false,
                compareMessage = messagesListView.getLastMessage()?.message)

            messagesListView.addNewMessages(*initMessage.toTypedArray())
            messagesListView.updateViewState(PageState.Nothing)
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
                ClearedHistory -> messagesListView.clearData()
                Left -> {
                    val leftUser = (it.channel as? GroupChannel)?.members?.getOrNull(0)?.id
                    if (leftUser == myId && (channel.channelType == ChannelTypeEnum.Direct || channel.channelType == ChannelTypeEnum.Private))
                        messagesListView.context.asActivity().finish()
                }
                Deleted -> messagesListView.context.asActivity().finish()
                else -> return@collect
            }
        }
    }

    lifecycleOwner.lifecycleScope.launch {
        ChannelsCash.channelDeletedFlow
            .filter { it == channel.id }
            .collect {
                messagesListView.context.asActivity().finish()
            }
    }

    lifecycleOwner.lifecycleScope.launch {
        onOutGoingMessageStatusFlow.collect {
            val sceytUiMessage = it.second
            sceytUiMessage.canShowAvatarAndName = shouldShowAvatarAndName(sceytUiMessage, messagesListView.getLastMessage()?.message)
            messagesListView.updateMessage(sceytUiMessage)
            messagesListView.sortMessages()
        }
    }

    joinLiveData.observe(lifecycleOwner, Observer {
        if (it is SceytResponse.Success) {
            it.data?.let { channel ->
                messagesListView.enableDisableClickActions(!replayInThread && channel.checkIsMemberInChannel(myId))
            }
        }
    })

    channelLiveData.observe(lifecycleOwner, Observer {
        if (it is SceytResponse.Success) {
            it.data?.let { channel ->
                messagesListView.enableDisableClickActions(!replayInThread && channel.checkIsMemberInChannel(myId))
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
                              replayInThreadMessage: SceytMessage?,
                              lifecycleOwner: LifecycleOwner) {

    messageInputView.setReplayInThreadMessageId(replayInThreadMessage?.id)
    messageInputView.checkIsParticipant(channel)
    getChannel(channel.id)

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
        messageInputView.message = it.toMessage()
    })

    onReplayMessageCommandLiveData.observe(lifecycleOwner, Observer {
        messageInputView.replayMessage(it.toMessage())
    })

    onChannelMemberAddedOrKickedLiveData.observe(lifecycleOwner, Observer {
        messageInputView.checkIsParticipant(channel)
    })

    lifecycleOwner.lifecycleScope.launch {
        onChannelEventFlow.collect {
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
                else -> return@collect
            }
        }
    }

    messageInputView.messageInputActionCallback = object : MessageInputView.MessageInputActionCallback {
        override fun sendMessage(message: Message) {
            messageInputView.cancelReplay {
                this@bind.sendMessage(message)
            }
        }

        override fun sendReplayMessage(message: Message, parent: Message?) {
            messageInputView.cancelReplay {
                this@bind.sendReplayMessage(message, parent)
            }
        }

        override fun sendEditMessage(message: SceytMessage) {
            this@bind.editMessage(message)
            messageInputView.cancelReplay()
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
                              replayInThreadMessage: SceytMessage?,
                              lifecycleOwner: LifecycleOwner) {

    if (replayInThread)
        headerView.setReplayMessage(channel, replayInThreadMessage)
    else
        headerView.setChannel(channel)

    if (channel is SceytDirectChannel)
        SceytPresenceChecker.addNewUserToPresenceCheck((channel as SceytDirectChannel).peer?.id)

    lifecycleOwner.lifecycleScope.launch {
        SceytPresenceChecker.onPresenceCheckUsersFlow.distinctUntilChanged().collect {
            headerView.onPresenceUpdate(it.map { presenceUser -> presenceUser.user })
        }
    }

    lifecycleOwner.lifecycleScope.launch {
        onChannelTypingEventFlow.collectLatest {
            headerView.onTyping(it)
        }
    }

    onChannelMemberAddedOrKickedLiveData.observe(lifecycleOwner, Observer {
        if (!replayInThread)
            headerView.setChannel(channel)
    })

    joinLiveData.observe(lifecycleOwner, Observer {
        if (!replayInThread)
            getChannel(channel.id)
    })

    channelLiveData.observe(lifecycleOwner, Observer {
        if (it is SceytResponse.Success) {
            channel = it.data ?: return@Observer
            if (!replayInThread)
                headerView.setChannel(it.data)
        }
    })
}

@Suppress("unused")
fun bindViewFromJava(viewModel: MessageListViewModel, messagesListView: MessagesListView, lifecycleOwner: LifecycleOwner) {
    viewModel.bind(messagesListView, lifecycleOwner)
}
