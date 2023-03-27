package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.viewmodels

import android.app.Application
import android.text.Editable
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.sceytchatuikit.SceytKitClient
import com.sceyt.sceytchatuikit.SceytSyncManager
import com.sceyt.sceytchatuikit.data.channeleventobserver.*
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageEventsObserver
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageStatusChangeData
import com.sceyt.sceytchatuikit.data.models.LoadKeyData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.PaginationResponse.LoadType.*
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.SendMessageResult
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytGroupChannel
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.MessageTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.ReactionData
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.repositories.MessagesRepository
import com.sceyt.sceytchatuikit.data.toFileListItem
import com.sceyt.sceytchatuikit.data.toSceytMember
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.persistence.*
import com.sceyt.sceytchatuikit.persistence.filetransfer.FileTransferHelper
import com.sceyt.sceytchatuikit.persistence.filetransfer.FileTransferService
import com.sceyt.sceytchatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.*
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.ChannelsCache
import com.sceyt.sceytchatuikit.persistence.workers.SendAttachmentWorkManager
import com.sceyt.sceytchatuikit.presentation.root.BaseViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.MessageActionBridge
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.reactions.ReactionItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.events.MessageCommandEvent
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.events.ReactionEvent
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.MentionUserData
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.mentionsrc.TokenCompleteTextView.ObjectDataIndexed
import com.sceyt.sceytchatuikit.presentation.uicomponents.searchinput.DebounceHelper
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject

class MessageListViewModel(
        private val conversationId: Long,
        internal val replyInThread: Boolean = false,
        internal var channel: SceytChannel,
) : BaseViewModel(), SceytKoinComponent {

    private val persistenceMessageMiddleWare: PersistenceMessagesMiddleWare by inject()
    private val persistenceChanelMiddleWare: PersistenceChanelMiddleWare by inject()
    private val persistenceAttachmentsMiddleWare: PersistenceAttachmentsMiddleWare by inject()
    private val persistenceReactionsMiddleWare: PersistenceReactionsMiddleWare by inject()
    private val persistenceMembersMiddleWare: PersistenceMembersMiddleWare by inject()
    private val messagesRepository: MessagesRepository by inject()
    private val application: Application by inject()
    private val syncManager: SceytSyncManager by inject()
    private val fileTransferService: FileTransferService by inject()
    internal var pinnedLastReadMessageId: Long = 0
    internal val sendDisplayedHelper by lazy { DebounceHelper(200L, viewModelScope) }
    internal val messageActionBridge by lazy { MessageActionBridge() }

    private val isGroup = channel.channelType != ChannelTypeEnum.Direct

    private val _loadMessagesFlow = MutableStateFlow<PaginationResponse<SceytMessage>>(PaginationResponse.Nothing())
    val loadMessagesFlow: StateFlow<PaginationResponse<SceytMessage>> = _loadMessagesFlow

    private val _messageEditedDeletedLiveData = MutableLiveData<SceytResponse<SceytMessage>>()
    val messageEditedDeletedLiveData: LiveData<SceytResponse<SceytMessage>> = _messageEditedDeletedLiveData

    private val _joinLiveData = MutableLiveData<SceytResponse<SceytChannel>>()
    val joinLiveData: LiveData<SceytResponse<SceytChannel>> = _joinLiveData

    private val _channelLiveData = MutableLiveData<SceytResponse<SceytChannel>>()
    val channelLiveData: LiveData<SceytResponse<SceytChannel>> = _channelLiveData

    private val _markAsReadLiveData = MutableLiveData<SceytResponse<MessageListMarker>>()
    val markAsReadLiveData: LiveData<SceytResponse<MessageListMarker>> = _markAsReadLiveData

    private val _onChannelMemberAddedOrKickedLiveData = MutableLiveData<SceytChannel>()
    val onChannelMemberAddedOrKickedLiveData: LiveData<SceytChannel> = _onChannelMemberAddedOrKickedLiveData


    // Message events
    val onNewMessageFlow: Flow<SceytMessage>
    val onNewOutGoingMessageFlow: Flow<SceytMessage>
    val onNewThreadMessageFlow: Flow<SceytMessage>
    val onMessageStatusFlow: Flow<MessageStatusChangeData>
    val onOutGoingMessageStatusFlow: Flow<Pair<Long, SceytMessage>>
    val onOutGoingThreadMessageFlow: Flow<SceytMessage>
    val onTransferUpdatedFlow: LiveData<TransferData>


    // Chanel events
    val onChannelEventFlow: Flow<ChannelEventData>
    val onChannelTypingEventFlow: Flow<ChannelTypingEventData>
    val onChannelUpdatedEventFlow: Flow<SceytChannel>

    //Command events
    private val _onEditMessageCommandLiveData = MutableLiveData<SceytMessage>()
    internal val onEditMessageCommandLiveData: LiveData<SceytMessage> = _onEditMessageCommandLiveData
    private val _onReplyMessageCommandLiveData = MutableLiveData<SceytMessage>()
    internal val onReplyMessageCommandLiveData: LiveData<SceytMessage> = _onReplyMessageCommandLiveData
    private val _onScrollToMessageLiveData = MutableLiveData<SceytMessage?>()
    internal val onScrollToLastMessageLiveData: LiveData<SceytMessage?> = _onScrollToMessageLiveData
    private val _onScrollToReplyMessageLiveData = MutableLiveData<SceytMessage>()
    internal val onScrollToReplyMessageLiveData: LiveData<SceytMessage> = _onScrollToReplyMessageLiveData


    init {
        onNewMessageFlow = persistenceMessageMiddleWare.getOnMessageFlow()
            .filter { it.first.id == channel.id && it.second.replyInThread == replyInThread }
            .mapNotNull { initMessageInfoData(it.second) }

        onNewThreadMessageFlow = MessageEventsObserver.onMessageFlow
            .filter { it.first.id == channel.id && it.second.replyInThread }
            .mapNotNull { initMessageInfoData(it.second) }

        onMessageStatusFlow = ChannelEventsObserver.onMessageStatusFlow
            .filter { it.channel.id == channel.id }

        onChannelEventFlow = ChannelEventsObserver.onChannelEventFlow
            .filter { it.channelId == channel.id }

        onChannelTypingEventFlow = ChannelEventsObserver.onChannelTypingEventFlow
            .filter { it.channel.id == channel.id && it.member.id != SceytKitClient.myId }

        onChannelUpdatedEventFlow = ChannelsCache.channelUpdatedFlow
            .filter { it.channel.id == channel.id }
            .map { it.channel }

        viewModelScope.launch {
            ChannelEventsObserver.onChannelMembersEventFlow
                .filter { it.channel?.id == channel.id }
                .collect(::onChannelMemberEvent)
        }

        onOutGoingMessageStatusFlow = MessageEventsObserver.onOutGoingMessageStatusFlow

        onNewOutGoingMessageFlow = MessageEventsObserver.onOutgoingMessageFlow
            .filter { it.channelId == channel.id && !it.replyInThread }

        onOutGoingThreadMessageFlow = MessageEventsObserver.onOutgoingMessageFlow
            .filter { it.channelId == channel.id && it.replyInThread }

        onTransferUpdatedFlow = MessageEventsObserver.onTransferUpdatedLiveData
    }

    fun loadPrevMessages(lastMessageId: Long, offset: Int, loadKey: LoadKeyData = LoadKeyData(value = lastMessageId)) {
        setPagingLoadingStarted(LoadPrev)
        val isLoadingMore = offset > 0

        notifyPageLoadingState(isLoadingMore)

        viewModelScope.launch(Dispatchers.IO) {
            persistenceMessageMiddleWare.loadPrevMessages(conversationId, lastMessageId, replyInThread, offset, loadKey).collect {
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

        viewModelScope.launch(Dispatchers.IO) {
            persistenceMessageMiddleWare.loadNextMessages(conversationId, lastMessageId, replyInThread, offset).collect {
                withContext(Dispatchers.Main) {
                    initPaginationResponse(it)
                }
            }
        }
    }

    fun loadNearMessages(messageId: Long, loadKey: LoadKeyData) {
        setPagingLoadingStarted(LoadNear, true)

        viewModelScope.launch(Dispatchers.IO) {
            persistenceMessageMiddleWare.loadNearMessages(conversationId, messageId, replyInThread, loadKey).collect { response ->
                withContext(Dispatchers.Main) {
                    initPaginationResponse(response)
                }
            }
        }
    }

    fun loadNewestMessages(loadKey: LoadKeyData) {
        setPagingLoadingStarted(LoadNear, true)

        viewModelScope.launch(Dispatchers.IO) {
            persistenceMessageMiddleWare.loadNewestMessages(conversationId, replyInThread, loadKey, true).collect { response ->
                withContext(Dispatchers.Main) {
                    initPaginationResponse(response)
                }
            }
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

    fun prepareToShowMessageActions(event: MessageCommandEvent.ShowHideMessageActions) {
        messageActionBridge.showMessageActions(event.message, event.popupWindow)
    }

    fun prepareToReplyMessage(message: SceytMessage) {
        _onReplyMessageCommandLiveData.postValue(message)
    }

    fun prepareToScrollToNewMessage() {
        _onScrollToMessageLiveData.postValue(channel.lastMessage)
    }

    fun prepareToScrollToReplyMessage(message: SceytMessage) {
        _onScrollToReplyMessageLiveData.postValue(message)
    }

    fun prepareToPauseOrResumeUpload(item: FileListItem) {
        val defaultState = if (!item.sceytMessage.incoming && item.sceytMessage.deliveryStatus == DeliveryStatus.Pending
                && !item.sceytMessage.isForwarded)
            PendingUpload else PendingDownload
        val transferData = TransferData(
            item.sceytMessage.tid, item.file.tid, item.file.progressPercent ?: 0f,
            item.file.transferState ?: defaultState, item.file.filePath, item.file.url)

        when (val state = item.file.transferState ?: return) {
            PendingUpload, ErrorUpload, FilePathChanged -> {
                transferData.state = Uploading
                MessageEventsObserver.emitAttachmentTransferUpdate(transferData)
                SendAttachmentWorkManager.schedule(application, item.sceytMessage.tid, channel.id)
            }
            PendingDownload, ErrorDownload -> {
                transferData.state = Downloading
                MessageEventsObserver.emitAttachmentTransferUpdate(transferData)
                fileTransferService.download(item.file, FileTransferHelper.createTransferTask(item.file, false))
            }
            PauseDownload -> {
                transferData.state = Downloading
                MessageEventsObserver.emitAttachmentTransferUpdate(transferData)
                val task = fileTransferService.findTransferTask(item.file)
                if (task != null)
                    fileTransferService.resume(item.sceytMessage.tid, item.file, state)
                else fileTransferService.download(item.file, FileTransferHelper.createTransferTask(item.file, false))

            }
            PauseUpload -> {
                transferData.state = Uploading
                MessageEventsObserver.emitAttachmentTransferUpdate(transferData)
                val task = fileTransferService.findTransferTask(item.file)
                if (task != null)
                    fileTransferService.resume(item.sceytMessage.tid, item.file, state)
                else SendAttachmentWorkManager.schedule(application, item.sceytMessage.tid, channel.id)
            }
            Uploading -> {
                transferData.state = PauseUpload
                fileTransferService.pause(item.sceytMessage.tid, item.file, state)
                viewModelScope.launch(Dispatchers.IO) {
                    persistenceAttachmentsMiddleWare.updateAttachmentWithTransferData(transferData)
                }
                MessageEventsObserver.emitAttachmentTransferUpdate(transferData)
            }
            Downloading -> {
                transferData.state = PauseDownload
                fileTransferService.pause(item.sceytMessage.tid, item.file, state)
                viewModelScope.launch(Dispatchers.IO) {
                    persistenceAttachmentsMiddleWare.updateAttachmentWithTransferData(transferData)
                }
                MessageEventsObserver.emitAttachmentTransferUpdate(transferData)
            }
            Uploaded, Downloaded, ThumbLoaded -> {
                transferData.state = state
                MessageEventsObserver.emitAttachmentTransferUpdate(transferData)
            }
        }
    }

    fun addReaction(message: SceytMessage, scoreKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = persistenceReactionsMiddleWare.addReaction(channel.id, message.id, scoreKey)
            notifyPageStateWithResponse(response)
        }
    }

    fun deleteReaction(message: SceytMessage, scoreKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = persistenceReactionsMiddleWare.deleteReaction(channel.id, message.id, scoreKey)
            notifyPageStateWithResponse(response)
        }
    }


    fun sendMessage(message: Message) {
        viewModelScope.launch(Dispatchers.IO) {
            persistenceMessageMiddleWare.sendMessageAsFlow(channel.id, message).collect { result ->
                if (result is SendMessageResult.Response) {
                    if (result.response is SceytResponse.Error) {
                        // Implement logic if you want to show failed status
                        Log.e("sendMessage", "send message error-> ${result.response.message}")
                    }
                }
            }
        }
    }

    fun sendMessages(messages: List<Message>) {
        viewModelScope.launch(Dispatchers.IO) {
            persistenceMessageMiddleWare.sendMessages(channel.id, messages)
        }
    }

    fun editMessage(message: SceytMessage) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = persistenceMessageMiddleWare.editMessage(channel.id, message)
            _messageEditedDeletedLiveData.postValue(response)
        }
    }

    fun deleteMessage(message: SceytMessage, onlyForMe: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = persistenceMessageMiddleWare.deleteMessage(channel.id, message, onlyForMe)
            _messageEditedDeletedLiveData.postValue(response)
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

    fun updateDraftMessage(text: Editable?, mentionUsers: List<ObjectDataIndexed<MentionUserData>>) {
        viewModelScope.launch(Dispatchers.IO) {
            persistenceChanelMiddleWare.updateDraftMessage(channel.id, text.toString(), mentionUsers)
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
            _channelLiveData.postValue(response)
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
            if (channel is SceytGroupChannel) {
                val count = persistenceMembersMiddleWare.getMembersCountDb(channel.id)
                if ((channel as SceytGroupChannel).memberCount > count && count < SceytKitConfig.CHANNELS_MEMBERS_LOAD_SIZE)
                    persistenceMembersMiddleWare.loadChannelMembers(channel.id, 0, null).collect()
            }
        }
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
                var prevMessage = compareMessage
                if (index > 0)
                    prevMessage = data.getOrNull(index - 1)

                if (shouldShowDate(sceytMessage, prevMessage))
                    messageItems.add(MessageListItem.DateSeparatorItem(sceytMessage.createdAt, sceytMessage.tid))

                val messageItem = MessageListItem.MessageItem(initMessageInfoData(sceytMessage, prevMessage, true))

                if (channel.lastMessage?.incoming == true && pinnedLastReadMessageId != 0L && prevMessage?.id == pinnedLastReadMessageId && unreadLineMessage == null) {
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
            files = attachments?.filter { it.type != AttachmentTypeEnum.Link.value() }?.map { it.toFileListItem(this) }
            if (initNameAndAvatar)
                canShowAvatarAndName = shouldShowAvatarAndName(this, prevMessage)
            messageReactions = initReactionsItems(this)
        }
    }

    private fun initReactionsItems(message: SceytMessage): List<ReactionItem.Reaction>? {
        return message.reactionScores?.map {
            ReactionItem.Reaction(ReactionData(it.key, it.score,
                message.selfReactions?.find { reaction ->
                    reaction.key == it.key && reaction.user.id == SceytKitClient.myId
                } != null), message)
        }
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
            val sameSender = prevMessage.from?.id == sceytMessage.from?.id
            isGroup && (!sameSender || shouldShowDate(sceytMessage, prevMessage)
                    || prevMessage.type == MessageTypeEnum.System.value())
        }
    }

    internal fun onMessageCommandEvent(event: MessageCommandEvent) {
        when (event) {
            is MessageCommandEvent.DeleteMessage -> {
                deleteMessage(event.message, event.onlyForMe)
            }
            is MessageCommandEvent.EditMessage -> {
                prepareToEditMessage(event.message)
            }
            is MessageCommandEvent.ShowHideMessageActions -> {
                prepareToShowMessageActions(event)
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
                prepareToPauseOrResumeUpload(event.item)
            }
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
                    fileTransferService.getThumb(attachment.messageTid, attachment, data.size)
                }
            }
        }
    }

    private fun onChannelMemberEvent(eventData: ChannelMembersEventData) {
        val sceytMembers = eventData.members?.map { member -> member.toSceytMember() }
        val channelMembers = (channel as SceytGroupChannel).members.toMutableList()

        when (eventData.eventType) {
            ChannelMembersEventEnum.Added -> {
                channelMembers.addAll(sceytMembers ?: return)
                (channel as SceytGroupChannel).apply {
                    members = channelMembers
                    memberCount += sceytMembers.size
                }
                _onChannelMemberAddedOrKickedLiveData.postValue(channel)
            }
            ChannelMembersEventEnum.Kicked -> {
                channelMembers.removeAll(sceytMembers ?: return)
                (channel as SceytGroupChannel).apply {
                    members = channelMembers
                    memberCount -= sceytMembers.size
                }
                _onChannelMemberAddedOrKickedLiveData.postValue(channel)
            }
            else -> return
        }
    }
}