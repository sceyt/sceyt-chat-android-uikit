package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.viewmodels

import android.app.Application
import android.text.Editable
import android.view.Menu
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.sceytchatuikit.SceytKitClient
import com.sceyt.sceytchatuikit.SceytSyncManager
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventData
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventsObserver
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelMembersEventData
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelMembersEventEnum
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelTypingEventData
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageEventsObserver
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageStatusChangeData
import com.sceyt.sceytchatuikit.data.models.LoadKeyData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.PaginationResponse.LoadType.LoadNear
import com.sceyt.sceytchatuikit.data.models.PaginationResponse.LoadType.LoadNewest
import com.sceyt.sceytchatuikit.data.models.PaginationResponse.LoadType.LoadNext
import com.sceyt.sceytchatuikit.data.models.PaginationResponse.LoadType.LoadPrev
import com.sceyt.sceytchatuikit.data.models.SceytPagingResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.SyncNearMessagesResult
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.data.models.messages.MessageTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.models.messages.SceytReactionTotal
import com.sceyt.sceytchatuikit.data.repositories.MessagesRepository
import com.sceyt.sceytchatuikit.data.toFileListItem
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.persistence.PersistenceAttachmentsMiddleWare
import com.sceyt.sceytchatuikit.persistence.PersistenceChanelMiddleWare
import com.sceyt.sceytchatuikit.persistence.PersistenceMembersMiddleWare
import com.sceyt.sceytchatuikit.persistence.PersistenceMessagesMiddleWare
import com.sceyt.sceytchatuikit.persistence.PersistenceReactionsMiddleWare
import com.sceyt.sceytchatuikit.persistence.PersistenceUsersMiddleWare
import com.sceyt.sceytchatuikit.persistence.extensions.asLiveData
import com.sceyt.sceytchatuikit.persistence.filetransfer.FileTransferHelper
import com.sceyt.sceytchatuikit.persistence.filetransfer.FileTransferService
import com.sceyt.sceytchatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.Downloaded
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.Downloading
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.ErrorDownload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.ErrorUpload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.FilePathChanged
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.PauseDownload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.PauseUpload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.PendingDownload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.PendingUpload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.Preparing
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.ThumbLoaded
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.Uploaded
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.Uploading
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.WaitingToUpload
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.ChannelsCache
import com.sceyt.sceytchatuikit.persistence.workers.SendAttachmentWorkManager
import com.sceyt.sceytchatuikit.presentation.root.BaseViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.MessageActionBridge
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.reactions.ReactionItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.events.MessageCommandEvent
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.events.ReactionEvent
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.SearchResult
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.Mention
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.style.BodyStyleRange
import com.sceyt.sceytchatuikit.presentation.uicomponents.searchinput.DebounceHelper
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig.MESSAGES_LOAD_SIZE
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

class MessageListViewModel(
        var conversationId: Long,
        val replyInThread: Boolean = false,
        var channel: SceytChannel,
) : BaseViewModel(), SceytKoinComponent {

    private val persistenceMessageMiddleWare: PersistenceMessagesMiddleWare by inject()
    internal val persistenceChanelMiddleWare: PersistenceChanelMiddleWare by inject()
    private val persistenceReactionsMiddleWare: PersistenceReactionsMiddleWare by inject()
    internal val persistenceAttachmentMiddleWare: PersistenceAttachmentsMiddleWare by inject()
    internal val persistenceMembersMiddleWare: PersistenceMembersMiddleWare by inject()
    internal val persistenceUsersMiddleWare: PersistenceUsersMiddleWare by inject()
    private val messagesRepository: MessagesRepository by inject()
    private val application: Application by inject()
    private val syncManager: SceytSyncManager by inject()
    private val fileTransferService: FileTransferService by inject()
    internal var pinnedLastReadMessageId: Long = 0
    internal val sendDisplayedHelper by lazy { DebounceHelper(200L, viewModelScope) }
    internal val messageActionBridge by lazy { MessageActionBridge() }
    internal val placeToSavePathsList = mutableSetOf<String>()
    internal val selectedMessagesMap by lazy { mutableMapOf<Long, SceytMessage>() }
    internal val notFoundMessagesToUpdate by lazy { mutableMapOf<Long, SceytMessage>() }
    private var showSenderAvatarAndNameIfNeeded = true
    private var loadPrevJob: Job? = null
    private val loadNextJob: Job? = null
    private var loadNearJob: Job? = null

    // Pagination sync
    internal var needSyncMessagesWhenScrollStateIdle = false
    internal var loadPrevOffsetId = 0L
    internal var loadNextOffsetId = 0L
    internal var lastSyncCenterOffsetId = 0L

    private val isGroup = channel.isGroup

    private val _loadMessagesFlow = MutableStateFlow<PaginationResponse<SceytMessage>>(PaginationResponse.Nothing())
    val loadMessagesFlow: StateFlow<PaginationResponse<SceytMessage>> = _loadMessagesFlow

    private val _messageForceDeleteLiveData = MutableLiveData<SceytResponse<SceytMessage>>()
    val checkMessageForceDeleteLiveData: LiveData<SceytResponse<SceytMessage>> = _messageForceDeleteLiveData

    private val _joinLiveData = MutableLiveData<SceytResponse<SceytChannel>>()
    val joinLiveData: LiveData<SceytResponse<SceytChannel>> = _joinLiveData

    private val _channelLiveData = MutableLiveData<SceytResponse<SceytChannel>>()
    val channelLiveData: LiveData<SceytResponse<SceytChannel>> = _channelLiveData

    private val _markAsReadLiveData = MutableLiveData<List<SceytResponse<MessageListMarker>>>()
    val markAsReadLiveData: LiveData<List<SceytResponse<MessageListMarker>>> = _markAsReadLiveData

    private val _onChannelMemberAddedOrKickedLiveData = MutableLiveData<SceytChannel>()
    val onChannelMemberAddedOrKickedLiveData: LiveData<SceytChannel> = _onChannelMemberAddedOrKickedLiveData

    private val _syncCenteredMessageLiveData = MutableLiveData<SyncNearMessagesResult>()
    val syncCenteredMessageLiveData: LiveData<SyncNearMessagesResult> = _syncCenteredMessageLiveData


    // Message events
    val onNewMessageFlow: Flow<SceytMessage>
    val onNewOutGoingMessageFlow: Flow<SceytMessage>

    //val onNewThreadMessageFlow: Flow<SceytMessage>// todo reply in thread
    val onMessageStatusFlow: Flow<MessageStatusChangeData>

    // val onOutGoingThreadMessageFlow: Flow<SceytMessage>// todo reply in thread
    val onTransferUpdatedLiveData: LiveData<TransferData>


    // Chanel events
    val onChannelEventFlow: Flow<ChannelEventData>
    val onChannelTypingEventFlow: Flow<ChannelTypingEventData>
    val onChannelUpdatedEventFlow: Flow<SceytChannel>

    //Command events
    private val _onEditMessageCommandLiveData = MutableLiveData<SceytMessage>()
    internal val onEditMessageCommandLiveData: LiveData<SceytMessage> = _onEditMessageCommandLiveData
    private val _onReplyMessageCommandLiveData = MutableLiveData<SceytMessage>()
    internal val onReplyMessageCommandLiveData: LiveData<SceytMessage> = _onReplyMessageCommandLiveData
    private val _onScrollToLastMessageLiveData = MutableLiveData<SceytMessage?>()
    internal val onScrollToLastMessageLiveData: LiveData<SceytMessage?> = _onScrollToLastMessageLiveData
    private val _onScrollToReplyMessageLiveData = MutableLiveData<SceytMessage>()
    internal val onScrollToReplyMessageLiveData: LiveData<SceytMessage> = _onScrollToReplyMessageLiveData
    private val _onScrollToSearchMessageLiveData = MutableLiveData<SceytMessage>()
    internal val onScrollToSearchMessageLiveData: LiveData<SceytMessage> = _onScrollToSearchMessageLiveData

    // Search messages
    internal val isSearchingMessageToScroll = AtomicBoolean(false)
    private val isLoadingNextSearchMessages = AtomicBoolean(false)
    private var _searchResult = MutableLiveData<SearchResult>()
    var searchResult = _searchResult.asLiveData()


    init {
        onNewMessageFlow = persistenceMessageMiddleWare.getOnMessageFlow()
            .filter { it.first.id == channel.id /*&& it.second.replyInThread == replyInThread*/ }
            .mapNotNull { initMessageInfoData(it.second) }

        /*
       // todo reply in thread
        onNewThreadMessageFlow = MessageEventsObserver.onMessageFlow
              .filter { it.first.id == channel.id && it.second.replyInThread }
              .mapNotNull { initMessageInfoData(it.second) }*/

        onMessageStatusFlow = ChannelEventsObserver.onMessageStatusFlow
            .filter { it.channel.id == channel.id }

        onChannelEventFlow = ChannelEventsObserver.onChannelEventFlow
            .filter { it.channelId == channel.id }

        onChannelTypingEventFlow = ChannelEventsObserver.onChannelTypingEventFlow
            .filter { it.channel.id == channel.id && it.member.id != SceytKitClient.myId }

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
                channel.id = newChannelId
                conversationId = newChannelId
                channel.pending = false
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
            persistenceMessageMiddleWare.loadPrevMessages(conversationId, lastMessageId, replyInThread, offset, loadKey = loadKey).collect {
                withContext(Dispatchers.Main) {
                    initPaginationResponse(it)
                }
            }
        }
    }

    fun loadNextMessages(lastMessageId: Long, offset: Int) {
        setPagingLoadingStarted(LoadNext)
        val isLoadingMore = offset > 0

        notifyPageLoadingState(isLoadingMore)

        loadPrevJob = viewModelScope.launch(Dispatchers.IO) {
            persistenceMessageMiddleWare.loadNextMessages(conversationId, lastMessageId, replyInThread, offset).collect {
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
            val limit = min(50, MESSAGES_LOAD_SIZE * 2)
            persistenceMessageMiddleWare.loadNearMessages(conversationId, messageId, replyInThread,
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
            persistenceMessageMiddleWare.loadNewestMessages(conversationId, replyInThread,
                loadKey = loadKey, ignoreDb = false).collect { response ->
                withContext(Dispatchers.Main) {
                    initPaginationResponse(response)
                }
            }
        }
    }

    fun syncCenteredMessage(messageId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
             val response = persistenceMessageMiddleWare.syncNearMessages(conversationId, messageId, replyInThread)
             _syncCenteredMessageLiveData.postValue(response)
        }
    }

    fun searchMessages(query: String) {
        viewModelScope.launch {
            val resp = persistenceMessageMiddleWare.searchMessages(conversationId, replyInThread, query)
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

    fun loadNextSearchedMessages() {
        if (isLoadingNextSearchMessages.getAndSet(true)) return
        viewModelScope.launch {
            val resp = persistenceMessageMiddleWare.loadNextSearchMessages()
            (resp as? SceytPagingResponse.Success)?.let {
                it.data?.let { messages ->
                    val oldValue = _searchResult.value ?: return@launch
                    val loadedMessages = ArrayList(oldValue.messages)
                    val newMessages: List<SceytMessage> = loadedMessages.plus(messages.reversed())
                    _searchResult.postValue(oldValue.copy(messages = newMessages, hasNext = resp.hasNext))
                }
            }
            isLoadingNextSearchMessages.set(false)
        }
    }

    fun sendPendingMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            persistenceMessageMiddleWare.sendPendingMessages(conversationId)
        }
    }

    private fun initPaginationResponse(response: PaginationResponse<SceytMessage>) {
        when (response) {
            is PaginationResponse.DBResponse -> {
                if (!checkIgnoreDatabasePagingResponse(response)) {
                    _loadMessagesFlow.value = response
                    notifyPageStateWithResponse(SceytResponse.Success(null), response.offset > 0,
                        response.data.isEmpty(), showError = false)
                }
            }

            is PaginationResponse.ServerResponse -> {
                _loadMessagesFlow.value = response
                notifyPageStateWithResponse(response.data, response.offset > 0,
                    response.cacheData.isEmpty(), showError = false)
            }

            else -> return
        }
        pagingResponseReceived(response)
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

    fun prepareToPauseOrResumeUpload(item: FileListItem) {
        val defaultState = if (!item.sceytMessage.incoming && item.sceytMessage.deliveryStatus == DeliveryStatus.Pending
                && !item.sceytMessage.isForwarded)
            PendingUpload else PendingDownload

        when (val state = item.file.transferState ?: return) {
            PendingUpload, ErrorUpload -> {
                SendAttachmentWorkManager.schedule(application, item.sceytMessage.tid, channel.id)
            }

            PendingDownload, ErrorDownload -> {
                fileTransferService.download(item.file, FileTransferHelper.createTransferTask(item.file, false))
            }

            PauseDownload -> {
                val task = fileTransferService.findTransferTask(item.file)
                if (task != null)
                    fileTransferService.resume(item.sceytMessage.tid, item.file, state)
                else fileTransferService.download(item.file, FileTransferHelper.createTransferTask(item.file, false))
            }

            PauseUpload -> {
                val task = fileTransferService.findTransferTask(item.file)
                if (task != null)
                    fileTransferService.resume(item.sceytMessage.tid, item.file, state)
                else {
                    // Update transfer state to Uploading, otherwise SendAttachmentWorkManager will
                    // not start uploading.
                    viewModelScope.launch(Dispatchers.IO) {
                        persistenceAttachmentMiddleWare.updateTransferDataByMsgTid(TransferData(
                            item.sceytMessage.tid, item.file.progressPercent
                                    ?: 0f, Uploading, item.file.filePath, item.file.url))
                    }

                    SendAttachmentWorkManager.schedule(application, item.sceytMessage.tid, channel.id, ExistingWorkPolicy.REPLACE)
                }
            }

            Uploading, Downloading, Preparing, FilePathChanged, WaitingToUpload -> {
                fileTransferService.pause(item.sceytMessage.tid, item.file, state)
            }

            Uploaded, Downloaded, ThumbLoaded -> {
                val transferData = TransferData(
                    item.sceytMessage.tid, item.file.progressPercent ?: 0f,
                    item.file.transferState ?: defaultState, item.file.filePath, item.file.url)
                FileTransferHelper.emitAttachmentTransferUpdate(transferData)
            }
        }
    }

    @SuppressWarnings("WeakerAccess")
    fun addReaction(message: SceytMessage, scoreKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = persistenceReactionsMiddleWare.addReaction(channel.id, message.id, scoreKey, 1)
            notifyPageStateWithResponse(response, showError = false)
        }
    }

    @SuppressWarnings("WeakerAccess")
    fun deleteReaction(message: SceytMessage, scoreKey: String, isPending: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = persistenceReactionsMiddleWare.deleteReaction(channel.id, message.id, scoreKey, isPending)
            notifyPageStateWithResponse(response, showError = false)
        }
    }

    fun sendMessage(message: Message) {
        viewModelScope.launch(Dispatchers.IO) {
            persistenceMessageMiddleWare.sendMessageAsFlow(channel.id, message).collect()
        }
    }

    fun sendMessages(messages: List<Message>) {
        viewModelScope.launch(Dispatchers.IO) {
            persistenceMessageMiddleWare.sendMessages(channel.id, messages)
        }
    }

    fun editMessage(message: SceytMessage) {
        viewModelScope.launch(Dispatchers.IO) {
            persistenceMessageMiddleWare.editMessage(channel.id, message)
        }
    }

    @SuppressWarnings("WeakerAccess")
    fun deleteMessage(message: SceytMessage, onlyForMe: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = persistenceMessageMiddleWare.deleteMessage(channel.id, message, onlyForMe)
            _messageForceDeleteLiveData.postValue(response)
        }
    }

    fun deleteMessages(message: List<SceytMessage>, onlyForMe: Boolean) {
        message.forEach {
            deleteMessage(it, onlyForMe)
        }
    }

    fun markMessageAsRead(vararg id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = persistenceMessageMiddleWare.markMessagesAsRead(channel.id, *id)
            _markAsReadLiveData.postValue(response)
        }
    }

    fun sendTypingEvent(typing: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            messagesRepository.sendTypingState(channel.id, typing)
        }
    }

    fun updateDraftMessage(text: Editable?, mentionUsers: List<Mention>, styling: List<BodyStyleRange>?,
                           replyOrEditMessage: SceytMessage?, isReply: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            persistenceChanelMiddleWare.updateDraftMessage(channel.id, text.toString(),
                mentionUsers, styling, replyOrEditMessage, isReply)
        }
    }

    fun join() {
        viewModelScope.launch(Dispatchers.IO) {
            val response = persistenceChanelMiddleWare.join(channel.id)
            _joinLiveData.postValue(response)
        }
    }

    fun getChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = persistenceChanelMiddleWare.getChannelFromServer(channelId)
            // If response is Error, try to get channel from db.
            if (response is SceytResponse.Error)
                persistenceChanelMiddleWare.getChannelFromDb(channelId)?.let {
                    _channelLiveData.postValue(SceytResponse.Success(it))
                } ?: _channelLiveData.postValue(response)
        }
    }

    fun markChannelAsRead(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = persistenceChanelMiddleWare.markChannelAsRead(channelId)
            _channelLiveData.postValue(response)
        }
    }

    fun loadChannelMembersIfNeeded() {
        viewModelScope.launch(Dispatchers.IO) {
            val count = persistenceMembersMiddleWare.getMembersCountDb(channel.id)
            if (channel.memberCount > count)
                loadChannelMembers(0, null).collect()
        }
    }

    fun loadChannelAllMembers() {
        viewModelScope.launch(Dispatchers.IO) {

            suspend fun loadMembers(offset: Int): PaginationResponse.ServerResponse<SceytMember>? {
                return persistenceMembersMiddleWare.loadChannelMembers(channel.id, offset, null).firstOrNull {
                    it is PaginationResponse.ServerResponse
                } as? PaginationResponse.ServerResponse<SceytMember>
            }

            val count = persistenceMembersMiddleWare.getMembersCountDb(channel.id)
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
        return persistenceMembersMiddleWare.loadChannelMembers(channel.id, offset, role)
    }

    fun clearHistory(forEveryOne: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            persistenceChanelMiddleWare.clearHistory(channel.id, forEveryOne)
        }
    }

    fun showSenderAvatarAndNameIfNeeded(show: Boolean) {
        showSenderAvatarAndNameIfNeeded = show
    }

    internal suspend fun mapToMessageListItem(
            data: List<SceytMessage>?, hasNext: Boolean, hasPrev: Boolean,
            compareMessage: SceytMessage? = null,
    ): List<MessageListItem> {
        if (data.isNullOrEmpty()) return arrayListOf()

        val messageItems = arrayListOf<MessageListItem>()

        withContext(Dispatchers.Default) {
            var unreadLineMessage: MessageListItem.UnreadMessagesSeparatorItem? = null
            data.forEachIndexed { index, sceytMessage ->
                sceytMessage.isSelected = selectedMessagesMap.containsKey(sceytMessage.tid)
                var prevMessage = compareMessage
                if (index > 0)
                    prevMessage = data.getOrNull(index - 1)

                if (shouldShowDate(sceytMessage, prevMessage))
                    messageItems.add(MessageListItem.DateSeparatorItem(sceytMessage.createdAt, sceytMessage.tid))

                val messageItem = MessageListItem.MessageItem(initMessageInfoData(sceytMessage, prevMessage, true))

                if (channel.lastMessage?.incoming == true && pinnedLastReadMessageId != 0L && prevMessage?.id == pinnedLastReadMessageId && unreadLineMessage == null) {
                    messageItem.message.apply {
                        shouldShowAvatarAndName = incoming && isGroup && showSenderAvatarAndNameIfNeeded
                        disabledShowAvatarAndName = !showSenderAvatarAndNameIfNeeded
                    }
                    messageItems.add(MessageListItem.UnreadMessagesSeparatorItem(sceytMessage.createdAt, pinnedLastReadMessageId).also {
                        unreadLineMessage = it
                    })
                }

                messageItems.add(messageItem)
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
        return sceytMessage.apply {
            isGroup = this@MessageListViewModel.isGroup
            files = attachments?.map { it.toFileListItem(this) }
            if (initNameAndAvatar && showSenderAvatarAndNameIfNeeded)
                shouldShowAvatarAndName = shouldShowAvatarAndName(this, prevMessage)
            disabledShowAvatarAndName = !showSenderAvatarAndNameIfNeeded
            messageReactions = initReactionsItems(this)
        }
    }

    internal fun checkMaybeHesNext(response: PaginationResponse.DBResponse<SceytMessage>): Boolean {
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
                    reaction.key == it.key && reaction.user?.id == SceytKitClient.myId
                } != null), message, false)
        }?.toMutableList()

        if (!pendingReactions.isNullOrEmpty() && reactionItems != null) {
            pendingReactions.forEach { pendingReaction ->
                reactionItems.find { it.reaction.key == pendingReaction.key }?.let { item ->
                    if (pendingReaction.isAdd) {
                        item.reaction.score += pendingReaction.score
                        item.reaction.containsSelf = true
                        item.isPending = true
                    } else {
                        item.reaction.score -= pendingReaction.score
                        if (item.reaction.score <= 0)
                            reactionItems.remove(item)
                        else {
                            item.reaction.containsSelf = false
                            item.isPending = false
                        }
                    }
                } ?: run {
                    if (pendingReaction.isAdd)
                        reactionItems.add(ReactionItem.Reaction(SceytReactionTotal(pendingReaction.key, pendingReaction.score, true), message, true))
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
                deleteReaction(event.message, event.scoreKey, event.isPending)
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

        if (searchResult.hasNext && messages.size - nextIndex < MESSAGES_LOAD_SIZE / 2) {
            loadNextSearchedMessages()
        }
    }

    private fun onChannelMemberEvent(eventData: ChannelMembersEventData) {
        val sceytMembers = eventData.members
        val channelMembers = channel.members?.toMutableList() ?: arrayListOf()

        when (eventData.eventType) {
            ChannelMembersEventEnum.Added -> {
                channelMembers.addAll(sceytMembers)
                channel.apply {
                    members = channelMembers
                    memberCount += sceytMembers.size
                }
                _onChannelMemberAddedOrKickedLiveData.postValue(channel)
            }

            ChannelMembersEventEnum.Kicked -> {
                channelMembers.removeAll(sceytMembers)
                channel.apply {
                    members = channelMembers
                    memberCount -= sceytMembers.size
                }
                _onChannelMemberAddedOrKickedLiveData.postValue(channel)
            }

            else -> return
        }
    }
}