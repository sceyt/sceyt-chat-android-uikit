package com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.viewmodel

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
import com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.adapter.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject

class MediaViewModel : BaseViewModel(), SceytKoinComponent {
    private val attachmentLogic: PersistenceAttachmentLogic by inject()
    private val fileTransferService: FileTransferService by inject()

    private val _loadPrevFilesFlow = MutableSharedFlow<List<MediaItem>>(
        extraBufferCapacity = 5,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val loadPrevFilesFlow: SharedFlow<List<MediaItem>> = _loadPrevFilesFlow

    private val _loadNextFilesFlow = MutableSharedFlow<List<MediaItem>>(
        extraBufferCapacity = 5,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val loadNextFilesFlow: SharedFlow<List<MediaItem>> = _loadNextFilesFlow

    private val _loadNearFilesFlow = MutableSharedFlow<List<MediaItem>>(
        extraBufferCapacity = 5,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val loadNearFilesFlow: SharedFlow<List<MediaItem>> = _loadNearFilesFlow


    private val _loadServerFilesFlow = MutableSharedFlow<List<MediaItem>>(
        extraBufferCapacity = 5,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val loadServerFilesFlow: SharedFlow<List<MediaItem>> = _loadServerFilesFlow


    private val _fileFilesFlow = MutableSharedFlow<PaginationResponse<AttachmentWithUserData>>(
        extraBufferCapacity = 5,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val fileFilesFlow: SharedFlow<PaginationResponse<AttachmentWithUserData>> = _fileFilesFlow


    fun loadPrevAttachments(channelId: Long, lastAttachmentId: Long, isLoadingMore: Boolean, type: List<String>, offset: Int) {
        setPagingLoadingStarted(PaginationResponse.LoadType.LoadPrev)

        notifyPageLoadingState(isLoadingMore)

        viewModelScope.launch(Dispatchers.IO) {
            attachmentLogic.getPrevAttachments(channelId, lastAttachmentId, type, offset).collect { response ->
                initPaginationResponse(response)
            }
        }
    }

    fun loadNextAttachments(channelId: Long, lastAttachmentId: Long, isLoadingMore: Boolean, type: List<String>, offset: Int) {
        setPagingLoadingStarted(PaginationResponse.LoadType.LoadNext)

        notifyPageLoadingState(isLoadingMore)

        viewModelScope.launch(Dispatchers.IO) {
            attachmentLogic.getNextAttachments(channelId, lastAttachmentId, type, offset).collect { response ->
                initPaginationResponse(response)
            }
        }
    }

    fun loadNearPrevAttachments(channelId: Long, lastAttachmentId: Long, type: List<String>, offset: Int) {
        setPagingLoadingStarted(PaginationResponse.LoadType.LoadNear)

        notifyPageLoadingState(false)

        viewModelScope.launch(Dispatchers.IO) {
            attachmentLogic.getNearAttachments(channelId, lastAttachmentId, type, offset).collect { response ->
                initPaginationResponse(response)
            }
        }
    }

    private suspend fun initPaginationResponse(response: PaginationResponse<AttachmentWithUserData>) {
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

    private suspend fun initPaginationDbResponse(response: PaginationResponse.DBResponse<AttachmentWithUserData>) {
        withContext(Dispatchers.Main){ _fileFilesFlow.emit(response) }
        notifyPageStateWithResponse(SceytResponse.Success(null), response.offset > 0, response.data.isEmpty())
    }

    private fun initPaginationServerResponse(response: PaginationResponse.ServerResponse<AttachmentWithUserData>) {
        _fileFilesFlow.tryEmit(response)
        when (response.data) {
            is SceytResponse.Success -> {
                if (response.hasDiff) {
                    /*   val newMessages = mapToFileListItem(data = response.cacheData,
                           hasPrev = response.hasPrev, hasNext = response.hasNext)*/

                    //emitByLoadType(true, response.loadType, response.cacheData)
                    //_loadNearFilesFlow.tryEmit(newMessages)
                } /*else if (response.hasPrev.not())
                    _loadPrevFilesFlow.tryEmit(emptyList())*/
            }
            is SceytResponse.Error -> {
                /* if (hasNextDb.not())
                     _loadPrevFilesFlow.tryEmit(emptyList())*/
            }
        }
        notifyPageStateWithResponse(response.data, response.offset > 0, response.cacheData.isEmpty())
    }


    fun mapToMediaItem(data: List<AttachmentWithUserData>?): List<MediaItem> {
        if (data.isNullOrEmpty()) return arrayListOf()
        val fileItems = arrayListOf<MediaItem>()

        data.map {
            val item: MediaItem? = when (it.attachment.type) {
                AttachmentTypeEnum.Video.value() -> MediaItem.Video(it)
                AttachmentTypeEnum.Image.value() -> MediaItem.Image(it)
                else -> null
            }
            item?.let { fileItem -> fileItems.add(fileItem) }
        }

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