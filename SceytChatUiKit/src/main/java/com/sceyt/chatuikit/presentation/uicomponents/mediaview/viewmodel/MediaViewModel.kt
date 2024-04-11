package com.sceyt.chatuikit.presentation.uicomponents.mediaview.viewmodel

import androidx.lifecycle.viewModelScope
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.AttachmentWithUserData
import com.sceyt.chatuikit.di.SceytKoinComponent
import com.sceyt.chatuikit.persistence.PersistenceAttachmentsMiddleWare
import com.sceyt.chatuikit.persistence.PersistenceMessagesMiddleWare
import com.sceyt.chatuikit.persistence.filetransfer.FileTransferService
import com.sceyt.chatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.chatuikit.presentation.root.BaseViewModel
import com.sceyt.chatuikit.presentation.uicomponents.mediaview.adapter.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject

class MediaViewModel : BaseViewModel(), SceytKoinComponent {
    private val messagesMiddleWare: PersistenceMessagesMiddleWare by inject()
    private val attachmentsMiddleWare: PersistenceAttachmentsMiddleWare by inject()
    private val fileTransferService: FileTransferService by inject()

    private val _fileFilesFlow = MutableSharedFlow<PaginationResponse<AttachmentWithUserData>>(
        extraBufferCapacity = 5,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val fileFilesFlow: SharedFlow<PaginationResponse<AttachmentWithUserData>> = _fileFilesFlow


    fun loadPrevAttachments(channelId: Long, lastAttachmentId: Long, isLoadingMore: Boolean, type: List<String>, offset: Int) {
        setPagingLoadingStarted(PaginationResponse.LoadType.LoadPrev)

        notifyPageLoadingState(isLoadingMore)

        viewModelScope.launch(Dispatchers.IO) {
            attachmentsMiddleWare.getPrevAttachments(channelId, lastAttachmentId, type, offset).collect { response ->
                initPaginationResponse(response)
            }
        }
    }

    fun loadNextAttachments(channelId: Long, lastAttachmentId: Long, isLoadingMore: Boolean, type: List<String>, offset: Int) {
        setPagingLoadingStarted(PaginationResponse.LoadType.LoadNext)

        notifyPageLoadingState(isLoadingMore)

        viewModelScope.launch(Dispatchers.IO) {
            attachmentsMiddleWare.getNextAttachments(channelId, lastAttachmentId, type, offset).collect { response ->
                initPaginationResponse(response)
            }
        }
    }

    fun loadNearAttachments(channelId: Long, lastAttachmentId: Long, type: List<String>, offset: Int) {
        setPagingLoadingStarted(PaginationResponse.LoadType.LoadNear)

        notifyPageLoadingState(false)

        viewModelScope.launch(Dispatchers.IO) {
            attachmentsMiddleWare.getNearAttachments(channelId, lastAttachmentId, type, offset).collect { response ->
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
        withContext(Dispatchers.Main) { _fileFilesFlow.emit(response) }
        notifyPageStateWithResponse(SceytResponse.Success(null), response.offset > 0, response.data.isEmpty())
    }

    private fun initPaginationServerResponse(response: PaginationResponse.ServerResponse<AttachmentWithUserData>) {
        _fileFilesFlow.tryEmit(response)
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
                    fileTransferService.getThumb(attachment.messageTid, attachment, data.thumbData)
                }
            }
        }
    }

    fun getMessageById(messageId: Long) = callbackFlow {
        messagesMiddleWare.getMessageDbById(messageId)?.let {
            trySend(it)
        } ?: trySend(null)
        channel.close()
        awaitClose()
    }
}