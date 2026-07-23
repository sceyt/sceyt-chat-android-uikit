package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import android.text.Editable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.message.DeleteMessageType
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.constants.SceytConstants
import com.sceyt.chatuikit.data.managers.channel.ChannelEventManager
import com.sceyt.chatuikit.data.managers.channel.event.ChannelActionEvent
import com.sceyt.chatuikit.data.managers.channel.event.ChannelMemberActivityEvent
import com.sceyt.chatuikit.data.managers.channel.event.ChannelMembersEventData
import com.sceyt.chatuikit.data.managers.message.MessageEventManager
import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNear
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNewest
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNext
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadPrev
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.SyncNearMessagesResult
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.MarkerType
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.onErrorNonNull
import com.sceyt.chatuikit.data.models.onSuccessNotNull
import com.sceyt.chatuikit.data.repositories.Keys.KEY_VIEW_ONCE_INFO_SHOWN
import com.sceyt.chatuikit.domain.usecases.PauseOrResumeTransferUseCase
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.media.audio.AudioRecordData
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.persistence.differs.diff
import com.sceyt.chatuikit.persistence.extensions.asLiveData
import com.sceyt.chatuikit.persistence.extensions.broadcastSharedFlow
import com.sceyt.chatuikit.persistence.extensions.getPeer
import com.sceyt.chatuikit.persistence.extensions.isDirect
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.interactor.AttachmentInteractor
import com.sceyt.chatuikit.persistence.interactor.ChannelInteractor
import com.sceyt.chatuikit.persistence.interactor.ChannelMemberInteractor
import com.sceyt.chatuikit.persistence.interactor.MessageInteractor
import com.sceyt.chatuikit.persistence.interactor.MessagePollInteractor
import com.sceyt.chatuikit.persistence.interactor.MessageReactionInteractor
import com.sceyt.chatuikit.persistence.interactor.UserInteractor
import com.sceyt.chatuikit.persistence.logic.PersistenceConnectionLogic
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelsCache
import com.sceyt.chatuikit.persistence.logicimpl.message.MessageTid
import com.sceyt.chatuikit.persistence.repositories.SceytSharedPreference
import com.sceyt.chatuikit.presentation.components.channel.input.data.InputUserAction
import com.sceyt.chatuikit.presentation.components.channel.input.format.BodyStyleRange
import com.sceyt.chatuikit.presentation.components.channel.input.mention.Mention
import com.sceyt.chatuikit.presentation.components.channel.messages.PendingMessageStatusReconciler
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.FileListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem.MessageItem
import com.sceyt.chatuikit.presentation.components.channel.messages.events.MessageCommandEvent
import com.sceyt.chatuikit.presentation.components.channel.messages.events.MessageInputCommand
import com.sceyt.chatuikit.presentation.components.channel.messages.events.PollEvent
import com.sceyt.chatuikit.presentation.components.channel.messages.events.ReactionEvent
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.bindings.LoadKeyType
import com.sceyt.chatuikit.presentation.extensions.getUpdateMessage
import com.sceyt.chatuikit.presentation.extensions.isNotPending
import com.sceyt.chatuikit.presentation.extensions.isPending
import com.sceyt.chatuikit.presentation.extensions.isSelfDestructed
import com.sceyt.chatuikit.presentation.helpers.DebounceHelper
import com.sceyt.chatuikit.presentation.root.BaseViewModel
import com.sceyt.chatuikit.presentation.root.PageState
import com.sceyt.chatuikit.services.SceytPresenceChecker
import com.sceyt.chatuikit.services.sync.SceytSyncManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min
import kotlin.time.Duration.Companion.seconds

class MessageListViewModel internal constructor(
    private var _conversationId: Long,
    private var _channel: SceytChannel,
    val replyInThread: Boolean,
    val initialTargetMessageId: Long?,
    private val ioDispatcher: CoroutineDispatcher,
    private val defaultDispatcher: CoroutineDispatcher,
    private val mainDispatcher: CoroutineDispatcher,
    private val editedOrDeletedMessagesFlow: Flow<SceytMessage>,
    private val outgoingMessagesFlow: Flow<SceytMessage>,
    private val channelEventsFlow: Flow<ChannelActionEvent>,
    private val channelMemberActivityEventsFlow: Flow<ChannelMemberActivityEvent>,
    private val channelMembersEventsFlow: Flow<ChannelMembersEventData>,
) : BaseViewModel(), SceytKoinComponent {
    constructor(
        conversationId: Long,
        channel: SceytChannel,
        replyInThread: Boolean = false,
        initialTargetMessageId: Long? = null,
    ) : this(
        _conversationId = conversationId,
        _channel = channel,
        replyInThread = replyInThread,
        initialTargetMessageId = initialTargetMessageId,
        ioDispatcher = Dispatchers.IO,
        defaultDispatcher = Dispatchers.Default,
        mainDispatcher = Dispatchers.Main,
        editedOrDeletedMessagesFlow = MessageEventManager.onMessageEditedOrDeletedFlow,
        outgoingMessagesFlow = MessageEventManager.onOutgoingMessageFlow,
        channelEventsFlow = ChannelEventManager.onChannelEventFlow,
        channelMemberActivityEventsFlow = ChannelEventManager.onChannelMemberActivityEventFlow,
        channelMembersEventsFlow = ChannelEventManager.onChannelMembersEventFlow,
    )

    private val messageInteractor: MessageInteractor by inject()
    internal val channelInteractor: ChannelInteractor by inject()
    private val messageReactionInteractor: MessageReactionInteractor by inject()
    private val messagePollInteractor: MessagePollInteractor by inject()
    internal val attachmentInteractor: AttachmentInteractor by inject()
    internal val channelMemberInteractor: ChannelMemberInteractor by inject()
    internal val connectionLogic: PersistenceConnectionLogic by inject()
    internal val userInteractor: UserInteractor by inject()
    private val syncManager: SceytSyncManager by inject()
    private val fileTransferService: FileTransferService by inject()
    private val pauseOrResumeTransferUseCase: PauseOrResumeTransferUseCase by inject()
    private val preferences: SceytSharedPreference by inject()
    internal var pinnedLastReadMessageId: Long = 0
    private val sendDisplayedHelper by lazy { DebounceHelper(200L, viewModelScope) }
    internal val messageActionBridge by lazy { MessageActionBridge() }
    internal val placeToSavePathsList = mutableSetOf<Pair<AttachmentTypeEnum, String>>()
    internal val selectedMessagesMap by lazy { mutableMapOf<MessageTid, SceytMessage>() }
    internal val expandedMessagesMap by lazy { mutableMapOf<Long, Boolean>() }
    internal val pendingStatusReconciler by lazy { PendingMessageStatusReconciler() }
    internal val outgoingMessageMutex by lazy { Mutex() }

    // Owns the rendered list state, render-effect stream and reducer-backed list mutations.
    private val store = MessageListStore(
        recoveryScope = viewModelScope,
        recoveryDispatcher = mainDispatcher,
    )
    private val messageListItemMapper by lazy { MessageListItemMapper() }
    private val windowSyncGuard by lazy { MessageWindowSyncGuard() }
    private val pagingRetryState by lazy { MessagePagingRetryState() }
    private val pendingDisplayMsgIds by lazy { Collections.synchronizedSet(mutableSetOf<Long>()) }
    private var showSenderAvatarAndNameIfNeeded = true
    internal var viewOnceSelected = false
    private var loadPrevJob: Job? = null
    private var loadNextJob: Job? = null
    private var loadNearJob: Job? = null
    internal var mentionJob: Job? = null

    private companion object {
        const val TAG = "MessageListViewModel"
        const val SCROLL_TO_MESSAGE_OFFSET = 200
    }

    // Pagination sync
    internal var needSyncMessagesWhenScrollStateIdle = false
    internal var loadPrevOffsetId = 0L
    internal var loadNextOffsetId = 0L
    internal var lastSyncCenterOffsetId = 0L

    private val myId: String? get() = userInteractor.getCurrentUserId()
    val channel: SceytChannel get() = _channel
    val conversationId: Long get() = _conversationId
    val state get() = store.state
    val renderEffects get() = store.renderEffects


    // Message events
    val onNewMessageFlow: Flow<SceytMessage>
    val onNewOutGoingMessageFlow: Flow<SceytMessage>

    //val onNewThreadMessageFlow: Flow<SceytMessage>// todo reply in thread

    // val onOutGoingThreadMessageFlow: Flow<SceytMessage>// todo reply in thread

    // Chanel events
    val onChannelEventFlow: Flow<ChannelActionEvent>
    val onChannelMemberActivityEventFlow: Flow<ChannelMemberActivityEvent>
    private val _onChannelUpdatedEventFlow = broadcastSharedFlow<SceytChannel>(replay = 1)
    val onChannelUpdatedEventFlow = _onChannelUpdatedEventFlow.asSharedFlow()

    // Input commands
    private val _inputCommands = MutableSharedFlow<MessageInputCommand>(extraBufferCapacity = 8)
    internal val inputCommands = _inputCommands.asSharedFlow()
    private val _peerPresenceUpdatedFlow = MutableLiveData<SceytChannel>()
    val peerPresenceUpdatedFlow = _peerPresenceUpdatedFlow.asLiveData()

    // Search messages
    internal val isPreparingToScrollToMessage = AtomicBoolean(false)
    private val mentionsController = createUnreadMentionsController()
    private val searchController = createMessageSearchController()
    private val reactionController = createReactionController()
    private val pollController = createPollController()
    private val memberController = createChannelMemberController()
    private val transferController = createMessageTransferController()
    private val draftController = createMessageDraftController()
    val searchResult get() = searchController.searchResult


    init {
        observeToUserPresenceUpdateIfNeeded()
        mentionsController.onInit()
        clearPreparingThumbs()
        loadInitialMessages()

        // If userRole is null or empty, get channel again to update channel
        if (channel.userRole.isNullOrEmpty())
            getChannel(channel.id)

        if (channel.unread)
            markChannelAsRead(channel.id)

        onNewMessageFlow = messageInteractor.getOnMessageFlow()
            .filter { (channel) ->
                channel.id == this.channel.id /*&& it.second.replyInThread == replyInThread*/
            }
            .map { (_, message) ->
                mentionsController.onNewMessage(message)
                initMessageInfoData(message)
            }

        editedOrDeletedMessagesFlow.onEach { message ->
            mentionsController.onMessageUpdated(message)
        }.launchIn(viewModelScope)

        channelMembersEventsFlow
            .filter { it.channel.id == channel.id }
            .onEach(memberController::onMemberEvent)
            .launchIn(viewModelScope)

        /*
       // todo reply in thread
        onNewThreadMessageFlow = MessageEventsObserver.onMessageFlow
              .filter { it.first.id == channel.id && it.second.replyInThread }
              .mapNotNull { initMessageInfoData(it.second) }*/

        onChannelEventFlow = channelEventsFlow
            .filter { it.channelId == channel.id }

        onChannelMemberActivityEventFlow = channelMemberActivityEventsFlow
            .filter { it.channelId == channel.id && it.userId != myId }

        ChannelsCache.channelUpdatedFlow
            .filter { it.channel.id == channel.id }
            .onEach {
                updateChannel { it.channel }
            }
            .launchIn(viewModelScope)

        ChannelsCache.pendingChannelCreatedFlow
            .filter { (pendingChannelId, _) -> pendingChannelId == channel.id }
            .onEach { (_, newChannel) ->
                onPendingChannelCreated(newChannel)
            }
            .launchIn(viewModelScope)

        onNewOutGoingMessageFlow = outgoingMessagesFlow
            .filter { it.channelId == channel.id /*&& !it.replyInThread*/ }

        /*onOutGoingThreadMessageFlow = MessageEventsObserver.onOutgoingMessageFlow
            .filter { it.channelId == channel.id && it.replyInThread }*/
    }

    fun configureMessageList(enableDateSeparator: Boolean) {
        store.enableDateSeparator = enableDateSeparator
    }

    internal fun currentMessageListItems(): List<MessageListItem> = store.items

    internal fun currentMessageItems(): List<SceytMessage> = store.messageItems()

    internal fun currentLastMessageItem(): MessageItem? = store.lastMessageItem()

    internal fun hasNewestMessageGap(): Boolean {
        return hasNext || hasNextDb ||
                currentMessageListItems().lastOrNull() is MessageListItem.LoadingNextItem
    }

    private fun emitRenderEffect(effect: MessageListRenderEffect) = store.emitEffect(effect)

    private fun replaceMessages(items: List<MessageListItem>, force: Boolean) =
        store.replace(items, force)

    private fun addPrevPageItems(items: List<MessageListItem>) = store.addPrevPage(items)

    private fun addNextPageItems(items: List<MessageListItem>) = store.addNextPage(items)

    private fun addRealtimeItems(items: List<MessageListItem>, isOutgoing: Boolean): Boolean =
        store.addRealtime(items, isOutgoing)


    internal fun deleteMessagesByTid(vararg tid: Long) {
        val outcome = store.deleteByTids(tid.toList())
        if (outcome.changed && outcome.isEmpty)
            pageStateLiveDataInternal.postValue(PageState.StateEmpty())
    }

    internal fun clearMessages() {
        store.clear()
        pageStateLiveDataInternal.postValue(PageState.StateEmpty())
    }

    internal fun deleteAllMessagesBefore(predicate: (MessageListItem) -> Boolean) {
        store.deleteAllBefore(predicate)
    }

    internal fun updateMessageSelection(message: SceytMessage) {
        store.updateItem(
            predicate = { it.message.tid == message.tid },
            diff = MessageDiff.DEFAULT_FALSE.copy(selectionChanged = true),
            update = { item ->
                item.copy(message = item.message.copy(isSelected = message.isSelected))
            }
        )
    }

    internal fun clearMessageSelectionState() {
        selectedMessagesMap.clear()
        store.updateAllItems(
            predicate = { it.message.isSelected },
            diff = MessageDiff.DEFAULT_FALSE.copy(selectionChanged = true),
            notifyVisibleOnly = true,
            update = { item -> item.copy(message = item.message.copy(isSelected = false)) }
        )
    }

    internal fun updateMessageByTid(message: SceytMessage): Boolean {
        return store.updateItem(
            predicate = { it.message.tid == message.tid },
            diffProvider = { old, new -> old.message.diff(new.message) },
            update = { item ->
                item.copy(message = item.message.getUpdateMessage(message))
            }
        )
    }

    internal fun messageEditedOrDeleted(updateMessage: SceytMessage) {
        if (updateMessage.isPending() && updateMessage.state == MessageState.Deleted) {
            deleteMessagesByTid(updateMessage.tid)
            return
        }

        store.updateItem(
            predicate = { it.message.id == updateMessage.id },
            diffProvider = { old, new ->
                if (updateMessage.state == MessageState.Deleted &&
                    old.message.state != MessageState.Deleted
                ) null else old.message.diff(new.message)
            },
            update = { item ->
                item.copy(message = item.message.getUpdateMessage(updateMessage))
            }
        )

        updateReplyParent(updateMessage)
    }

    internal fun messageSelfDestructed(updateMessage: SceytMessage) {
        store.updateItem(
            predicate = { it.message.id == updateMessage.id },
            diff = null,
            update = { item ->
                item.copy(message = item.message.getUpdateMessage(updateMessage))
            }
        )
        updateReplyParent(updateMessage)
    }

    private fun updateReplyParent(updateMessage: SceytMessage) {
        store.updateAllItems(
            predicate = { it.message.parentMessage?.id == updateMessage.id },
            diffProvider = { old, new -> old.message.diff(new.message) },
            update = { item ->
                item.copy(message = item.message.copy(parentMessage = updateMessage))
            }
        )
    }

    fun expandMessageBody(messageTid: Long) {
        expandedMessagesMap[messageTid] = true
        store.setBodyExpanded(messageTid)
    }

    private fun observeToUserPresenceUpdateIfNeeded() {
        if (!channel.isDirect() || channel.isSelf) return
        val peer = channel.getPeer() ?: return
        SceytPresenceChecker.addNewUserToPresenceCheck(peer.id)

        SceytPresenceChecker.onPresenceCheckUsersFlow
            .onEach { users ->
                val peer = channel.getPeer() ?: return@onEach
                val presenceUser = users.find { it.user.id == peer.id } ?: return@onEach
                if (peer.user.diff(presenceUser.user).hasDifference()) {
                    updateChannel(notifyUpdate = false) {
                        copy(members = members?.map {
                            if (it.id == peer.id) {
                                it.copy(user = presenceUser.user)
                            } else it
                        })
                    }
                    _peerPresenceUpdatedFlow.postValue(channel)
                }
            }
            .launchIn(viewModelScope)
    }

    fun loadPrevMessages(
        lastMessageId: Long,
        offset: Int,
        loadKey: LoadKeyData = LoadKeyData(value = lastMessageId)
    ) {
        setPagingLoadingStarted(LoadPrev)
        val isLoadingMore = offset > 0

        notifyPageLoadingState(isLoadingMore)

        loadPrevJob = viewModelScope.launch(ioDispatcher) {
            messageInteractor.loadPrevMessages(
                conversationId = conversationId,
                lastMessageId = lastMessageId,
                replyInThread = replyInThread,
                offset = offset,
                loadKey = loadKey,
                awaitToConnectTimeout = 0
            ).collect {
                withContext(mainDispatcher) {
                    initPaginationResponse(it)
                }
            }
        }
    }

    fun loadInitialMessages() {
        val lastMessage = channel.lastMessage
        val lastDisplayedMessageId = channel.lastDisplayedMessageId
        val lastMessageId = lastMessage?.id ?: 0
        when {
            initialTargetMessageId != null -> {
                loadTargetMessage(initialTargetMessageId)
            }

            lastDisplayedMessageId == 0L || lastMessage?.isPending() == true
                    || lastDisplayedMessageId == lastMessageId -> {
                loadPrevMessages(lastMessageId, 0)
            }

            lastDisplayedMessageId >= lastMessageId -> {
                loadPrevMessages(lastDisplayedMessageId, 0)
            }

            else -> {
                pinUnreadSeparatorIfNeeded()
                loadNearMessages(
                    messageId = pinnedLastReadMessageId,
                    loadKey = LoadKeyData(key = LoadKeyType.ScrollToUnreadMessage.longValue),
                    ignoreServer = false
                )
            }
        }
    }

    fun loadTargetMessage(messageId: Long) {
        pinUnreadSeparatorIfNeeded()
        loadNearMessages(
            messageId = messageId,
            loadKey = LoadKeyData(key = LoadKeyType.ScrollToMessageBy.longValue, value = messageId),
            ignoreServer = false
        )
    }

    private fun pinUnreadSeparatorIfNeeded() {
        val lastDisplayedMessageId = channel.lastDisplayedMessageId
        val lastMessageId = channel.lastMessage?.id ?: 0L
        if (lastDisplayedMessageId != 0L && lastDisplayedMessageId < lastMessageId)
            pinnedLastReadMessageId = lastDisplayedMessageId
    }

    fun loadNextMessages(lastMessageId: Long, offset: Int) {
        setPagingLoadingStarted(LoadNext)
        val isLoadingMore = offset > 0

        notifyPageLoadingState(isLoadingMore)

        loadNextJob = viewModelScope.launch(ioDispatcher) {
            messageInteractor.loadNextMessages(
                conversationId = conversationId,
                lastMessageId = lastMessageId,
                replyInThread = replyInThread,
                offset = offset,
            ).collect {
                withContext(mainDispatcher) {
                    initPaginationResponse(it)
                }
            }
        }
    }

    fun loadNewestMessages(loadKey: LoadKeyData) {
        invalidateCenteredSync()
        setPagingLoadingStarted(LoadNewest)
        notifyPageLoadingState(false)

        loadPrevJob?.cancel()
        loadNextJob?.cancel()
        loadNearJob?.cancel()
        loadPrevJob = viewModelScope.launch(ioDispatcher) {
            messageInteractor.loadNewestMessages(
                conversationId = conversationId,
                replyInThread = replyInThread,
                loadKey = loadKey,
                ignoreDb = false,
                awaitToConnectTimeout = 0
            ).collect { response ->
                withContext(mainDispatcher) {
                    initPaginationResponse(response)
                }
            }
        }
    }

    fun loadNearMessages(
        messageId: Long,
        loadKey: LoadKeyData,
        ignoreServer: Boolean,
        awaitToConnectTimeout: Long = 10.seconds.inWholeMilliseconds
    ) {
        invalidateCenteredSync()
        setPagingLoadingStarted(LoadNear, ignoreServer = ignoreServer)
        notifyPageLoadingState(false)

        loadPrevJob?.cancel()
        loadNextJob?.cancel()
        loadNearJob?.cancel()
        loadNearJob = viewModelScope.launch(ioDispatcher) {
            val limit = min(50, SceytChatUIKit.config.queryLimits.messageListQueryLimit * 2)
            messageInteractor.loadNearMessages(
                conversationId = conversationId,
                messageId = messageId,
                replyInThread = replyInThread,
                limit = limit,
                loadKey = loadKey,
                ignoreServer = ignoreServer,
                awaitToConnectTimeout = awaitToConnectTimeout
            ).collect { response ->
                withContext(mainDispatcher) {
                    initPaginationResponse(response)
                }
            }
        }
    }

    fun syncCenteredMessage(messageId: Long) {
        val generation = startCenteredSync(messageId)
        viewModelScope.launch(ioDispatcher) {
            val response = messageInteractor.syncNearMessages(
                conversationId = conversationId,
                messageId = messageId,
                replyInThread = replyInThread
            )
            if (windowSyncGuard.canEmitCenteredSyncResult(response.centerMessageId, generation)) {
                emitRenderEffect(
                    MessageListRenderEffect.ApplyCenteredSync(
                        CenteredSyncMessagesResult(generation = generation, data = response)
                    )
                )
            }
        }
    }

    fun searchMessages(query: String) {
        searchController.search(query)
    }

    internal fun scrollToSearchMessage(isPrev: Boolean) {
        searchController.scrollToSearchMessage(isPrev)
    }

    private fun updateChannel(
        notifyUpdate: Boolean = true,
        updateAction: SceytChannel.() -> SceytChannel,
    ) {
        _channel = _channel.updateAction()
        _conversationId = _channel.id
        if (notifyUpdate)
            _onChannelUpdatedEventFlow.tryEmit(_channel)
    }

    private fun onPendingChannelCreated(newChannel: SceytChannel) {
        val pendingChannelId = channel.id
        cancelMessageLoading()
        resetMessageLoadingState()
        updateChannel { newChannel }
        if (ChannelsCache.currentChannelId == pendingChannelId)
            ChannelsCache.currentChannelId = newChannel.id
        loadInitialMessages()
    }

    private fun cancelMessageLoading() {
        loadPrevJob?.cancel()
        loadNextJob?.cancel()
        loadNearJob?.cancel()
    }

    private fun resetMessageLoadingState() {
        loadPrevOffsetId = 0
        loadNextOffsetId = 0
        pagingRetryState.reset()
        invalidateCenteredSync()
        needSyncMessagesWhenScrollStateIdle = false
        isPreparingToScrollToMessage.set(false)
        store.reset()
    }

    private fun startCenteredSync(messageId: Long): Long {
        lastSyncCenterOffsetId = messageId
        return windowSyncGuard.startCenteredSync(messageId)
    }

    internal fun invalidateCenteredSync() {
        lastSyncCenterOffsetId = 0L
        windowSyncGuard.invalidateCenteredSync()
    }

    private suspend fun getCompareMessage(
        loadType: PaginationResponse.LoadType,
        proportion: List<SceytMessage>,
    ): SceytMessage? = withContext(defaultDispatcher) {
        if (proportion.isEmpty()) return@withContext null
        val proportionFirstId = proportion.first().id
        return@withContext when (loadType) {
            LoadNext, LoadNewest, LoadNear -> {
                currentMessageListItems().lastOrNull {
                    it is MessageItem && it.message.id < proportionFirstId
                }?.let { (it as MessageItem).message }
            }

            LoadPrev -> null
        }
    }

    private fun checkToHideLoadingMoreItemByLoadType(loadType: PaginationResponse.LoadType) {
        when (loadType) {
            LoadPrev if !hasPrevDb -> {
                store.removeItems { it is MessageListItem.LoadingPrevItem }
                emitRenderEffect(MessageListRenderEffect.HideLoadingPrev)
            }

            LoadNext if !hasNextDb -> {
                store.removeItems { it is MessageListItem.LoadingNextItem }
                emitRenderEffect(MessageListRenderEffect.HideLoadingNext)
            }

            LoadNear -> {
                if (!hasPrevDb) {
                    store.removeItems { it is MessageListItem.LoadingPrevItem }
                    emitRenderEffect(MessageListRenderEffect.HideLoadingPrev)
                }
                if (!hasNextDb) {
                    store.removeItems { it is MessageListItem.LoadingNextItem }
                    emitRenderEffect(MessageListRenderEffect.HideLoadingNext)
                }
            }

            else -> Unit
        }
    }

    internal fun canRetryLoadPrevAfterReconnect(): Boolean {
        return pagingRetryState.canRetryPrev(
            loadingFromDb = loadingPrevItemsDb.get(),
            loadingFromServer = loadingPrevItems.get()
        )
    }

    internal fun canRetryLoadNextAfterReconnect(): Boolean {
        return pagingRetryState.canRetryNext(
            loadingFromDb = loadingNextItemsDb.get(),
            loadingFromServer = loadingNextItems.get()
        )
    }

    private fun checkToScrollAfterResponse(response: PaginationResponse<SceytMessage>) {
        val loadKey = when (response) {
            is PaginationResponse.DBResponse -> response.loadKey
            is PaginationResponse.ServerResponse -> response.loadKey
            else -> null
        } ?: return
        val requestId = (loadKey.data as? ScrollRequestData)?.requestId

        when (loadKey.key) {
            LoadKeyType.ScrollToUnreadMessage.longValue -> {
                emitRenderEffect(MessageListRenderEffect.ScrollToUnreadMessage)
            }

            LoadKeyType.ScrollToLastMessage.longValue -> {
                emitRenderEffect(MessageListRenderEffect.ScrollToLastMessage(requestId))
            }

            LoadKeyType.ScrollToReplyMessage.longValue -> {
                emitRenderEffect(
                    MessageListRenderEffect.ScrollToMessage(
                        messageId = loadKey.value,
                        highlight = true,
                        offset = 200,
                        requestId = requestId
                    )
                )
            }

            LoadKeyType.ScrollToMessageBy.longValue -> {
                emitRenderEffect(
                    MessageListRenderEffect.ScrollToMessage(
                        messageId = loadKey.value,
                        highlight = true,
                        offset = 200,
                        requestId = requestId
                    )
                )
                if (response is PaginationResponse.ServerResponse)
                    isPreparingToScrollToMessage.set(false)
            }
        }
    }

    internal suspend fun mapToMessageListItem(
        data: List<SceytMessage>?, hasNext: Boolean, hasPrev: Boolean,
        compareMessage: SceytMessage? = null,
        ignoreUnreadMessagesSeparator: Boolean = false,
    ): List<MessageListItem> = withContext(defaultDispatcher) {
        messageListItemMapper.map(
            data = data,
            hasNext = hasNext,
            hasPrev = hasPrev,
            compareMessage = compareMessage,
            ignoreUnreadMessagesSeparator = ignoreUnreadMessagesSeparator,
            enableDateSeparator = store.enableDateSeparator,
            context = messageListItemMappingContext()
        )
    }

    internal fun initMessageInfoData(
        sceytMessage: SceytMessage,
        prevMessage: SceytMessage? = null,
        initNameAndAvatar: Boolean = false,
    ): SceytMessage {
        return messageListItemMapper.initMessageInfoData(
            sceytMessage = sceytMessage,
            prevMessage = prevMessage,
            initNameAndAvatar = initNameAndAvatar,
            context = messageListItemMappingContext()
        )
    }

    private fun messageListItemMappingContext() = MessageListItemMappingContext(
        channel = channel,
        myIdProvider = { myId },
        pinnedLastReadMessageId = pinnedLastReadMessageId,
        showSenderAvatarAndName = showSenderAvatarAndNameIfNeeded,
        selectedMessageTids = selectedMessagesMap.keys.toSet(),
        expandedMessageTids = expandedMessagesMap.keys.toSet(),
    )

    internal fun checkMaybeHasNext(response: PaginationResponse.DBResponse<SceytMessage>): Boolean {
        var hasNext = response.hasNext
        if (!hasNext) {
            response.data.lastOrNull()?.let { lastMsg ->
                if (lastMsg.isNotPending() && lastMsg.id < (channel.lastMessage?.id ?: 0)) {
                    hasNext = true
                }
            }
        }
        return hasNext
    }

    private suspend fun initPaginationResponse(response: PaginationResponse<SceytMessage>) {
        when (response) {
            is PaginationResponse.DBResponse -> {
                if (!checkIgnoreDatabasePagingResponse(response)) {
                    initPaginationDbResponse(response)
                    notifyPageStateWithResponse(
                        response = SceytResponse.Success(null),
                        wasLoadingMore = response.offset > 0,
                        isEmpty = response.data.isEmpty(),
                        showError = false
                    )
                }
            }

            is PaginationResponse.ServerResponse -> {
                initPaginationServerResponse(response)
                notifyPageStateWithResponse(
                    response = response.data,
                    wasLoadingMore = response.offset > 0,
                    isEmpty = response.cacheData.isEmpty(),
                    showError = false
                )
            }

            else -> return
        }
        pagingResponseReceived(response)
    }

    private suspend fun initPaginationDbResponse(
        response: PaginationResponse.DBResponse<SceytMessage>
    ) = store.withMutation {
        if (response.offset == 0) {
            replaceMessages(
                items = mapToMessageListItem(
                    data = response.data,
                    hasNext = response.hasNext,
                    hasPrev = response.hasPrev,
                ),
                force = true,
            )
        } else {
            when (response.loadType) {
                LoadPrev -> {
                    addPrevPageItems(
                        mapToMessageListItem(
                            data = response.data,
                            hasNext = response.hasNext,
                            hasPrev = response.hasPrev,
                        )
                    )
                }

                LoadNext -> {
                    val hasNext = checkMaybeHasNext(response)
                    val compareMessage = getCompareMessage(response.loadType, response.data)
                    addNextPageItems(
                        mapToMessageListItem(
                            data = response.data,
                            hasNext = hasNext,
                            hasPrev = response.hasPrev,
                            compareMessage = compareMessage,
                        )
                    )
                }

                LoadNear -> {
                    val hasNext = checkMaybeHasNext(response)
                    replaceMessages(
                        items = mapToMessageListItem(
                            data = response.data,
                            hasNext = hasNext,
                            hasPrev = response.hasPrev,
                        ),
                        force = true
                    )
                }

                LoadNewest -> {
                    replaceMessages(
                        items = mapToMessageListItem(
                            data = response.data,
                            hasNext = response.hasNext,
                            hasPrev = response.hasPrev,
                        ),
                        force = true
                    )
                }
            }
        }
        checkToScrollAfterResponse(response)
    }

    private suspend fun initPaginationServerResponse(
        response: PaginationResponse.ServerResponse<SceytMessage>
    ) = store.withMutation {
        pagingRetryState.onServerResponse(response)
        when (response.data) {
            is SceytResponse.Success -> {
                if (response.hasDiff) {
                    val dataToMap = if (response.dbResultWasEmpty) {
                        response.data.data ?: return@withMutation
                    } else response.cacheData

                    val newMessages = mapToMessageListItem(
                        data = dataToMap,
                        hasNext = response.hasNext,
                        hasPrev = response.hasPrev,
                        compareMessage = getCompareMessage(response.loadType, dataToMap),
                    )

                    if (response.dbResultWasEmpty) {
                        when (response.loadType) {
                            LoadNear -> replaceMessages(newMessages, force = true)
                            LoadNext -> addNextPageItems(newMessages)
                            LoadNewest -> replaceMessages(newMessages, force = true)
                            LoadPrev -> addPrevPageItems(newMessages)
                        }
                    } else {
                        replaceMessages(
                            items = newMessages,
                            force = response.loadKey?.key ==
                                    LoadKeyType.ScrollToLastMessage.longValue
                        )
                    }
                } else {
                    checkToHideLoadingMoreItemByLoadType(response.loadType)
                }

                if (response.dbResultWasEmpty)
                    checkToScrollAfterResponse(response)

                loadPrevOffsetId = response.data.data?.firstOrNull()?.id ?: 0
                loadNextOffsetId = response.data.data?.lastOrNull()?.id ?: 0
            }

            is SceytResponse.Error -> {
                if (response.loadKey?.key == LoadKeyType.ScrollToMessageBy.longValue)
                    isPreparingToScrollToMessage.set(false)
            }
        }
    }

    internal suspend fun syncAndAppendMessagesAfter(
        fromMessageId: Long,
        scrollToLastAfterAppend: Boolean,
    ): Boolean {
        val syncedMessages = withContext(ioDispatcher) {
            syncManager.syncConversationMessagesAfter(
                channelId = conversationId,
                fromMessageId = fromMessageId
            )
        } ?: return false

        if (syncedMessages.channel.id != channel.id)
            return false

        updatePinnedLastReadMessageId(syncedMessages.channel)
        return appendSyncedMessages(
            messages = syncedMessages.messages,
            scrollToLastAfterAppend = scrollToLastAfterAppend,
            requiredExistingMessageId = syncedMessages.fromMessageId
        )
    }

    private suspend fun appendSyncedMessages(
        messages: List<SceytMessage>,
        scrollToLastAfterAppend: Boolean,
        requiredExistingMessageId: Long?,
    ): Boolean = store.withMutation {
        if (!canAppendNewestSyncedMessages()) return@withMutation false

        val currentMessages = currentMessageItems()
        if (requiredExistingMessageId != null
            && currentMessages.none { it.id == requiredExistingMessageId }
        )
            return@withMutation false

        val newMessages = messages.minus(currentMessages.toSet())
        if (newMessages.isEmpty()) return@withMutation false

        val merged = store.mergeSynced(
            mapToMessageListItem(
                data = newMessages,
                hasNext = false,
                hasPrev = false,
                compareMessage = currentLastMessageItem()?.message,
            )
        )
        if (merged && scrollToLastAfterAppend)
            emitRenderEffect(MessageListRenderEffect.ScrollToLastMessage())

        merged
    }

    private fun updatePinnedLastReadMessageId(syncChannel: SceytChannel) {
        if (pinnedLastReadMessageId == 0L && syncChannel.lastDisplayedMessageId != 0L
            && syncChannel.lastDisplayedMessageId != syncChannel.lastMessage?.id
        )
            pinnedLastReadMessageId = syncChannel.lastDisplayedMessageId
    }

    private fun canAppendNewestSyncedMessages(): Boolean {
        return windowSyncGuard.canAppendNewestSyncedMessages(
            hasNext = hasNext,
            hasNextDb = hasNextDb,
            isNewestSidePaging = loadingNextItems.get() || loadingNextItemsDb.get()
        )
    }

    internal fun canApplyCenteredSyncResult(
        centerMessageId: Long,
        generation: Long,
        topOffset: Int,
    ): Boolean {
        return windowSyncGuard.canApplyCenteredSyncResult(
            centerMessageId = centerMessageId,
            generation = generation,
            topOffset = topOffset,
            isPaging = loadingFromServer || loadingFromDb,
            isPreparingJump = isPreparingToScrollToMessage.get()
        )
    }

    internal suspend fun mergeMissingMessagesAroundCenter(
        data: SyncNearMessagesResult,
        topOffset: Int,
    ) = store.withMutation {
        if (data.missingMessages.isEmpty()) return@withMutation

        val compareMessage = getCompareMessage(LoadNear, data.missingMessages)
        val merged = store.mergeAroundCenter(
            centerMessageId = data.centerMessageId,
            newItems = mapToMessageListItem(
                data = data.missingMessages,
                hasNext = false,
                hasPrev = false,
                compareMessage = compareMessage,
                ignoreUnreadMessagesSeparator = true,
            )
        )
        if (!merged) return@withMutation

        emitRenderEffect(
            MessageListRenderEffect.ScrollToMessage(
                messageId = data.centerMessageId,
                highlight = false,
                offset = topOffset
            )
        )
    }

    internal suspend fun appendIncomingMessage(message: SceytMessage): Boolean {
        return store.withMutation {
            appendIncomingMessageInternal(message)
        }
    }

    private suspend fun appendIncomingMessageInternal(message: SceytMessage): Boolean {
        if (hasNewestMessageGap()) return false
        val items = mapToMessageListItem(
            data = arrayListOf(message),
            hasNext = false,
            hasPrev = false,
            compareMessage = currentLastMessageItem()?.message,
        )
        val added = addRealtimeItems(items, isOutgoing = false)
        if (added)
            pageStateLiveDataInternal.postValue(PageState.Nothing)
        return added
    }

    internal suspend fun appendOutgoingMessage(message: SceytMessage): Boolean {
        return store.withMutation {
            appendOutgoingMessageInternal(message)
        }
    }

    internal suspend fun handleLocalOutgoingMessage(message: SceytMessage) {
        val appended = appendOutgoingMessage(message)
        /*  if (!appended && hasNewestMessageGap())
              prepareToScrollToNewMessage()*/
    }

    private suspend fun appendOutgoingMessageInternal(message: SceytMessage): Boolean {
        if (hasNewestMessageGap()) return false

        val messageToRender = pendingStatusReconciler.take(message.tid)?.let {
            SceytLog.d(TAG, "Rendering previously not found updated message with tid: ${it.tid}")
            it
        } ?: message

        val items = mapToMessageListItem(
            data = arrayListOf(messageToRender),
            hasNext = false,
            hasPrev = false,
            compareMessage = currentLastMessageItem()?.message,
        )
        val added = addRealtimeItems(items, isOutgoing = true)
        if (added)
            pageStateLiveDataInternal.postValue(PageState.Nothing)
        return added
    }

    internal fun flushNotFoundStatusUpdates() {
        if (pendingStatusReconciler.parkedCount == 0) return
        viewModelScope.launch(mainDispatcher) {
            outgoingMessageMutex.withLock {
                pendingStatusReconciler.reconcile { updateMessageByTid(it) }
            }
        }
    }

    internal fun applyMessageUpdates(messages: List<SceytMessage>) {

        suspend fun update(sceytMessage: SceytMessage) {
            val message = initMessageInfoData(sceytMessage)
            withContext(mainDispatcher) {
                when {
                    message.state == MessageState.Deleted || message.state == MessageState.Edited -> {
                        messageEditedOrDeleted(updateMessage = message)
                    }

                    message.isSelfDestructed() -> {
                        messageSelfDestructed(message)
                    }

                    else -> {
                        pendingStatusReconciler.onStatusUpdate(message) {
                            updateMessageByTid(it)
                        }
                    }
                }
            }
        }

        viewModelScope.launch(defaultDispatcher) {
            messages.forEach { message ->
                if (message.incoming) {
                    update(message)
                } else outgoingMessageMutex.withLock {
                    update(message)
                }
            }
        }
    }

    fun sendPendingMessages() {
        viewModelScope.launch(ioDispatcher) {
            messageInteractor.sendPendingMessages(conversationId)
        }
    }

    fun prepareToEditMessage(message: SceytMessage) {
        _inputCommands.tryEmit(MessageInputCommand.Edit(message))
    }

    fun prepareToShowMessageActions(event: MessageCommandEvent.ShowHideMessageActions) {
        if (event.show)
            messageActionBridge.showMessageActions(event.message)
        else messageActionBridge.hideMessageActions()
    }

    fun prepareToShowSearchMessage(event: MessageCommandEvent.SearchMessages) {
        messageActionBridge.showSearchMessage(event)
    }

    fun prepareToReplyMessage(message: SceytMessage) {
        messageActionBridge.exitSearchMode()
        _inputCommands.tryEmit(MessageInputCommand.Reply(message))
    }

    fun prepareToScrollToNewMessage() {
        emitRenderEffect(MessageListRenderEffect.ScrollToNewMessage(channel.lastMessage))
    }

    fun prepareToScrollToReplyMessage(message: SceytMessage) {
        val parent = message.parentMessage ?: return
        emitRenderEffect(
            MessageListRenderEffect.ScrollToMessage(
                messageId = parent.id,
                highlight = true,
                offset = SCROLL_TO_MESSAGE_OFFSET,
                loadOnMissing = ScrollLoadOnMissing(
                    loadKey = LoadKeyType.ScrollToReplyMessage.longValue,
                    ignoreServer = false,
                ),
            )
        )
    }

    fun prepareToScrollToUnreadMention() {
        mentionsController.prepareToScrollToNext()
    }

    private fun scrollToMessageBy(messageId: Long, addToPendingDisplay: Boolean = false) {
        if (addToPendingDisplay) queuePendingDisplayedMessage(messageId)
        emitRenderEffect(
            MessageListRenderEffect.ScrollToMessage(
                messageId = messageId,
                highlight = true,
                offset = SCROLL_TO_MESSAGE_OFFSET,
                loadOnMissing = ScrollLoadOnMissing(
                    loadKey = LoadKeyType.ScrollToMessageBy.longValue,
                    ignoreServer = false,
                ),
            )
        )
    }

    fun prepareToPauseOrResumeUpload(item: FileListItem) =
        transferController.pauseOrResumeUpload(item)

    internal suspend fun updateProgress(data: TransferData, updateRecyclerView: Boolean) =
        transferController.updateProgress(data, updateRecyclerView)

    internal fun needMediaInfo(data: NeedMediaInfoData) = transferController.needMediaInfo(data)

    internal fun shouldDeferTransferUpdate(transfer: TransferData): Boolean =
        transferController.shouldDeferTransferUpdate(transfer)

    private fun clearPreparingThumbs() = transferController.clearPreparingThumbs()

    internal fun deferTransferUpdate(transfer: TransferData) =
        transferController.deferUpdate(transfer)

    internal suspend fun flushDeferredTransferUpdates() = transferController.flushDeferred()

    @SuppressWarnings("WeakerAccess")
    fun addReaction(
        message: SceytMessage,
        scoreKey: String,
        score: Int = 1,
        reason: String = "",
        enforceUnique: Boolean = false,
    ) {
        reactionController.add(message, scoreKey, score, reason, enforceUnique)
    }

    @SuppressWarnings("WeakerAccess")
    fun deleteReaction(message: SceytMessage, scoreKey: String) {
        reactionController.delete(message, scoreKey)
    }

    internal fun onReactionEvent(event: ReactionEvent) {
        reactionController.onEvent(event)
    }

    internal fun onPollEvent(event: PollEvent) {
        pollController.onEvent(event)
    }

    fun sendMessage(message: Message) {
        val channelId = channel.id
        val interactor = messageInteractor
        viewModelScope.launch {
            withContext(NonCancellable) {
                interactor.sendMessageAsFlow(channelId, message).collect()
            }
        }
    }

    fun sendMessages(messages: List<Message>) {
        val channelId = channel.id
        val interactor = messageInteractor
        viewModelScope.launch {
            withContext(NonCancellable) {
                interactor.sendMessages(channelId, messages)
            }
        }
    }

    fun editMessage(message: SceytMessage) {
        viewModelScope.launch(ioDispatcher) {
            messageInteractor.editMessage(channel.id, message)
        }
    }

    fun deleteMessage(message: SceytMessage, deleteType: DeleteMessageType) {
        viewModelScope.launch(ioDispatcher) {
            messageInteractor.deleteMessage(channel.id, message, deleteType)
        }
    }

    fun deleteMessages(messages: List<SceytMessage>, deleteType: DeleteMessageType) {
        messages.forEach { message ->
            deleteMessage(message, deleteType)
        }
    }

    fun markMessageAsRead(vararg messageIds: Long) {
        viewModelScope.launch(ioDispatcher) {
            val response = messageInteractor.markMessagesAs(
                channelId = channel.id,
                marker = MarkerType.Displayed,
                ids = messageIds
            )

            // Clear unread mentions when message is read
            response.forEach {
                it.onSuccessNotNull { marker ->
                    mentionsController.removeReadMentions(marker.messageIds)
                }
            }
        }
    }

    internal fun markMessageAsDisplayedIfNeeded(message: SceytMessage, isResumed: Boolean) {
        if (channel.userRole.isNullOrEmpty())
            return

        if (!message.incoming || message.userMarkers?.any { it.name == MarkerType.Displayed.value } == true)
            return

        queuePendingDisplayedMessage(message.id)
        if (isResumed) {
            sendDisplayedHelper.submit {
                flushPendingDisplayedMessages()
            }
        }
    }

    internal fun flushPendingDisplayedMessages() {
        if (pendingDisplayMsgIds.isEmpty()) return
        markMessageAsRead(*pendingDisplayMsgIds.toLongArray())
        pendingDisplayMsgIds.clear()
    }

    private fun queuePendingDisplayedMessage(messageId: Long) {
        pendingDisplayMsgIds.add(messageId)
    }

    fun addMessageMarker(marker: String, vararg messageIds: Long) {
        viewModelScope.launch(ioDispatcher) {
            messageInteractor.addMessagesMarker(channel.id, marker, *messageIds)
        }
    }

    fun sendChannelEvent(action: InputUserAction) {
        if (channel.pending) return
        viewModelScope.launch(ioDispatcher) {
            val event = when (action) {
                is InputUserAction.Typing -> {
                    if (action.typing) {
                        SceytConstants.startTypingEvent
                    } else SceytConstants.stopTypingEvent
                }

                is InputUserAction.Recording -> {
                    if (action.recording) {
                        SceytConstants.startRecordingEvent
                    } else SceytConstants.stopRecordingEvent
                }
            }
            messageInteractor.sendChannelEvent(channel.id, event)
        }
    }

    fun updateDraftMessage(
        text: Editable?,
        attachments: List<Attachment>,
        audioRecordData: AudioRecordData?,
        mentionUsers: List<Mention>,
        styling: List<BodyStyleRange>?,
        replyOrEditMessage: SceytMessage?,
        isReply: Boolean,
    ) = draftController.updateDraftMessage(
        text = text,
        attachments = attachments,
        audioRecordData = audioRecordData,
        mentionUsers = mentionUsers,
        styling = styling,
        replyOrEditMessage = replyOrEditMessage,
        isReply = isReply,
    )

    fun join() {
        viewModelScope.launch(ioDispatcher) {
            channelInteractor.join(channel.id).onErrorNonNull {
                pageStateLiveDataInternal.postValue(PageState.StateError(it))
            }
        }
    }

    fun getChannel(channelId: Long) {
        viewModelScope.launch(ioDispatcher) {
            channelInteractor.getChannelFromServer(channelId)
        }
    }

    fun markChannelAsRead(channelId: Long) {
        viewModelScope.launch(ioDispatcher) {
            channelInteractor.markChannelAsRead(channelId).onErrorNonNull {
                pageStateLiveDataInternal.postValue(PageState.StateError(it, showMessage = false))
            }
        }
    }

    fun loadChannelMembersIfNeeded() = memberController.loadIfNeeded()

    fun clearHistory(forEveryOne: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            channelInteractor.clearHistory(channel.id, forEveryOne).onErrorNonNull {
                pageStateLiveDataInternal.postValue(PageState.StateError(it))
            }
        }
    }

    @Suppress("unused")
    fun showSenderAvatarAndNameIfNeeded(show: Boolean) {
        showSenderAvatarAndNameIfNeeded = show
    }

    fun shouldShowViewOnceDialog(): Boolean {
        return preferences.getBoolean(KEY_VIEW_ONCE_INFO_SHOWN, true)
    }

    fun setShouldShowViewOnceDialog(show: Boolean) = viewModelScope.launch(ioDispatcher) {
        preferences.setBoolean(KEY_VIEW_ONCE_INFO_SHOWN, show)
    }

    private fun createUnreadMentionsController() = UnreadMentionsController(
        scope = viewModelScope,
        messageInteractor = messageInteractor,
        channelInteractor = channelInteractor,
        currentChannel = { channel },
        conversationId = { conversationId },
        updateChannel = { action -> updateChannel(updateAction = action) },
        onScrollToMention = { scrollToMessageBy(it, addToPendingDisplay = true) },
        currentUserId = { userInteractor.getCurrentUserId() },
    )

    private fun createMessageSearchController() = MessageSearchController(
        scope = viewModelScope,
        messageInteractor = messageInteractor,
        conversationId = { conversationId },
        replyInThread = replyInThread,
        isPreparingToScrollToMessage = isPreparingToScrollToMessage,
        messageListQueryLimit = { SceytChatUIKit.config.queryLimits.messageListQueryLimit },
        onScrollToSearchMessage = { scrollToMessageBy(it.id) },
    )

    private fun createReactionController() = ReactionController(
        scope = viewModelScope,
        reactionInteractor = messageReactionInteractor,
        channelId = { channel.id },
        notifyResponse = { response, showError ->
            notifyPageStateWithResponse(response, showError = showError)
        },
        ioDispatcher = ioDispatcher,
    )

    private fun createPollController() = PollController(
        scope = viewModelScope,
        pollInteractor = messagePollInteractor,
        channelId = { channel.id },
        notifyResponse = { response, showError ->
            notifyPageStateWithResponse(response, showError = showError)
        },
    )

    private fun createChannelMemberController() = ChannelMemberController(
        scope = viewModelScope,
        memberInteractor = channelMemberInteractor,
        currentChannel = { channel },
        updateChannel = { action -> updateChannel(updateAction = action) },
        ioDispatcher = ioDispatcher,
    )

    private fun createMessageTransferController() = MessageTransferController(
        scope = viewModelScope,
        defaultDispatcher = defaultDispatcher,
        mainDispatcher = mainDispatcher,
        fileTransferService = fileTransferService,
        pauseOrResumeTransferUseCase = pauseOrResumeTransferUseCase,
        store = store,
        channelId = { channel.id },
        ioDispatcher = ioDispatcher,
    )

    private fun createMessageDraftController() = MessageDraftController(
        scope = viewModelScope,
        ioDispatcher = ioDispatcher,
        channelInteractor = channelInteractor,
        conversationId = { conversationId },
        isViewOnceSelected = { viewOnceSelected },
        setViewOnceSelected = { viewOnceSelected = it },
    )
}
