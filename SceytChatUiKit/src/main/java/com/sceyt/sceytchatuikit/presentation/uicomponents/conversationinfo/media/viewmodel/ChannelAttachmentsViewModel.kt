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
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class ChannelAttachmentsViewModel : BaseViewModel(), SceytKoinComponent {
    private val attachmentLogic: PersistenceAttachmentLogic by inject()
    private val fileTransferService: FileTransferService by inject()

    private val _filesFlow = MutableSharedFlow<List<ChannelFileItem>>(
        extraBufferCapacity = 5,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val filesFlow: SharedFlow<List<ChannelFileItem>> = _filesFlow

    private val _loadMoreFilesFlow = MutableSharedFlow<List<ChannelFileItem>>(
        extraBufferCapacity = 5,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val loadMoreFilesFlow: SharedFlow<List<ChannelFileItem>> = _loadMoreFilesFlow

    fun loadAttachments(channelId: Long, lastAttachmentId: Long, isLoadingMore: Boolean, type: List<String>, offset: Int) {
        setPagingLoadingStarted(PaginationResponse.LoadType.LoadPrev)

        notifyPageLoadingState(isLoadingMore)

        viewModelScope.launch(Dispatchers.IO) {
            attachmentLogic.getPrevAttachments(channelId, lastAttachmentId, type, offset).collect { response ->
                initPaginationResponse(response)
            }
        }
    }

    private fun initPaginationResponse(response: PaginationResponse<AttachmentWithUserData>) {
        when (response) {
            is PaginationResponse.DBResponse -> {
                if (!checkIgnoreDatabasePagingResponse(response)) {
                    initPaginationDbResponse(response)
                }
            }
            is PaginationResponse.ServerResponse ->
                initPaginationServerResponse(response)
            else -> return
        }
        pagingResponseReceived(response)
    }

    private fun initPaginationDbResponse(response: PaginationResponse.DBResponse<AttachmentWithUserData>) {
        if (response.offset == 0) {
            _filesFlow.tryEmit(mapToFileListItem(response.data, response.hasPrev))
        } else {
            _loadMoreFilesFlow.tryEmit(mapToFileListItem(response.data, response.hasPrev))
        }
        notifyPageStateWithResponse(SceytResponse.Success(null), response.offset > 0, response.data.isEmpty())
    }

    private fun initPaginationServerResponse(response: PaginationResponse.ServerResponse<AttachmentWithUserData>) {
        when (response.data) {
            is SceytResponse.Success -> {
                if (response.hasDiff) {
                    val newMessages = mapToFileListItem(data = response.cacheData,
                        hasPrev = response.hasPrev)
                    _filesFlow.tryEmit(newMessages)
                } else if (response.hasPrev.not())
                    _loadMoreFilesFlow.tryEmit(emptyList())
            }
            is SceytResponse.Error -> {
                if (hasNextDb.not())
                    _loadMoreFilesFlow.tryEmit(emptyList())
            }
        }
        notifyPageStateWithResponse(response.data, response.offset > 0, response.cacheData.isEmpty())
    }

    private fun mapToFileListItem(data: List<AttachmentWithUserData>?, hasPrev: Boolean): List<ChannelFileItem> {
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

        if (hasPrev)
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