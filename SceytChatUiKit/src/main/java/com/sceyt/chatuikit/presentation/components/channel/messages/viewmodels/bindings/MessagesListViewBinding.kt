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
import com.sceyt.chatuikit.SceytChatUIKit.navigator
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
import com.sceyt.chatuikit.navigation.Destination
import com.sceyt.chatuikit.navigation.navigate
import com.sceyt.chatuikit.persistence.extensions.checkIsMemberInChannel
import com.sceyt.chatuikit.persistence.extensions.getPeer
import com.sceyt.chatuikit.persistence.extensions.isPublic
import com.sceyt.chatuikit.persistence.extensions.safeResume
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferHelper
import com.sceyt.chatuikit.persistence.file_transfer.ThumbFor
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelUpdatedType
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelsCache
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import com.sceyt.chatuikit.presentation.components.channel.messages.MessagesListView
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem.MessageItem
import com.sceyt.chatuikit.presentation.components.channel.messages.events.MessageCommandEvent
import com.sceyt.chatuikit.presentation.components.channel.messages.events.MessageScrollCommand
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.MessageActionBridge
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.MessageListViewModel
import com.sceyt.chatuikit.presentation.extensions.isNotPending
import com.sceyt.chatuikit.presentation.extensions.isSelfDestructed
import com.sceyt.chatuikit.presentation.helpers.TransferUpdateUiPolicy
import com.sceyt.chatuikit.presentation.root.PageState
import com.sceyt.chatuikit.styles.extensions.messages_list.setEmptyStateForSelfChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "MessagesListViewBinding"

@JvmName("bind")
fun MessageListViewModel.bind(messagesListView: MessagesListView, lifecycleOwner: LifecycleOwner) {
    val lifecycleScope = lifecycleOwner.lifecycleScope
    var shouldRetryPagingOnReconnect = !ConnectionEventManager.isConnected

    fun applyActionEffect(effect: MessageActionBridge.Effect) {
        when (effect) {
            is MessageActionBridge.Effect.MessageActionsShown -> messagesListView.setMultiSelectableMode()
            is MessageActionBridge.Effect.MultiSelectCanceled -> {
                selectedMessagesMap.clear()
                messagesListView.cancelMultiSelectMode()
            }

            is MessageActionBridge.Effect.MessageActionsHidden,
            is MessageActionBridge.Effect.SearchRequested,
            is MessageActionBridge.Effect.ExitSearchRequested,
            is MessageActionBridge.Effect.SearchModeChanged -> Unit
        }
    }

    fun applyMenuEvent(event: MessageActionBridge.MenuEvent) {
        when (event) {
            is MessageActionBridge.MenuEvent.Copy -> {
                messagesListView.messageActionsViewClickListeners.onCopyMessagesClick(*event.messages.toTypedArray())
            }

            is MessageActionBridge.MenuEvent.Delete -> {
                messagesListView.messageActionsViewClickListeners.onDeleteMessageClick(
                    messages = event.messages.toTypedArray(),
                    requireForMe = event.requireForMe,
                    actionFinish = event.actionFinish
                )
            }

            is MessageActionBridge.MenuEvent.Edit -> {
                messagesListView.messageActionsViewClickListeners.onEditMessageClick(event.message)
            }

            is MessageActionBridge.MenuEvent.MessageInfo -> {
                messagesListView.messageActionsViewClickListeners.onMessageInfoClick(event.message)
            }

            is MessageActionBridge.MenuEvent.Forward -> {
                messagesListView.messageActionsViewClickListeners.onForwardMessageClick(*event.messages.toTypedArray())
            }

            is MessageActionBridge.MenuEvent.Reply -> {
                messagesListView.messageActionsViewClickListeners.onReplyMessageClick(event.message)
            }

            is MessageActionBridge.MenuEvent.ReplyInThread -> {
                messagesListView.messageActionsViewClickListeners.onReplyMessageInThreadClick(event.message)
            }

            is MessageActionBridge.MenuEvent.RetractVote -> {
                messagesListView.messageActionsViewClickListeners.onRetractVoteClick(event.message)
            }

            is MessageActionBridge.MenuEvent.EndVote -> {
                messagesListView.messageActionsViewClickListeners.onEndVoteClick(event.message)
            }
        }
    }

    messageActionBridge.effects.onEach(::applyActionEffect).launchIn(lifecycleScope)
    messageActionBridge.menuEvents.onEach(::applyMenuEvent).launchIn(lifecycleScope)
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
                    lifecycleOwner.lifecycleScope.launch {
                        needToUpdateTransferAfterOnResume.drain().forEach { data ->
                            messagesListView.updateProgress(data, true)
                        }
                    }
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

    loadInitialMessagesForCurrentChannel()

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
        when (loadType) {
            LoadPrev if !hasPrevDb -> messagesListView.hideLoadingPrev()
            LoadNext if !hasNextDb -> messagesListView.hideLoadingNext()
            LoadNear -> {
                if (!hasPrevDb)
                    messagesListView.hideLoadingPrev()
                if (!hasNextDb)
                    messagesListView.hideLoadingNext()
            }

            else -> Unit
        }
    }

    fun checkToScrollAfterResponse(response: PaginationResponse<SceytMessage>) {
        val loadKey = response.getLoadKey() ?: return
        when (loadKey.key) {
            LoadKeyType.ScrollToUnreadMessage.longValue -> {
                messagesListView.scrollToUnReadMessage(loadKey.value)
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
                        if (found) {
                            resetPreparingToScrollToMessage()
                            return@scrollToMessage
                        }

                        if (response !is PaginationResponse.ServerResponse)
                            return@scrollToMessage

                        resetPreparingToScrollToMessage()
                        SceytLog.w(
                            TAG,
                            "Called load near messages in channelId: ${channel.id} for scroll to message id: ${loadKey.value}, but message not found in server response." +
                                    " Resetting the scroll preparation state to avoid infinite waiting."
                        )
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
                        when (response.loadType) {
                            LoadNear, LoadNewest -> {
                                messagesListView.setMessagesList(newMessages, lifecycleScope, true)
                            }

                            LoadNext -> {
                                messagesListView.addNextPageMessages(newMessages, lifecycleScope)
                            }

                            LoadPrev -> {
                                messagesListView.addPrevPageMessages(newMessages, lifecycleScope)
                            }
                        }
                    } else
                        messagesListView.setMessagesList(
                            data = newMessages,
                            lifecycleScope = lifecycleScope,
                            force = response.loadKey?.key == LoadKeyType.ScrollToLastMessage.longValue
                        )
                } else
                    checkToHildeLoadingMoreItemByLoadType(response.loadType)

                if (response.dbResultWasEmpty)
                    checkToScrollAfterResponse(response)

                loadPrevOffsetId = response.data.data?.firstOrNull()?.id ?: 0
                loadNextOffsetId = response.data.data?.lastOrNull()?.id ?: 0
            }

            is SceytResponse.Error -> {
                // set isSearchingMessageToScroll value to false, to enable jumping to next message
                if (response.loadKey?.key == LoadKeyType.ScrollToMessageBy.longValue)
                    resetPreparingToScrollToMessage()
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
        val item = messagesListView.getData().getOrNull(centerPosition) as? MessageItem
        val messageId = item?.message?.id ?: return
        if (lastSyncCenterOffsetId != messageId) {
            syncCenteredMessage(messageId = messageId)
        }
    }

    fun retryVisibleEdgePagingAfterReconnect() {
        if (loadingFromServer || loadingFromDb) return

        val messageItems = messagesListView.getData().filterIsInstance<MessageItem>()
        if (messageItems.isEmpty()) {
            // Edge paging requires a loaded message as an anchor. Retry the initial load
            // after a paging failure; otherwise refresh the newest messages in case the
            // channel changed while disconnected.
            if (canRetryLoadPrevAfterReconnect() || canRetryLoadNextAfterReconnect()) {
                loadInitialMessagesForCurrentChannel()
            } else {
                loadNewestMessages()
            }
            return
        }

        val offset = messageItems.size
        if (messagesListView.isNearStartForPaging() &&
            (canLoadPrev() || canRetryLoadPrevAfterReconnect())
        ) {
            loadPrevMessages(messageItems.first().message.id, offset)
            needSyncMessagesWhenScrollStateIdle = true
        }

        if (messagesListView.isNearEndForPaging() &&
            (canLoadNext() || canRetryLoadNextAfterReconnect())
        ) {
            val lastSentMessage = messageItems.lastOrNull { it.message.isNotPending() }
            if (lastSentMessage != null) {
                loadNextMessages(lastSentMessage.message.id, offset)
                needSyncMessagesWhenScrollStateIdle = true
            }
        }
    }

    fun onMessageDisplayed(message: SceytMessage) {
        if (channel.userRole.isNullOrEmpty())
            return

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

    suspend fun syncChannelMessagesAfter(message: SceytMessage) {
        val result = syncManager.syncConversationMessagesAfter(conversationId, message.id) ?: return
        val resultChannel = result.channel
        if (resultChannel.id == channel.id) {
            if (pinnedLastReadMessageId == 0L && resultChannel.lastDisplayedMessageId != 0L
                && resultChannel.lastDisplayedMessageId != resultChannel.lastMessage?.id
            )
                pinnedLastReadMessageId = resultChannel.lastDisplayedMessageId

            withContext(Dispatchers.Main) {
                if (!canAppendNewestSyncedMessages()) return@withContext

                val currentMessages = messagesListView.getData()
                    .filterIsInstance<MessageItem>()
                    .map { item -> item.message }
                if (currentMessages.none { it.id == message.id }) return@withContext

                val newMessages = result.messages.minus(currentMessages.toSet())
                if (newMessages.isNotEmpty()) {
                    val isLastDisplaying =
                        messagesListView.isLastCompletelyItemDisplaying()
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
                }
            }
        }
    }

    suspend fun syncAfterPendingEventsSent() {
        if (!ConnectionEventManager.isConnected) return

        if (shouldRetryPagingOnReconnect) {
            needSyncMessagesWhenScrollStateIdle = true
            retryVisibleEdgePagingAfterReconnect()
            shouldRetryPagingOnReconnect = false
        }

        (messagesListView.getData().lastOrNull {
            it is MessageItem && it.message.isNotPending()
        } as? MessageItem)?.let { item ->
            syncChannelMessagesAfter(item.message)
        }

        syncNearCenterVisibleMessageIfNeeded()
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

    connectionLogic.allPendingEventsSentFlow
        .onEach {
            syncAfterPendingEventsSent()
        }
        .launchIn(lifecycleOwner.lifecycleScope)

    ConnectionEventManager.onChangedConnectStatusFlow
        .distinctUntilChanged()
        .onEach { stateData ->
            if (stateData.state == ConnectionState.Connected) {
                if (shouldRetryPagingOnReconnect)
                    needSyncMessagesWhenScrollStateIdle = true
            } else {
                invalidateCenteredSync()
                needSyncMessagesWhenScrollStateIdle = true
                shouldRetryPagingOnReconnect = true
            }
        }.launchIn(lifecycleOwner.lifecycleScope)

    syncCenteredMessageFlow
        .onEach { centeredSync ->
            val data = centeredSync.data
            if (data.missingMessages.isEmpty()) return@onEach

            val items = messagesListView.getData().toMutableList()
            val (index) = items.findIndexed {
                it is MessageItem && it.message.id == data.centerMessageId
            } ?: return@onEach
            val topOffset = messagesListView.getMessagesRecyclerView().getChildTopByPosition(index)
            if (!canApplyCenteredSyncResult(
                    centerMessageId = data.centerMessageId,
                    generation = centeredSync.generation,
                    topOffset = topOffset
                )
            ) return@onEach

            withContext(Dispatchers.Default) {
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
        .launchIn(lifecycleOwner.lifecycleScope)

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

    fun scrollToTargetMessage(messageId: Long) {
        messagesListView.scrollToMessage(
            messageId = messageId,
            offset = 200,
            highlight = true,
            onCompleted = { found ->
                if (found) {
                    resetPreparingToScrollToMessage()
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

    lifecycleScope.launch {
        scrollCommands.collectLatest { command ->
            when (command) {
                is MessageScrollCommand.ToLastMessage -> {
                    val lastMsgId = command.messageId
                    if (messagesListView.getMessageIndexedById(lastMsgId) != null) {
                        messagesListView.scrollToLastMessage()
                        delay(200.milliseconds)
                        syncNearCenterVisibleMessageIfNeeded()
                    } else {
                        loadPrevMessages(
                            lastMessageId = lastMsgId,
                            offset = 0,
                            loadKey = LoadKeyData(key = LoadKeyType.ScrollToLastMessage.longValue)
                        )
                        markChannelAsRead(channel.id)
                    }
                }

                is MessageScrollCommand.ToReplyMessage -> {
                    val messageId = command.messageId
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

                is MessageScrollCommand.ToSearchMessage -> {
                    scrollToTargetMessage(command.messageId)
                }

                is MessageScrollCommand.ToUnreadMention -> {
                    scrollToTargetMessage(command.messageId)
                    pendingDisplayMsgIds.add(command.messageId)
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

        // Use the parked update if available. It was already updated, but for some reason was not
        // found in the UI to apply the update.
        val messageToRender = pendingStatusReconciler.take(message.tid)?.let {
            SceytLog.d(TAG, "Rendering previously not found updated message with tid: ${it.tid}")
            it
        } ?: message

        val messageItems = mapToMessageListItem(
            data = arrayListOf(messageToRender),
            hasNext = false,
            hasPrev = false,
            compareMessage = messagesListView.getLastMessage()?.message,
            enableDateSeparator = messagesListView.style.enableDateSeparator
        )

        suspendCancellableCoroutine { continuation ->
            messagesListView.addNewMessages(
                data = messageItems.toTypedArray(),
                lifecycleScope = lifecycleScope,
                addedCallback = {
                    continuation.safeResume(Unit)
                }
            )
            messagesListView.updateViewState(PageState.Nothing)
        }
    }

    // Retries status updates parked while their target was missing during a list rebuild
    // (e.g. a forwarded message stuck on "Pending"). Run on every list commit.
    fun flushNotFoundStatusUpdates() {
        if (pendingStatusReconciler.parkedCount == 0) return
        viewModelScope.launch(Dispatchers.Main) {
            outgoingMessageMutex.withLock {
                pendingStatusReconciler.reconcile { messagesListView.updateMessage(it) }
            }
        }
    }

    fun onMessageUpdated(data: Pair<Long, List<SceytMessage>>) {
        val (_, messages) = data

        suspend fun update(sceytMessage: SceytMessage) {
            val message = initMessageInfoData(sceytMessage)
            withContext(Dispatchers.Main) {
                when {
                    message.state == MessageState.Deleted || message.state == MessageState.Edited -> {
                        messagesListView.messageEditedOrDeleted(updateMessage = message)
                    }

                    message.isSelfDestructed() -> {
                        messagesListView.messageSelfDestructed(message)
                    }

                    else -> {
                        pendingStatusReconciler.onStatusUpdate(message) {
                            messagesListView.updateMessage(it)
                        }
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

    FileTransferHelper.onTransferUpdatedLiveData.asFlow()
        .filter { TransferUpdateUiPolicy.shouldApplyDeferredUpdate(it, ThumbFor.MessagesLisView) }
        .onEach { transfer ->
            if (lifecycleOwner.isResumed()) {
                messagesListView.updateProgress(transfer, false)
            } else {
                needToUpdateTransferAfterOnResume.add(transfer)
            }
        }.launchIn(lifecycleScope)

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

    pageStateLiveData.observe(lifecycleOwner) { state ->
        when (state) {
            // If the page state is error, and channel is pending and there is no last message,
            // this means that there is no messages to show, so set empty state instead of error state.
            is PageState.StateError if channel.pending && channel.lastMessage == null -> {
                messagesListView.updateViewState(PageState.StateEmpty())
            }

            // Nothing is displayed and the load failed while disconnected. Paging is retried
            // automatically once the connection is back, so keep the loader instead of leaving
            // a blank screen. Messages already on screen keep the real error state.
            is PageState.StateError if !ConnectionEventManager.isConnected
                    && messagesListView.getData().none { item -> item is MessageItem } -> {
                messagesListView.updateViewState(PageState.StateLoading(true), false)
            }

            else -> messagesListView.updateViewState(state, false)
        }
    }

    messagesListView.setMessageCommandEventListener { event ->
        when (event) {
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
                        messageActionBridge.cancelMultiSelectMode()
                    } else {
                        messageActionBridge.showMessageActions(*selectedMessagesMap.values.toTypedArray())
                    }
                } else {
                    selectedMessagesMap[message.tid] = message
                    messageActionBridge.showMessageActions(*selectedMessagesMap.values.toTypedArray())
                }
            }

            is MessageCommandEvent.CancelMultiselectEvent -> {
                messageActionBridge.cancelMultiSelectMode()
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
                    channelInteractor.findOrCreatePendingChannelByMembers(
                        data = CreateChannelData(
                            type = ChannelTypeEnum.Direct.value,
                            members = listOf(
                                SceytMember(roleName = RoleTypeEnum.Owner.value, user = user)
                            ),
                        )
                    ).onSuccessNotNull {
                        navigator.navigate(
                            context = messagesListView.context,
                            destination = Destination.ChannelInfo(it)
                        )
                    }
                }
            }

            is MessageCommandEvent.ReplyInThread -> Unit

            is MessageCommandEvent.PollViewResultsClick -> {
                val poll = event.message.poll ?: return@setMessageCommandEventListener
                if (poll.anonymous || poll.maxVotedCountWithPendingVotes == 0)
                    return@setMessageCommandEventListener

                navigator.navigate(
                    context = messagesListView.context,
                    destination = Destination.PollResults(event.message)
                )
            }
        }
    }

    messagesListView.setOnListCommittedListener {
        flushNotFoundStatusUpdates()
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
