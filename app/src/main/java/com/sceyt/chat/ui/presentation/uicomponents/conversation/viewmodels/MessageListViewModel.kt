package com.sceyt.chat.ui.presentation.uicomponents.conversation.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.ReactionScore
import com.sceyt.chat.ui.data.ChannelEventsObserverService
import com.sceyt.chat.ui.data.MessagesRepositoryImpl
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.models.messages.SceytUiMessage
import com.sceyt.chat.ui.data.toSceytUiMessage
import com.sceyt.chat.ui.presentation.root.BaseViewModel
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.events.ReactionEvent
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig
import com.sceyt.chat.ui.utils.DateTimeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MessageListViewModel(channelId: Long, private val isGroup: Boolean) : BaseViewModel() {

    var isLoadingMessages = false
    var hasNext = false

    // todo di
    private val repo = MessagesRepositoryImpl(channelId, false)


    private val _messagesFlow = MutableStateFlow<SceytResponse<List<MessageListItem>>>(SceytResponse.Success(null))
    val messagesFlow: StateFlow<SceytResponse<List<MessageListItem>>> = _messagesFlow

    private val _loadMoreMessagesFlow = MutableStateFlow<SceytResponse<List<MessageListItem>>>(SceytResponse.Success(null))
    val loadMoreMessagesFlow: StateFlow<SceytResponse<List<MessageListItem>>> = _loadMoreMessagesFlow

    private val _updateReactionLiveData = MutableLiveData<SceytResponse<SceytUiMessage>>(SceytResponse.Success(null))
    val updateReactionLiveData: LiveData<SceytResponse<SceytUiMessage>> = _updateReactionLiveData

    private val onMessageFlow get() = repo.onMessageFlow.shareIn(viewModelScope, SharingStarted.Lazily)
    val onMessageLiveData = MutableLiveData<SceytUiMessage>()


    private val onMessageStatusFlow get() = repo.onMessageStatusFlow.shareIn(viewModelScope, SharingStarted.Lazily)
    val onMessageStatusLiveData = MutableLiveData<ChannelEventsObserverService.MessageStatusChange>()

    private val _updateMessageLiveData = MutableLiveData<SceytUiMessage>()
    val updateMessageLiveData = _updateMessageLiveData

    init {
        viewModelScope.launch {
            onMessageFlow.collect {
                onMessageLiveData.value = it
            }
        }

        viewModelScope.launch {
            onMessageStatusFlow.collect {
                onMessageStatusLiveData.value = it
            }
        }
    }

    fun loadMessages(lastMessageId: Long, isLoadingMore: Boolean) {
        isLoadingMessages = true

        notifyPageLoadingState(isLoadingMore)

        viewModelScope.launch(Dispatchers.IO) {
            val response = repo.getMessages(lastMessageId)
            initResponse(response, isLoadingMore)
        }
    }

    private fun initResponse(it: SceytResponse<List<SceytUiMessage>>, loadingNext: Boolean) {
        isLoadingMessages = false
        when (it) {
            is SceytResponse.Success -> {
                hasNext = it.data?.size == SceytUIKitConfig.MESSAGES_LOAD_SIZE
                emitResponse(SceytResponse.Success(mapToMessageListItem(it.data, hasNext)), loadingNext)
            }
            is SceytResponse.Error -> emitResponse(SceytResponse.Error(it.message), loadingNext)
        }
    }

    private fun emitResponse(response: SceytResponse<List<MessageListItem>>, loadingNext: Boolean) {
        if (loadingNext)
            _loadMoreMessagesFlow.value = response
        else _messagesFlow.value = response

        notifyPageStateWithResponse(loadingNext, response.data.isNullOrEmpty())
    }

    private fun addReaction(message: SceytUiMessage, score: ReactionScore) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = repo.addReaction(message.id, score)
            _updateReactionLiveData.postValue(response)
        }
    }

    private fun deleteReaction(message: SceytUiMessage, score: ReactionScore) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = repo.deleteReaction(message.id, score)
            _updateReactionLiveData.postValue(response)
        }
    }

    internal fun mapToMessageListItem(data: List<SceytUiMessage>?, hasNext: Boolean,
                                      lastMessage: MessageListItem.MessageItem? = null): List<MessageListItem> {
        if (data.isNullOrEmpty()) return arrayListOf()

        val messageItems: List<MessageListItem> = data.mapIndexed { index, item ->
            MessageListItem.MessageItem(item.apply {
                isGroup = this@MessageListViewModel.isGroup
                if (index > 0) {
                    val prevMessage = data[index - 1]
                    setMessageDateAndState(item, prevMessage)
                } else
                    setMessageDateAndState(item, lastMessage?.message)
            })
        }
        if (hasNext)
            (messageItems as ArrayList).add(0, MessageListItem.LoadingMoreItem)
        return messageItems
    }

    internal fun setMessageDateAndState(sceytUiMessage: SceytUiMessage, prevMessage: SceytUiMessage?): SceytUiMessage {
        with(sceytUiMessage) {
            if (prevMessage != null) {
                canShowAvatarAndName = prevMessage.from?.id != from?.id && isGroup
                showDate = !DateTimeUtil.isSameDay(createdAt, prevMessage.createdAt)
            } else {
                canShowAvatarAndName = isGroup
                showDate = true
            }
            return this
        }
    }

    fun onReactionEvent(event: ReactionEvent) {
        when (event) {
            is ReactionEvent.AddReaction -> {
                addReaction(event.message, event.score)
            }
            is ReactionEvent.DeleteReaction -> {
                deleteReaction(event.message, event.score)
            }
        }
    }

    fun sendMessage(message: Message) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = repo.sendMessage(message) { tmpMessage ->
                onMessageLiveData.postValue(tmpMessage.toSceytUiMessage(isGroup))
            }
            updateMessageLiveData.postValue(response.data)
        }
    }
}