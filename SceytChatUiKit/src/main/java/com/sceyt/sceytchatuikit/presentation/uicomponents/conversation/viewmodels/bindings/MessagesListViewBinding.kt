package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.viewmodels.bindings

import androidx.lifecycle.*
import com.sceyt.chat.models.ConnectionState
import com.sceyt.chat.models.channel.GroupChannel
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MessageState
import com.sceyt.sceytchatuikit.SceytKitClient.myId
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
import com.sceyt.sceytchatuikit.data.models.getLoadKey
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.models.messages.SelfMarkerTypeEnum
import com.sceyt.sceytchatuikit.extensions.asActivity
import com.sceyt.sceytchatuikit.extensions.customToastSnackBar
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.ChannelsCache
import com.sceyt.sceytchatuikit.persistence.logics.messageslogic.MessagesCache
import com.sceyt.sceytchatuikit.presentation.common.checkIsMemberInChannel
import com.sceyt.sceytchatuikit.presentation.common.isPeerDeleted
import com.sceyt.sceytchatuikit.presentation.root.PageState
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.LoadKeyType
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.MessagesListView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.viewmodels.MessageListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun MessageListViewModel.bind(messagesListView: MessagesListView, lifecycleOwner: LifecycleOwner) {
    messageActionBridge.setMessagesListView(messagesListView)
    clearPreparingThumbs()

    val pendingDisplayMsgIds by lazy { mutableSetOf<Long>() }

    /** Send pending markers and pending messages when lifecycle come back onResume state,
     * Also set update current chat Id in ChannelsCache*/
    lifecycleOwner.lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (ConnectionEventsObserver.connectionState == ConnectionState.Connected) {
                if (pendingDisplayMsgIds.isNotEmpty()) {
                    markMessageAsRead(*pendingDisplayMsgIds.toLongArray())
                    pendingDisplayMsgIds.clear()
                }
                sendPendingMessages()
            }
        }
    }

    messagesListView.setOnWindowFocusChangeListener { hasFocus ->
        if (hasFocus)
            ChannelsCache.currentChannelId = channel.id
        else ChannelsCache.currentChannelId = null
    }

    if (channel.markedUsUnread)
        markChannelAsRead(channel.id)

    if (channel.lastReadMessageId == 0L || channel.lastMessage?.deliveryStatus == DeliveryStatus.Pending
            || channel.lastReadMessageId == channel.lastMessage?.id)
        loadPrevMessages(0, 0)
    else {
        pinnedLastReadMessageId = channel.lastReadMessageId
        loadNearMessages(pinnedLastReadMessageId, LoadKeyData(key = LoadKeyType.ScrollToUnreadMessage.longValue))
    }

    messagesListView.setUnreadCount(channel.unreadMessageCount.toInt())

    messagesListView.setNeedDownloadListener {
        needMediaInfo(it)
    }

    fun checkEnableDisableActions(channel: SceytChannel) {
        messagesListView.enableDisableClickActions(
            enabled = !replyInThread && channel.checkIsMemberInChannel() && !channel.isPeerDeleted()
                    && (channel.isGroup || (channel as? SceytDirectChannel)?.peer?.user?.blocked != true), false)
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
                    val newMessages = mapToMessageListItem(data = response.cacheData,
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

    ChannelsCache.channelDeletedFlow
        .filter { it == channel.id }
        .onEach {
            messagesListView.context.asActivity().finish()
        }.launchIn(lifecycleOwner.lifecycleScope)

    SceytSyncManager.syncChannelMessagesFinished.observe(lifecycleOwner) {
        if (it.first.id == channel.id) {
            channel = it.first

            if (pinnedLastReadMessageId == 0L && channel.lastReadMessageId != 0L && channel.lastReadMessageId != channel.lastMessage?.id)
                pinnedLastReadMessageId = channel.lastReadMessageId

            lifecycleOwner.lifecycleScope.launch {
                val currentMessages = messagesListView.getData()?.filterIsInstance<MessageListItem.MessageItem>()?.map { item -> item.message }
                        ?: arrayListOf()
                val newMessages = it.second.minus(currentMessages.toSet())
                if (newMessages.isNotEmpty()) {
                    val isLastDisplaying = messagesListView.isLastCompletelyItemDisplaying()
                    messagesListView.addNextPageMessages(mapToMessageListItem(data = newMessages,
                        hasNext = false, hasPrev = false, messagesListView.getLastMessage()?.message))
                    if (isLastDisplaying)
                        messagesListView.scrollToLastMessage()

                    messagesListView.sortMessages()
                }
            }
        }
    }

    ConnectionEventsObserver.onChangedConnectStatusFlow.onEach { stateData ->
        if (stateData.state == ConnectionState.Connected) {
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

    MessagesCache.messageUpdatedFlow.onEach { messages ->
        viewModelScope.launch(Dispatchers.Default) {
            messages.forEach {
                val message = initMessageInfoData(it)
                withContext(Dispatchers.Main) {
                    if (it.state == MessageState.Deleted || it.state == MessageState.Edited)
                        messagesListView.messageEditedOrDeleted(message)
                    else messagesListView.updateMessage(message)
                }
            }
        }
    }.launchIn(lifecycleOwner.lifecycleScope)

    MessagesCache.messagesClearedFlow.filter { it.first == channel.id }.onEach { pair ->
        val date = pair.second
        messagesListView.deleteAllMessagesBefore {
            it.getMessageCreatedAt() <= date && (it !is MessageListItem.MessageItem ||
                    it.message.deliveryStatus != DeliveryStatus.Pending)
        }
    }.launchIn(lifecycleOwner.lifecycleScope)

    loadMessagesFlow.onEach(::initMessagesResponse).launchIn(lifecycleOwner.lifecycleScope)

    onChannelUpdatedEventFlow.onEach {
        channel = it
        messagesListView.setUnreadCount(it.unreadMessageCount.toInt())
        checkEnableDisableActions(channel)
        if (channel.lastMessage == null)
            messagesListView.clearData()
    }.launchIn(lifecycleOwner.lifecycleScope)

    onScrollToLastMessageLiveData.observe(lifecycleOwner) {
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
    }

    onScrollToReplyMessageLiveData.observe(lifecycleOwner) {
        it.parent?.id?.let { parentId ->
            viewModelScope.launch(Dispatchers.Default) {
                messagesListView.getMessageIndexedById(parentId)?.let {
                    withContext(Dispatchers.Main) {
                        it.second.highlighted = true
                        messagesListView.scrollToPositionAndHighlight(it.first, true)
                    }
                } ?: run {
                    loadNearMessages(parentId, LoadKeyData(
                        key = LoadKeyType.ScrollToMessageById.longValue,
                        value = parentId))
                }
            }
        }
    }

    markAsReadLiveData.observe(lifecycleOwner, Observer {
        it.forEach { response ->
            if (response is SceytResponse.Success) {
                val data = response.data ?: return@Observer
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
        }
    })

    messageEditedDeletedLiveData.observe(lifecycleOwner) {
        if (it is SceytResponse.Success) {
            if (it.data?.deliveryStatus == DeliveryStatus.Pending ||
                    it.data?.deliveryStatus == DeliveryStatus.Failed) {
                messagesListView.messageEditedOrDeleted(it.data)
            }
        } else
            customToastSnackBar(messagesListView, it.message ?: "")
    }

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

    onChannelMemberAddedOrKickedLiveData.observe(lifecycleOwner) {
        checkEnableDisableActions(it)
    }

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

    onTransferUpdatedLiveData.observe(lifecycleOwner) { data ->
        lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            messagesListView.updateProgress(data)
        }
    }

    onChannelEventFlow.onEach {
        when (it.eventType) {
            ClearedHistory -> messagesListView.clearData()
            Left -> {
                val leftUser = (it.channel as? GroupChannel)?.lastActiveMembers?.getOrNull(0)?.id
                if (leftUser == myId && (channel.channelType == ChannelTypeEnum.Direct || channel.channelType == ChannelTypeEnum.Private))
                    messagesListView.context.asActivity().finish()
            }

            Deleted -> messagesListView.context.asActivity().finish()
            else -> return@onEach
        }
    }.launchIn(lifecycleOwner.lifecycleScope)

    onOutGoingMessageStatusFlow.onEach {
        messagesListView.updateMessagesStatusByTid(DeliveryStatus.Sent, it.second.tid)
    }.launchIn(lifecycleOwner.lifecycleScope)

    joinLiveData.observe(lifecycleOwner) {
        if (it is SceytResponse.Success) {
            it.data?.let { channel ->
                checkEnableDisableActions(channel)
            }
        }
    }

    channelLiveData.observe(lifecycleOwner) {
        if (it is SceytResponse.Success) {
            it.data?.let { channel ->
                checkEnableDisableActions(channel)
                messagesListView.setUnreadCount(channel.unreadMessageCount.toInt())
            }
        }
    }

    pageStateLiveData.observe(lifecycleOwner) {
        messagesListView.updateViewState(it, false)
    }

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

@Suppress("unused")
fun bindViewFromJava(viewModel: MessageListViewModel, messagesListView: MessagesListView, lifecycleOwner: LifecycleOwner) {
    viewModel.bind(messagesListView, lifecycleOwner)
}
