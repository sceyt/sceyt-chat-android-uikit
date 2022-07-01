package com.sceyt.chat.ui.presentation.uicomponents.conversation.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.ClientWrapper
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.ui.data.*
import com.sceyt.chat.ui.data.channeleventobserverservice.ChannelEventData
import com.sceyt.chat.ui.data.channeleventobserverservice.ChannelTypingEventData
import com.sceyt.chat.ui.data.channeleventobserverservice.MessageStatusChange
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.models.channels.ChannelTypeEnum
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.data.models.messages.SceytReaction
import com.sceyt.chat.ui.presentation.root.BaseViewModel
import com.sceyt.chat.ui.presentation.uicomponents.conversation.ConversationActivity
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.ReactionItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.events.MessageEvent
import com.sceyt.chat.ui.presentation.uicomponents.conversation.events.ReactionEvent
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig
import com.sceyt.chat.ui.utils.DateTimeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MessageListViewModel(conversationId: Long,
                           internal val replayInThread: Boolean = false,
                           internal val channel: SceytChannel) : BaseViewModel() {
    private val isGroup = channel.channelType != ChannelTypeEnum.Direct

    // todo di
    private val messagesRepository: MessagesRepository = MessagesRepositoryImpl(conversationId, channel.toChannel(), replayInThread)

    private val _messagesFlow = MutableStateFlow<SceytResponse<List<MessageListItem>>>(SceytResponse.Success(null))
    val messagesFlow: StateFlow<SceytResponse<List<MessageListItem>>> = _messagesFlow

    private val _loadMoreMessagesFlow = MutableStateFlow<SceytResponse<List<MessageListItem>>>(SceytResponse.Success(null))
    val loadMoreMessagesFlow: StateFlow<SceytResponse<List<MessageListItem>>> = _loadMoreMessagesFlow

    private val _messageSentLiveData = MutableLiveData<SceytResponse<SceytMessage?>>()
    val messageSentLiveData: LiveData<SceytResponse<SceytMessage?>> = _messageSentLiveData

    private val _messageEditedDeletedLiveData = MutableLiveData<SceytResponse<SceytMessage>>()
    val messageEditedDeletedLiveData: LiveData<SceytResponse<SceytMessage>> = _messageEditedDeletedLiveData

    private val _addDeleteReactionLiveData = MutableLiveData<SceytResponse<SceytMessage>>(SceytResponse.Success(null))
    val addDeleteReactionLiveData: LiveData<SceytResponse<SceytMessage>> = _addDeleteReactionLiveData

    val onNewMessageLiveData = MutableLiveData<SceytMessage>()
    val onNewThreadMessageLiveData = MutableLiveData<SceytMessage>()
    val onMessageStatusLiveData = MutableLiveData<MessageStatusChange>()
    val onMessageReactionUpdatedLiveData = MutableLiveData<SceytMessage>()
    val onMessageEditedOrDeletedLiveData = MutableLiveData<SceytMessage>()

    val onEditMessageCommandLiveData = MutableLiveData<SceytMessage>()
    val onReplayMessageCommandLiveData = MutableLiveData<SceytMessage>()

    // Chanel events
    val onChannelEventLiveData: MutableLiveData<ChannelEventData> = MutableLiveData<ChannelEventData>()
    val onChannelTypingEventLiveData = MutableLiveData<ChannelTypingEventData>()


    init {
        addChannelListeners()
    }

    private fun addChannelListeners() {
        viewModelScope.launch {
            messagesRepository.onMessageFlow.collect {
                onNewMessageLiveData.value = it
            }
        }

        viewModelScope.launch {
            messagesRepository.onThreadMessageFlow.collect {
                onNewThreadMessageLiveData.value = it
            }
        }

        viewModelScope.launch {
            messagesRepository.onMessageStatusFlow.collect {
                onMessageStatusLiveData.value = it
            }
        }

        viewModelScope.launch {
            messagesRepository.onMessageReactionUpdatedFlow.collect {
                onMessageReactionUpdatedLiveData.value = it.toSceytUiMessage(isGroup).apply {
                    messageReactions = initReactionsItems(this)
                }
            }
        }
        viewModelScope.launch {
            messagesRepository.onMessageEditedOrDeleteFlow.collect {
                onMessageEditedOrDeletedLiveData.value = it.toSceytUiMessage(isGroup)
            }
        }

        viewModelScope.launch {
            messagesRepository.onChannelEventFlow.collect {
                onChannelEventLiveData.value = it
            }
        }

        viewModelScope.launch {
            messagesRepository.onChannelTypingEventFlow.collect {
                onChannelTypingEventLiveData.value = it
            }
        }
    }

    fun loadMessages(lastMessageId: Long, isLoadingMore: Boolean) {
        loadingItems = true

        notifyPageLoadingState(isLoadingMore)

        viewModelScope.launch(Dispatchers.IO) {
            val response = messagesRepository.getMessages(lastMessageId)
            initResponse(response, isLoadingMore)
            if (!isLoadingMore)
                messagesRepository.markAllAsRead()
        }
    }

    private fun initResponse(it: SceytResponse<List<SceytMessage>>, loadingNext: Boolean) {
        loadingItems = false
        when (it) {
            is SceytResponse.Success -> {
                hasNext = it.data?.size == SceytUIKitConfig.MESSAGES_LOAD_SIZE
                emitMessagesListResponse(SceytResponse.Success(mapToMessageListItem(it.data, hasNext)), loadingNext)
            }
            is SceytResponse.Error -> emitMessagesListResponse(SceytResponse.Error(it.message), loadingNext)
        }
    }

    private fun emitMessagesListResponse(response: SceytResponse<List<MessageListItem>>, loadingNext: Boolean) {
        if (loadingNext)
            _loadMoreMessagesFlow.value = response
        else _messagesFlow.value = response

        notifyPageStateWithResponse(response, loadingNext, response.data.isNullOrEmpty())
    }

    private fun deleteMessage(message: SceytMessage) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = messagesRepository.deleteMessage(message.toMessage())
            _messageEditedDeletedLiveData.postValue(response)
        }
    }

    private fun addReaction(message: SceytMessage, scoreKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = messagesRepository.addReaction(message.id, scoreKey)
            _addDeleteReactionLiveData.postValue(response.apply {
                if (this is SceytResponse.Success) {
                    data?.let {
                        it.messageReactions = initReactionsItems(it)
                    }
                }
            })
        }
    }

    private fun deleteReaction(message: SceytMessage, scoreKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = messagesRepository.deleteReaction(message.id, scoreKey)
            _addDeleteReactionLiveData.postValue(response.apply {
                if (this is SceytResponse.Success) {
                    data?.let {
                        it.messageReactions = initReactionsItems(it)
                    }
                }
            })
        }
    }

    fun sendMessage(message: Message) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = messagesRepository.sendMessage(message) { tmpMessage ->
                onNewMessageLiveData.postValue(tmpMessage.toSceytUiMessage(isGroup))
            }
            _messageSentLiveData.postValue(response)
        }
    }

    fun sendReplayMessage(message: Message, parent: Message?) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = messagesRepository.sendMessage(message) { tmpMessage ->
                onNewMessageLiveData.postValue(tmpMessage.toSceytUiMessage(isGroup).apply {
                    this.parent = parent
                })
            }
            _messageSentLiveData.postValue(response)
        }
    }

    fun editMessage(message: Message) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = messagesRepository.editMessage(message)
            _messageEditedDeletedLiveData.postValue(response)
        }
    }

    fun markMessageAsDisplayed(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            messagesRepository.markAsRead(id)
        }
    }

    fun sendTypingEvent(typing: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            messagesRepository.sendTypingState(typing)
        }
    }

    internal fun mapToMessageListItem(data: List<SceytMessage>?, hasNext: Boolean,
                                      lastMessage: MessageListItem.MessageItem? = null): List<MessageListItem> {
        if (data.isNullOrEmpty()) return arrayListOf()

        val messageItems = arrayListOf<MessageListItem>()
        data.forEachIndexed { index, sceytMessage ->
            var prevMessage = lastMessage?.message
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
            messageItems.add(messageItem)
        }
        if (hasNext)
            messageItems.add(0, MessageListItem.LoadingMoreItem)
        return messageItems
    }

    private fun initReactionsItems(message: SceytMessage): List<ReactionItem.Reaction>? {
        val currentUserId = ClientWrapper.currentUser.id
        return message.reactionScores?.map {
            ReactionItem.Reaction(SceytReaction(it.key, it.score,
                message.lastReactions?.find { reaction ->
                    reaction.key == it.key && reaction.user.id == currentUserId
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
        else isGroup && (prevMessage.from?.id != sceytMessage.from?.id || shouldShowDate(sceytMessage, prevMessage))
    }

    fun onMessageEvent(event: MessageEvent) {
        when (event) {
            is MessageEvent.DeleteMessage -> {
                deleteMessage(event.message)
            }
            is MessageEvent.EditMessage -> {
                onEditMessageCommandLiveData.postValue(event.message)
            }
            is MessageEvent.Replay -> {
                onReplayMessageCommandLiveData.postValue(event.message)
            }
            is MessageEvent.ReplayInThread -> {
                ConversationActivity.newInstance(event.context, channel, event.message)
            }
        }
    }

    fun onReactionEvent(event: ReactionEvent) {
        when (event) {
            is ReactionEvent.AddReaction -> {
                addReaction(event.message, event.scoreKey)
            }
            is ReactionEvent.DeleteReaction -> {
                deleteReaction(event.message, event.scoreKey)
            }
        }
    }
}