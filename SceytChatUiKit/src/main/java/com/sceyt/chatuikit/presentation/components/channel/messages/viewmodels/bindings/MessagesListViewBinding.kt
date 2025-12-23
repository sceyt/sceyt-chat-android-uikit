package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.bindings

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.ConnectionState
import com.sceyt.chat.models.message.DeleteMessageType
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.managers.channel.event.ChannelActionEvent.ClearedHistory
import com.sceyt.chatuikit.data.managers.channel.event.ChannelActionEvent.Deleted
import com.sceyt.chatuikit.data.managers.channel.event.ChannelActionEvent.Left
import com.sceyt.chatuikit.data.managers.connection.ConnectionEventManager
import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNear
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNewest
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNext
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadPrev
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.channels.CreateChannelData
import com.sceyt.chatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.getLoadKey
import com.sceyt.chatuikit.data.models.messages.MarkerType
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.data.models.messages.SceytMarker
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.data.models.onSuccessNotNull
import com.sceyt.chatuikit.extensions.asActivity
import com.sceyt.chatuikit.extensions.centerVisibleItemPosition
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.extensions.findIndexed
import com.sceyt.chatuikit.extensions.getChildTopByPosition
import com.sceyt.chatuikit.extensions.getString
import com.sceyt.chatuikit.extensions.isResumed
import com.sceyt.chatuikit.extensions.isThePositionVisible
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.extensions.checkIsMemberInChannel
import com.sceyt.chatuikit.persistence.extensions.getPeer
import com.sceyt.chatuikit.persistence.extensions.isPublic
import com.sceyt.chatuikit.persistence.extensions.safeResume
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferHelper
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelUpdatedType
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelsCache
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import com.sceyt.chatuikit.presentation.components.channel.messages.MessagesListView
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem.MessageItem
import com.sceyt.chatuikit.presentation.components.channel.messages.events.MessageCommandEvent
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.MessageListViewModel
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelInfoActivity
import com.sceyt.chatuikit.presentation.components.poll_results.PollResultsActivity
import com.sceyt.chatuikit.presentation.extensions.isNotPending
import com.sceyt.chatuikit.presentation.extensions.isPending
import com.sceyt.chatuikit.presentation.root.PageState
import com.sceyt.chatuikit.services.SceytSyncManager
import com.sceyt.chatuikit.styles.extensions.messages_list.setEmptyStateForSelfChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val TAG = "MessagesListViewBinding"

@JvmName("bind")
fun MessageListViewModel.bind(messagesListView: MessagesListView, lifecycleOwner: LifecycleOwner) {
    val lifecycleScope = lifecycleOwner.lifecycleScope
    messageActionBridge.setMessagesListView(messagesListView)
    messagesListView.setMultiselectDestination(selectedMessagesMap)
    if (channel.isSelf) {
        messagesListView.setEmptyStateForSelfChannel()
    }

    clearPreparingThumbs()

    /** Send pending markers, pending messages and update attachments transfer states when
     * lifecycle come back onResume state. */
    viewModelScope.launch {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (ConnectionEventManager.connectionState == ConnectionState.Connected) {
                if (pendingDisplayMsgIds.isNotEmpty()) {
                    markMessageAsRead(*pendingDisplayMsgIds.toLongArray())
                    pendingDisplayMsgIds.clear()
                }
                sendPendingMessages()
            }
            messagesListView.post {
                if (needToUpdateTransferAfterOnResume.isNotEmpty()) {
                    needToUpdateTransferAfterOnResume.values.forEach { data ->
                        lifecycleOwner.lifecycleScope.launch {
                            messagesListView.updateProgress(data, true)
                        }
                    }
                    needToUpdateTransferAfterOnResume.clear()
                }
            }
        }
    }

    if (channel.unread)
        markChannelAsRead(channel.id)

    // Cancel notification for current channel
    SceytChatUIKit.notifications.pushNotification.notificationHandler.cancelNotification(
        notificationId = channel.id.toInt()
    )

    // If userRole is null or empty, get channel again to update channel
    if (channel.userRole.isNullOrEmpty())
        getChannel(channel.id)

    val lastMessage = channel.lastMessage
    val lastDisplayedMessageId = channel.lastDisplayedMessageId
    val lastMessageId = lastMessage?.id ?: 0
    when {
        lastDisplayedMessageId == 0L || lastMessage?.isPending() == true
                || lastDisplayedMessageId == lastMessageId -> {
            loadPrevMessages(lastMessageId, 0)
        }

        // If last displayed message is less than last message id, this means some messages were deleted.
        // Load previous messages from last displayed message id to detect deleted messages and remove them.
        lastDisplayedMessageId >= lastMessageId -> {
            loadPrevMessages(lastDisplayedMessageId, 0)
        }

        else -> {
            pinnedLastReadMessageId = lastDisplayedMessageId
            loadNearMessages(
                messageId = pinnedLastReadMessageId,
                loadKey = LoadKeyData(key = LoadKeyType.ScrollToUnreadMessage.longValue),
                ignoreServer = false
            )
        }
    }

    fun setUnreadCounts(channel: SceytChannel) {
        messagesListView.setUnreadMessagesCount(channel.newMessageCount)
        messagesListView.setUnreadMentionsCount(channel.newMentionCount)
    }

    fun checkEnableDisableActions(channel: SceytChannel) {
        messagesListView.setActionsEnabled(
            enabled = !replyInThread && channel.checkIsMemberInChannel() &&
                    (channel.isGroup || channel.getPeer()?.user?.blocked != true), false
        )
    }

    checkEnableDisableActions(channel)
    setUnreadCounts(channel)

    suspend fun getCompareMessage(
        loadType: PaginationResponse.LoadType,
        proportion: List<SceytMessage>,
    ): SceytMessage? = withContext(Dispatchers.Default) {
        if (proportion.isEmpty()) return@withContext null
        val proportionFirstId = proportion.first().id
        return@withContext when (loadType) {
            LoadNext, LoadNewest, LoadNear -> {
                (messagesListView.getData().lastOrNull {
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

            LoadKeyType.ScrollToMessageBy.longValue -> {
                messagesListView.scrollToMessage(
                    messageId = loadKey.value,
                    highlight = true,
                    offset = 200,
                    onCompleted = { found ->
                        if (response !is PaginationResponse.ServerResponse) {
                            return@scrollToMessage
                        }
                        isPreparingToScrollToMessage.set(false)
                        if (!found) {
                            SceytLog.w(
                                TAG,
                                "Called load near messages in channelId: ${channel.id} for scroll to message id: ${loadKey.value}, but message not found in server response." +
                                        " Resetting isPreparingToScrollToMessage to false to avoid infinite waiting."
                            )
                        }
                    })
            }
        }
    }

    suspend fun initPaginationDbResponse(response: PaginationResponse.DBResponse<SceytMessage>) {
        val enableDateSeparator = messagesListView.style.enableDateSeparator
        if (response.offset == 0) {
            messagesListView.setMessagesList(
                data = mapToMessageListItem(
                    data = response.data,
                    hasNext = response.hasNext,
                    hasPrev = response.hasPrev,
                    enableDateSeparator = enableDateSeparator
                ),
                lifecycleScope = lifecycleScope,
                force = true
            )
        } else {
            when (response.loadType) {
                LoadPrev -> {
                    messagesListView.addPrevPageMessages(
                        mapToMessageListItem(
                            data = response.data,
                            hasNext = response.hasNext,
                            hasPrev = response.hasPrev,
                            enableDateSeparator = enableDateSeparator
                        ),
                        lifecycleScope = lifecycleScope,
                    )
                }

                LoadNext -> {
                    val hasNext = checkMaybeHesNext(response)
                    val compareMessage = getCompareMessage(response.loadType, response.data)
                    messagesListView.addNextPageMessages(
                        mapToMessageListItem(
                            data = response.data,
                            hasNext = hasNext,
                            hasPrev = response.hasPrev,
                            compareMessage = compareMessage,
                            enableDateSeparator = enableDateSeparator
                        ),
                        lifecycleScope = lifecycleScope,
                    )
                }

                LoadNear -> {
                    val hasNext = checkMaybeHesNext(response)
                    messagesListView.setMessagesList(
                        data = mapToMessageListItem(
                            data = response.data,
                            hasNext = hasNext,
                            hasPrev = response.hasPrev,
                            enableDateSeparator = enableDateSeparator
                        ),
                        lifecycleScope = lifecycleScope,
                        force = true
                    )
                }

                LoadNewest -> {
                    messagesListView.setMessagesList(
                        data = mapToMessageListItem(
                            data = response.data,
                            hasNext = response.hasNext,
                            hasPrev = response.hasPrev,
                            enableDateSeparator = enableDateSeparator
                        ),
                        lifecycleScope = lifecycleScope,
                        force = true
                    )
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

                    val newMessages = mapToMessageListItem(
                        data = dataToMap,
                        hasNext = response.hasNext,
                        hasPrev = response.hasPrev,
                        compareMessage = getCompareMessage(response.loadType, dataToMap),
                        enableDateSeparator = messagesListView.style.enableDateSeparator
                    )

                    if (response.dbResultWasEmpty) {
                        if (response.loadType == LoadNear)
                            messagesListView.setMessagesList(newMessages, lifecycleScope, true)
                        else {
                            if (response.loadType == LoadNext || response.loadType == LoadNewest)
                                messagesListView.addNextPageMessages(newMessages, lifecycleScope)
                            else messagesListView.addPrevPageMessages(newMessages, lifecycleScope)
                        }
                    } else
                        messagesListView.setMessagesList(
                            data = newMessages,
                            lifecycleScope = lifecycleScope,
                            force = response.loadKey?.key == LoadKeyType.ScrollToLastMessage.longValue
                        )
                } else
                    checkToHildeLoadingMoreItemByLoadType(response.loadType)

                checkToScrollAfterResponse(response)

                loadPrevOffsetId = response.data.data?.firstOrNull()?.id ?: 0
                loadNextOffsetId = response.data.data?.lastOrNull()?.id ?: 0
            }

            is SceytResponse.Error -> {
                checkToHildeLoadingMoreItemByLoadType(response.loadType)

                // set isSearchingMessageToScroll value to false, to enable jumping to next message
                if (response.loadKey?.value == LoadKeyType.ScrollToMessageBy.longValue)
                    isPreparingToScrollToMessage.set(false)
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
        if (!needSyncMessagesWhenScrollStateIdle || loadingFromServer) return
        val centerPosition = messagesListView.getMessagesRecyclerView().centerVisibleItemPosition()
        if (centerPosition == RecyclerView.NO_POSITION) return
        val item = messagesListView.getData().getOrNull(centerPosition)
        val messageId = item?.getMessageId() ?: return
        if (lastSyncCenterOffsetId != messageId) {
            lastSyncCenterOffsetId = messageId
            syncCenteredMessage(messageId = messageId)
        }
    }

    fun onMessageDisplayed(message: SceytMessage) {
        if (!message.incoming || message.userMarkers?.any { it.name == MarkerType.Displayed.value } == true)
            return

        if (lifecycleOwner.isResumed()) {
            pendingDisplayMsgIds.add(message.id)
            sendDisplayedHelper.submit {
                markMessageAsRead(*(pendingDisplayMsgIds).toLongArray())
                pendingDisplayMsgIds.clear()
            }
        } else pendingDisplayMsgIds.add(message.id)
    }

    ChannelsCache.channelsDeletedFlow
        .filter { it.contains(channel.id) }
        .onEach {
            messagesListView.context.asActivity().finish()
        }.launchIn(lifecycleOwner.lifecycleScope)

    ChannelsCache.channelUpdatedFlow
        .filter { it.channel.id == channel.id && it.eventType == ChannelUpdatedType.ClearedHistory }
        .onEach {
            messagesListView.clearData()
        }
        .launchIn(lifecycleOwner.lifecycleScope)

    SceytSyncManager.syncChannelMessagesFinished
        .filter { it.first.id == channel.id }
        .onEach { (syncChannel, messages) ->
            if (syncChannel.id == channel.id) {
                if (pinnedLastReadMessageId == 0L && syncChannel.lastDisplayedMessageId != 0L
                    && syncChannel.lastDisplayedMessageId != syncChannel.lastMessage?.id
                )
                    pinnedLastReadMessageId = syncChannel.lastDisplayedMessageId

                lifecycleOwner.lifecycleScope.launch {
                    val currentMessages = messagesListView.getData()
                        .filterIsInstance<MessageItem>()
                        .map { item -> item.message }
                    val newMessages = messages.minus(currentMessages.toSet())
                    if (newMessages.isNotEmpty()) {
                        val isLastDisplaying = messagesListView.isLastCompletelyItemDisplaying()
                        messagesListView.addNextPageMessages(
                            mapToMessageListItem(
                                data = newMessages,
                                hasNext = false,
                                hasPrev = false,
                                compareMessage = messagesListView.getLastMessage()?.message,
                                enableDateSeparator = messagesListView.style.enableDateSeparator
                            ),
                            lifecycleScope = lifecycleScope,
                        )
                        if (isLastDisplaying)
                            messagesListView.scrollToLastMessage()

                        messagesListView.sortMessages()
                    }
                }
            }
        }
        .launchIn(lifecycleOwner.lifecycleScope)

    connectionLogic.allPendingEventsSentFlow
        .onEach {
            // Sync messages near center visible message
            syncNearCenterVisibleMessageIfNeeded()
        }
        .launchIn(viewModelScope)

    ConnectionEventManager.onChangedConnectStatusFlow
        .distinctUntilChanged()
        .onEach { stateData ->
            viewModelScope.launch(Dispatchers.IO) {
                if (stateData.state == ConnectionState.Connected) {
                    val message = messagesListView.getLastMessageBy {
                        // First trying to get last displayed message
                        it is MessageItem && it.message.deliveryStatus == MessageDeliveryStatus.Displayed
                    } ?: messagesListView.getFirstMessageBy {
                        // Next trying to get fist sent message
                        it is MessageItem && it.message.deliveryStatus == MessageDeliveryStatus.Sent
                    } ?: messagesListView.getFirstMessageBy {
                        // Next trying to get fist received message
                        it is MessageItem && it.message.deliveryStatus == MessageDeliveryStatus.Received
                    }
                    (message as? MessageItem)?.let {
                        syncManager.syncConversationMessagesAfter(conversationId, it.message.id)
                    }
                } else {
                    lastSyncCenterOffsetId = 0L
                    needSyncMessagesWhenScrollStateIdle = true
                }
            }
        }.launchIn(lifecycleOwner.lifecycleScope)

    syncCenteredMessageLiveData.observe(lifecycleOwner) { data ->
        lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            if (data.missingMessages.isNotEmpty()) {
                val items = messagesListView.getData().toMutableList()
                items.findIndexed {
                    it is MessageItem && it.message.id == data.centerMessageId
                }?.let { (index) ->
                    val topOffset =
                        messagesListView.getMessagesRecyclerView().getChildTopByPosition(index)
                    val compareMessage = getCompareMessage(LoadNear, data.missingMessages)

                    items.addAll(
                        mapToMessageListItem(
                            data = data.missingMessages, hasNext = false, hasPrev = false,
                            compareMessage, ignoreUnreadMessagesSeparator = true,
                            enableDateSeparator = messagesListView.style.enableDateSeparator
                        )
                    )

                    items.sortBy { item -> item.getMessageCreatedAt() }
                    val filtered = mutableSetOf(*items.toTypedArray())

                    withContext(Dispatchers.Main) {
                        messagesListView.setMessagesList(filtered.toList(), lifecycleScope)

                        val (position) = items.findIndexed { item ->
                            item is MessageItem && item.message.id == data.centerMessageId
                        } ?: return@withContext

                        if (messagesListView.getMessagesRecyclerView()
                                .isThePositionVisible(position)
                        )
                            messagesListView.scrollToMessage(data.centerMessageId, false, topOffset)
                    }
                }
            }
        }
    }

    MessagesCache.messagesClearedFlow
        .filter { (channelId, _) -> channelId == channel.id }
        .onEach { (_, date) ->
            messagesListView.deleteAllMessagesBefore {
                it.getMessageCreatedAt() <= date && (it !is MessageItem || it.message.isNotPending())
            }
        }.launchIn(lifecycleOwner.lifecycleScope)

    MessagesCache.messagesHardDeletedFlow
        .filter { (channelId, _) -> channelId == channel.id }
        .onEach { (_, tIds) ->
            messagesListView.forceDeleteMessageByTid(*tIds.toLongArray())
        }.launchIn(lifecycleOwner.lifecycleScope)

    loadMessagesFlow
        .onEach(::initMessagesResponse)
        .launchIn(lifecycleOwner.lifecycleScope)

    onChannelUpdatedEventFlow.onEach { channel ->
        setUnreadCounts(channel)
        checkEnableDisableActions(channel)
    }.launchIn(lifecycleOwner.lifecycleScope)

    onScrollToLastMessageLiveData.observe(lifecycleOwner) { lastMessage ->
        viewModelScope.launch(Dispatchers.Default) {
            val lastMsgId = lastMessage?.id ?: return@launch
            messagesListView.getMessageIndexedById(lastMsgId)?.let {
                withContext(Dispatchers.Main) {
                    messagesListView.scrollToLastMessage()
                    lifecycleOwner.lifecycleScope.launch {
                        delay(200)
                        syncNearCenterVisibleMessageIfNeeded()
                    }
                }
            } ?: run {
                loadPrevMessages(
                    lastMessageId = lastMsgId,
                    offset = 0,
                    loadKey = LoadKeyData(key = LoadKeyType.ScrollToLastMessage.longValue)
                )
                markChannelAsRead(channel.id)
            }
        }
    }

    onScrollToReplyMessageLiveData.observe(lifecycleOwner) {
        val messageId = it.id
        messagesListView.scrollToMessage(
            messageId = messageId,
            offset = 200,
            highlight = true,
            onCompleted = { found ->
                if (!found) {
                    loadNearMessages(
                        messageId = messageId,
                        loadKey = LoadKeyData(
                            key = LoadKeyType.ScrollToReplyMessage.longValue,
                            value = messageId
                        ),
                        ignoreServer = false
                    )
                }
            },
        )
    }

    onScrollToSearchMessageLiveData.observe(lifecycleOwner) { message ->
        val messageId = message.id
        messagesListView.scrollToMessage(
            messageId = message.id,
            offset = 200,
            highlight = true,
            onCompleted = { found ->
                if (found) {
                    isPreparingToScrollToMessage.set(false)
                    return@scrollToMessage
                }
                loadNearMessages(
                    messageId = messageId,
                    loadKey = LoadKeyData(
                        key = LoadKeyType.ScrollToMessageBy.longValue,
                        value = messageId
                    ),
                    ignoreServer = false
                )
            },
        )
    }

    onScrollToUnredMentionMessageLiveData.observe(lifecycleOwner) { messageId ->
        messagesListView.scrollToMessage(
            messageId = messageId,
            offset = 200,
            highlight = true,
            onCompleted = { found ->
                if (found) {
                    isPreparingToScrollToMessage.set(false)
                    return@scrollToMessage
                }
                loadNearMessages(
                    messageId = messageId,
                    loadKey = LoadKeyData(
                        key = LoadKeyType.ScrollToMessageBy.longValue,
                        value = messageId
                    ),
                    ignoreServer = false
                )
            },
        )
        pendingDisplayMsgIds.add(messageId)
    }

    messageMarkerLiveData.observe(lifecycleOwner) {
        it.forEach { response ->
            if (response is SceytResponse.Success) {
                val data = response.data ?: return@observe
                viewModelScope.launch(Dispatchers.Default) {
                    val user = SceytChatUIKit.currentUser ?: return@launch
                    val messages = messagesListView.getData()
                    messages.forEachIndexed { index, listItem ->
                        (listItem as? MessageItem)?.message?.let { message ->
                            if (data.messageIds.contains(message.id)) {
                                val updatedMessage = message.copy(
                                    userMarkers = message.userMarkers.orEmpty()
                                        .plus(
                                            SceytMarker(
                                                messageId = message.id,
                                                user = user,
                                                name = data.name,
                                                createdAt = data.createdAt
                                            )
                                        )
                                )
                                val updatedItem = listItem.copy(message = updatedMessage)
                                messagesListView.updateItemAt(index, updatedItem)
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun onMessage(message: SceytMessage) {
        if (hasNext || hasNextDb) return
        val initMessage = mapToMessageListItem(
            data = arrayListOf(message),
            hasNext = false,
            hasPrev = false,
            compareMessage = messagesListView.getLastMessage()?.message,
            enableDateSeparator = messagesListView.style.enableDateSeparator
        )

        messagesListView.addNewMessages(
            data = initMessage.toTypedArray(),
            lifecycleScope = lifecycleScope
        )
        messagesListView.updateViewState(PageState.Nothing)
    }

    suspend fun onOutgoingMessage(message: SceytMessage) {
        if (hasNext || hasNextDb) return
        val initMessage = mapToMessageListItem(
            data = arrayListOf(message),
            hasNext = false,
            hasPrev = false,
            compareMessage = messagesListView.getLastMessage()?.message,
            enableDateSeparator = messagesListView.style.enableDateSeparator
        )

        if (notFoundMessagesToUpdate.containsKey(message.tid)) {
            notFoundMessagesToUpdate.remove(message.tid)?.let {
                onOutgoingMessage(it)
                return
            }
        }

        suspendCancellableCoroutine { continuation ->
            messagesListView.addNewMessages(
                data = initMessage.toTypedArray(),
                lifecycleScope = lifecycleScope,
                addedCallback = {
                    continuation.safeResume(Unit)
                }
            )
            messagesListView.updateViewState(PageState.Nothing)
        }
    }

    fun onMessageUpdated(data: Pair<Long, List<SceytMessage>>) {
        val (_, messages) = data
        suspend fun update(sceytMessage: SceytMessage) {
            val message = initMessageInfoData(sceytMessage)
            withContext(Dispatchers.Main) {
                if (message.state == MessageState.Deleted || message.state == MessageState.Edited)
                    messagesListView.messageEditedOrDeleted(message)
                else {
                    val foundToUpdate = messagesListView.updateMessage(message)
                    if (!foundToUpdate) {
                        notFoundMessagesToUpdate[message.tid] = message
                    }
                }
            }
        }

        viewModelScope.launch(Dispatchers.Default) {
            messages.forEach { message ->
                if (message.incoming) {
                    update(message)
                } else outgoingMessageMutex.withLock {
                    update(message)
                }
            }
        }
    }

    onNewOutGoingMessageFlow.onEach { message ->
        outgoingMessageMutex.withLock {
            onOutgoingMessage(message)
        }
    }.launchIn(lifecycleOwner.lifecycleScope)

    onNewMessageFlow.onEach(::onMessage).launchIn(lifecycleOwner.lifecycleScope)

    MessagesCache.messageUpdatedFlow.onEach { data ->
        onMessageUpdated(data)
    }.launchIn(lifecycleOwner.lifecycleScope)

    fun onVocePlaying(message: SceytMessage) {
        if (message.userMarkers?.any { it.name == MarkerType.Played.value } == true)
            return

        addMessageMarker(MarkerType.Played.value, message.id)
    }

    // todo reply in thread
    /*
    onNewThreadMessageFlow.onEach {
          messagesListView.updateReplyCount(it)
      }.launchIn(lifecycleOwner.lifecycleScope)

      onOutGoingThreadMessageFlow.onEach {
          messagesListView.newReplyMessage(it.parentMessage?.id)
      }.launchIn(lifecycleOwner.lifecycleScope)
  */
    FileTransferHelper.onTransferUpdatedLiveData.asFlow().onEach {
        viewModelScope.launch(Dispatchers.Default) {
            if (lifecycleOwner.isResumed()) {
                messagesListView.updateProgress(it, false)
            } else if (it.state != TransferState.Downloading && it.state != TransferState.Uploading)
                needToUpdateTransferAfterOnResume[it.messageTid] = it
        }
    }.launchIn(viewModelScope)

    linkPreviewLiveData.asFlow().onEach {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            messagesListView.updateLinkPreview(it)
        }
    }.launchIn(lifecycleOwner.lifecycleScope)

    onChannelEventFlow.onEach { event ->
        when (event) {
            is ClearedHistory -> messagesListView.clearData()
            is Left -> {
                event.leftMembers.forEach { member ->
                    if (member.id == SceytChatUIKit.chatUIFacade.myId && !channel.isPublic())
                        messagesListView.context.asActivity().finish()
                }
            }

            is Deleted -> messagesListView.context.asActivity().finish()
            else -> return@onEach
        }
    }.launchIn(lifecycleOwner.lifecycleScope)

    joinLiveData.observe(lifecycleOwner) { response ->
        response.onSuccessNotNull { channel ->
            checkEnableDisableActions(channel)
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
                val type = if (event.onlyForMe)
                    DeleteMessageType.DeleteForMe
                else {
                    if (SceytChatUIKit.config.hardDeleteMessageForAll)
                        DeleteMessageType.DeleteHard else DeleteMessageType.DeleteForEveryone
                }
                deleteMessages(event.messages, deleteType = type)
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

            is MessageCommandEvent.MultiselectEvent -> {
                val wasSelected = selectedMessagesMap.containsKey(event.message.tid)
                val maxCount = SceytChatUIKit.config.messageMultiselectLimit

                if (!wasSelected && selectedMessagesMap.size >= maxCount) {
                    val errorMessage = String.format(
                        messagesListView.getString(
                            R.string.sceyt_reach_max_message_select_count, maxCount.toString()
                        )
                    )
                    customToastSnackBar(messagesListView, errorMessage)
                    return@setMessageCommandEventListener
                }

                val message = event.message.copy(isSelected = !wasSelected)
                messagesListView.updateMessageSelection(message)

                if (wasSelected) {
                    selectedMessagesMap.remove(message.tid)
                    if (selectedMessagesMap.isEmpty()) {
                        messageActionBridge.hideMessageActions()
                        messagesListView.cancelMultiSelectMode()
                    } else {
                        messageActionBridge.showMessageActions(*selectedMessagesMap.values.toTypedArray())
                    }
                } else {
                    selectedMessagesMap[message.tid] = message
                    messageActionBridge.showMessageActions(*selectedMessagesMap.values.toTypedArray())
                    messagesListView.setMultiSelectableMode()
                }
            }

            is MessageCommandEvent.CancelMultiselectEvent -> {
                selectedMessagesMap.clear()
                messagesListView.cancelMultiSelectMode()
            }

            is MessageCommandEvent.Reply -> {
                prepareToReplyMessage(event.message)
            }

            is MessageCommandEvent.ScrollToDown -> {
                prepareToScrollToNewMessage()
            }

            is MessageCommandEvent.ScrollToUnreadMention -> {
                prepareToScrollToUnreadMention()
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
                if (event.userId == SceytChatUIKit.chatUIFacade.myId)
                    return@setMessageCommandEventListener

                viewModelScope.launch(Dispatchers.IO) {
                    val user = userInteractor.getUserFromDbById(
                        id = event.userId
                    ) ?: SceytUser(event.userId)
                    val response = channelInteractor.findOrCreatePendingChannelByMembers(
                        data = CreateChannelData(
                            type = ChannelTypeEnum.Direct.value,
                            members = listOf(
                                SceytMember(
                                    roleName = RoleTypeEnum.Owner.value,
                                    user = user
                                )
                            ),
                        )
                    )
                    if (response is SceytResponse.Success)
                        response.data?.let {
                            ChannelInfoActivity.launch(messagesListView.context, response.data)
                        }
                }
            }

            is MessageCommandEvent.ReplyInThread -> {

            }

            is MessageCommandEvent.PollViewResultsClick -> {
                val poll = event.message.poll ?: return@setMessageCommandEventListener
                if (poll.anonymous || poll.maxVotedCountWithPendingVotes == 0)
                    return@setMessageCommandEventListener

                PollResultsActivity.launch(messagesListView.context, event.message)
            }
        }
    }

    messagesListView.setOnWindowFocusChangeListener { hasFocus ->
        if (hasFocus)
            ChannelsCache.currentChannelId = channel.id
        else ChannelsCache.currentChannelId = null
    }

    messagesListView.setNeedDownloadListener {
        needMediaInfo(it)
    }

    messagesListView.setMessageReactionsEventListener {
        onReactionEvent(it)
    }

    messagesListView.setMessagePollEventListener {
        onPollEvent(it)
    }

    messagesListView.setExpandMessageBodyListener { messageTid ->
        expandMessageBody(messageTid)
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
            onMessageDisplayed(it.message)
    }

    messagesListView.setVoicePlayPauseListener { _, message, playing ->
        if (playing)
            onVocePlaying(message)
    }
}
