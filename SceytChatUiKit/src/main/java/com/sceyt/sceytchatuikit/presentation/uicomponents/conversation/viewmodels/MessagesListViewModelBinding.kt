package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.viewmodels

import androidx.lifecycle.*
import com.sceyt.chat.models.channel.GroupChannel
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.Message
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventEnum.*
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.PaginationResponse.LoadType.*
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.SceytDirectChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytGroupChannel
import com.sceyt.sceytchatuikit.data.models.getLoadKey
import com.sceyt.sceytchatuikit.data.models.getOffset
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch


fun MessageListViewModel.bind(messagesListView: MessagesListView, lifecycleOwner: LifecycleOwner) {
    val pendingDisplayMsgIds by lazy { arrayListOf<Long>() }

    if (lastReadMessageId == 0L || lastReadMessageId == channel.lastMessage?.id)
        loadPrevMessages(0, 0)
    else loadNearMessages(channel.lastReadMessageId, LoadKeyType.ScrollToUnreadMessage.longValue)

    messagesListView.setUnreadCount(channel.unreadMessageCount.toInt())

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
        ChannelsCash.channelUpdatedFlow.collect {
            channel = it
            messagesListView.setUnreadCount(it.unreadMessageCount.toInt())
        }
    }

    /*lifecycleOwner.lifecycleScope.launch {
        loadPrevMessagesFlow.collect { response ->
            when (response) {
                is PaginationResponse.DBResponse -> {
                    if (response.offset == 0) {
                        messagesListView.setMessagesList(mapToMessageListItem(data = response.data,
                            hasNext = response.hasNext,
                            hasPrev = response.hasPrev))
                    } else {
                        val lastMsg = if (response.hasPrev)
                            messagesListView.getMessageById(response.loadKey) else null
                        messagesListView.addPrevPageMessages(mapToMessageListItem(data = response.data,
                            hasNext = response.hasNext,
                            hasPrev = response.hasPrev,
                            lastMessage = lastMsg))
                    }
                }
                is PaginationResponse.ServerResponse2 -> {
                    when (response.data) {
                        is SceytResponse.Success -> {
                            if (response.hasDiff) {
                                val lastMsg = if (response.hasPrev)
                                    messagesListView.getMessageById(response.loadKey) else null
                                val newMessages = mapToMessageListItem(data = response.cashData,
                                    hasNext = response.hasNext,
                                    hasPrev = response.hasPrev,
                                    lastMessage = lastMsg)
                                messagesListView.setMessagesList(newMessages)
                            } else
                                if (response.hasPrev.not())
                                    messagesListView.hideLoadingPrev()
                        }
                        else -> if (!hasPrevDb) messagesListView.hideLoadingPrev()
                    }
                }
                else -> return@collect
            }
        }
    }*/

    fun checkIgnoreDatabasePagingResponse(response: PaginationResponse.DBResponse<*>): Boolean {
        if (response.data.isNotEmpty()) return false

        return when (response.loadType) {
            LoadPrev, LoadNewest -> hasPrev && loadingPrevItems.get()
            LoadNext -> hasNext && loadingNextItems.get()
            LoadNear -> hasPrev && loadingPrevItems.get() && !hasNext && loadingNextItems.get()
        }
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
        val loadKey = response.getLoadKey()
        if (loadKey == LoadKeyType.ScrollToUnreadMessage.longValue) {
            messagesListView.scrollToUnReadMessage()

        } else if (loadKey == LoadKeyType.ScrollToLastMessage.longValue) {
            messagesListView.scrollToLastMessage()

        } else if (response.getOffset() == -1) {//Todo scroll to current message
            messagesListView.scrollToMessage(loadKey)
        }
    }

    suspend fun initDbListWithDb(response: PaginationResponse.DBResponse<SceytMessage>): Boolean {
        if (checkIgnoreDatabasePagingResponse(response))
            return true

        if (response.offset == 0) {
            messagesListView.setMessagesList(mapToMessageListItem(data = response.data,
                hasNext = response.hasNext,
                hasPrev = response.hasPrev))

        } else {
            when (response.loadType) {
                LoadPrev -> {
                    val lastMsg = if (response.hasPrev)
                        messagesListView.getMessageById(response.loadKey) else null

                    messagesListView.addPrevPageMessages(mapToMessageListItem(data = response.data,
                        hasNext = response.hasNext, hasPrev = response.hasPrev, lastMessage = lastMsg))
                }
                LoadNext -> {
                    messagesListView.addNextPageMessages(mapToMessageListItem(data = response.data,
                        hasNext = response.hasNext, hasPrev = response.hasPrev/* compare message todo*/))
                }
                LoadNear, LoadNewest -> {
                    messagesListView.setMessagesList(mapToMessageListItem(data = response.data, hasNext = response.hasNext,
                        hasPrev = response.hasPrev), true)//force need review todo
                }
            }
        }
        checkToScrollAfterResponse(response)

        notifyPageStateWithResponse(SceytResponse.Success(null), response.offset > 0, response.data.isEmpty())
        return false
    }


    suspend fun initServerListWithDb(response: PaginationResponse.ServerResponse2<SceytMessage>) {
        when (response.data) {
            is SceytResponse.Success -> {
                val compareMessage: MessageListItem.MessageItem? = messagesListView.getMessageById(response.loadKey)//todo
                if (response.hasDiff) {
                    val newMessages = mapToMessageListItem(data = response.cashData,
                        hasNext = response.hasNext,
                        hasPrev = response.hasPrev,
                        lastMessage = compareMessage)
                    messagesListView.setMessagesList(newMessages, response.loadKey == LoadKeyType.ScrollToLastMessage.longValue)

                    checkToScrollAfterResponse(response)
                } else
                    checkToHildeLoadingMoreItemByLoadType(response.loadType)
            }
            is SceytResponse.Error -> checkToHildeLoadingMoreItemByLoadType(response.loadType)
        }
    }

    suspend fun initRsp(response: PaginationResponse<SceytMessage>) {
        when (response) {
            is PaginationResponse.DBResponse -> initDbListWithDb(response)
            is PaginationResponse.ServerResponse2 -> initServerListWithDb(response)
            else -> return
        }
    }

    lifecycleOwner.lifecycleScope.launch {
        loadMessagesFlow.collect { response ->
            initRsp(response)
        }
    }

    onScrollToMessageLiveData.observe(lifecycleOwner, Observer {
        channel.lastMessage?.id?.let { lastMsgId ->
            messagesListView.getMessageIndexedById(lastMsgId)?.let {
                messagesListView.scrollToLastMessage()
            } ?: run {
                loadNewestMessages(LoadKeyType.ScrollToLastMessage.longValue)
                markChannelAsRead(channel.id)
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
                lastMessage = messagesListView.getLastMessage())

            messagesListView.addNewMessages(*initMessage.toTypedArray())
            messagesListView.updateViewState(PageState.Nothing)
        }
    })

    onChannelMemberAddedOrKickedLiveData.observe(lifecycleOwner, Observer {
        messagesListView.enableDisableClickActions(!replayInThread && it.checkIsMemberInChannel(myId))
    })

    fun checkStateAndMarkAsRead(message: SceytMessage) {
        if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.RESUMED)
            markMessageAsDisplayed(message.id)
        else pendingDisplayMsgIds.add(message.id)
    }

    lifecycleOwner.lifecycleScope.launch {
        onNewMessageFlow.collect {
            if (hasNext || hasNextDb) return@collect
            val initMessage = mapToMessageListItem(
                data = arrayListOf(it),
                hasNext = false,
                hasPrev = false,
                lastMessage = messagesListView.getLastMessage())

            messagesListView.addNewMessages(*initMessage.toTypedArray())
            messagesListView.updateViewState(PageState.Nothing)

            checkStateAndMarkAsRead(it)
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

    /* pageStateLiveData.observe(lifecycleOwner) {
         if (it is PageState.StateError)
             customToastSnackBar(messageInputView, it.errorMessage.toString())
     }*/

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


fun bindViewFromJava(viewModel: MessageListViewModel, messagesListView: MessagesListView, lifecycleOwner: LifecycleOwner) {
    viewModel.bind(messagesListView, lifecycleOwner)
}
