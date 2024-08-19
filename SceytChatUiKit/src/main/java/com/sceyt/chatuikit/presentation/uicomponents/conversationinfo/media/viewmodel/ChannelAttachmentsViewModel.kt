package com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.media.viewmodel

import android.app.Application
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.AttachmentWithUserData
import com.sceyt.chatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.extensions.asLiveData
import com.sceyt.chatuikit.persistence.filetransfer.FileTransferHelper
import com.sceyt.chatuikit.persistence.filetransfer.FileTransferService
import com.sceyt.chatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.chatuikit.persistence.filetransfer.TransferData
import com.sceyt.chatuikit.persistence.filetransfer.TransferState
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.persistence.workers.SendAttachmentWorkManager
import com.sceyt.chatuikit.presentation.root.BaseViewModel
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.ChannelFileItem
import com.sceyt.chatuikit.shared.helpers.LinkPreviewHelper
import com.sceyt.chatuikit.shared.utils.DateTimeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class ChannelAttachmentsViewModel : BaseViewModel(), SceytKoinComponent {
    private val attachmentLogic: PersistenceAttachmentLogic by inject()
    private val fileTransferService: FileTransferService by inject()
    private val application: Application by inject()
    private val linkPreviewHelper by lazy { LinkPreviewHelper(application, viewModelScope) }
    private val needToUpdateTransferAfterOnResume = hashMapOf<Long, TransferData>()

    private val _filesFlow = MutableSharedFlow<List<ChannelFileItem>>(
        extraBufferCapacity = 5,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val filesFlow: SharedFlow<List<ChannelFileItem>> = _filesFlow

    private val _loadMoreFilesFlow = MutableSharedFlow<List<ChannelFileItem>>(
        extraBufferCapacity = 5,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val loadMoreFilesFlow: SharedFlow<List<ChannelFileItem>> = _loadMoreFilesFlow

    private val _linkPreviewLiveData = MutableLiveData<LinkPreviewDetails>()
    val linkPreviewLiveData = _linkPreviewLiveData.asLiveData()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            attachmentLogic.setupFileTransferUpdateObserver()
        }
    }

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
        val data = mapToFileListItem(response.data, response.hasPrev)
        if (response.offset == 0) {
            _filesFlow.tryEmit(data)
        } else _loadMoreFilesFlow.tryEmit(data)

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
        var prevItem: AttachmentWithUserData? = null

        data.sortedByDescending { it.attachment.createdAt }.forEach { item ->
            if (prevItem == null || !DateTimeUtil.isSameDay(prevItem?.attachment?.createdAt
                            ?: 0, item.attachment.createdAt)) {
                fileItems.add(ChannelFileItem.MediaDate(item))
            }
            val fileItem: ChannelFileItem? = when (item.attachment.type) {
                AttachmentTypeEnum.Video.value() -> ChannelFileItem.Video(item)
                AttachmentTypeEnum.Image.value() -> ChannelFileItem.Image(item)
                AttachmentTypeEnum.File.value() -> ChannelFileItem.File(item)
                AttachmentTypeEnum.Voice.value() -> ChannelFileItem.Voice(item)
                AttachmentTypeEnum.Link.value() -> ChannelFileItem.Link(item)
                else -> null
            }
            fileItem?.let { fileItems.add(it) }
            prevItem = item
        }

        if (hasPrev)
            fileItems.add(ChannelFileItem.LoadingMoreItem)

        return fileItems
    }

    private fun prepareToPauseOrResumeUpload(item: SceytAttachment, channelId: Long) {
        when (val state = item.transferState ?: return) {
            TransferState.PendingUpload, TransferState.ErrorUpload -> {
                SendAttachmentWorkManager.schedule(application, item.messageTid, channelId)
            }

            TransferState.PendingDownload, TransferState.ErrorDownload -> {
                fileTransferService.download(item, FileTransferHelper.createTransferTask(item, false))
            }

            TransferState.PauseDownload -> {
                val task = fileTransferService.findTransferTask(item)
                if (task != null)
                    fileTransferService.resume(item.messageTid, item, state)
                else fileTransferService.download(item, FileTransferHelper.createTransferTask(item, false))
            }

            TransferState.PauseUpload -> {
                val task = fileTransferService.findTransferTask(item)
                if (task != null)
                    fileTransferService.resume(item.messageTid, item, state)
                else {
                    // Update transfer state to Uploading, otherwise SendAttachmentWorkManager will
                    // not start uploading.
                    viewModelScope.launch(Dispatchers.IO) {
                        attachmentLogic.updateTransferDataByMsgTid(TransferData(
                            item.messageTid, item.progressPercent
                                    ?: 0f, TransferState.Uploading, item.filePath, item.url))
                    }

                    SendAttachmentWorkManager.schedule(application, item.messageTid, channelId, ExistingWorkPolicy.REPLACE)
                }
            }

            TransferState.Uploading, TransferState.Downloading, TransferState.Preparing, TransferState.FilePathChanged, TransferState.WaitingToUpload -> {
                fileTransferService.pause(item.messageTid, item, state)
            }

            TransferState.Uploaded, TransferState.Downloaded, TransferState.ThumbLoaded -> {
                val transferData = TransferData(
                    item.messageTid, item.progressPercent ?: 0f,
                    item.transferState, item.filePath, item.url)

                FileTransferHelper.emitAttachmentTransferUpdate(transferData)
            }
        }
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

            is NeedMediaInfoData.NeedLinkPreview -> {
                if (data.onlyCheckMissingData && attachment.linkPreviewDetails != null) {
                    linkPreviewHelper.checkMissedData(attachment.linkPreviewDetails) {
                        _linkPreviewLiveData.postValue(it)
                    }
                } else {
                    linkPreviewHelper.getPreview(attachment, true, successListener = {
                        _linkPreviewLiveData.postValue(it)
                    })
                }
            }
        }
    }

    fun pauseOrResumeUpload(item: ChannelFileItem, channelId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            if (item.isFileItemInitialized)
                prepareToPauseOrResumeUpload(item.file, channelId)
        }
    }

    fun observeToUpdateAfterOnResume(fragment: Fragment) {
        FileTransferHelper.onTransferUpdatedLiveData.asFlow().onEach {
            viewModelScope.launch(Dispatchers.Default) {
                if (!fragment.isResumed)
                    needToUpdateTransferAfterOnResume[it.messageTid] = it
            }
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            fragment.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                needToUpdateTransferAfterOnResume.forEach { (_, transferData) ->
                    FileTransferHelper.emitAttachmentTransferUpdate(transferData)
                }
                needToUpdateTransferAfterOnResume.clear()
            }
        }
    }
}