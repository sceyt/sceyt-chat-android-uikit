package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.links.viewmodels

import androidx.lifecycle.viewModelScope
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.repositories.MessagesRepository
import com.sceyt.sceytchatuikit.presentation.root.BaseViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.links.adapters.LinkItem
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LinksViewModel(private val messagesRepository: MessagesRepository) : BaseViewModel() {

    private val _messagesFlow = MutableStateFlow<List<LinkItem>>(arrayListOf())
    val messagesFlow: StateFlow<List<LinkItem>> = _messagesFlow

    private val _loadMoreMessagesFlow = MutableStateFlow<List<LinkItem>>(arrayListOf())
    val loadMoreMessagesFlow: StateFlow<List<LinkItem>> = _loadMoreMessagesFlow

    fun loadMessages(channelId: Long, lastMessageId: Long, isLoadingMore: Boolean, type: String) {
        loadingNextItems.set(true)

        notifyPageLoadingState(isLoadingMore)

        viewModelScope.launch(Dispatchers.IO) {
            val response = messagesRepository.getMessagesByType(channelId, lastMessageId, type)
            initResponse(response, isLoadingMore)
        }
    }

    private fun initResponse(it: SceytResponse<List<SceytMessage>>, loadingNext: Boolean) {
        if (it is SceytResponse.Success) {
            hasNext = it.data?.size == SceytKitConfig.MESSAGES_LOAD_SIZE
            emitMessagesListResponse(mapToMessageListItem(it.data, hasNext), loadingNext)
        }
        notifyPageStateWithResponse(it, loadingNext, it.data.isNullOrEmpty())
        loadingNextItems.set(false)
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