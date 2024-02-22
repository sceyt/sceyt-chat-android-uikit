package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.viewmodels.bindings

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.ConnectionState
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.Marker
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.models.user.User
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.SceytKitClient.myId
import com.sceyt.sceytchatuikit.SceytSyncManager
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventEnum.ClearedHistory
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventEnum.Deleted
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventEnum.Left
import com.sceyt.sceytchatuikit.data.connectionobserver.ConnectionEventsObserver
import com.sceyt.sceytchatuikit.data.models.LoadKeyData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.PaginationResponse.LoadType.LoadNear
import com.sceyt.sceytchatuikit.data.models.PaginationResponse.LoadType.LoadNewest
import com.sceyt.sceytchatuikit.data.models.PaginationResponse.LoadType.LoadNext
import com.sceyt.sceytchatuikit.data.models.PaginationResponse.LoadType.LoadPrev
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.getLoadKey
import com.sceyt.sceytchatuikit.data.models.messages.MarkerTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.extensions.asActivity
import com.sceyt.sceytchatuikit.extensions.centerVisibleItemPosition
import com.sceyt.sceytchatuikit.extensions.customToastSnackBar
import com.sceyt.sceytchatuikit.extensions.findIndexed
import com.sceyt.sceytchatuikit.extensions.getChildTopByPosition
import com.sceyt.sceytchatuikit.extensions.isResumed
import com.sceyt.sceytchatuikit.extensions.isThePositionVisible
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.ChannelsCache
import com.sceyt.sceytchatuikit.persistence.logics.messageslogic.MessagesCache
import com.sceyt.sceytchatuikit.presentation.common.checkIsMemberInChannel
import com.sceyt.sceytchatuikit.presentation.common.getFirstMember
import com.sceyt.sceytchatuikit.presentation.common.isPeerDeleted
import com.sceyt.sceytchatuikit.presentation.common.isPublic
import com.sceyt.sceytchatuikit.presentation.root.PageState
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.LoadKeyType
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.MessagesListView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem.MessageItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.events.MessageCommandEvent
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.viewmodels.MessageListViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.ConversationInfoActivity
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig.MAX_MULTISELECT_MESSAGES_COUNT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections
import kotlin.collections.set


fun MessageListViewModel.bind(messagesListView: MessagesListView, lifecycleOwner: LifecycleOwner) {
    messageActionBridge.setMessagesListView(messagesListView)
    messagesListView.setMultiselectDestination(selectedMessagesMap)
    clearPreparingThumbs()

    val pendingDisplayMsgIds by lazy { Collections.synchronizedSet(mutableSetOf<Long>()) }
    val needToUpdateTransferAfterOnResume = hashMapOf<Long, TransferData>()

    /** Send pending markers, pending messages and update attachments transfer states when
     * lifecycle come back onResume state. */
    viewModelScope.launch {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (ConnectionEventsObserver.connectionState == ConnectionState.Connected) {
                if (pendingDisplayMsgIds.isNotEmpty()) {
                    markMessageAsRead(*pendingDisplayMsgIds.toLongArray())
                    pendingDisplayMsgIds.clear()
                }
                sendPendingMessages()
            }
            messagesListView.post {
                if (needToUpdateTransferAfterOnResume.isNotEmpty()) {
                    needToUpdateTransferAfterOnResume.values.forEach { data ->
                        messagesListView.updateProgress(data, true)
                    }
                    needToUpdateTransferAfterOnResume.clear()
                }
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
        loadPrevMessages(channel.lastMessage?.id ?: 0, 0)
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

    fun getCompareMessage(loadType: PaginationResponse.LoadType, proportion: List<SceytMessage>): SceytMessage? {
        if (proportion.isEmpty()) return null
        val proportionFirstId = proportion.first().id
        return when (loadType) {
            LoadNext, LoadNewest -> {
                (messagesListView.getData().lastOrNull {
                    it is MessageItem && it.message.id < proportionFirstId
                } as? MessageItem)?.message
            }

            LoadNear -> {
                (messagesListView.getData().firstOrNull {
                    it is MessageItem && it.message.id < proportionFirstId
                } as? MessageItem)?.message
            }

            LoadPrev -> null
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
        val loadKey = response.getLoadKey() ?: return
        when (loadKey.key) {
            LoadKeyType.ScrollToUnreadMessage.longValue -> {
                messagesListView.scrollToUnReadMessage()
            }

            LoadKeyType.ScrollToLastMessage.longValue -> {
                messagesListView.scrollToLastMessage()
            }

            LoadKeyType.ScrollToReplyMessage.longValue -> {
                messagesListView.scrollToMessage(loadKey.value, true, 200)
            }

            LoadKeyType.ScrollToSearchMessageBy.longValue -> {
                messagesListView.scrollToMessage(loadKey.value, true, 200) {
                    if (response is PaginationResponse.ServerResponse)
                        isSearchingMessageToScroll.set(false)
                }
            }
        }
    }

    suspend fun initPaginationDbResponse(response: PaginationResponse.DBResponse<SceytMessage>) {
        if (response.offset == 0) {
            messagesListView.setMessagesList(mapToMessageListItem(data = response.data,
                hasNext = response.hasNext, hasPrev = response.hasPrev), true)
        } else {
            when (response.loadType) {
                LoadPrev -> {
                    messagesListView.addPrevPageMessages(mapToMessageListItem(data = response.data,
                        hasNext = response.hasNext, hasPrev = response.hasPrev))
                }

                LoadNext -> {
                    val hasNext = checkMaybeHesNext(response)
                    val compareMessage = getCompareMessage(response.loadType, response.data)
                    messagesListView.addNextPageMessages(mapToMessageListItem(data = response.data,
                        hasNext = hasNext, hasPrev = response.hasPrev, compareMessage))
                }

                LoadNear -> {
                    val hasNext = checkMaybeHesNext(response)
                    messagesListView.setMessagesList(mapToMessageListItem(data = response.data, hasNext = hasNext,
                        hasPrev = response.hasPrev), true)
                }

                LoadNewest -> {
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
                    val dataToMap = if (response.dbResultWasEmpty) {
                        response.data.data ?: return
                    } else response.cacheData

                    val newMessages = mapToMessageListItem(data = dataToMap,
                        hasNext = response.hasNext,
                        hasPrev = response.hasPrev,
                        compareMessage = getCompareMessage(response.loadType, dataToMap))

                    if (response.dbResultWasEmpty) {
                        if (response.loadType == LoadNear)
                            messagesListView.setMessagesList(newMessages, response.loadKey?.key == LoadKeyType.ScrollToLastMessage.longValue)
                        else {
                            if (response.loadType == LoadNext || response.loadType == LoadNewest)
                                messagesListView.addNextPageMessages(newMessages)
                            else messagesListView.addPrevPageMessages(newMessages)
                        }
                    } else
                        messagesListView.setMessagesList(newMessages, response.loadKey?.key == LoadKeyType.ScrollToLastMessage.longValue)
                } else
                    checkToHildeLoadingMoreItemByLoadType(response.loadType)

                checkToScrollAfterResponse(response)

                loadPrevOffsetId = response.data.data?.firstOrNull()?.id ?: 0
                loadNextOffsetId = response.data.data?.lastOrNull()?.id ?: 0
            }

            is SceytResponse.Error -> {
                checkToHildeLoadingMoreItemByLoadType(response.loadType)

                // set isSearchingMessageToScroll value to false, to enable jumping to next search message
                if (response.loadKey?.value == LoadKeyType.ScrollToSearchMessageBy.longValue)
                    isSearchingMessageToScroll.set(false)
            }
        }
    }

    suspend fun initMessagesResponse(response: PaginationResponse<SceytMessage>) {
        when (response) {
            is PaginationResponse.DBResponse -> initPaginationDbResponse(response)
            is PaginationResponse.ServerResponse -> initPaginationServerResponse(response)
            else -> return
        }
    }

    fun syncNearCenterVisibleMessageIfNeeded() {
        if (!needSyncMessagesWhenScrollStateIdle) return
        val centerPosition = messagesListView.getMessagesRecyclerView().centerVisibleItemPosition()
        if (centerPosition == RecyclerView.NO_POSITION) return
        val item = messagesListView.getData().getOrNull(centerPosition)
        if (item is MessageItem && lastSyncCenterOffsetId != item.message.id) {
            lastSyncCenterOffsetId = item.message.id
            syncCenteredMessage(messageId = item.message.id)
        }
    }

    ChannelsCache.channelDeletedFlow
        .filter { it == channel.id }
        .onEach {
            messagesListView.context.asActivity().finish()
        }.launchIn(viewModelScope)

    ChannelsCache.pendingChannelCreatedFlow
        .filter { it.first == channel.id }
        .onEach {
            loadPrevMessages(channel.lastMessage?.id ?: 0, 0)
        }.launchIn(viewModelScope)

    SceytSyncManager.syncChannelMessagesFinished.observe(lifecycleOwner) {
        if (it.first.id == channel.id) {
            channel = it.first

            if (pinnedLastReadMessageId == 0L && channel.lastDisplayedMessageId != 0L && channel.lastDisplayedMessageId != channel.lastMessage?.id)
                pinnedLastReadMessageId = channel.lastDisplayedMessageId

            lifecycleOwner.lifecycleScope.launch {
                val currentMessages = messagesListView.getData().filterIsInstance<MessageItem>().map { item -> item.message }
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
                it is MessageItem && it.message.deliveryStatus == DeliveryStatus.Displayed
            } ?: messagesListView.getFirstMessageBy {
                // Next trying to get fist sent message
                it is MessageItem && it.message.deliveryStatus == DeliveryStatus.Sent
            } ?: messagesListView.getFirstMessageBy {
                // Next trying to get fist received message
                it is MessageItem && it.message.deliveryStatus == DeliveryStatus.Received
            }
            (message as? MessageItem)?.let {
                syncConversationMessagesAfter(it.message.id)
            }
            // Sync messages near center visible message
            syncNearCenterVisibleMessageIfNeeded()
        } else {
            lastSyncCenterOffsetId = 0L
            needSyncMessagesWhenScrollStateIdle = true
        }
    }.launchIn(viewModelScope)

    MessagesCache.messageUpdatedFlow.onEach { data ->
        lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            data.second.forEach {
                val message = initMessageInfoData(it)
                withContext(Dispatchers.Main) {
                    if (it.state == MessageState.Deleted || it.state == MessageState.Edited)
                        messagesListView.messageEditedOrDeleted(message)
                    else {
                        val foundToUpdate = messagesListView.updateMessage(message)
                        if (!foundToUpdate)
                            notFoundMessagesToUpdate[message.tid] = message
                    }
                }
            }
        }
    }.launchIn(viewModelScope)

    syncCenteredMessageLiveData.observe(lifecycleOwner) { data ->
        viewModelScope.launch(Dispatchers.Default) {
            if (data.missingMessages.isNotEmpty()) {
                val items = messagesListView.getData().toMutableList()
                items.findIndexed { it is MessageItem && it.message.id == data.centerMessageId }?.let {
                    val index = it.first

                    val topOffset = messagesListView.getMessagesRecyclerView().getChildTopByPosition(index)
                    val compareMessage = getCompareMessage(LoadNear, data.missingMessages)

                    items.addAll(mapToMessageListItem(data = data.missingMessages, hasNext = false, hasPrev = false,
                        compareMessage, ignoreUnreadMessagesSeparator = true))

                    items.sortBy { item -> item.getMessageCreatedAt() }
                    val filtered = mutableSetOf(*items.toTypedArray())

                    withContext(Dispatchers.Main) {
                        messagesListView.setMessagesList(filtered.toList())

                        val position = items.findIndexed { item ->
                            item is MessageItem && item.message.id == data.centerMessageId
                        }?.first ?: return@withContext

                        if (messagesListView.getMessagesRecyclerView().isThePositionVisible(position))
                            messagesListView.scrollToMessage(data.centerMessageId, false, topOffset)
                    }
                }
            }
        }
    }

    MessagesCache.messagesClearedFlow.filter { it.first == channel.id }.onEach { pair ->
        val date = pair.second
        messagesListView.deleteAllMessagesBefore {
            it.getMessageCreatedAt() <= date && (it !is MessageItem || it.message.deliveryStatus != DeliveryStatus.Pending)
        }
    }.launchIn(viewModelScope)

    loadMessagesFlow.onEach(::initMessagesResponse).launchIn(viewModelScope)

    onChannelUpdatedEventFlow.onEach {
        channel = it
        messagesListView.setUnreadCount(it.newMessageCount.toInt())
        checkEnableDisableActions(channel)
        if (it.lastMessage == null)
            messagesListView.clearData()
    }.launchIn(viewModelScope)

    onScrollToLastMessageLiveData.observe(lifecycleOwner) {
        viewModelScope.launch(Dispatchers.Default) {
            channel.lastMessage?.id?.let { lastMsgId ->
                messagesListView.getMessageIndexedById(lastMsgId)?.let {
                    withContext(Dispatchers.Main) {
                        messagesListView.scrollToLastMessage()
                        lifecycleOwner.lifecycleScope.launch {
                            delay(200)
                            syncNearCenterVisibleMessageIfNeeded()
                        }
                    }
                } ?: run {
                    loadPrevMessages(lastMsgId, 0, LoadKeyData(key = LoadKeyType.ScrollToLastMessage.longValue))
                    markChannelAsRead(channel.id)
                }
            }
        }
    }

    onScrollToReplyMessageLiveData.observe(lifecycleOwner) {
        val messageId = it.id
        viewModelScope.launch(Dispatchers.Default) {
            messagesListView.getMessageIndexedById(messageId)?.let {
                withContext(Dispatchers.Main) {
                    it.second.highligh = true
                    messagesListView.scrollToPosition(it.first, true, 200)
                }
            } ?: run {
                loadNearMessages(messageId, LoadKeyData(
                    key = LoadKeyType.ScrollToReplyMessage.longValue,
                    value = messageId), false)
            }
        }
    }

    onScrollToSearchMessageLiveData.observe(lifecycleOwner) {
        val messageId = it.id
        scrollToSearchMessageJob?.cancel()
        scrollToSearchMessageJob = viewModelScope.launch(Dispatchers.Default) {
            messagesListView.getMessageIndexedById(messageId)?.let {
                withContext(Dispatchers.Main) {
                    it.second.highligh = true
                    messagesListView.scrollToPosition(it.first, true, 200)
                    isSearchingMessageToScroll.set(false)
                }
            } ?: run {
                loadNearMessages(messageId, LoadKeyData(
                    key = LoadKeyType.ScrollToSearchMessageBy.longValue,
                    value = messageId), false)
            }
        }
    }

    markAsReadLiveData.observe(lifecycleOwner, Observer {
        it.forEach { response ->
            if (response is SceytResponse.Success) {
                val data = response.data ?: return@Observer
                viewModelScope.launch(Dispatchers.Default) {
                    val user = ClientWrapper.currentUser ?: User(myId ?: return@launch)
                    messagesListView.getData().forEach { listItem ->
                        (listItem as? MessageItem)?.message?.let { message ->
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

    checkMessageForceDeleteLiveData.observe(lifecycleOwner) {
        if (it is SceytResponse.Success) {
            if (it.data?.deliveryStatus == DeliveryStatus.Pending && it.data.state == MessageState.Deleted)
                messagesListView.forceDeleteMessageByTid(it.data.tid)
        }
    }

    suspend fun onMessage(message: SceytMessage) {
        if (hasNext || hasNextDb) return
        val initMessage = mapToMessageListItem(
            data = arrayListOf(message),
            hasNext = false,
            hasPrev = false,
            compareMessage = messagesListView.getLastMessage()?.message)

        messagesListView.addNewMessages(*initMessage.toTypedArray())
        messagesListView.updateViewState(PageState.Nothing)
    }

    onNewOutGoingMessageFlow.onEach {
        var message = it
        if (notFoundMessagesToUpdate.containsKey(message.tid))
            message = notFoundMessagesToUpdate.remove(message.tid) ?: it

        onMessage(message)
    }.launchIn(viewModelScope)

    onNewMessageFlow.onEach(::onMessage).launchIn(viewModelScope)

    onChannelMemberAddedOrKickedLiveData.observe(lifecycleOwner) {
        checkEnableDisableActions(it)
    }

    fun checkStateAndMarkAsRead(messageItem: MessageItem) {
        val message = messageItem.message
        if (!message.incoming || message.userMarkers?.any { it.name == MarkerTypeEnum.Displayed.value() } == true)
            return

        if (lifecycleOwner.isResumed()) {
            pendingDisplayMsgIds.add(message.id)
            sendDisplayedHelper.submit {
                markMessageAsRead(*(pendingDisplayMsgIds).toLongArray())
                pendingDisplayMsgIds.clear()
            }
        } else pendingDisplayMsgIds.add(message.id)
    }


    // todo reply in thread
    /*
    onNewThreadMessageFlow.onEach {
          messagesListView.updateReplyCount(it)
      }.launchIn(viewModelScope)

      onOutGoingThreadMessageFlow.onEach {
          messagesListView.newReplyMessage(it.parentMessage?.id)
      }.launchIn(viewModelScope)
  */
    onMessageStatusFlow.onEach {
        messagesListView.updateMessagesStatus(it.status, it.messageIds)
    }.launchIn(viewModelScope)

    onTransferUpdatedLiveData.asFlow().onEach {
        viewModelScope.launch(Dispatchers.Default) {
            if (lifecycleOwner.isResumed()) {
                messagesListView.updateProgress(it, false)
            } else
                needToUpdateTransferAfterOnResume[it.messageTid] = it
        }
    }.launchIn(viewModelScope)

    onChannelEventFlow.onEach {
        when (val event = it.eventType) {
            is ClearedHistory -> messagesListView.clearData()
            is Left -> {
                event.leftMembers.forEach { member ->
                    if (member.id == myId && !channel.isPublic())
                        messagesListView.context.asActivity().finish()
                }
            }

            is Deleted -> messagesListView.context.asActivity().finish()
            else -> return@onEach
        }
    }.launchIn(viewModelScope)

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
        if (it is PageState.StateError && messagesListView.getData().isEmpty())
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

            is MessageCommandEvent.SearchMessages -> {
                prepareToShowSearchMessage(event)
            }

            is MessageCommandEvent.OnMultiselectEvent -> {
                val wasSelected = selectedMessagesMap.containsKey(event.message.tid)

                if (!wasSelected && selectedMessagesMap.size >= MAX_MULTISELECT_MESSAGES_COUNT) {
                    val errorMessage = String.format(messagesListView.context.getString(R.string.sceyt_rich_max_message_select_count, MAX_MULTISELECT_MESSAGES_COUNT.toString()))
                    customToastSnackBar(messagesListView, errorMessage)
                    return@setMessageCommandEventListener
                }

                event.message.isSelected = !wasSelected
                messagesListView.updateMessageSelection(event.message)

                if (wasSelected) {
                    selectedMessagesMap.remove(event.message.tid)
                    if (selectedMessagesMap.isEmpty()) {
                        messageActionBridge.hideMessageActions()
                        messagesListView.cancelMultiSelectMode()
                    } else {
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
                            ConversationInfoActivity.launch(event.view.context, response.data)
                        }
                }
            }
        }
    }

    messagesListView.setMessageReactionsEventListener {
        onReactionEvent(it)
    }

    messagesListView.setScrollStateChangeListener {
        if (it == RecyclerView.SCROLL_STATE_IDLE)
            syncNearCenterVisibleMessageIfNeeded()
    }

    messagesListView.setNeedLoadPrevMessagesListener { offset, message ->
        if (canLoadPrev()) {
            val messageId = (message as? MessageItem)?.message?.id ?: 0
            loadPrevMessages(messageId, offset)

            if (messageId != loadPrevOffsetId)
                needSyncMessagesWhenScrollStateIdle = true
        }
    }

    messagesListView.setNeedLoadNextMessagesListener { offset, message ->
        if (canLoadNext()) {
            val messageId = (message as? MessageItem)?.message?.id ?: 0
            loadNextMessages(messageId, offset)

            if (messageId != loadNextOffsetId)
                needSyncMessagesWhenScrollStateIdle = true
        }
    }

    messagesListView.setMessageDisplayedListener {
        if (it is MessageItem)
            checkStateAndMarkAsRead(it)
    }
}

@Suppress("unused")
fun bindViewFromJava(viewModel: MessageListViewModel, messagesListView: MessagesListView, lifecycleOwner: LifecycleOwner) {
    viewModel.bind(messagesListView, lifecycleOwner)
}
