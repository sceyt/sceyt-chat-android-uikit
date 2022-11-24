package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.sceytchatuikit.SceytSyncManager
import com.sceyt.sceytchatuikit.data.SceytSharedPreference
import com.sceyt.sceytchatuikit.data.channeleventobserver.*
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageEventsObserver
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageStatusChangeData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.PaginationResponse.LoadType.*
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.SendMessageResult
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytGroupChannel
import com.sceyt.sceytchatuikit.data.models.messages.MessageTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.models.messages.SceytReaction
import com.sceyt.sceytchatuikit.data.repositories.MessagesRepository
import com.sceyt.sceytchatuikit.data.toFileListItem
import com.sceyt.sceytchatuikit.data.toSceytMember
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.persistence.PersistenceChanelMiddleWare
import com.sceyt.sceytchatuikit.persistence.PersistenceMessagesMiddleWare
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.ChannelsCash
import com.sceyt.sceytchatuikit.persistence.mappers.toMessage
import com.sceyt.sceytchatuikit.persistence.mappers.toSceytUiMessage
import com.sceyt.sceytchatuikit.presentation.root.BaseViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.reactions.ReactionItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.events.MessageCommandEvent
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.events.ReactionEvent
import com.sceyt.sceytchatuikit.presentation.uicomponents.searchinput.DebounceHelper
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject

class MessageListViewModel(private val conversationId: Long,
                           internal val replyInThread: Boolean = false,
                           internal var channel: SceytChannel) : BaseViewModel(), SceytKoinComponent {

    private val persistenceMessageMiddleWare: PersistenceMessagesMiddleWare by inject()
    private val persistenceChanelMiddleWare: PersistenceChanelMiddleWare by inject()
    private val messagesRepository: MessagesRepository by inject()
    private val preference: SceytSharedPreference by inject()
    private val syncManager: SceytSyncManager by inject()
    internal val myId = preference.getUserId()
    internal var pinnedLastReadMessageId: Long = 0
    internal val sendDisplayedHelper by lazy { DebounceHelper(200L, viewModelScope) }

    private val isGroup = channel.channelType != ChannelTypeEnum.Direct

    private val _loadMessagesFlow = MutableStateFlow<PaginationResponse<SceytMessage>>(PaginationResponse.Nothing())
    val loadMessagesFlow: StateFlow<PaginationResponse<SceytMessage>> = _loadMessagesFlow

    private val _messageEditedDeletedLiveData = MutableLiveData<SceytResponse<SceytMessage>>()
    val messageEditedDeletedLiveData: LiveData<SceytResponse<SceytMessage>> = _messageEditedDeletedLiveData

    private val _addDeleteReactionLiveData = MutableLiveData<SceytResponse<SceytMessage>>()
    val addDeleteReactionLiveData: LiveData<SceytResponse<SceytMessage>> = _addDeleteReactionLiveData

    private val _joinLiveData = MutableLiveData<SceytResponse<SceytChannel>>()
    val joinLiveData: LiveData<SceytResponse<SceytChannel>> = _joinLiveData

    private val _channelLiveData = MutableLiveData<SceytResponse<SceytChannel>>()
    val channelLiveData: LiveData<SceytResponse<SceytChannel>> = _channelLiveData

    private val _markAsReadLiveData = MutableLiveData<SceytResponse<MessageListMarker>>()
    val markAsReadLiveData: LiveData<SceytResponse<MessageListMarker>> = _markAsReadLiveData

    private val _onChannelMemberAddedOrKickedLiveData = MutableLiveData<SceytChannel>()
    val onChannelMemberAddedOrKickedLiveData: LiveData<SceytChannel> = _onChannelMemberAddedOrKickedLiveData

    private val _onNewOutgoingMessageLiveData = MutableLiveData<SceytMessage>()
    val onNewOutgoingMessageLiveData: LiveData<SceytMessage> = _onNewOutgoingMessageLiveData

    // Message events
    val onNewMessageFlow: Flow<SceytMessage>
    val onNewThreadMessageFlow: Flow<SceytMessage>
    val onMessageStatusFlow: Flow<MessageStatusChangeData>
    val onMessageReactionUpdatedFlow: Flow<SceytMessage>
    val onMessageEditedOrDeletedFlow: Flow<SceytMessage>
    val onOutGoingMessageStatusFlow: Flow<Pair<Long, SceytMessage>>
    val onOutGoingThreadMessageFlow: Flow<SceytMessage>

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
    internal val onScrollToMessageLiveData: LiveData<SceytMessage?> = _onScrollToMessageLiveData


    init {
        onMessageReactionUpdatedFlow = MessageEventsObserver.onMessageReactionUpdatedFlow
            .filterNotNull()
            .filter { it.channelId == channel.id || it.replyInThread != replyInThread }
            .map {
                it.toSceytUiMessage(isGroup).apply {
                    messageReactions = initReactionsItems(this)
                }
            }
        onMessageEditedOrDeletedFlow = MessageEventsObserver.onMessageEditedOrDeletedFlow
            .filterNotNull()
            .filter { it.channelId == channel.id || it.replyInThread != replyInThread }
            .map { it.toSceytUiMessage(isGroup) }

        onNewMessageFlow = persistenceMessageMiddleWare.getOnMessageFlow()
            .filter { it.first.id == channel.id && it.second.replyInThread == replyInThread }
            .mapNotNull { it.second }

        onNewThreadMessageFlow = MessageEventsObserver.onMessageFlow
            .filter { it.first.id == channel.id && it.second.replyInThread }
            .mapNotNull { it.second }

        onMessageStatusFlow = ChannelEventsObserver.onMessageStatusFlow
            .filter { it.channel.id == channel.id }

        onChannelEventFlow = ChannelEventsObserver.onChannelEventFlow
            .filter { it.channelId == channel.id }

        onChannelTypingEventFlow = ChannelEventsObserver.onChannelTypingEventFlow
            .filter { it.channel.id == channel.id }

        onChannelUpdatedEventFlow = ChannelsCash.channelUpdatedFlow
            .filter { it.id == channel.id }

        viewModelScope.launch {
            ChannelEventsObserver.onChannelMembersEventFlow
                .filter { it.channel?.id == channel.id }
                .collect(::onChannelMemberEvent)
        }

        onOutGoingMessageStatusFlow = MessageEventsObserver.onOutGoingMessageStatusFlow

        onOutGoingThreadMessageFlow = MessageEventsObserver.onOutgoingMessageFlow
            .filter { it.channelId == channel.id && it.replyInThread }
    }

    fun loadPrevMessages(lastMessageId: Long, offset: Int, loadKey: Long = lastMessageId) {
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

    fun loadNearMessages(messageId: Long, loadKey: Long) {
        setPagingLoadingStarted(LoadNear, true)

        viewModelScope.launch(Dispatchers.IO) {
            persistenceMessageMiddleWare.loadNearMessages(conversationId, messageId, replyInThread, loadKey).collect { response ->
                withContext(Dispatchers.Main) {
                    initPaginationResponse(response)
                }
            }
        }
    }

    fun loadNewestMessages(loadKey: Long) {
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
                    notifyPageStateWithResponse(SceytResponse.Success(null), response.offset > 0, response.data.isEmpty())
                }
            }
            is PaginationResponse.ServerResponse -> {
                _loadMessagesFlow.value = response
                notifyPageStateWithResponse(response.data, response.offset > 0, response.cashData.isEmpty())
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

    fun deleteMessage(message: SceytMessage, onlyForMe: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = persistenceMessageMiddleWare.deleteMessage(channel.id, message, onlyForMe)
            _messageEditedDeletedLiveData.postValue(response)
            if (response is SceytResponse.Success)
                MessageEventsObserver.emitMessageEditedOrDeletedByMe(response.data?.toMessage()
                        ?: return@launch)
        }
    }

    fun prepareToEditMessage(message: SceytMessage) {
        _onEditMessageCommandLiveData.postValue(message)
    }

    fun prepareToReplyMessage(message: SceytMessage) {
        _onReplyMessageCommandLiveData.postValue(message)
    }

    fun prepareToScrollToNewMessage() {
        _onScrollToMessageLiveData.postValue(channel.lastMessage)
    }

    fun addReaction(message: SceytMessage, scoreKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = persistenceMessageMiddleWare.addReaction(channel.id, message.id, scoreKey)
            _addDeleteReactionLiveData.postValue(response.apply {
                if (this is SceytResponse.Success) {
                    data?.let {
                        it.messageReactions = initReactionsItems(it)
                    }
                }
            })
        }
    }

    fun deleteReaction(message: SceytMessage, scoreKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = persistenceMessageMiddleWare.deleteReaction(channel.id, message.id, scoreKey)
            _addDeleteReactionLiveData.postValue(response.apply {
                if (this is SceytResponse.Success) {
                    data?.let {
                        it.messageReactions = initReactionsItems(it)
                    }
                }
            })
        }
    }


    fun sendMessage(message: Message, parent: Message? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val sceytMessage = message.toSceytUiMessage(isGroup).apply {
                this.parent = parent?.toSceytUiMessage(isGroup)
            }
            persistenceMessageMiddleWare.sendMessageAsFlow(channel.id, sceytMessage).collect { result ->
                when (result) {
                    is SendMessageResult.TempMessage -> {
                        val outMessage = result.message.apply {
                            this.parent = parent?.toSceytUiMessage()
                        }
                        _onNewOutgoingMessageLiveData.postValue(outMessage)
                    }
                    is SendMessageResult.Response -> {
                        if (result.response is SceytResponse.Error) {
                            // Implement logic if you want to show failed status
                            Log.e("sendMessage", "send message error-> ${result.response.message}")
                        }
                    }
                }
            }
        }
    }

    fun editMessage(message: SceytMessage) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = persistenceMessageMiddleWare.editMessage(channel.id, message)
            _messageEditedDeletedLiveData.postValue(response)
            if (response is SceytResponse.Success)
                MessageEventsObserver.emitMessageEditedOrDeletedByMe(response.data?.toMessage()
                        ?: return@launch)
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

    internal suspend fun mapToMessageListItem(data: List<SceytMessage>?, hasNext: Boolean, hasPrev: Boolean,
                                              compareMessage: SceytMessage? = null): List<MessageListItem> {
        if (data.isNullOrEmpty()) return arrayListOf()

        val messageItems = arrayListOf<MessageListItem>()

        withContext(Dispatchers.Default) {
            var unreadLineMessage: MessageListItem.UnreadMessagesSeparatorItem? = null
            data.forEachIndexed { index, sceytMessage ->
                var prevMessage = compareMessage
                if (index > 0)
                    prevMessage = data.getOrNull(index - 1)


                if (shouldShowDate(sceytMessage, prevMessage))
                    messageItems.add(MessageListItem.DateSeparatorItem(sceytMessage.createdAt, sceytMessage.id))

                val messageItem = MessageListItem.MessageItem(sceytMessage.apply {
                    isGroup = this@MessageListViewModel.isGroup
                    files = sceytMessage.attachments?.map { it.toFileListItem(sceytMessage) }
                    canShowAvatarAndName = shouldShowAvatarAndName(sceytMessage, prevMessage)
                    messageReactions = initReactionsItems(this)
                })

                if (pinnedLastReadMessageId != 0L && prevMessage?.id == pinnedLastReadMessageId && unreadLineMessage == null) {
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

    private fun initReactionsItems(message: SceytMessage): List<ReactionItem.Reaction>? {
        return message.reactionScores?.map {
            ReactionItem.Reaction(SceytReaction(it.key, it.score,
                message.lastReactions?.find { reaction ->
                    reaction.key == it.key && reaction.user.id == myId
                } != null), message)
        }?.sortedByDescending { it.reaction.score }
    }

    private fun shouldShowDate(sceytMessage: SceytMessage, prevMessage: SceytMessage?): Boolean {
        return if (prevMessage == null)
            true
        else !DateTimeUtil.isSameDay(sceytMessage.createdAt, prevMessage.createdAt)
    }

    internal fun shouldShowAvatarAndName(sceytMessage: SceytMessage, prevMessage: SceytMessage?): Boolean {
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
            is MessageCommandEvent.Reply -> {
                prepareToReplyMessage(event.message)
            }
            is MessageCommandEvent.ScrollToDown -> {
                prepareToScrollToNewMessage()
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