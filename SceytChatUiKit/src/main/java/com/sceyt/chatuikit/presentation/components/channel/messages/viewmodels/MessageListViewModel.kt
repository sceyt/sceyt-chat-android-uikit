package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import android.app.Application
import android.text.Editable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import com.sceyt.chat.models.message.DeleteMessageType
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.managers.channel.ChannelEventManager
import com.sceyt.chatuikit.data.managers.channel.event.ChannelEventData
import com.sceyt.chatuikit.data.managers.channel.event.ChannelMembersEventData
import com.sceyt.chatuikit.data.managers.channel.event.ChannelMembersEventEnum
import com.sceyt.chatuikit.data.managers.channel.event.ChannelTypingEventData
import com.sceyt.chatuikit.data.managers.message.MessageEventManager
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
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.chatuikit.data.models.messages.MarkerType
import com.sceyt.chatuikit.data.models.messages.MessageTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytReactionTotal
import com.sceyt.chatuikit.data.toFileListItem
import com.sceyt.chatuikit.extensions.findIndexed
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.extensions.asLiveData
import com.sceyt.chatuikit.persistence.extensions.toArrayList
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferHelper
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Downloaded
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Downloading
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ErrorDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ErrorUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.FilePathChanged
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PauseDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PauseUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PendingDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PendingUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Preparing
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ThumbLoaded
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Uploaded
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Uploading
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.WaitingToUpload
import com.sceyt.chatuikit.persistence.interactor.AttachmentInteractor
import com.sceyt.chatuikit.persistence.interactor.ChannelInteractor
import com.sceyt.chatuikit.persistence.interactor.ChannelMemberInteractor
import com.sceyt.chatuikit.persistence.interactor.MessageInteractor
import com.sceyt.chatuikit.persistence.interactor.MessageReactionInteractor
import com.sceyt.chatuikit.persistence.interactor.UserInteractor
import com.sceyt.chatuikit.persistence.logic.PersistenceConnectionLogic
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelsCache
import com.sceyt.chatuikit.persistence.workers.UploadAndSendAttachmentWorkManager
import com.sceyt.chatuikit.presentation.common.DebounceHelper
import com.sceyt.chatuikit.presentation.components.channel.input.data.SearchResult
import com.sceyt.chatuikit.presentation.components.channel.input.format.BodyStyleRange
import com.sceyt.chatuikit.presentation.components.channel.input.mention.Mention
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.FileListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.reactions.ReactionItem
import com.sceyt.chatuikit.presentation.components.channel.messages.events.MessageCommandEvent
import com.sceyt.chatuikit.presentation.components.channel.messages.events.ReactionEvent
import com.sceyt.chatuikit.presentation.root.BaseViewModel
import com.sceyt.chatuikit.services.SceytSyncManager
import com.sceyt.chatuikit.shared.helpers.LinkPreviewHelper
import com.sceyt.chatuikit.shared.utils.DateTimeUtil
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

class MessageListViewModel(
        private var _conversationId: Long,
        private var _channel: SceytChannel,
        val replyInThread: Boolean = false,
) : BaseViewModel(), SceytKoinComponent {
    private val messageInteractor: MessageInteractor by inject()
    internal val channelInteractor: ChannelInteractor by inject()
    private val messageReactionInteractor: MessageReactionInteractor by inject()
    internal val attachmentInteractor: AttachmentInteractor by inject()
    internal val channelMemberInteractor: ChannelMemberInteractor by inject()
    internal val connectionLogic: PersistenceConnectionLogic by inject()
    internal val userInteractor: UserInteractor by inject()
    private val application: Application by inject()
    internal val syncManager: SceytSyncManager by inject()
    private val fileTransferService: FileTransferService by inject()
    private val linkPreviewHelper by lazy { LinkPreviewHelper(application, viewModelScope) }
    internal var pinnedLastReadMessageId: Long = 0
    internal val sendDisplayedHelper by lazy { DebounceHelper(200L, viewModelScope) }
    internal val messageActionBridge by lazy { MessageActionBridge() }
    internal val placeToSavePathsList = mutableSetOf<Pair<AttachmentTypeEnum, String>>()
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
    internal var loadNextOffsetId = 0L
    internal var lastSyncCenterOffsetId = 0L

    private val myId: String? get() = SceytChatUIKit.chatUIFacade.myId
    val channel: SceytChannel get() = _channel
    val conversationId: Long get() = _conversationId

    private val _loadMessagesFlow = MutableStateFlow<PaginationResponse<SceytMessage>>(PaginationResponse.Nothing())
    val loadMessagesFlow: StateFlow<PaginationResponse<SceytMessage>> = _loadMessagesFlow

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
        onNewMessageFlow = messageInteractor.getOnMessageFlow()
            .filter { it.first.id == channel.id /*&& it.second.replyInThread == replyInThread*/ }
            .mapNotNull { initMessageInfoData(it.second) }

        /*
       // todo reply in thread
        onNewThreadMessageFlow = MessageEventsObserver.onMessageFlow
              .filter { it.first.id == channel.id && it.second.replyInThread }
              .mapNotNull { initMessageInfoData(it.second) }*/

        onChannelEventFlow = ChannelEventManager.onChannelEventFlow
            .filter { it.channelId == channel.id }

        onChannelTypingEventFlow = ChannelEventManager.onChannelTypingEventFlow
            .filter { it.channel.id == channel.id && it.member.id != myId }

        onChannelUpdatedEventFlow = ChannelsCache.channelUpdatedFlow
            .filter { it.channel.id == channel.id }
            .map {
                updateChannel(it.channel)
                it.channel
            }

        viewModelScope.launch(Dispatchers.IO) {
            ChannelEventManager.onChannelMembersEventFlow
                .filter { it.channel.id == channel.id }
                .collect(::onChannelMemberEvent)
        }

        ChannelsCache.pendingChannelCreatedFlow
            .filter { (pendingChannelId, _) -> pendingChannelId == channel.id }
            .onEach { (_, newChannel) ->
                updateChannel(newChannel)
            }
            .launchIn(viewModelScope)

        SceytSyncManager.syncChannelMessagesFinished
            .filter { (syncedChannel, _) -> syncedChannel.id == channel.id }
            .onEach { (syncedChannel, _) ->
                updateChannel(syncedChannel)
            }
            .launchIn(viewModelScope)

        onNewOutGoingMessageFlow = MessageEventManager.onOutgoingMessageFlow
            .filter { it.channelId == channel.id /*&& !it.replyInThread*/ }

        /*onOutGoingThreadMessageFlow = MessageEventsObserver.onOutgoingMessageFlow
            .filter { it.channelId == channel.id && it.replyInThread }*/
    }

    fun loadPrevMessages(lastMessageId: Long, offset: Int, loadKey: LoadKeyData = LoadKeyData(value = lastMessageId)) {
        setPagingLoadingStarted(LoadPrev)
        val isLoadingMore = offset > 0

        notifyPageLoadingState(isLoadingMore)

        loadPrevJob = viewModelScope.launch(Dispatchers.IO) {
            messageInteractor.loadPrevMessages(conversationId, lastMessageId, replyInThread, offset, loadKey = loadKey).collect {
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
            val limit = min(50, SceytChatUIKit.config.queryLimits.messageListQueryLimit * 2)
            messageInteractor.loadNearMessages(conversationId, messageId, replyInThread,
                limit, loadKey, ignoreServer = ignoreServer).collect { response ->
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
                    _searchResult.postValue(SearchResult(0, messages, resp.hasNext))
                    _onScrollToSearchMessageLiveData.postValue(messages.firstOrNull()
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

    fun sendPendingMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            messageInteractor.sendPendingMessages(conversationId)
        }
    }

    fun prepareToEditMessage(message: SceytMessage) {
        _onEditMessageCommandLiveData.postValue(message)
    }

    fun prepareToShowMessageActions(event: MessageCommandEvent.ShowHideMessageActions) {
        messageActionBridge.showMessageActions(event.message)
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
        val attachment = item.attachment
        val messageTid = attachment.messageTid
        when (val state = attachment.transferState ?: return) {
            PendingUpload, ErrorUpload -> {
                UploadAndSendAttachmentWorkManager.schedule(application, messageTid, channel.id)
            }

            PendingDownload, ErrorDownload -> {
                fileTransferService.download(attachment, FileTransferHelper.createTransferTask(attachment))
            }

            PauseDownload -> {
                val task = fileTransferService.findTransferTask(attachment)
                if (task != null)
                    fileTransferService.resume(attachment.messageTid, attachment, state)
                else fileTransferService.download(attachment, FileTransferHelper.createTransferTask(attachment))
            }

            PauseUpload -> {
                val task = fileTransferService.findTransferTask(attachment)
                if (task != null)
                    fileTransferService.resume(messageTid, attachment, state)
                else {
                    // Update transfer state to Uploading, otherwise SendAttachmentWorkManager will
                    // not start uploading.
                    viewModelScope.launch(Dispatchers.IO) {
                        attachmentInteractor.updateTransferDataByMsgTid(TransferData(
                            messageTid, attachment.progressPercent
                                    ?: 0f, Uploading, attachment.filePath, attachment.url))
                    }

                    UploadAndSendAttachmentWorkManager.schedule(application, messageTid, channel.id, ExistingWorkPolicy.REPLACE)
                }
            }

            Uploading, Downloading, Preparing, FilePathChanged, WaitingToUpload -> {
                fileTransferService.pause(messageTid, attachment, state)
            }

            Uploaded, Downloaded, ThumbLoaded -> {
                val transferData = TransferData(
                    messageTid, attachment.progressPercent ?: 0f,
                    attachment.transferState, attachment.filePath, attachment.url)
                FileTransferHelper.emitAttachmentTransferUpdate(transferData)
            }
        }
    }

    @SuppressWarnings("WeakerAccess")
    fun addReaction(
            message: SceytMessage,
            scoreKey: String,
            score: Int = 1,
            reason: String = "",
            enforceUnique: Boolean = false
    ) {
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
            messageInteractor.deleteMessage(channel.id, message, deleteType)
        }
    }

    fun deleteMessages(message: List<SceytMessage>, deleteType: DeleteMessageType) {
        message.forEach {
            deleteMessage(it, deleteType)
        }
    }

    fun markMessageAsRead(vararg messageIds: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = messageInteractor.markMessagesAs(channel.id, MarkerType.Displayed, *messageIds)
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
        if (channel.pending) return
        viewModelScope.launch(Dispatchers.IO) {
            messageInteractor.sendTyping(channel.id, typing)
        }
    }

    fun updateDraftMessage(
            text: Editable?,
            mentionUsers: List<Mention>,
            styling: List<BodyStyleRange>?,
            replyOrEditMessage: SceytMessage?,
            isReply: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            channelInteractor.updateDraftMessage(channel.id, text?.toString(),
                mentionUsers, styling, replyOrEditMessage, isReply)
        }
    }

    fun join() {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.join(channel.id)
            if (response is SceytResponse.Success) {
                updateChannel(response.data ?: return@launch)
            }
            _joinLiveData.postValue(response)
        }
    }

    fun getChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            when (val response = channelInteractor.getChannelFromServer(channelId)) {
                // If response is Error, try to get channel from db.
                is SceytResponse.Error -> {
                    channelInteractor.getChannelFromDb(channelId)?.let {
                        _channelLiveData.postValue(SceytResponse.Success(it))
                    } ?: _channelLiveData.postValue(response)
                }

                is SceytResponse.Success -> {
                    updateChannel(response.data ?: return@launch)
                    _channelLiveData.postValue(response)
                }
            }
        }
    }

    fun markChannelAsRead(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = channelInteractor.markChannelAsRead(channelId)
            if (response is SceytResponse.Success) {
                val data = response.data ?: return@launch
                updateChannel(data)
            }
            _channelLiveData.postValue(response)
        }
    }

    fun loadChannelMembersIfNeeded() {
        viewModelScope.launch(Dispatchers.IO) {
            val count = channelMemberInteractor.getMembersCountFromDb(channel.id)
            if (channel.memberCount > count)
                loadChannelMembers(0, null).collect()
        }
    }

    @Suppress("unused")
    fun loadChannelAllMembers() {
        viewModelScope.launch(Dispatchers.IO) {

            suspend fun loadMembers(offset: Int): PaginationResponse.ServerResponse<SceytMember>? {
                return channelMemberInteractor.loadChannelMembers(channel.id, offset, null).firstOrNull {
                    it is PaginationResponse.ServerResponse
                } as? PaginationResponse.ServerResponse<SceytMember>
            }

            val count = channelMemberInteractor.getMembersCountFromDb(channel.id)
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

    @Suppress("unused")
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
                        shouldShowAvatarAndName = messageWithData.incoming && channel.isGroup
                                && showSenderAvatarAndNameIfNeeded,
                        disabledShowAvatarAndName = !showSenderAvatarAndNameIfNeeded,
                    )
                    if (!ignoreUnreadMessagesSeparator)
                        messageItems.add(
                            MessageListItem.UnreadMessagesSeparatorItem(message.createdAt, pinnedLastReadMessageId)
                                .also {
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


    internal fun initMessageInfoData(
            sceytMessage: SceytMessage,
            prevMessage: SceytMessage? = null,
            initNameAndAvatar: Boolean = false
    ): SceytMessage {
        return sceytMessage.copy(
            isGroup = channel.isGroup,
            files = sceytMessage.attachments?.map { it.toFileListItem() },
            shouldShowAvatarAndName = if (initNameAndAvatar && showSenderAvatarAndNameIfNeeded)
                shouldShowAvatarAndName(sceytMessage, prevMessage)
            else sceytMessage.shouldShowAvatarAndName,
            disabledShowAvatarAndName = !showSenderAvatarAndNameIfNeeded,
            messageReactions = initReactionsItems(sceytMessage),
        )
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
            channel.isGroup
        else {
            val sameSender = prevMessage.user?.id == sceytMessage.user?.id
            channel.isGroup && (!sameSender || shouldShowDate(sceytMessage, prevMessage)
                    || prevMessage.type == MessageTypeEnum.System.value)
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

        val queryLimit = SceytChatUIKit.config.queryLimits.messageListQueryLimit
        if (searchResult.hasNext && messages.size - nextIndex < queryLimit / 2) {
            loadNextSearchedMessages()
        }
    }

    private fun onChannelMemberEvent(eventData: ChannelMembersEventData) {
        val sceytMembers = eventData.members
        val channelMembers = channel.members?.toMutableList() ?: arrayListOf()

        when (eventData.eventType) {
            ChannelMembersEventEnum.Added -> {
                channelMembers.addAll(sceytMembers)
                _channel = channel.copy(
                    members = channelMembers,
                    memberCount = channel.memberCount + sceytMembers.size
                )
                _onChannelMemberAddedOrKickedLiveData.postValue(channel)
            }

            ChannelMembersEventEnum.Kicked -> {
                channelMembers.removeAll(sceytMembers)
                _channel = channel.copy(
                    members = channelMembers,
                    memberCount = channel.memberCount - sceytMembers.size
                )
                _onChannelMemberAddedOrKickedLiveData.postValue(channel)
            }

            else -> return
        }
    }

    private fun updateChannel(channel: SceytChannel) {
        _channel = channel
        _conversationId = channel.id
    }
}