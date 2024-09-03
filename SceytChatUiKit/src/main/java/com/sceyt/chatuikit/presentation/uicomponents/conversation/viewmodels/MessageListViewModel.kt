package com.sceyt.chatuikit.presentation.uicomponents.conversation.viewmodels

import android.app.Application
import android.text.Editable
import android.view.Menu
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import com.sceyt.chat.models.message.DeleteMessageType
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.channeleventobserver.ChannelEventData
import com.sceyt.chatuikit.data.channeleventobserver.ChannelEventsObserver
import com.sceyt.chatuikit.data.channeleventobserver.ChannelMembersEventData
import com.sceyt.chatuikit.data.channeleventobserver.ChannelMembersEventEnum
import com.sceyt.chatuikit.data.channeleventobserver.ChannelTypingEventData
import com.sceyt.chatuikit.data.messageeventobserver.MessageEventsObserver
import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNear
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNewest
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNext
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadPrev
import com.sceyt.chatuikit.data.models.SceytPagingResponse
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.SyncNearMessagesResult
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.chatuikit.data.models.messages.MarkerTypeEnum
import com.sceyt.chatuikit.data.models.messages.MessageTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytReactionTotal
import com.sceyt.chatuikit.data.toFileListItem
import com.sceyt.chatuikit.extensions.findIndexed
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.extensions.asLiveData
import com.sceyt.chatuikit.persistence.extensions.toArrayList
import com.sceyt.chatuikit.persistence.filetransfer.FileTransferHelper
import com.sceyt.chatuikit.persistence.filetransfer.FileTransferService
import com.sceyt.chatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.chatuikit.persistence.filetransfer.TransferData
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.Downloaded
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.Downloading
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.ErrorDownload
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.ErrorUpload
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.FilePathChanged
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.PauseDownload
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.PauseUpload
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.PendingDownload
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.PendingUpload
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.Preparing
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.ThumbLoaded
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.Uploaded
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.Uploading
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.WaitingToUpload
import com.sceyt.chatuikit.persistence.interactor.AttachmentInteractor
import com.sceyt.chatuikit.persistence.interactor.ChannelInteractor
import com.sceyt.chatuikit.persistence.interactor.ChannelMemberInteractor
import com.sceyt.chatuikit.persistence.interactor.MessageInteractor
import com.sceyt.chatuikit.persistence.interactor.MessageReactionInteractor
import com.sceyt.chatuikit.persistence.interactor.UserInteractor
import com.sceyt.chatuikit.persistence.logicimpl.channelslogic.ChannelsCache
import com.sceyt.chatuikit.persistence.logicimpl.messageslogic.MessagesCache
import com.sceyt.chatuikit.persistence.workers.SendAttachmentWorkManager
import com.sceyt.chatuikit.presentation.root.BaseViewModel
import com.sceyt.chatuikit.presentation.uicomponents.conversation.LoadKeyType
import com.sceyt.chatuikit.presentation.uicomponents.conversation.MessageActionBridge
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.reactions.ReactionItem
import com.sceyt.chatuikit.presentation.uicomponents.conversation.events.MessageCommandEvent
import com.sceyt.chatuikit.presentation.uicomponents.conversation.events.ReactionEvent
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.SearchResult
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.mention.Mention
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.style.BodyStyleRange
import com.sceyt.chatuikit.presentation.uicomponents.searchinput.DebounceHelper
import com.sceyt.chatuikit.services.SceytSyncManager
import com.sceyt.chatuikit.shared.helpers.LinkPreviewHelper
import com.sceyt.chatuikit.shared.utils.DateTimeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

class MessageListViewModel(
        var conversationId: Long,
        val replyInThread: Boolean = false,
        var channel: SceytChannel,
) : BaseViewModel(), SceytKoinComponent {

    private val messageInteractor: MessageInteractor by inject()
    internal val channelInteractor: ChannelInteractor by inject()
    private val messageReactionInteractor: MessageReactionInteractor by inject()
    internal val attachmentInteractor: AttachmentInteractor by inject()
    internal val channelMemberInteractor: ChannelMemberInteractor by inject()
    internal val userInteractor: UserInteractor by inject()
    private val application: Application by inject()
    private val syncManager: SceytSyncManager by inject()
    private val fileTransferService: FileTransferService by inject()
    private val messagesCache: MessagesCache by inject()
    private val linkPreviewHelper by lazy { LinkPreviewHelper(application, viewModelScope) }
    internal var pinnedLastReadMessageId: Long = 0
    internal val sendDisplayedHelper by lazy { DebounceHelper(200L, viewModelScope) }
    internal val messageActionBridge by lazy { MessageActionBridge() }
    internal val placeToSavePathsList = mutableSetOf<String>()
    internal val selectedMessagesMap by lazy { mutableMapOf<Long, SceytMessage>() }
    internal val notFoundMessagesToUpdate by lazy { mutableMapOf<Long, SceytMessage>() }
    internal var scrollToSearchMessageJob: Job? = null
    internal val outgoingMessageMutex by lazy { Mutex() }
    internal val pendingDisplayMsgIds by lazy { Collections.synchronizedSet(mutableSetOf<Long>()) }
    internal val needToUpdateTransferAfterOnResume = hashMapOf<Long, TransferData>()
    private var showSenderAvatarAndNameIfNeeded = true
    private var loadPrevJob: Job? = null
    private val loadNextJob: Job? = null
    private var loadNearJob: Job? = null

    // Pagination sync
    internal var needSyncMessagesWhenScrollStateIdle = false
    internal var loadPrevOffsetId = 0L
        private set
    internal var loadNextOffsetId = 0L
        private set
    internal var lastSyncCenterOffsetId = 0L

    private val isGroup = channel.isGroup
    private val myId: String? get() = SceytChatUIKit.chatUIFacade.myId

    private val _loadMessagesFlow = MutableStateFlow<Pair<PaginationResponse<SceytMessage>, List<MessageListItem>>>(
        PaginationResponse.Nothing<SceytMessage>() to emptyList()
    )
    val loadMessagesFlow = _loadMessagesFlow.asStateFlow()

    private val _messageForceDeleteLiveData = MutableLiveData<SceytResponse<SceytMessage>>()
    val checkMessageForceDeleteLiveData = _messageForceDeleteLiveData.asLiveData()

    private val _joinLiveData = MutableLiveData<SceytResponse<SceytChannel>>()
    val joinLiveData = _joinLiveData.asLiveData()

    private val _channelLiveData = MutableLiveData<SceytResponse<SceytChannel>>()
    val channelLiveData = _channelLiveData.asLiveData()

    private val _messageMarkerLiveData = MutableLiveData<List<SceytResponse<MessageListMarker>>>()
    val messageMarkerLiveData = _messageMarkerLiveData.asLiveData()

    private val _onChannelMemberAddedOrKickedLiveData = MutableLiveData<SceytChannel>()
    val onChannelMemberAddedOrKickedLiveData = _onChannelMemberAddedOrKickedLiveData.asLiveData()

    private val _syncCenteredMessageLiveData = MutableLiveData<SyncNearMessagesResult>()
    val syncCenteredMessageLiveData = _syncCenteredMessageLiveData.asLiveData()

    private val _linkPreviewLiveData = MutableLiveData<LinkPreviewDetails>()
    val linkPreviewLiveData = _linkPreviewLiveData.asLiveData()


    // Message events
    val onNewMessageFlow: Flow<SceytMessage>
    val onNewOutGoingMessageFlow: Flow<SceytMessage>

    //val onNewThreadMessageFlow: Flow<SceytMessage>// todo reply in thread

    // val onOutGoingThreadMessageFlow: Flow<SceytMessage>// todo reply in thread
    val onTransferUpdatedLiveData: LiveData<TransferData>


    // Chanel events
    val onChannelEventFlow: Flow<ChannelEventData>
    val onChannelTypingEventFlow: Flow<ChannelTypingEventData>
    val onChannelUpdatedEventFlow: Flow<SceytChannel>

    //Command events
    private val _onEditMessageCommandLiveData = MutableLiveData<SceytMessage>()
    internal val onEditMessageCommandLiveData = _onEditMessageCommandLiveData.asLiveData()
    private val _onReplyMessageCommandLiveData = MutableLiveData<SceytMessage>()
    internal val onReplyMessageCommandLiveData = _onReplyMessageCommandLiveData.asLiveData()
    private val _onScrollToLastMessageLiveData = MutableLiveData<SceytMessage?>()
    internal val onScrollToLastMessageLiveData = _onScrollToLastMessageLiveData.asLiveData()
    private val _onScrollToReplyMessageLiveData = MutableLiveData<SceytMessage>()
    internal val onScrollToReplyMessageLiveData = _onScrollToReplyMessageLiveData.asLiveData()
    private val _onScrollToSearchMessageLiveData = MutableLiveData<SceytMessage>()
    internal val onScrollToSearchMessageLiveData = _onScrollToSearchMessageLiveData.asLiveData()

    // Search messages
    internal val isSearchingMessageToScroll = AtomicBoolean(false)
    private val isLoadingNearToSearchMessagesServer = AtomicBoolean(false)
    private var _searchResult = MutableLiveData<SearchResult>()
    var searchResult = _searchResult.asLiveData()


    init {
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

        onNewMessageFlow = messageInteractor.getOnMessageFlow()
            .filter { it.first.id == channel.id /*&& it.second.replyInThread == replyInThread*/ }
            .mapNotNull { initMessageInfoData(it.second) }

        /*
       // todo reply in thread
        onNewThreadMessageFlow = MessageEventsObserver.onMessageFlow
              .filter { it.first.id == channel.id && it.second.replyInThread }
              .mapNotNull { initMessageInfoData(it.second) }*/

        onChannelEventFlow = ChannelEventsObserver.onChannelEventFlow
            .filter { it.channelId == channel.id }

        onChannelTypingEventFlow = ChannelEventsObserver.onChannelTypingEventFlow
            .filter { it.channel.id == channel.id && it.member.id != myId }

        onChannelUpdatedEventFlow = ChannelsCache.channelUpdatedFlow
            .filter { it.channel.id == channel.id }
            .map { it.channel }

        viewModelScope.launch(Dispatchers.IO) {
            ChannelEventsObserver.onChannelMembersEventFlow
                .filter { it.channel.id == channel.id }
                .collect(::onChannelMemberEvent)
        }

        ChannelsCache.pendingChannelCreatedFlow
            .filter { it.first == channel.id }
            .onEach { data ->
                val newChannelId = data.second.id
                channel = channel.copy(id = newChannelId, pending = false)
                conversationId = newChannelId
            }.launchIn(viewModelScope)

        onNewOutGoingMessageFlow = MessageEventsObserver.onOutgoingMessageFlow
            .filter { it.channelId == channel.id /*&& !it.replyInThread*/ }

        /*onOutGoingThreadMessageFlow = MessageEventsObserver.onOutgoingMessageFlow
            .filter { it.channelId == channel.id && it.replyInThread }*/

        onTransferUpdatedLiveData = FileTransferHelper.onTransferUpdatedLiveData
    }

    fun loadPrevMessages(lastMessageId: Long, offset: Int, loadKey: LoadKeyData = LoadKeyData(value = lastMessageId)) {
        setPagingLoadingStarted(LoadPrev)
        val isLoadingMore = offset > 0

        notifyPageLoadingState(isLoadingMore)

        loadPrevJob = viewModelScope.launch(Dispatchers.IO) {
            messageInteractor.loadPrevMessages(conversationId, lastMessageId, replyInThread, offset, loadKey = loadKey).collect {
                initPaginationResponse(it)
            }
        }
    }

    fun loadNextMessages(lastMessageId: Long, offset: Int) {
        setPagingLoadingStarted(LoadNext)
        val isLoadingMore = offset > 0

        notifyPageLoadingState(isLoadingMore)

        loadPrevJob = viewModelScope.launch(Dispatchers.IO) {
            messageInteractor.loadNextMessages(conversationId, lastMessageId, replyInThread, offset).collect {
                withContext(Dispatchers.Main) {
                    initPaginationResponse(it)
                }
            }
        }
    }

    fun loadNearMessages(messageId: Long, loadKey: LoadKeyData, ignoreServer: Boolean) {
        setPagingLoadingStarted(LoadNear, ignoreServer = ignoreServer)
        loadPrevJob?.cancel()
        loadNextJob?.cancel()
        loadNearJob = viewModelScope.launch(Dispatchers.IO) {
            val limit = min(50, SceytChatUIKit.config.messagesLoadSize * 2)
            messageInteractor.loadNearMessages(conversationId, messageId, replyInThread,
                limit, loadKey, ignoreServer = ignoreServer).collect { response ->
                withContext(Dispatchers.Main) {
                    initPaginationResponse(response)
                }
            }
        }
    }

    fun loadNewestMessages(loadKey: LoadKeyData) {
        setPagingLoadingStarted(LoadNewest)

        loadNearJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            messageInteractor.loadNewestMessages(conversationId, replyInThread,
                loadKey = loadKey, ignoreDb = false).collect { response ->
                withContext(Dispatchers.Main) {
                    initPaginationResponse(response)
                }
            }
        }
    }

    fun syncCenteredMessage(messageId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = messageInteractor.syncNearMessages(conversationId, messageId, replyInThread)
            _syncCenteredMessageLiveData.postValue(response)
        }
    }

    fun searchMessages(query: String) {
        viewModelScope.launch {
            val resp = messageInteractor.searchMessages(conversationId, replyInThread, query)
            (resp as? SceytPagingResponse.Success)?.let {
                it.data?.let { messages ->
                    val reversed = messages.reversed()
                    _searchResult.postValue(SearchResult(0, reversed, resp.hasNext))
                    _onScrollToSearchMessageLiveData.postValue(reversed.firstOrNull()
                            ?: return@launch)
                }
            }
        }
    }

    private fun loadNextSearchedMessages() {
        if (isLoadingNearToSearchMessagesServer.getAndSet(true)) return
        viewModelScope.launch {
            val resp = messageInteractor.loadNextSearchMessages()
            (resp as? SceytPagingResponse.Success)?.let {
                it.data?.let { messages ->
                    val oldValue = _searchResult.value ?: return@launch
                    val loadedMessages = ArrayList(oldValue.messages)
                    val newMessages: List<SceytMessage> = loadedMessages.plus(messages.reversed())
                    _searchResult.postValue(oldValue.copy(messages = newMessages, hasNext = resp.hasNext))
                }
            }
            isLoadingNearToSearchMessagesServer.set(false)
        }
    }

    private suspend fun initPaginationResponse(response: PaginationResponse<SceytMessage>) {
        when (response) {
            is PaginationResponse.DBResponse -> {
                if (!checkIgnoreDatabasePagingResponse(response)) {
                    notifyPageStateWithResponse(SceytResponse.Success(null), response.offset > 0,
                        response.data.isEmpty(), showError = false)
                    notifyDbResponse(response, enableDateSeparator = true)
                }
            }

            is PaginationResponse.ServerResponse -> {
                notifyPageStateWithResponse(response.data, response.offset > 0,
                    response.cacheData.isEmpty(), showError = false)
                notifyServerResponse(response, enableDateSeparator = true)
            }

            else -> return
        }
        pagingResponseReceived(response)
    }

    private suspend fun notifyDbResponse(
            response: PaginationResponse.DBResponse<SceytMessage>,
            enableDateSeparator: Boolean
    ) = withContext(Dispatchers.Default) {
        if (response.offset == 0) {
            val data = mapToMessageListItem(data = response.data,
                hasNext = response.hasNext,
                hasPrev = response.hasPrev,
                enableDateSeparator = enableDateSeparator)

            _loadMessagesFlow.value = response to data
        } else {
            when (response.loadType) {
                LoadPrev -> {
                    val data = mapToMessageListItem(data = response.data,
                        hasNext = response.hasNext,
                        hasPrev = response.hasPrev,
                        enableDateSeparator = enableDateSeparator)

                    _loadMessagesFlow.value = response to data
                }

                LoadNext -> {
                    val hasNext = checkMaybeHesNext(response)
                    val compareMessage = getCompareMessage(response.loadType, response.data)
                    val data = mapToMessageListItem(data = response.data,
                        hasNext = hasNext,
                        hasPrev = response.hasPrev,
                        compareMessage,
                        enableDateSeparator = enableDateSeparator)

                    _loadMessagesFlow.value = response to data
                }

                LoadNear -> {
                    val hasNext = checkMaybeHesNext(response)
                    val data = mapToMessageListItem(data = response.data,
                        hasNext = hasNext,
                        hasPrev = response.hasPrev,
                        enableDateSeparator = enableDateSeparator)

                    _loadMessagesFlow.value = response to data
                }

                LoadNewest -> {
                    val data = mapToMessageListItem(data = response.data,
                        hasNext = response.hasNext,
                        hasPrev = response.hasPrev,
                        enableDateSeparator = enableDateSeparator)

                    _loadMessagesFlow.value = response to data
                }
            }
        }
    }

    private suspend fun notifyServerResponse(
            response: PaginationResponse.ServerResponse<SceytMessage>,
            enableDateSeparator: Boolean
    ) {
        when (response.data) {
            is SceytResponse.Success -> {
                loadPrevOffsetId = response.data.data?.firstOrNull()?.id ?: 0
                loadNextOffsetId = response.data.data?.lastOrNull()?.id ?: 0

                if (response.hasDiff) {
                    val dataToMap = if (response.dbResultWasEmpty) {
                        response.data.data ?: return
                    } else response.cacheData

                    val newMessages = mapToMessageListItem(data = dataToMap,
                        hasNext = response.hasNext,
                        hasPrev = response.hasPrev,
                        compareMessage = getCompareMessage(response.loadType, dataToMap),
                        enableDateSeparator = enableDateSeparator)

                    _loadMessagesFlow.value = response to newMessages
                    //  _uiSate.update { it.copy(messages = newMessages) }
                }
            }

            is SceytResponse.Error -> {
                // set isSearchingMessageToScroll value to false, to enable jumping to next search message
                if (response.loadKey?.value == LoadKeyType.ScrollToSearchMessageBy.longValue)
                    isSearchingMessageToScroll.set(false)
            }
        }
    }

    internal suspend fun getCompareMessage(
            loadType: PaginationResponse.LoadType,
            proportion: List<SceytMessage>
    ): SceytMessage? = withContext(Dispatchers.Default) {
        if (proportion.isEmpty()) return@withContext null
        val proportionFirstId = proportion.first().id
        return@withContext when (loadType) {
            LoadNext, LoadNewest, LoadNear -> {
                messagesCache.getChannelMessages(conversationId)?.lastOrNull {
                    it.id < proportionFirstId
                }
            }

            LoadPrev -> null
        }
    }

    fun sendPendingMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            messageInteractor.sendPendingMessages(conversationId)
        }
    }

    fun syncConversationMessagesAfter(messageId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            syncManager.syncConversationMessagesAfter(conversationId, messageId)
        }
    }

    fun prepareToEditMessage(message: SceytMessage) {
        _onEditMessageCommandLiveData.postValue(message)
    }

    fun prepareToShowMessageActions(event: MessageCommandEvent.ShowHideMessageActions): Menu? {
        return messageActionBridge.showMessageActions(event.message)
    }

    fun prepareToShowSearchMessage(event: MessageCommandEvent.SearchMessages) {
        messageActionBridge.showSearchMessage(event)
    }

    fun prepareToReplyMessage(message: SceytMessage) {
        _onReplyMessageCommandLiveData.postValue(message)
    }

    fun prepareToScrollToNewMessage() {
        _onScrollToLastMessageLiveData.postValue(channel.lastMessage)
    }

    fun prepareToScrollToReplyMessage(message: SceytMessage) {
        _onScrollToReplyMessageLiveData.postValue(message.parentMessage ?: return)
    }

    fun prepareToPauseOrResumeUpload(item: FileListItem, message: SceytMessage) {
        val defaultState = if (!message.incoming && message.deliveryStatus == DeliveryStatus.Pending
                && !message.isForwarded)
            PendingUpload else PendingDownload

        when (val state = item.file.transferState ?: return) {
            PendingUpload, ErrorUpload -> {
                SendAttachmentWorkManager.schedule(application, item.file.messageTid, channel.id)
            }

            PendingDownload, ErrorDownload -> {
                fileTransferService.download(item.file, FileTransferHelper.createTransferTask(item.file))
            }

            PauseDownload -> {
                val task = fileTransferService.findTransferTask(item.file)
                if (task != null)
                    fileTransferService.resume(item.file.messageTid, item.file, state)
                else fileTransferService.download(item.file, FileTransferHelper.createTransferTask(item.file))
            }

            PauseUpload -> {
                val messageTid = item.file.messageTid
                val task = fileTransferService.findTransferTask(item.file)
                if (task != null)
                    fileTransferService.resume(messageTid, item.file, state)
                else {
                    // Update transfer state to Uploading, otherwise SendAttachmentWorkManager will
                    // not start uploading.
                    viewModelScope.launch(Dispatchers.IO) {
                        attachmentInteractor.updateTransferDataByMsgTid(TransferData(
                            messageTid, item.file.progressPercent
                                    ?: 0f, Uploading, item.file.filePath, item.file.url))
                    }

                    SendAttachmentWorkManager.schedule(application, messageTid, channel.id, ExistingWorkPolicy.REPLACE)
                }
            }

            Uploading, Downloading, Preparing, FilePathChanged, WaitingToUpload -> {
                fileTransferService.pause(item.file.messageTid, item.file, state)
            }

            Uploaded, Downloaded, ThumbLoaded -> {
                val transferData = TransferData(
                    item.file.messageTid, item.file.progressPercent ?: 0f,
                    item.file.transferState ?: defaultState, item.file.filePath, item.file.url)
                FileTransferHelper.emitAttachmentTransferUpdate(transferData)
            }
        }
    }

    @SuppressWarnings("WeakerAccess")
    fun addReaction(message: SceytMessage, scoreKey: String, score: Int = 1,
                    reason: String = "", enforceUnique: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = messageReactionInteractor.addReaction(channel.id, message.id, scoreKey,
                score, reason, enforceUnique)
            notifyPageStateWithResponse(response, showError = false)
        }
    }

    @SuppressWarnings("WeakerAccess")
    fun deleteReaction(message: SceytMessage, scoreKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = messageReactionInteractor.deleteReaction(channel.id, message.id, scoreKey)
            notifyPageStateWithResponse(response, showError = false)
        }
    }

    fun sendMessage(message: Message) {
        viewModelScope.launch(Dispatchers.IO) {
            messageInteractor.sendMessageAsFlow(channel.id, message).collect()
        }
    }

    fun sendMessages(messages: List<Message>) {
        viewModelScope.launch(Dispatchers.IO) {
            messageInteractor.sendMessages(channel.id, messages)
        }
    }

    fun editMessage(message: SceytMessage) {
        viewModelScope.launch(Dispatchers.IO) {
            messageInteractor.editMessage(channel.id, message)
        }
    }

    fun deleteMessage(message: SceytMessage, deleteType: DeleteMessageType) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = messageInteractor.deleteMessage(channel.id, message, deleteType)
            _messageForceDeleteLiveData.postValue(response)
        }
    }

    fun deleteMessages(message: List<SceytMessage>, deleteType: DeleteMessageType) {
        message.forEach {
            deleteMessage(it, deleteType)
        }
    }

    fun markMessageAsRead(vararg messageIds: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = messageInteractor.markMessagesAs(channel.id, MarkerTypeEnum.Displayed, *messageIds)
            _messageMarkerLiveData.postValue(response)
        }
    }

    fun addMessageMarker(marker: String, vararg messageIds: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = messageInteractor.addMessagesMarker(channel.id, marker, *messageIds)
            _messageMarkerLiveData.postValue(response)
        }
    }

    fun sendTypingEvent(typing: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            messageInteractor.sendTyping(channel.id, typing)
        }
    }

    fun updateDraftMessage(text: Editable?, mentionUsers: List<Mention>, styling: List<BodyStyleRange>?,
                           replyOrEditMessage: SceytMessage?, isReply: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            channelInteractor.updateDraftMessage(channel.id, text.toString(),
                mentionUsers, styling, replyOrEditMessage, isReply)
        }
    }

    fun join() {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.join(channel.id)
            _joinLiveData.postValue(response)
        }
    }

    fun getChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.getChannelFromServer(channelId)
            // If response is Error, try to get channel from db.
            if (response is SceytResponse.Error)
                channelInteractor.getChannelFromDb(channelId)?.let {
                    _channelLiveData.postValue(SceytResponse.Success(it))
                } ?: _channelLiveData.postValue(response)
        }
    }

    fun markChannelAsRead(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.markChannelAsRead(channelId)
            _channelLiveData.postValue(response)
        }
    }

    fun loadChannelMembersIfNeeded() {
        viewModelScope.launch(Dispatchers.IO) {
            val count = channelMemberInteractor.getMembersCountDb(channel.id)
            if (channel.memberCount > count)
                loadChannelMembers(0, null).collect()
        }
    }

    fun loadChannelAllMembers() {
        viewModelScope.launch(Dispatchers.IO) {

            suspend fun loadMembers(offset: Int): PaginationResponse.ServerResponse<SceytMember>? {
                return channelMemberInteractor.loadChannelMembers(channel.id, offset, null).firstOrNull {
                    it is PaginationResponse.ServerResponse
                } as? PaginationResponse.ServerResponse<SceytMember>
            }

            val count = channelMemberInteractor.getMembersCountDb(channel.id)
            if (channel.memberCount > count) {
                var offset = 0
                var rest = loadMembers(0)
                while (rest?.hasNext == true) {
                    offset += rest.data.data?.size ?: return@launch
                    rest = loadMembers(offset)
                }
            }
        }
    }

    @SuppressWarnings("WeakerAccess")
    fun loadChannelMembers(offset: Int, role: String?): Flow<PaginationResponse<SceytMember>> {
        return channelMemberInteractor.loadChannelMembers(channel.id, offset, role)
    }

    fun clearHistory(forEveryOne: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            channelInteractor.clearHistory(channel.id, forEveryOne)
        }
    }

    fun showSenderAvatarAndNameIfNeeded(show: Boolean) {
        showSenderAvatarAndNameIfNeeded = show
    }

    internal suspend fun mapToMessageListItem(
            data: List<SceytMessage>?, hasNext: Boolean, hasPrev: Boolean,
            compareMessage: SceytMessage? = null,
            ignoreUnreadMessagesSeparator: Boolean = false,
            enableDateSeparator: Boolean
    ): List<MessageListItem> {
        if (data.isNullOrEmpty()) return arrayListOf()

        val messageItems = arrayListOf<MessageListItem>()

        withContext(Dispatchers.Default) {
            var unreadLineMessage: MessageListItem.UnreadMessagesSeparatorItem? = null
            data.forEachIndexed { index, message ->
                var prevMessage = compareMessage
                if (index > 0)
                    prevMessage = data.getOrNull(index - 1)

                if (enableDateSeparator && shouldShowDate(message, prevMessage))
                    messageItems.add(MessageListItem.DateSeparatorItem(message.createdAt, message.tid))

                var messageWithData = initMessageInfoData(message, prevMessage, true)
                val isSelected = selectedMessagesMap.containsKey(message.tid)

                if (channel.lastMessage?.incoming == true && pinnedLastReadMessageId != 0L
                        && prevMessage?.id == pinnedLastReadMessageId && unreadLineMessage == null) {

                    messageWithData = messageWithData.copy(
                        shouldShowAvatarAndName = messageWithData.incoming && isGroup && showSenderAvatarAndNameIfNeeded,
                        disabledShowAvatarAndName = !showSenderAvatarAndNameIfNeeded,
                    )
                    if (!ignoreUnreadMessagesSeparator)
                        messageItems.add(MessageListItem.UnreadMessagesSeparatorItem(message.createdAt, pinnedLastReadMessageId).also {
                            unreadLineMessage = it
                        })
                }

                messageItems.add(MessageListItem.MessageItem(messageWithData.copy(isSelected = isSelected)))
            }

            if (hasNext)
                messageItems.add(MessageListItem.LoadingNextItem)

            if (hasPrev)
                messageItems.add(0, MessageListItem.LoadingPrevItem)
        }

        return messageItems
    }


    internal fun initMessageInfoData(sceytMessage: SceytMessage, prevMessage: SceytMessage? = null,
                                     initNameAndAvatar: Boolean = false): SceytMessage {
        return sceytMessage.copy(
            isGroup = this@MessageListViewModel.isGroup,
            files = sceytMessage.attachments?.map { it.toFileListItem() },
            shouldShowAvatarAndName = if (initNameAndAvatar && showSenderAvatarAndNameIfNeeded)
                shouldShowAvatarAndName(sceytMessage, prevMessage)
            else sceytMessage.shouldShowAvatarAndName,
            disabledShowAvatarAndName = !showSenderAvatarAndNameIfNeeded,
            messageReactions = initReactionsItems(sceytMessage),
        )
    }

    private fun checkMaybeHesNext(response: PaginationResponse.DBResponse<SceytMessage>): Boolean {
        var hasNext = response.hasNext
        if (!hasNext) {
            response.data.lastOrNull()?.let { lastMsg ->
                if (lastMsg.deliveryStatus != DeliveryStatus.Pending
                        && lastMsg.id < (channel.lastMessage?.id ?: 0)) {
                    hasNext = true
                }
            }
        }
        return hasNext
    }

    private fun initReactionsItems(message: SceytMessage): List<ReactionItem.Reaction>? {
        val pendingReactions = message.pendingReactions
        val reactionItems = message.reactionTotals?.map {
            ReactionItem.Reaction(SceytReactionTotal(it.key, it.score.toInt(),
                message.userReactions?.find { reaction ->
                    reaction.key == it.key && reaction.user?.id == myId
                } != null), message.tid, false)
        }?.toArrayList()

        if (!pendingReactions.isNullOrEmpty() && reactionItems != null) {
            pendingReactions.forEach { pendingReaction ->
                reactionItems.findIndexed { it.reaction.key == pendingReaction.key }?.let { (index, item) ->
                    val reaction = item.reaction
                    if (pendingReaction.isAdd) {
                        reactionItems[index] = item.copy(
                            reaction = reaction.copy(
                                score = reaction.score + pendingReaction.score,
                                containsSelf = true),
                            isPending = true)
                    } else {
                        val score = reaction.score - pendingReaction.score
                        if (score <= 0)
                            reactionItems.remove(item)
                        else {
                            reactionItems[index] = item.copy(
                                reaction = reaction.copy(
                                    score = reaction.score - pendingReaction.score,
                                    containsSelf = false),
                                isPending = false)
                        }
                    }
                } ?: run {
                    if (pendingReaction.isAdd)
                        reactionItems.add(ReactionItem.Reaction(
                            reaction = SceytReactionTotal(pendingReaction.key, pendingReaction.score, true),
                            messageTid = message.tid,
                            isPending = true))
                }
            }
        }
        return reactionItems?.sortedBy { it.reaction.key }
    }

    private fun shouldShowDate(sceytMessage: SceytMessage, prevMessage: SceytMessage?): Boolean {
        return if (prevMessage == null)
            true
        else !DateTimeUtil.isSameDay(sceytMessage.createdAt, prevMessage.createdAt)
    }

    private fun shouldShowAvatarAndName(sceytMessage: SceytMessage, prevMessage: SceytMessage?): Boolean {
        if (!sceytMessage.incoming) return false
        return if (prevMessage == null)
            isGroup
        else {
            val sameSender = prevMessage.user?.id == sceytMessage.user?.id
            isGroup && (!sameSender || shouldShowDate(sceytMessage, prevMessage)
                    || prevMessage.type == MessageTypeEnum.System.value())
        }
    }

    internal fun onReactionEvent(event: ReactionEvent) {
        when (event) {
            is ReactionEvent.AddReaction -> {
                addReaction(event.message, event.scoreKey)
            }

            is ReactionEvent.RemoveReaction -> {
                deleteReaction(event.message, event.scoreKey)
            }
        }
    }

    internal fun needMediaInfo(data: NeedMediaInfoData) {
        val attachment = data.item
        when (data) {
            is NeedMediaInfoData.NeedDownload -> {
                viewModelScope.launch(Dispatchers.IO) {
                    fileTransferService.download(attachment, fileTransferService.findOrCreateTransferTask(attachment))
                }
            }

            is NeedMediaInfoData.NeedThumb -> {
                viewModelScope.launch(Dispatchers.IO) {
                    fileTransferService.getThumb(attachment.messageTid, attachment, data.thumbData)
                }
            }

            is NeedMediaInfoData.NeedLinkPreview -> {
                if (data.onlyCheckMissingData && attachment.linkPreviewDetails != null) {
                    linkPreviewHelper.checkMissedData(attachment.linkPreviewDetails) {
                        _linkPreviewLiveData.postValue(it)
                    }
                } else {
                    linkPreviewHelper.getPreview(attachment, true, successListener = {
                        _linkPreviewLiveData.postValue(it)
                    })
                }
            }
        }
    }

    internal fun clearPreparingThumbs() {
        fileTransferService.clearPreparingThumbPaths()
    }

    internal fun scrollToSearchMessage(isPrev: Boolean) {
        if (isSearchingMessageToScroll.get()) return
        val searchResult = searchResult.value ?: return
        val messages = searchResult.messages
        val nextIndex = if (isPrev) {
            searchResult.currentIndex + 1
        } else searchResult.currentIndex - 1
        if (nextIndex < 0 || nextIndex >= messages.size)
            return

        isSearchingMessageToScroll.set(true)
        _searchResult.postValue(searchResult.copy(currentIndex = nextIndex))
        _onScrollToSearchMessageLiveData.postValue(messages[nextIndex])

        if (searchResult.hasNext && messages.size - nextIndex < SceytChatUIKit.config.messagesLoadSize / 2) {
            loadNextSearchedMessages()
        }
    }

    private fun onChannelMemberEvent(eventData: ChannelMembersEventData) {
        val sceytMembers = eventData.members
        val channelMembers = channel.members?.toMutableList() ?: arrayListOf()

        when (eventData.eventType) {
            ChannelMembersEventEnum.Added -> {
                channelMembers.addAll(sceytMembers)
                channel = channel.copy(
                    members = channelMembers,
                    memberCount = channel.memberCount + sceytMembers.size
                )
                _onChannelMemberAddedOrKickedLiveData.postValue(channel)
            }

            ChannelMembersEventEnum.Kicked -> {
                channelMembers.removeAll(sceytMembers)
                channel = channel.copy(
                    members = channelMembers,
                    memberCount = channel.memberCount - sceytMembers.size
                )
                _onChannelMemberAddedOrKickedLiveData.postValue(channel)
            }

            else -> return
        }
    }
}