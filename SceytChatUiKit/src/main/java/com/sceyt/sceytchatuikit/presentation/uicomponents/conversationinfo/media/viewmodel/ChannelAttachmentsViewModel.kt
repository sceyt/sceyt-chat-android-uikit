package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.viewmodel

import androidx.lifecycle.viewModelScope
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.repositories.MessagesRepository
import com.sceyt.sceytchatuikit.data.toFileListItem
import com.sceyt.sceytchatuikit.presentation.root.BaseViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.sceytconfigs.SceytUIKitConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChannelAttachmentsViewModel(private val messagesRepository: MessagesRepository) : BaseViewModel() {

    private val _filesFlow = MutableStateFlow<List<FileListItem>>(arrayListOf())
    val filesFlow: StateFlow<List<FileListItem>> = _filesFlow

    private val _loadMoreFilesFlow = MutableStateFlow<List<FileListItem>>(arrayListOf())
    val loadMoreFilesFlow: StateFlow<List<FileListItem>> = _loadMoreFilesFlow

    fun loadMessages(channel: SceytChannel, lastMessageId: Long, isLoadingMore: Boolean, type: String) {
        loadingItems.set(true)

        notifyPageLoadingState(isLoadingMore)

        viewModelScope.launch(Dispatchers.IO) {
            val response = messagesRepository.getMessagesByType(channel, lastMessageId, type)
            initResponse(response, isLoadingMore)
        }
    }

    private fun initResponse(it: SceytResponse<List<SceytMessage>>, loadingNext: Boolean) {
        if (it is SceytResponse.Success) {
            hasNext = it.data?.size == SceytUIKitConfig.MESSAGES_LOAD_SIZE
            emitMessagesListResponse(mapToFileListItem(it.data, hasNext), loadingNext)
        }
        notifyPageStateWithResponse(it, loadingNext, it.data.isNullOrEmpty())
        loadingItems.set(false)
    }

    private fun emitMessagesListResponse(response: List<FileListItem>, loadingNext: Boolean) {
        if (loadingNext)
            _loadMoreFilesFlow.value = response
        else _filesFlow.value = response
    }

    private fun mapToFileListItem(data: List<SceytMessage>?, hasNext: Boolean): List<FileListItem> {
        if (data.isNullOrEmpty()) return arrayListOf()
        val fileItems = arrayListOf<FileListItem>()

        data.forEach { sceytMessage ->
            sceytMessage.attachments?.forEach {
                fileItems.add(it.toFileListItem(sceytMessage))
            }
        }
        if (hasNext)
            fileItems.add(FileListItem.LoadingMoreItem)

        return fileItems
    }
}