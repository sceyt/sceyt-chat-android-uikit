package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.viewmodel

import androidx.lifecycle.viewModelScope
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.toFileListItem
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.persistence.filetransfer.FileTransferService
import com.sceyt.sceytchatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.sceytchatuikit.persistence.logics.messageslogic.PersistenceMessagesLogic
import com.sceyt.sceytchatuikit.presentation.root.BaseViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class ChannelAttachmentsViewModel : BaseViewModel(), SceytKoinComponent {
    private val messagesLogic: PersistenceMessagesLogic by inject()
    private val fileTransferService: FileTransferService by inject()

    private val _filesFlow = MutableStateFlow<List<FileListItem>>(arrayListOf())
    val filesFlow: StateFlow<List<FileListItem>> = _filesFlow

    private val _loadMoreFilesFlow = MutableStateFlow<List<FileListItem>>(arrayListOf())
    val loadMoreFilesFlow: StateFlow<List<FileListItem>> = _loadMoreFilesFlow

    fun loadMessages(channelId: Long, lastMessageId: Long, isLoadingMore: Boolean, type: String) {
        loadingNextItems.set(true)

        notifyPageLoadingState(isLoadingMore)

        viewModelScope.launch(Dispatchers.IO) {
            val response = messagesLogic.getMessagesByType(channelId, lastMessageId, type)
            initResponse(response, isLoadingMore)
        }
    }

    private fun initResponse(it: SceytResponse<List<SceytMessage>>, loadingNext: Boolean) {
        if (it is SceytResponse.Success) {
            hasNext = it.data?.size == SceytKitConfig.MESSAGES_LOAD_SIZE
            emitMessagesListResponse(mapToFileListItem(it.data, hasNext), loadingNext)
        }
        notifyPageStateWithResponse(it, loadingNext, it.data.isNullOrEmpty())
        loadingNextItems.set(false)
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


    fun needMediaInfo(data: NeedMediaInfoData) {
        val attachment = data.item.file
        when (data) {
            is NeedMediaInfoData.NeedDownload -> {
                viewModelScope.launch(Dispatchers.IO) {
                    fileTransferService.download(attachment, fileTransferService.findOrCreateTransferTask(attachment))
                }
            }
            is NeedMediaInfoData.NeedThumb -> {
                viewModelScope.launch(Dispatchers.IO) {
                    fileTransferService.getThumb(data.item.sceytMessage.tid, attachment, data.size)
                }
            }
        }
    }
}