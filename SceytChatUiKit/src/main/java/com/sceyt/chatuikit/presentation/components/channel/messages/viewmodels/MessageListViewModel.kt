package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import android.text.Editable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.message.DeleteMessageType
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.constants.SceytConstants
import com.sceyt.chatuikit.data.managers.channel.ChannelEventManager
import com.sceyt.chatuikit.data.managers.channel.event.ChannelActionEvent
import com.sceyt.chatuikit.data.managers.channel.event.ChannelMemberActivityEvent
import com.sceyt.chatuikit.data.managers.message.MessageEventManager
import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNear
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNewest
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNext
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadPrev
import com.sceyt.chatuikit.data.models.PaginationResponse.Nothing
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.SyncNearMessagesResult
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.MarkerType
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.onErrorNonNull
import com.sceyt.chatuikit.data.models.onSuccessNotNull
import com.sceyt.chatuikit.data.repositories.Keys.KEY_VIEW_ONCE_INFO_SHOWN
import com.sceyt.chatuikit.domain.usecases.PauseOrResumeTransferUseCase
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.media.audio.AudioRecordData
import com.sceyt.chatuikit.persistence.differs.diff
import com.sceyt.chatuikit.persistence.extensions.asLiveData
import com.sceyt.chatuikit.persistence.extensions.broadcastSharedFlow
import com.sceyt.chatuikit.persistence.extensions.getPeer
import com.sceyt.chatuikit.persistence.extensions.isDirect
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.persistence.file_transfer.ThumbFor
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
import com.sceyt.chatuikit.presentation.components.channel.messages.events.MessageCommandEvent
import com.sceyt.chatuikit.presentation.components.channel.messages.events.MessageInputCommand
import com.sceyt.chatuikit.presentation.components.channel.messages.events.PollEvent
import com.sceyt.chatuikit.presentation.components.channel.messages.events.ReactionEvent
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.bindings.LoadKeyType
import com.sceyt.chatuikit.presentation.extensions.isNotPending
import com.sceyt.chatuikit.presentation.helpers.DebounceHelper
import com.sceyt.chatuikit.presentation.helpers.DeferredTransferUpdateBuffer
import com.sceyt.chatuikit.presentation.root.BaseViewModel
import com.sceyt.chatuikit.presentation.root.PageState
import com.sceyt.chatuikit.services.SceytPresenceChecker
import com.sceyt.chatuikit.services.sync.SceytSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import java.util.Collections
import kotlin.math.min

class MessageListViewModel(
    private var _conversationId: Long,
    private var _channel: SceytChannel,
    val replyInThread: Boolean = false,
    val initialTargetMessageId: Long? = null,
) : BaseViewModel(), SceytKoinComponent {
    private val messageInteractor: MessageInteractor by inject()
    internal val channelInteractor: ChannelInteractor by inject()
    private val messageReactionInteractor: MessageReactionInteractor by inject()
    private val messagePollInteractor: MessagePollInteractor by inject()
    internal val attachmentInteractor: AttachmentInteractor by inject()
    internal val channelMemberInteractor: ChannelMemberInteractor by inject()
    internal val connectionLogic: PersistenceConnectionLogic by inject()
    internal val userInteractor: UserInteractor by inject()
    internal val syncManager: SceytSyncManager by inject()
    private val fileTransferService: FileTransferService by inject()
    private val pauseOrResumeTransferUseCase: PauseOrResumeTransferUseCase by inject()
    private val preferences: SceytSharedPreference by inject()
    internal var pinnedLastReadMessageId: Long = 0
    internal val sendDisplayedHelper by lazy { DebounceHelper(200L, viewModelScope) }
    internal val messageActionBridge by lazy { MessageActionBridge() }
    internal val placeToSavePathsList = mutableSetOf<Pair<AttachmentTypeEnum, String>>()
    internal val selectedMessagesMap by lazy { mutableMapOf<MessageTid, SceytMessage>() }
    internal val expandedMessagesMap by lazy { mutableMapOf<Long, Boolean>() }
    internal val pendingStatusReconciler by lazy { PendingMessageStatusReconciler() }
    internal val outgoingMessageMutex by lazy { Mutex() }
    internal val pendingDisplayMsgIds by lazy { Collections.synchronizedSet(mutableSetOf<Long>()) }
    internal val needToUpdateTransferAfterOnResume = DeferredTransferUpdateBuffer(ThumbFor.MessagesLisView)
    private var showSenderAvatarAndNameIfNeeded = true
    internal var viewOnceSelected = false
    private var loadPrevJob: Job? = null
    private var loadNextJob: Job? = null
    private var loadNearJob: Job? = null
    internal var mentionJob: Job? = null

    // Pagination sync
    internal var needSyncMessagesWhenScrollStateIdle = false
    internal var loadPrevOffsetId = 0L
    internal var loadNextOffsetId = 0L
    internal var lastSyncCenterOffsetId = 0L

    private val myId: String? get() = userInteractor.getCurrentUserId()
    val channel: SceytChannel get() = _channel
    val conversationId: Long get() = _conversationId

    private val _loadMessagesFlow = MutableStateFlow<PaginationResponse<SceytMessage>>(Nothing())
    val loadMessagesFlow = _loadMessagesFlow.asStateFlow()

    private val _messageMarkerLiveData = MutableLiveData<List<SceytResponse<MessageListMarker>>>()
    val messageMarkerLiveData = _messageMarkerLiveData.asLiveData()

    private val _syncCenteredMessageLiveData = MutableLiveData<SyncNearMessagesResult>()
    val syncCenteredMessageLiveData = _syncCenteredMessageLiveData.asLiveData()

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

    private val _peerPresenceUpdatedFlow = MutableLiveData<SceytChannel>()
    val peerPresenceUpdatedFlow = _peerPresenceUpdatedFlow.asLiveData()

    //Command events
    private val _inputCommands = MutableSharedFlow<MessageInputCommand>(extraBufferCapacity = 8)
    internal val inputCommands = _inputCommands.asSharedFlow()
    private val _onScrollToLastMessageLiveData = MutableLiveData<SceytMessage?>()
    internal val onScrollToLastMessageLiveData = _onScrollToLastMessageLiveData.asLiveData()
    private val _onScrollToReplyMessageLiveData = MutableLiveData<SceytMessage>()
    internal val onScrollToReplyMessageLiveData = _onScrollToReplyMessageLiveData.asLiveData()
    private val _onScrollToSearchMessageLiveData = MutableLiveData<SceytMessage>()
    internal val onScrollToSearchMessageLiveData = _onScrollToSearchMessageLiveData.asLiveData()
    private val _onScrollToUnredMentionMessageLiveData = MutableLiveData<Long>()
    internal val onScrollToUnredMentionMessageLiveData =
        _onScrollToUnredMentionMessageLiveData.asLiveData()

    private val mentionsController = createUnreadMentionsController()
    private val reactionController = createReactionController()
    private val pollController = createPollController()
    private val memberController = createChannelMemberController()
    private val draftController = createMessageDraftController()
    private val searchController = createMessageSearchController()
    private val messageListItemMapper by lazy { MessageListItemMapper() }

    val searchResult get() = searchController.searchResult


    init {
        observeToUserPresenceUpdateIfNeeded()

        onNewMessageFlow = messageInteractor.getOnMessageFlow()
            .filter { (channel) ->
                channel.id == this.channel.id /*&& it.second.replyInThread == replyInThread*/
            }
            .map { (_, message) ->
                mentionsController.onNewMessage(message)
                initMessageInfoData(message)
            }

        MessageEventManager.onMessageEditedOrDeletedFlow.onEach { message ->
            mentionsController.onMessageUpdated(message)
        }.launchIn(viewModelScope)

        ChannelEventManager.onChannelMembersEventFlow
            .filter { it.channel.id == channel.id }
            .onEach(memberController::onMemberEvent)
            .launchIn(viewModelScope)

        /*
       // todo reply in thread
        onNewThreadMessageFlow = MessageEventsObserver.onMessageFlow
              .filter { it.first.id == channel.id && it.second.replyInThread }
              .mapNotNull { initMessageInfoData(it.second) }*/

        onChannelEventFlow = ChannelEventManager.onChannelEventFlow
            .filter { it.channelId == channel.id }

        onChannelMemberActivityEventFlow = ChannelEventManager.onChannelMemberActivityEventFlow
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

        onNewOutGoingMessageFlow = MessageEventManager.onOutgoingMessageFlow
            .filter { it.channelId == channel.id /*&& !it.replyInThread*/ }

        /*onOutGoingThreadMessageFlow = MessageEventsObserver.onOutgoingMessageFlow
            .filter { it.channelId == channel.id && it.replyInThread }*/

        mentionsController.onInit()
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

        loadPrevJob = viewModelScope.launch(Dispatchers.IO) {
            messageInteractor.loadPrevMessages(
                conversationId = conversationId,
                lastMessageId = lastMessageId,
                replyInThread = replyInThread,
                offset = offset,
                loadKey = loadKey
            ).collect {
                withContext(Dispatchers.Main) {
                    initPaginationResponse(it)
                }
            }
        }
    }

    fun loadInitialMessagesForCurrentChannel() {
        val lastMessage = channel.lastMessage
        val lastDisplayedMessageId = channel.lastDisplayedMessageId
        val lastMessageId = lastMessage?.id ?: 0
        when {
            initialTargetMessageId != null -> {
                loadNearMessages(
                    messageId = initialTargetMessageId,
                    loadKey = LoadKeyData(
                        key = LoadKeyType.ScrollToMessageBy.longValue,
                        value = initialTargetMessageId
                    ),
                    ignoreServer = false
                )
            }

            lastDisplayedMessageId == 0L || lastMessage?.incoming == false
                    || lastDisplayedMessageId == lastMessageId -> {
                loadPrevMessages(lastMessageId, 0)
            }

            lastDisplayedMessageId >= lastMessageId -> {
                loadPrevMessages(lastDisplayedMessageId, 0)
            }

            else -> {
                pinnedLastReadMessageId = lastDisplayedMessageId
                loadNearMessages(
                    messageId = pinnedLastReadMessageId,
                    loadKey = LoadKeyData(
                        key = LoadKeyType.ScrollToUnreadMessage.longValue,
                        value = pinnedLastReadMessageId
                    ),
                    ignoreServer = false
                )
            }
        }
    }

    fun loadNextMessages(lastMessageId: Long, offset: Int) {
        setPagingLoadingStarted(LoadNext)
        val isLoadingMore = offset > 0

        notifyPageLoadingState(isLoadingMore)

        loadNextJob = viewModelScope.launch(Dispatchers.IO) {
            messageInteractor.loadNextMessages(
                conversationId = conversationId,
                lastMessageId = lastMessageId,
                replyInThread = replyInThread,
                offset = offset
            ).collect {
                withContext(Dispatchers.Main) {
                    initPaginationResponse(it)
                }
            }
        }
    }

    fun loadNearMessages(messageId: Long, loadKey: LoadKeyData, ignoreServer: Boolean) {
        setPagingLoadingStarted(LoadNear, ignoreServer = ignoreServer)
        notifyPageLoadingState(false)

        loadPrevJob?.cancel()
        loadNextJob?.cancel()
        loadNearJob?.cancel()
        loadNearJob = viewModelScope.launch(Dispatchers.IO) {
            val limit = min(50, SceytChatUIKit.config.queryLimits.messageListQueryLimit * 2)
            messageInteractor.loadNearMessages(
                conversationId = conversationId,
                messageId = messageId,
                replyInThread = replyInThread,
                limit = limit,
                loadKey = loadKey,
                ignoreServer = ignoreServer
            ).collect { response ->
                withContext(Dispatchers.Main) {
                    initPaginationResponse(response)
                }
            }
        }
    }

    @Suppress("unused")
    fun loadNewestMessages(loadKey: LoadKeyData) {
        setPagingLoadingStarted(LoadNewest)

        loadNearJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            messageInteractor.loadNewestMessages(
                conversationId = conversationId,
                replyInThread = replyInThread,
                loadKey = loadKey,
                ignoreDb = false
            ).collect { response ->
                withContext(Dispatchers.Main) {
                    initPaginationResponse(response)
                }
            }
        }
    }

    fun syncCenteredMessage(messageId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = messageInteractor.syncNearMessages(
                conversationId = conversationId,
                messageId = messageId,
                replyInThread = replyInThread
            )
            _syncCenteredMessageLiveData.postValue(response)
        }
    }

    fun searchMessages(query: String) {
        searchController.search(query)
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
        loadInitialMessagesForCurrentChannel()
    }

    private fun cancelMessageLoading() {
        loadPrevJob?.cancel()
        loadNextJob?.cancel()
        loadNearJob?.cancel()
    }

    private fun resetMessageLoadingState() {
        loadPrevOffsetId = 0
        loadNextOffsetId = 0
        lastSyncCenterOffsetId = 0
        needSyncMessagesWhenScrollStateIdle = false
        resetPreparingToScrollToMessage()
    }

    private fun initPaginationResponse(response: PaginationResponse<SceytMessage>) {
        when (response) {
            is PaginationResponse.DBResponse -> {
                if (!checkIgnoreDatabasePagingResponse(response)) {
                    _loadMessagesFlow.value = response
                    notifyPageStateWithResponse(
                        response = SceytResponse.Success(null),
                        wasLoadingMore = response.offset > 0,
                        isEmpty = response.data.isEmpty(),
                        showError = false
                    )
                }
            }

            is PaginationResponse.ServerResponse -> {
                _loadMessagesFlow.value = response
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

    fun sendPendingMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            messageInteractor.sendPendingMessages(conversationId)
        }
    }

    fun prepareToEditMessage(message: SceytMessage) {
        _inputCommands.tryEmit(MessageInputCommand.Edit(message))
    }

    fun prepareToShowMessageActions(event: MessageCommandEvent.ShowHideMessageActions) {
        messageActionBridge.showMessageActions(event.message)
    }

    fun prepareToShowSearchMessage(event: MessageCommandEvent.SearchMessages) {
        messageActionBridge.showSearchMessage(event)
    }

    fun prepareToReplyMessage(message: SceytMessage) {
        messageActionBridge.exitSearchMode()
        _inputCommands.tryEmit(MessageInputCommand.Reply(message))
    }

    fun prepareToScrollToNewMessage() {
        _onScrollToLastMessageLiveData.postValue(channel.lastMessage)
    }

    fun prepareToScrollToReplyMessage(message: SceytMessage) {
        _onScrollToReplyMessageLiveData.postValue(message.parentMessage ?: return)
    }

    fun prepareToScrollToUnreadMention() {
        mentionsController.prepareToScrollToNext()
    }

    fun prepareToPauseOrResumeUpload(item: FileListItem) {
        viewModelScope.launch {
            pauseOrResumeTransferUseCase(item.attachment, channel.id)
        }
    }

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
        viewModelScope.launch(Dispatchers.IO) {
            messageInteractor.editMessage(channel.id, message)
        }
    }

    fun deleteMessage(message: SceytMessage, deleteType: DeleteMessageType) {
        viewModelScope.launch(Dispatchers.IO) {
            messageInteractor.deleteMessage(channel.id, message, deleteType)
        }
    }

    fun deleteMessages(messages: List<SceytMessage>, deleteType: DeleteMessageType) {
        messages.forEach { message ->
            deleteMessage(message, deleteType)
        }
    }

    fun markMessageAsRead(vararg messageIds: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = messageInteractor.markMessagesAs(
                channelId = channel.id,
                marker = MarkerType.Displayed,
                ids = messageIds
            )
            _messageMarkerLiveData.postValue(response)

            // Cleat unread mentions when message is read
            response.forEach {
                it.onSuccessNotNull { marker ->
                    mentionsController.removeReadMentions(marker.messageIds)
                }
            }
        }
    }

    fun addMessageMarker(marker: String, vararg messageIds: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = messageInteractor.addMessagesMarker(channel.id, marker, *messageIds)
            _messageMarkerLiveData.postValue(response)
        }
    }

    fun sendChannelEvent(action: InputUserAction) {
        if (channel.pending) return
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
            channelInteractor.join(channel.id).onErrorNonNull {
                pageStateLiveDataInternal.postValue(PageState.StateError(it))
            }
        }
    }

    fun getChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            channelInteractor.getChannelFromServer(channelId)
        }
    }

    fun markChannelAsRead(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            channelInteractor.markChannelAsRead(channelId).onErrorNonNull {
                pageStateLiveDataInternal.postValue(PageState.StateError(it))
            }
        }
    }

    fun loadChannelMembersIfNeeded() = memberController.loadIfNeeded()

    @Suppress("unused")
    fun loadChannelAllMembers() {
        viewModelScope.launch(Dispatchers.IO) {

            suspend fun loadMembers(
                offset: Int,
                nextToken: String
            ): PaginationResponse.ServerResponse<SceytMember>? {
                return channelMemberInteractor.loadChannelMembers(
                    channel.id,
                    offset,
                    nextToken,
                    null
                )
                    .firstOrNull {
                        it is PaginationResponse.ServerResponse
                    } as? PaginationResponse.ServerResponse<SceytMember>
            }

            val count = channelMemberInteractor.getMembersCountFromDb(channel.id)
            if (channel.memberCount > count) {
                var offset = 0
                var rest = loadMembers(0, "")
                while (rest?.hasNext == true) {
                    offset += rest.data.data?.size ?: return@launch
                    rest = loadMembers(offset, rest.nextToken)
                }
            }
        }
    }

    @SuppressWarnings("WeakerAccess")
    fun loadChannelMembers(
        offset: Int,
        nextToken: String,
        role: String?
    ): Flow<PaginationResponse<SceytMember>> {
        return memberController.loadMembers(
            offset = offset,
            nextToken = nextToken,
            role = role
        )
    }

    fun clearHistory(forEveryOne: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            channelInteractor.clearHistory(channel.id, forEveryOne).onErrorNonNull {
                pageStateLiveDataInternal.postValue(PageState.StateError(it))
            }
        }
    }

    fun expandMessageBody(messageTid: Long) {
        expandedMessagesMap[messageTid] = true
    }

    @Suppress("unused")
    fun showSenderAvatarAndNameIfNeeded(show: Boolean) {
        showSenderAvatarAndNameIfNeeded = show
    }

    fun shouldShowViewOnceDialog(): Boolean {
        return preferences.getBoolean(KEY_VIEW_ONCE_INFO_SHOWN, true)
    }

    fun setShouldShowViewOnceDialog(show: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        preferences.setBoolean(KEY_VIEW_ONCE_INFO_SHOWN, show)
    }

    internal suspend fun mapToMessageListItem(
        data: List<SceytMessage>?, hasNext: Boolean, hasPrev: Boolean,
        compareMessage: SceytMessage? = null,
        ignoreUnreadMessagesSeparator: Boolean = false,
        enableDateSeparator: Boolean,
    ): List<MessageListItem> = withContext(Dispatchers.Default) {
        messageListItemMapper.map(
            data = data,
            hasNext = hasNext,
            hasPrev = hasPrev,
            compareMessage = compareMessage,
            ignoreUnreadMessagesSeparator = ignoreUnreadMessagesSeparator,
            enableDateSeparator = enableDateSeparator,
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

    internal fun checkMaybeHesNext(response: PaginationResponse.DBResponse<SceytMessage>): Boolean {
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

    internal fun onReactionEvent(event: ReactionEvent) {
        reactionController.onEvent(event)
    }

    internal fun onPollEvent(event: PollEvent) {
        pollController.onEvent(event)
    }

    internal fun needMediaInfo(data: NeedMediaInfoData) {
        val attachment = data.item
        when (data) {
            is NeedMediaInfoData.NeedDownload -> {
                viewModelScope.launch(Dispatchers.IO) {
                    fileTransferService.download(
                        attachment = attachment,
                        transferTask = fileTransferService.findOrCreateTransferTask(attachment)
                    )
                }
            }

            is NeedMediaInfoData.NeedThumb -> {
                viewModelScope.launch(Dispatchers.IO) {
                    fileTransferService.getThumb(attachment.messageTid, attachment, data.thumbData)
                }
            }
        }
    }

    internal fun clearPreparingThumbs() {
        fileTransferService.clearPreparingThumbPaths()
    }

    internal fun scrollToSearchMessage(isPrev: Boolean) {
        searchController.scrollToSearchMessage(isPrev)
    }

    internal fun resetPreparingToScrollToMessage() {
        searchController.resetScrollPreparation()
    }

    private fun createChannelMemberController() = ChannelMemberController(
        scope = viewModelScope,
        memberInteractor = channelMemberInteractor,
        currentChannel = { channel },
        updateChannel = { action -> updateChannel(updateAction = action) },
    )

    private fun createUnreadMentionsController() = UnreadMentionsController(
        scope = viewModelScope,
        messageInteractor = messageInteractor,
        channelInteractor = channelInteractor,
        currentChannel = { channel },
        conversationId = { conversationId },
        updateChannel = { action -> updateChannel(updateAction = action) },
        onScrollToMention = { _onScrollToUnredMentionMessageLiveData.postValue(it) },
        currentUserId = { userInteractor.getCurrentUserId() },
    )

    private fun createMessageSearchController() = MessageSearchController(
        scope = viewModelScope,
        messageInteractor = messageInteractor,
        conversationId = { conversationId },
        replyInThread = replyInThread,
        messageListQueryLimit = { SceytChatUIKit.config.queryLimits.messageListQueryLimit },
        onScrollToSearchMessage = { _onScrollToSearchMessageLiveData.postValue(it) },
    )

    private fun createMessageDraftController() = MessageDraftController(
        scope = viewModelScope,
        ioDispatcher = Dispatchers.IO,
        channelInteractor = channelInteractor,
        conversationId = { conversationId },
        isViewOnceSelected = { viewOnceSelected },
        setViewOnceSelected = { viewOnceSelected = it },
    )

    private fun createReactionController() = ReactionController(
        scope = viewModelScope,
        reactionInteractor = messageReactionInteractor,
        channelId = { channel.id },
        notifyResponse = { response, showError ->
            notifyPageStateWithResponse(response, showError = showError)
        },
    )

    private fun createPollController() = PollController(
        scope = viewModelScope,
        pollInteractor = messagePollInteractor,
        channelId = { channel.id },
        notifyResponse = { response, showError ->
            notifyPageStateWithResponse(response, showError = showError)
        },
    )
}
