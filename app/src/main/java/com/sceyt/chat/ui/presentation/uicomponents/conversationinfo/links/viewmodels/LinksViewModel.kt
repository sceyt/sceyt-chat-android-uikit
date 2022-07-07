package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.links.viewmodels

import androidx.lifecycle.viewModelScope
import com.sceyt.chat.ui.data.MessagesRepository
import com.sceyt.chat.ui.data.MessagesRepositoryImpl
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.data.toChannel
import com.sceyt.chat.ui.presentation.root.BaseViewModel
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.links.adapters.LinkItem
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LinksViewModel(conversationId: Long,
                     channel: SceytChannel) : BaseViewModel() {

    private val messagesRepository: MessagesRepository = MessagesRepositoryImpl(conversationId, channel.toChannel(), false)

    private val _messagesFlow = MutableStateFlow<List<LinkItem>>(arrayListOf())
    val messagesFlow: StateFlow<List<LinkItem>> = _messagesFlow

    private val _loadMoreMessagesFlow = MutableStateFlow<List<LinkItem>>(arrayListOf())
    val loadMoreMessagesFlow: StateFlow<List<LinkItem>> = _loadMoreMessagesFlow

    fun loadMessages(lastMessageId: Long, isLoadingMore: Boolean, type: String) {
        loadingItems = true

        notifyPageLoadingState(isLoadingMore)

        viewModelScope.launch(Dispatchers.IO) {
            val response = messagesRepository.getMessagesByType(lastMessageId, type)
            initResponse(response, isLoadingMore)
        }
    }

    private fun initResponse(it: SceytResponse<List<SceytMessage>>, loadingNext: Boolean) {
        if (it is SceytResponse.Success) {
            hasNext = it.data?.size == SceytUIKitConfig.MESSAGES_LOAD_SIZE
            emitMessagesListResponse(mapToMessageListItem(it.data, hasNext), loadingNext)
        }
        notifyPageStateWithResponse(it, loadingNext, it.data.isNullOrEmpty())
        loadingItems = false
    }

    private fun emitMessagesListResponse(response: List<LinkItem>, loadingNext: Boolean) {
        if (loadingNext)
            _loadMoreMessagesFlow.value = response
        else _messagesFlow.value = response
    }

    private fun mapToMessageListItem(data: List<SceytMessage>?, hasNext: Boolean): List<LinkItem> {
        if (data.isNullOrEmpty()) return arrayListOf()
        val messagesItems: List<LinkItem> = data.map { LinkItem.Link(it) }

        if (hasNext)
            (messagesItems as ArrayList).add(LinkItem.LoadingMore)

        return messagesItems
    }
}