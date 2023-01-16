package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.viewmodel

import androidx.lifecycle.viewModelScope
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentWithUserData
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.persistence.filetransfer.FileTransferService
import com.sceyt.sceytchatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.sceytchatuikit.persistence.logics.attachmentlogic.PersistenceAttachmentLogic
import com.sceyt.sceytchatuikit.presentation.root.BaseViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.ChannelFileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class ChannelAttachmentsViewModel : BaseViewModel(), SceytKoinComponent {
    private val attachmentLogic: PersistenceAttachmentLogic by inject()
    private val fileTransferService: FileTransferService by inject()

    private val _filesFlow = MutableStateFlow<List<ChannelFileItem>>(arrayListOf())
    val filesFlow: StateFlow<List<ChannelFileItem>> = _filesFlow

    private val _loadMoreFilesFlow = MutableStateFlow<List<ChannelFileItem>>(arrayListOf())
    val loadMoreFilesFlow: StateFlow<List<ChannelFileItem>> = _loadMoreFilesFlow

    fun loadMessages(channelId: Long, lastMessageId: Long, isLoadingMore: Boolean, type: List<String>) {
        loadingNextItems.set(true)

        notifyPageLoadingState(isLoadingMore)

        viewModelScope.launch(Dispatchers.IO) {
            attachmentLogic.getPrevAttachments(channelId, lastMessageId, type).collect { response ->
                initResponse(response, isLoadingMore)
            }
        }
    }

    private fun initResponse(response: PaginationResponse<AttachmentWithUserData>, loadingNext: Boolean) {
        when (response) {
            is PaginationResponse.DBResponse -> {}
            is PaginationResponse.ServerResponse -> {
                if (response.data is SceytResponse.Success) {
                    emitMessagesListResponse(mapToFileListItem(response.data.data, response.hasPrev), loadingNext)
                }
                notifyPageStateWithResponse(response.data, loadingNext, response.data.data.isNullOrEmpty())
                loadingNextItems.set(false)

            }
            is PaginationResponse.Nothing -> return
        }
        pagingResponseReceived(response)
    }

    private fun emitMessagesListResponse(response: List<ChannelFileItem>, loadingNext: Boolean) {
        if (loadingNext)
            _loadMoreFilesFlow.value = response
        else _filesFlow.value = response
    }

    private fun mapToFileListItem(data: List<AttachmentWithUserData>?, hasNext: Boolean): List<ChannelFileItem> {
        if (data.isNullOrEmpty()) return arrayListOf()
        val fileItems = arrayListOf<ChannelFileItem>()

        data.map {
            val item: ChannelFileItem? = when (it.attachment.type) {
                AttachmentTypeEnum.Video.value() -> ChannelFileItem.Video(it)
                AttachmentTypeEnum.Image.value() -> ChannelFileItem.Image(it)
                AttachmentTypeEnum.File.value() -> ChannelFileItem.File(it)
                AttachmentTypeEnum.Voice.value() -> ChannelFileItem.Voice(it)
                AttachmentTypeEnum.Link.value() -> ChannelFileItem.Link(it)
                else -> null
            }
            item?.let { fileItem -> fileItems.add(fileItem) }
        }

        if (hasNext)
            fileItems.add(ChannelFileItem.LoadingMoreItem)

        return fileItems
    }

    fun needMediaInfo(data: NeedMediaInfoData) {
        val attachment = data.item
        when (data) {
            is NeedMediaInfoData.NeedDownload -> {
                viewModelScope.launch(Dispatchers.IO) {
                    fileTransferService.download(attachment, fileTransferService.findOrCreateTransferTask(attachment))
                }
            }
            is NeedMediaInfoData.NeedThumb -> {
                viewModelScope.launch(Dispatchers.IO) {
                    fileTransferService.getThumb(attachment.messageTid, attachment, data.size)
                }
            }
        }
    }
}