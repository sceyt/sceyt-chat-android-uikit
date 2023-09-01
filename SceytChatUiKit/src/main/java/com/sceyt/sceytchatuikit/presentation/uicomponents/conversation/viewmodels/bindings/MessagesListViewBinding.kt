package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.viewmodels.bindings

import androidx.lifecycle.*
import com.sceyt.chat.models.ConnectionState
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.Marker
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.models.user.User
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.sceytchatuikit.SceytKitClient.myId
import com.sceyt.sceytchatuikit.SceytSyncManager
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventEnum.*
import com.sceyt.sceytchatuikit.data.connectionobserver.ConnectionEventsObserver
import com.sceyt.sceytchatuikit.data.models.LoadKeyData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.PaginationResponse.LoadType.*
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.getLoadKey
import com.sceyt.sceytchatuikit.data.models.messages.MarkerTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.extensions.asActivity
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.ChannelsCache
import com.sceyt.sceytchatuikit.persistence.logics.messageslogic.MessagesCache
import com.sceyt.sceytchatuikit.presentation.common.checkIsMemberInChannel
import com.sceyt.sceytchatuikit.presentation.common.getFirstMember
import com.sceyt.sceytchatuikit.presentation.common.isPeerDeleted
import com.sceyt.sceytchatuikit.presentation.common.isPublic
import com.sceyt.sceytchatuikit.presentation.root.PageState
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.LoadKeyType
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.MessagesListView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.events.MessageCommandEvent
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.viewmodels.MessageListViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.ConversationInfoActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections

fun MessageListViewModel.bind(messagesListView: MessagesListView, lifecycleOwner: LifecycleOwner) {
    messageActionBridge.setMessagesListView(messagesListView)
    messagesListView.setMultiselectDestination(selectedMessagesMap)
    clearPreparingThumbs()

    val pendingDisplayMsgIds by lazy { Collections.synchronizedSet(mutableSetOf<Long>()) }

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

    if (channel.unread)
        markChannelAsRead(channel.id)

    // If userRole is null or empty, get channel again to update channel
    if (channel.userRole.isNullOrEmpty())
        getChannel(channel.id)

    if (channel.lastDisplayedMessageId == 0L || channel.lastMessage?.deliveryStatus == DeliveryStatus.Pending
            || channel.lastDisplayedMessageId == channel.lastMessage?.id)
        loadPrevMessages(0, 0)
    else {
        pinnedLastReadMessageId = channel.lastDisplayedMessageId
        loadNearMessages(pinnedLastReadMessageId, LoadKeyData(key = LoadKeyType.ScrollToUnreadMessage.longValue), false)
    }

    messagesListView.setUnreadCount(channel.newMessageCount.toInt())

    messagesListView.setNeedDownloadListener {
        needMediaInfo(it)
    }

    fun checkEnableDisableActions(channel: SceytChannel) {
        messagesListView.enableDisableActions(
            enabled = !replyInThread && channel.checkIsMemberInChannel() && !channel.isPeerDeleted()
                    && (channel.isGroup || channel.getFirstMember()?.user?.blocked != true), false)
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

    ChannelsCache.pendingChannelCreatedFlow
        .filter { it.first == channel.id }
        .onEach {
            loadPrevMessages(0, 0)
        }.launchIn(lifecycleOwner.lifecycleScope)

    SceytSyncManager.syncChannelMessagesFinished.observe(lifecycleOwner) {
        if (it.first.id == channel.id) {
            channel = it.first

            if (pinnedLastReadMessageId == 0L && channel.lastDisplayedMessageId != 0L && channel.lastDisplayedMessageId != channel.lastMessage?.id)
                pinnedLastReadMessageId = channel.lastDisplayedMessageId

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
                // First trying to get last displayed message
                it is MessageListItem.MessageItem && it.message.deliveryStatus == DeliveryStatus.Displayed
            } ?: messagesListView.getFirstMessageBy {
                // Next trying to get fist sent message
                it is MessageListItem.MessageItem && it.message.deliveryStatus == DeliveryStatus.Sent
            } ?: messagesListView.getFirstMessageBy {
                // Next trying to get fist received message
                it is MessageListItem.MessageItem && it.message.deliveryStatus == DeliveryStatus.Received
            }
            (message as? MessageListItem.MessageItem)?.let {
                syncConversationMessagesAfter(it.message.id)
            }
        }
    }.launchIn(lifecycleOwner.lifecycleScope)

    MessagesCache.messageUpdatedFlow.onEach { data ->
        viewModelScope.launch(Dispatchers.Default) {
            data.second.forEach {
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
        messagesListView.setUnreadCount(it.newMessageCount.toInt())
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
        it.parentMessage?.id?.let { parentId ->
            viewModelScope.launch(Dispatchers.Default) {
                messagesListView.getMessageIndexedById(parentId)?.let {
                    withContext(Dispatchers.Main) {
                        it.second.highlighted = true
                        messagesListView.scrollToPositionAndHighlight(it.first, true)
                    }
                } ?: run {
                    loadNearMessages(parentId, LoadKeyData(
                        key = LoadKeyType.ScrollToMessageById.longValue,
                        value = parentId), true)
                }
            }
        }
    }

    markAsReadLiveData.observe(lifecycleOwner, Observer {
        it.forEach { response ->
            if (response is SceytResponse.Success) {
                val data = response.data ?: return@Observer
                viewModelScope.launch(Dispatchers.Default) {
                    val user = ClientWrapper.currentUser ?: User(myId ?: return@launch)
                    messagesListView.getData()?.forEach { listItem ->
                        (listItem as? MessageListItem.MessageItem)?.message?.let { message ->
                            if (data.messageIds.contains(message.id)) {
                                message.userMarkers = message.userMarkers?.toMutableSet()?.apply {
                                    add(Marker(message.id, user, data.name, data.createdAt))
                                }?.toTypedArray()
                            }
                        }
                    }
                }
            }
        }
    })

    messageForceDeleteLiveData.observe(lifecycleOwner) {
        if (it is SceytResponse.Success) {
            if (it.data?.deliveryStatus == DeliveryStatus.Pending && it.data.state == MessageState.Deleted)
                messagesListView.forceDeleteMessageByTid(it.data.tid)
        }
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
            if (!message.incoming || message.userMarkers?.any { it.name == MarkerTypeEnum.Displayed.value() } == true)
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

    // todo reply in thread
    /*  onNewThreadMessageFlow.onEach {
          messagesListView.updateReplyCount(it)
      }.launchIn(lifecycleOwner.lifecycleScope)

      onOutGoingThreadMessageFlow.onEach {
          messagesListView.newReplyMessage(it.parentMessage?.id)
      }.launchIn(lifecycleOwner.lifecycleScope)
  */
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
                val leftUser = it.channel?.members?.getOrNull(0)?.id
                if (leftUser == myId && !channel.isPublic())
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
                messagesListView.setUnreadCount(channel.newMessageCount.toInt())
            }
        }
    }

    pageStateLiveData.observe(lifecycleOwner) {
        if (it is PageState.StateError && messagesListView.getData().isNullOrEmpty())
            messagesListView.updateViewState(PageState.StateEmpty())
        else
            messagesListView.updateViewState(it, false)
    }

    messagesListView.setMessageCommandEventListener {
        when (val event = it) {
            is MessageCommandEvent.DeleteMessage -> {
                deleteMessages(event.message.toList(), event.onlyForMe)
            }

            is MessageCommandEvent.EditMessage -> {
                prepareToEditMessage(event.message)
            }

            is MessageCommandEvent.ShowHideMessageActions -> {
                prepareToShowMessageActions(event)
            }

            is MessageCommandEvent.OnMultiselectEvent -> {
                val wasSelected = selectedMessagesMap.containsKey(event.message.tid)

                event.message.isSelected = !wasSelected
                messagesListView.updateMessageSelection(event.message)

                if (wasSelected) {
                    selectedMessagesMap.remove(event.message.tid)
                    if (selectedMessagesMap.isEmpty()) {
                        messageActionBridge.hideMessageActions()
                        messagesListView.cancelMultiSelectMode()
                    }else{
                        messageActionBridge.showMessageActions(*selectedMessagesMap.values.toTypedArray())
                    }
                } else {
                    selectedMessagesMap[event.message.tid] = event.message
                    messageActionBridge.showMessageActions(*selectedMessagesMap.values.toTypedArray())
                    messagesListView.setMultiSelectableMode()
                }
            }

            is MessageCommandEvent.OnCancelMultiselectEvent -> {
                selectedMessagesMap.clear()
                messagesListView.cancelMultiSelectMode()
            }

            is MessageCommandEvent.Reply -> {
                prepareToReplyMessage(event.message)
            }

            is MessageCommandEvent.ScrollToDown -> {
                prepareToScrollToNewMessage()
            }

            is MessageCommandEvent.ScrollToReplyMessage -> {
                prepareToScrollToReplyMessage(event.message)
            }

            is MessageCommandEvent.AttachmentLoaderClick -> {
                viewModelScope.launch(Dispatchers.IO) {
                    prepareToPauseOrResumeUpload(event.item)
                }
            }

            is MessageCommandEvent.UserClick -> {
                if (event.userId == myId) return@setMessageCommandEventListener
                viewModelScope.launch(Dispatchers.IO) {
                    val user = persistenceUsersMiddleWare.getUserDbById(event.userId)
                            ?: User(event.userId)
                    val response = persistenceChanelMiddleWare.findOrCreateDirectChannel(user)
                    if (response is SceytResponse.Success)
                        response.data?.let {
                            ConversationInfoActivity.newInstance(event.view.context, response.data, true)
                        }
                }
            }
        }
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
