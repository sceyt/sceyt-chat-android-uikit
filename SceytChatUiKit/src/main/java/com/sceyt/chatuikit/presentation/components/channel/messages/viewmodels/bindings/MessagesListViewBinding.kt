package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.bindings

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.ConnectionState
import com.sceyt.chat.models.message.DeleteMessageType
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.SceytChatUIKit.navigator
import com.sceyt.chatuikit.data.managers.channel.event.ChannelActionEvent.ClearedHistory
import com.sceyt.chatuikit.data.managers.channel.event.ChannelActionEvent.Deleted
import com.sceyt.chatuikit.data.managers.channel.event.ChannelActionEvent.Left
import com.sceyt.chatuikit.data.managers.connection.ConnectionEventManager
import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.channels.CreateChannelData
import com.sceyt.chatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.messages.MarkerType
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.data.models.messages.SceytMarker
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.asActivity
import com.sceyt.chatuikit.extensions.centerVisibleItemPosition
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.extensions.findIndexed
import com.sceyt.chatuikit.extensions.getChildTopByPosition
import com.sceyt.chatuikit.extensions.getString
import com.sceyt.chatuikit.extensions.isResumed
import com.sceyt.chatuikit.navigation.Destination
import com.sceyt.chatuikit.navigation.navigate
import com.sceyt.chatuikit.persistence.extensions.checkIsMemberInChannel
import com.sceyt.chatuikit.persistence.extensions.getPeer
import com.sceyt.chatuikit.persistence.extensions.isPublic
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferHelper
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelUpdatedType
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelsCache
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import com.sceyt.chatuikit.presentation.components.channel.messages.MessagesListView
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem.MessageItem
import com.sceyt.chatuikit.presentation.components.channel.messages.events.MessageCommandEvent
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.AppendRealtimeScroll
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.MessageListRenderEffect
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.MessageListViewModel
import com.sceyt.chatuikit.presentation.extensions.isNotPending
import com.sceyt.chatuikit.presentation.root.PageState
import com.sceyt.chatuikit.services.sync.SceytSyncManager
import com.sceyt.chatuikit.styles.extensions.messages_list.setEmptyStateForSelfChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

@JvmName("bind")
fun MessageListViewModel.bind(messagesListView: MessagesListView, lifecycleOwner: LifecycleOwner) {
    val lifecycleScope = lifecycleOwner.lifecycleScope
    messageActionBridge.setMessagesListView(messagesListView)
    messagesListView.setMultiselectDestination(selectedMessagesMap)
    if (channel.isSelf) {
        messagesListView.setEmptyStateForSelfChannel()
    }

    configureMessageList(messagesListView.style.enableDateSeparator)

    fun applyRenderEffect(effect: MessageListRenderEffect) {
        when (effect) {
            is MessageListRenderEffect.Replace -> {
                messagesListView.setMessagesList(
                    data = effect.items,
                    lifecycleScope = lifecycleScope,
                    force = effect.force
                )
            }

            is MessageListRenderEffect.PrependPage -> {
                messagesListView.setMessagesList(effect.resultItems, lifecycleScope)
            }

            is MessageListRenderEffect.AppendPage -> {
                messagesListView.addNextPageMessages(effect.resultItems, lifecycleScope)
            }

            is MessageListRenderEffect.AppendRealtime -> {
                messagesListView.addPreparedNewMessages(
                    data = effect.items.toTypedArray(),
                    lifecycleScope = lifecycleScope,
                    addedCallback = {
                        messagesListView.scrollToEndAfterRealtimeAppend(
                            addedItemsCount = effect.items.size,
                            alwaysScroll = effect.scroll == AppendRealtimeScroll.Always
                        )
                    }
                )
            }

            is MessageListRenderEffect.UpdateItem -> {
                messagesListView.renderItemUpdate(
                    index = effect.index,
                    item = effect.item,
                    diff = effect.diff,
                    notifyVisibleOnly = effect.notifyVisibleOnly,
                    notify = effect.notify
                )
            }

            is MessageListRenderEffect.DeleteTids -> {
                messagesListView.forceDeleteMessageByTid(*effect.tids.toLongArray())
            }

            MessageListRenderEffect.Clear -> messagesListView.clearData()
            MessageListRenderEffect.HideLoadingPrev -> messagesListView.hideLoadingPrev()
            MessageListRenderEffect.HideLoadingNext -> messagesListView.hideLoadingNext()

            is MessageListRenderEffect.ScrollToMessage -> {
                messagesListView.scrollToMessage(
                    messageId = effect.messageId,
                    offset = effect.offset,
                    highlight = effect.highlight,
                    onCompleted = { found ->
                        if (found) {
                            isPreparingToScrollToMessage.set(false)
                            return@scrollToMessage
                        }
                        effect.loadOnMissing?.let {
                            loadNearMessages(
                                messageId = effect.messageId,
                                loadKey = LoadKeyData(
                                    key = it.loadKey,
                                    value = effect.messageId
                                ),
                                ignoreServer = it.ignoreServer
                            )
                        }
                    }
                )
            }

            MessageListRenderEffect.ScrollToUnreadMessage -> {
                messagesListView.scrollToUnReadMessage()
            }

            MessageListRenderEffect.ScrollToLastMessage -> {
                messagesListView.scrollToLastMessage()
            }

            is MessageListRenderEffect.Sort -> {
                messagesListView.setMessagesList(effect.resultItems, lifecycleScope)
            }
        }
    }

    val retainedState = state.value
    if (retainedState.hasLoadedInitialMessages && retainedState.items.isNotEmpty()) {
        messagesListView.setMessagesList(retainedState.items, lifecycleScope, force = true)
    }

    renderEffects.onEach(::applyRenderEffect).launchIn(lifecycleScope)

    lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            messageActionBridge.clearMessagesListView(messagesListView)
        }
    })

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
                            updateProgress(data, true)
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

    fun syncNearCenterVisibleMessageIfNeeded() {
        if (!needSyncMessagesWhenScrollStateIdle || loadingFromServer) return
        val centerPosition = messagesListView.getMessagesRecyclerView().centerVisibleItemPosition()
        if (centerPosition == RecyclerView.NO_POSITION) return
        val item = currentMessageListItems().getOrNull(centerPosition)
        val messageId = item?.getMessageId() ?: return
        if (lastSyncCenterOffsetId != messageId) {
            lastSyncCenterOffsetId = messageId
            syncCenteredMessage(messageId = messageId)
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

    ChannelsCache.channelsDeletedFlow
        .filter { it.contains(channel.id) }
        .onEach {
            messagesListView.context.asActivity().finish()
        }.launchIn(lifecycleOwner.lifecycleScope)

    ChannelsCache.channelUpdatedFlow
        .filter { it.channel.id == channel.id && it.eventType == ChannelUpdatedType.ClearedHistory }
        .onEach {
            clearMessages()
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
                    appendSyncedMessages(
                        messages = messages,
                        scrollToLastAfterAppend = messagesListView.isLastCompletelyItemDisplaying()
                    )
                }
            }
        }
        .launchIn(lifecycleOwner.lifecycleScope)

    connectionLogic.allPendingEventsSentFlow
        .onEach {
            // Sync messages near center visible message
            syncNearCenterVisibleMessageIfNeeded()
        }
        .launchIn(lifecycleOwner.lifecycleScope)

    ConnectionEventManager.onChangedConnectStatusFlow
        .distinctUntilChanged()
        .onEach { stateData ->
            viewModelScope.launch(Dispatchers.IO) {
                if (stateData.state == ConnectionState.Connected) {
                    val message = currentMessageListItems().lastOrNull {
                        // First trying to get last displayed message
                        it is MessageItem && it.message.deliveryStatus == MessageDeliveryStatus.Displayed
                    } ?: currentMessageListItems().firstOrNull {
                        // Next trying to get fist sent message
                        it is MessageItem && it.message.deliveryStatus == MessageDeliveryStatus.Sent
                    } ?: currentMessageListItems().firstOrNull {
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
        lifecycleOwner.lifecycleScope.launch {
            val (index) = currentMessageListItems().findIndexed {
                it is MessageItem && it.message.id == data.centerMessageId
            } ?: return@launch
            val topOffset = messagesListView.getMessagesRecyclerView().getChildTopByPosition(index)
            mergeMissingMessagesAroundCenter(data, topOffset)
        }
    }

    MessagesCache.messagesClearedFlow
        .filter { (channelId, _) -> channelId == channel.id }
        .onEach { (_, date) ->
            deleteAllMessagesBefore {
                it.getMessageCreatedAt() <= date && (it !is MessageItem || it.message.isNotPending())
            }
        }.launchIn(lifecycleOwner.lifecycleScope)

    MessagesCache.messagesHardDeletedFlow
        .filter { (channelId, _) -> channelId == channel.id }
        .onEach { (_, tIds) ->
            deleteMessagesByTid(*tIds.toLongArray())
        }.launchIn(lifecycleOwner.lifecycleScope)

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
                        delay(200.milliseconds)
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
                viewModelScope.launch {
                    val user = SceytChatUIKit.currentUser ?: return@launch
                    applyDisplayedMarkers(
                        marker = data,
                        userMarker = SceytMarker(
                            messageId = 0,
                            user = user,
                            name = data.name,
                            createdAt = data.createdAt
                        )
                    )
                }
            }
        }
    }

    onNewOutGoingMessageFlow.onEach { message ->
        outgoingMessageMutex.withLock {
            appendOutgoingMessage(message)
        }
    }.launchIn(lifecycleOwner.lifecycleScope)

    onNewMessageFlow.onEach { message ->
        outgoingMessageMutex.withLock {
            appendIncomingMessage(message)
        }
    }.launchIn(lifecycleOwner.lifecycleScope)

    MessagesCache.messageUpdatedFlow.onEach { data ->
        applyMessageUpdates(data)
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

    FileTransferHelper.onTransferUpdatedLiveData.asFlow().onEach { transfer ->
        if (lifecycleOwner.isResumed()) {
            updateProgress(transfer, false)
        } else if (shouldDeferTransferUpdate(transfer)) {
            needToUpdateTransferAfterOnResume[transfer.messageTid] = transfer
        }
    }.launchIn(lifecycleScope)

    onChannelEventFlow.onEach { event ->
        when (event) {
            is ClearedHistory -> clearMessages()
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

    pageStateLiveData.observe(lifecycleOwner) {
        // If the page state is error, and channel is pending and there is no last message,
        // this means that there is no messages to show, so set empty state instead of error state.
        if (it is PageState.StateError && channel.pending && channel.lastMessage == null)
            messagesListView.updateViewState(PageState.StateEmpty())
        else
            messagesListView.updateViewState(it, false)
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
                updateMessageSelection(message)

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
                clearMessageSelectionState()
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
                            navigator.navigate(
                                context = messagesListView.context,
                                destination = Destination.ChannelInfo(response.data)
                            )
                        }
                }
            }

            is MessageCommandEvent.ReplyInThread -> {

            }

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
        this@bind.flushNotFoundStatusUpdates()
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
        if (it == RecyclerView.SCROLL_STATE_IDLE) {
            syncNearCenterVisibleMessageIfNeeded()
        }
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
