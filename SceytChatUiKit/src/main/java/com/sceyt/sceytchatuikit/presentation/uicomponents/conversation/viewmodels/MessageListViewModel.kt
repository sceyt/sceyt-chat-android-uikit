package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.message.Message
import com.sceyt.sceytchatuikit.data.*
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventsObserver
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelMembersEventEnum
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageEventsObserver
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageStatusChangeData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytGroupChannel
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.models.messages.SceytReaction
import com.sceyt.sceytchatuikit.data.repositories.MessagesRepository
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.persistence.PersistenceChanelMiddleWare
import com.sceyt.sceytchatuikit.persistence.PersistenceMessagesMiddleWare
import com.sceyt.sceytchatuikit.presentation.root.BaseViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.reactions.ReactionItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.events.MessageCommandEvent
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.events.ReactionEvent
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject

class MessageListViewModel(private val conversationId: Long,
                           internal val replayInThread: Boolean = false,
                           internal var channel: SceytChannel) : BaseViewModel(), SceytKoinComponent {

    private val persistenceMessageMiddleWare: PersistenceMessagesMiddleWare by inject()
    private val persistenceChanelMiddleWare: PersistenceChanelMiddleWare by inject()
    private val messagesRepository: MessagesRepository by inject()
    private val preference: SceytSharedPreference by inject()
    internal val myId = preference.getUserId()


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
    val onChannelEventFlow: Flow<com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventData>
    val onChannelTypingEventFlow: Flow<com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelTypingEventData>

    //Command events
    private val _onEditMessageCommandLiveData = MutableLiveData<SceytMessage>()
    val onEditMessageCommandLiveData: LiveData<SceytMessage> = _onEditMessageCommandLiveData
    private val _onReplayMessageCommandLiveData = MutableLiveData<SceytMessage>()
    val onReplayMessageCommandLiveData: LiveData<SceytMessage> = _onReplayMessageCommandLiveData


    init {
        onMessageReactionUpdatedFlow = MessageEventsObserver.onMessageReactionUpdatedFlow
            .filterNotNull()
            .filter { it.channelId == channel.id || it.replyInThread != replayInThread }
            .map {
                it.toSceytUiMessage(isGroup).apply {
                    messageReactions = initReactionsItems(this)
                }
            }
        onMessageEditedOrDeletedFlow = MessageEventsObserver.onMessageEditedOrDeletedFlow
            .filterNotNull()
            .filter { it.channelId == channel.id || it.replyInThread != replayInThread }
            .map { it.toSceytUiMessage(isGroup) }

        onNewMessageFlow = MessageEventsObserver.onMessageFlow
            .filter { it.first.id == channel.id && it.second.replyInThread == replayInThread }
            .mapNotNull { it.second }

        onNewThreadMessageFlow = MessageEventsObserver.onMessageFlow
            .filter { it.first.id == channel.id && it.second.replyInThread }
            .mapNotNull { it.second }

        onMessageStatusFlow = ChannelEventsObserver.onMessageStatusFlow
            .filter { it.channelId == channel.id }

        onChannelEventFlow = ChannelEventsObserver.onChannelEventFlow
            .filter { it.channelId == channel.id }

        onChannelTypingEventFlow = ChannelEventsObserver.onChannelTypingEventFlow
            .filter { it.channel.id == channel.id }

        viewModelScope.launch {
            ChannelEventsObserver.onChannelMembersEventFlow
                .filter { it.channel?.id == channel.id }
                .collect(::onChannelMemberEvent)
        }

        onOutGoingMessageStatusFlow = MessageEventsObserver.onOutGoingMessageStatusFlow

        onOutGoingThreadMessageFlow = MessageEventsObserver.onOutgoingMessageFlow
            .filter { it.channelId == channel.id && it.replyInThread }
    }

    fun loadMessages(lastMessageId: Long, offset: Int) {
        setPagingLoadingStarted()
        val isLoadingMore = offset > 0

        notifyPageLoadingState(isLoadingMore)

        viewModelScope.launch(Dispatchers.IO) {
            persistenceMessageMiddleWare.loadMessages(conversationId, lastMessageId, replayInThread, offset).collect {
                withContext(Dispatchers.Main){
                    initResponse(it)
                }
            }
        }
    }

    fun sendPendingMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            persistenceMessageMiddleWare.sendPendingMessages(conversationId)
        }
    }

    private fun initResponse(response: PaginationResponse<SceytMessage>) {
        when (response) {
            is PaginationResponse.DBResponse -> {
                if (response.data.isNotEmpty()) {
                    _loadMessagesFlow.value = response
                    notifyPageStateWithResponse(SceytResponse.Success(null), response.offset > 0, response.data.isEmpty())
                }
            }
            is PaginationResponse.ServerResponse2 -> {
                _loadMessagesFlow.value = response
                notifyPageStateWithResponse(response.data, response.offset > 0, response.cashData.isEmpty())
            }
            else -> return
        }
        pagingResponseReceived(response)
    }

    private fun deleteMessage(messageId: Long, messageTid: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = persistenceMessageMiddleWare.deleteMessage(channel.id, messageId, messageTid)
            _messageEditedDeletedLiveData.postValue(response)
            if (response is SceytResponse.Success)
                MessageEventsObserver.emitMessageEditedOrDeletedByMe(response.data?.toMessage()
                        ?: return@launch)
        }
    }

    private fun addReaction(message: SceytMessage, scoreKey: String) {
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

    private fun deleteReaction(message: SceytMessage, scoreKey: String) {
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

    private fun onChannelMemberEvent(eventData: com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelMembersEventData) {
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

    fun sendMessage(message: Message) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = persistenceMessageMiddleWare.sendMessage(channel.id, message) { tmpMessage ->
                val outMessage = tmpMessage.toSceytUiMessage(isGroup)
                _onNewOutgoingMessageLiveData.postValue(outMessage)
            }
            if (response is SceytResponse.Error) {
                // Implement logic if you want to show failed status
                Log.e("sendMessage", "send message error-> ${response.message}")
            }
        }
    }

    internal fun sendReplayMessage(message: Message, parent: Message?) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = persistenceMessageMiddleWare.sendMessage(channel.id, message) { tmpMessage ->
                _onNewOutgoingMessageLiveData.postValue(tmpMessage.toSceytUiMessage(isGroup).apply {
                    this.parent = parent?.toSceytUiMessage()
                })
            }
            if (response is SceytResponse.Error) {
                // Implement logic if you want to show failed status
                Log.e("sendMessage", "send message error-> ${response.message}")
            }
        }
    }

    internal fun editMessage(message: SceytMessage) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = persistenceMessageMiddleWare.editMessage(channel.id, message)
            _messageEditedDeletedLiveData.postValue(response)
            if (response is SceytResponse.Success)
                MessageEventsObserver.emitMessageEditedOrDeletedByMe(response.data?.toMessage()
                        ?: return@launch)
        }
    }

    internal fun markMessageAsDisplayed(vararg id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            persistenceMessageMiddleWare.markAsRead(channel.id, *id)
        }
    }

    internal fun sendTypingEvent(typing: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            messagesRepository.sendTypingState(channel.id, typing)
        }
    }

    internal fun join() {
        viewModelScope.launch(Dispatchers.IO) {
            val response = persistenceChanelMiddleWare.join(channel.id)
            _joinLiveData.postValue(response)
        }
    }

    internal fun getChannel(channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = persistenceChanelMiddleWare.getChannelFromServer(channelId)
            _channelLiveData.postValue(response)
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
        else isGroup && (prevMessage.from?.id != sceytMessage.from?.id || shouldShowDate(sceytMessage, prevMessage))
    }

    internal fun onMessageCommandEvent(event: MessageCommandEvent) {
        when (event) {
            is MessageCommandEvent.DeleteMessage -> {
                deleteMessage(event.message.id, event.message.tid)
            }
            is MessageCommandEvent.EditMessage -> {
                _onEditMessageCommandLiveData.postValue(event.message)
            }
            is MessageCommandEvent.Replay -> {
                _onReplayMessageCommandLiveData.postValue(event.message)
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
}