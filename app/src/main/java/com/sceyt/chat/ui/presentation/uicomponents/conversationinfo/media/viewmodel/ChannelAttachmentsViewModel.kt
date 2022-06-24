package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.media.viewmodel

import androidx.lifecycle.viewModelScope
import com.sceyt.chat.ui.data.MessagesRepository
import com.sceyt.chat.ui.data.MessagesRepositoryImpl
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.data.toChannel
import com.sceyt.chat.ui.data.toFileListItem
import com.sceyt.chat.ui.presentation.root.BaseViewModel
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChannelAttachmentsViewModel(conversationId: Long,
                                  channel: SceytChannel) : BaseViewModel() {

    private val messagesRepository: MessagesRepository = MessagesRepositoryImpl(conversationId, channel.toChannel(), false)
    var isLoadingAttachments = false
    var hasNext = false

    private val _filesFlow = MutableStateFlow<List<FileListItem>>(arrayListOf())
    val filesFlow: StateFlow<List<FileListItem>> = _filesFlow

    private val _loadMoreFilesFlow = MutableStateFlow<List<FileListItem>>(arrayListOf())
    val loadMoreFilesFlow: StateFlow<List<FileListItem>> = _loadMoreFilesFlow

    fun loadMessages(lastMessageId: Long, isLoadingMore: Boolean, type: String) {
        isLoadingAttachments = true

        notifyPageLoadingState(isLoadingMore)

        viewModelScope.launch(Dispatchers.IO) {
            val response = messagesRepository.getMessagesByType(lastMessageId, type)
            initResponse(response, isLoadingMore)
        }
    }

    private fun initResponse(it: SceytResponse<List<SceytMessage>>, loadingNext: Boolean) {
        isLoadingAttachments = false
        when (it) {
            is SceytResponse.Success -> {
                hasNext = it.data?.size == SceytUIKitConfig.MESSAGES_LOAD_SIZE
                emitMessagesListResponse(mapToFileListItem(it.data, hasNext), loadingNext)
            }
            is SceytResponse.Error -> notifyPageStateWithResponse(it, loadingNext)
        }
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