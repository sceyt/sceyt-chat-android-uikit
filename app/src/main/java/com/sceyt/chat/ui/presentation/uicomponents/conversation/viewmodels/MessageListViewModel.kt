package com.sceyt.chat.ui.presentation.uicomponents.conversation.viewmodels

import androidx.lifecycle.viewModelScope
import com.sceyt.chat.ui.data.MessagesRepositoryImpl
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.models.messages.SceytUiMessage
import com.sceyt.chat.ui.presentation.root.BaseViewModel
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.MessageListItem
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MessageListViewModel(channelId: Long) : BaseViewModel() {

    var isLoadingMessages = false
    var hasNext = false

    // todo di
    private val repo = MessagesRepositoryImpl(channelId, false)

    private val _messagesFlow = MutableStateFlow<SceytResponse<List<MessageListItem>>>(SceytResponse.Success(null))
    val messagesFlow: StateFlow<SceytResponse<List<MessageListItem>>> = _messagesFlow

    private val _loadMoreMessagesFlow = MutableStateFlow<SceytResponse<List<MessageListItem>>>(SceytResponse.Success(null))
    val loadMoreMessagesFlow: StateFlow<SceytResponse<List<MessageListItem>>> = _loadMoreMessagesFlow


    fun loadMessages(lastMessageId: Long, isLoadingMore: Boolean) {
        isLoadingMessages = true

        notifyPageLoadingState(isLoadingMore)

        viewModelScope.launch(Dispatchers.IO) {
            isLoadingMessages = false

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

    private fun mapToMessageListItem(data: List<SceytUiMessage>?, hasNext: Boolean): List<MessageListItem> {
        if (data.isNullOrEmpty()) return arrayListOf()

        val messageItems: List<MessageListItem> = data.map { item -> MessageListItem.MessageItem(item) }
        if (hasNext)
            (messageItems as ArrayList).add(0, MessageListItem.LoadingMoreItem)
        return messageItems
    }
}