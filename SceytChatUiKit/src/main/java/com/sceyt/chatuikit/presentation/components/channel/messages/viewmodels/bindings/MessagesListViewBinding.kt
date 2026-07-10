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
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem.MessageItem
import com.sceyt.chatuikit.presentation.components.channel.messages.events.MessageCommandEvent
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.AppendRealtimeScroll
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.MessageActionBridge
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.MessageListRenderEffect
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.MessageListViewModel
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.ScrollRequestData
import com.sceyt.chatuikit.presentation.extensions.isNotPending
import com.sceyt.chatuikit.presentation.root.PageState
import com.sceyt.chatuikit.styles.extensions.messages_list.setEmptyStateForSelfChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.milliseconds

@JvmName("bind")
fun MessageListViewModel.bind(messagesListView: MessagesListView, lifecycleOwner: LifecycleOwner) {
    val lifecycleScope = lifecycleOwner.lifecycleScope
    val scrollCoordinator = MessageScrollCoordinator()
    var shouldRetryPagingOnReconnect = !ConnectionEventManager.isConnected
    messagesListView.setMultiselectDestination(selectedMessagesMap)
    if (channel.isSelf) {
        messagesListView.setEmptyStateForSelfChannel()
    }

    configureMessageList(messagesListView.style.enableDateSeparator)

    fun syncNearCenterVisibleMessageIfNeeded() {
        if (!needSyncMessagesWhenScrollStateIdle || loadingFromServer) return
        val recyclerView = messagesListView.getMessagesRecyclerView()
        val centerPosition = recyclerView.centerVisibleItemPosition()
        if (centerPosition == RecyclerView.NO_POSITION) return
        val item = currentMessageListItems().getOrNull(centerPosition) as? MessageItem
        val messageId = item?.message?.id ?: return
        if (lastSyncCenterOffsetId != messageId) {
            syncCenteredMessage(messageId = messageId)
        }
    }

    fun retryVisibleEdgePagingAfterReconnect() {
        if (loadingFromServer || loadingFromDb) return

        val messageItems = currentMessageListItems().filterIsInstance<MessageItem>()
        if (messageItems.isEmpty()) return

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

    fun hasNextMessageGap(): Boolean {
        return hasNext || hasNextDb ||
                currentMessageListItems().lastOrNull() is MessageListItem.LoadingNextItem
    }

    fun scrollToNewestMessageIfLoaded(
        request: MessageScrollCoordinator.Request,
        syncAfterScroll: Boolean = false,
    ): Boolean {
        val messageId = request.targetMessageId ?: return false
        if (messagesListView.getMessageIndexedById(messageId) == null)
            return false

        messagesListView.scrollToLastMessage()
        scrollCoordinator.clearIfSettled(request, loadingFromServer || loadingFromDb)
        if (syncAfterScroll) {
            lifecycleOwner.lifecycleScope.launch {
                delay(200.milliseconds)
                if (scrollCoordinator.canRunDelayedWorkFor(request))
                    syncNearCenterVisibleMessageIfNeeded()
            }
        }
        return true
    }

    fun scrollToPendingNewMessageIfPossible() {
        val request = scrollCoordinator.activeNewestMessageRequest() ?: return
        if (scrollToNewestMessageIfLoaded(request))
            return

        scrollCoordinator.clearIfSettled(request, loadingFromServer || loadingFromDb)
    }

    suspend fun syncAfterPendingEventsSent() {
        if (!ConnectionEventManager.isConnected) return

        if (shouldRetryPagingOnReconnect) {
            needSyncMessagesWhenScrollStateIdle = true
            retryVisibleEdgePagingAfterReconnect()
            shouldRetryPagingOnReconnect = false
        }

        (state.value.items.lastOrNull {
            it is MessageItem && it.message.isNotPending()
        } as? MessageItem)?.let { item ->
            syncAndAppendMessagesAfter(
                fromMessageId = item.message.id,
                scrollToLastAfterAppend = scrollCoordinator.activeNewestMessageRequest() != null ||
                        messagesListView.isLastCompletelyItemDisplaying()
            )
        }

        syncNearCenterVisibleMessageIfNeeded()
    }

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
                val request = if (effect.requestId == null) {
                    scrollCoordinator.beginMessageRequest(effect.messageId)
                } else {
                    scrollCoordinator.activeRequestFor(effect.requestId) ?: return
                }
                messagesListView.scrollToMessage(
                    messageId = effect.messageId,
                    offset = effect.offset,
                    highlight = effect.highlight,
                    onCompleted = { found ->
                        if (scrollCoordinator.activeRequestFor(request.id) == null)
                            return@scrollToMessage

                        if (found) {
                            isPreparingToScrollToMessage.set(false)
                            scrollCoordinator.clear(request)
                            return@scrollToMessage
                        }

                        effect.loadOnMissing?.let {
                            loadNearMessages(
                                messageId = effect.messageId,
                                loadKey = LoadKeyData(
                                    key = it.loadKey,
                                    value = effect.messageId,
                                    data = ScrollRequestData(request.id)
                                ),
                                ignoreServer = it.ignoreServer,
                                awaitToConnectTimeout = 0
                            )
                            return@scrollToMessage
                        }
                        scrollCoordinator.clear(request)
                    }
                )
            }

            MessageListRenderEffect.ScrollToUnreadMessage -> {
                messagesListView.scrollToUnReadMessage()
            }

            is MessageListRenderEffect.ScrollToLastMessage -> {
                val request = scrollCoordinator.activeRequestFor(effect.requestId)
                if (effect.requestId != null && request == null)
                    return

                messagesListView.scrollToLastMessage()
                request?.let {
                    scrollCoordinator.clearIfSettled(it, loadingFromServer || loadingFromDb)
                }
            }

            is MessageListRenderEffect.ScrollToNewMessage -> {
                val targetId = effect.lastMessage?.id ?: return
                val request = scrollCoordinator.beginNewestMessageRequest(targetId)
                if (!hasNextMessageGap() && scrollToNewestMessageIfLoaded(
                        request,
                        syncAfterScroll = true
                    )
                )
                    return

                loadNewestMessages(
                    loadKey = LoadKeyData(
                        key = LoadKeyType.ScrollToLastMessage.longValue,
                        value = targetId,
                        data = ScrollRequestData(request.id)
                    )
                )
                markChannelAsRead(channel.id)
            }

            is MessageListRenderEffect.Sort -> {
                messagesListView.setMessagesList(effect.resultItems, lifecycleScope)
            }

            is MessageListRenderEffect.ApplyCenteredSync -> {
                lifecycleOwner.lifecycleScope.launch {
                    val data = effect.result.data
                    val recyclerView = messagesListView.getMessagesRecyclerView()
                    val (index) = currentMessageListItems().findIndexed {
                        it is MessageItem && it.message.id == data.centerMessageId
                    } ?: return@launch
                    val topOffset = recyclerView.getChildTopByPosition(index)
                    if (!canApplyCenteredSyncResult(
                            centerMessageId = data.centerMessageId,
                            generation = effect.result.generation,
                            topOffset = topOffset
                        )
                    ) return@launch

                    mergeMissingMessagesAroundCenter(data, topOffset)
                }
            }
        }
    }

    fun applyActionEffect(effect: MessageActionBridge.Effect) {
        when (effect) {
            is MessageActionBridge.Effect.MessageActionsShown -> messagesListView.setMultiSelectableMode()
            is MessageActionBridge.Effect.MultiSelectCanceled -> {
                clearMessageSelectionState()
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

    fun onMessageDisplayed(message: SceytMessage) {
        markMessageAsDisplayedIfNeeded(message, lifecycleOwner.isResumed())
    }

    fun onVoicePlaying(message: SceytMessage) {
        if (message.userMarkers?.any { it.name == MarkerType.Played.value } == true)
            return

        addMessageMarker(MarkerType.Played.value, message.id)
    }

    val retainedState = state.value
    if (retainedState.hasLoadedInitialMessages && retainedState.items.isNotEmpty()) {
        messagesListView.setMessagesList(retainedState.items, lifecycleScope, force = true)
    }

    if (selectedMessagesMap.isNotEmpty())
        messagesListView.setMultiSelectableMode()

    // Cancel notification for current channel
    SceytChatUIKit.notifications.pushNotification.notificationHandler.cancelNotification(
        notificationId = channel.id.toInt()
    )

    checkEnableDisableActions(channel)
    setUnreadCounts(channel)

    renderEffects.onEach(::applyRenderEffect).launchIn(lifecycleScope)
    messageActionBridge.effects.onEach(::applyActionEffect).launchIn(lifecycleScope)
    messageActionBridge.menuEvents.onEach(::applyMenuEvent).launchIn(lifecycleScope)

    /** Send pending markers, pending messages and update attachments transfer states when
     * lifecycle come back onResume state. */
    viewModelScope.launch {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (ConnectionEventManager.connectionState == ConnectionState.Connected) {
                flushPendingDisplayedMessages()
                sendPendingMessages()
            }
            messagesListView.post {
                lifecycleOwner.lifecycleScope.launch {
                    flushDeferredTransferUpdates()
                }
            }
        }
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

    connectionLogic.allPendingEventsSentFlow
        .onEach {
            syncAfterPendingEventsSent()
        }
        .launchIn(lifecycleOwner.lifecycleScope)

    ConnectionEventManager.onChangedConnectStatusFlow
        .distinctUntilChanged()
        .onEach { stateData ->
            if (stateData.state == ConnectionState.Connected) {
                if (shouldRetryPagingOnReconnect) {
                    needSyncMessagesWhenScrollStateIdle = true
                }
            } else {
                shouldRetryPagingOnReconnect = true
                invalidateCenteredSync()
                needSyncMessagesWhenScrollStateIdle = true
            }
        }.launchIn(lifecycleOwner.lifecycleScope)

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

    MessagesCache.messageUpdatedFlow.onEach { (channelId, messages) ->
        if (channelId != channel.id) return@onEach
        applyMessageUpdates(messages)
    }.launchIn(lifecycleOwner.lifecycleScope)

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
            deferTransferUpdate(transfer)
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
                prepareToPauseOrResumeUpload(event.item)
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
        scrollToPendingNewMessageIfPossible()
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
        when (it) {
            RecyclerView.SCROLL_STATE_DRAGGING -> {
                scrollCoordinator.cancelActiveRequest()
                isPreparingToScrollToMessage.set(false)
            }

            RecyclerView.SCROLL_STATE_IDLE -> {
                syncNearCenterVisibleMessageIfNeeded()
            }
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
            onVoicePlaying(message)
    }
}
