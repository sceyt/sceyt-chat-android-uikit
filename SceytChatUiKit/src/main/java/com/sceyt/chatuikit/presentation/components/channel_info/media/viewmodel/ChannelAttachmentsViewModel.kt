package com.sceyt.chatuikit.presentation.components.channel_info.media.viewmodel

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.asFlow
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.AttachmentWithUserData
import com.sceyt.chatuikit.domain.usecases.PauseOrResumeTransferUseCase
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferHelper
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.persistence.mappers.getInfoFromMetadata
import com.sceyt.chatuikit.persistence.mappers.toTransferData
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelFileItem
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelFileItemType
import com.sceyt.chatuikit.presentation.root.BaseViewModel
import com.sceyt.chatuikit.shared.utils.DateTimeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ChannelAttachmentsViewModel(
    private val attachmentLogic: PersistenceAttachmentLogic,
    private val fileTransferService: FileTransferService,
    private val pauseOrResumeTransferUseCase: PauseOrResumeTransferUseCase,
) : BaseViewModel() {
    private val needToUpdateTransferAfterOnResume = hashMapOf<Long, TransferData>()

    private val _filesFlow = MutableSharedFlow<List<ChannelFileItem>>(
        extraBufferCapacity = 5,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val filesFlow: SharedFlow<List<ChannelFileItem>> = _filesFlow

    private val _loadMoreAttachmentsFlow = MutableSharedFlow<List<ChannelFileItem>>(
        extraBufferCapacity = 5,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val loadMoreAttachmentsFlow: SharedFlow<List<ChannelFileItem>> = _loadMoreAttachmentsFlow

    init {
        viewModelScope.launch(Dispatchers.IO) {
            attachmentLogic.setupFileTransferUpdateObserver()
        }
    }

    fun loadAttachments(
        channelId: Long,
        lastAttachmentId: Long,
        isLoadingMore: Boolean,
        type: List<String>,
        offset: Int
    ) {
        setPagingLoadingStarted(PaginationResponse.LoadType.LoadPrev)

        notifyPageLoadingState(isLoadingMore)

        viewModelScope.launch(Dispatchers.IO) {
            attachmentLogic.getPrevAttachments(
                conversationId = channelId,
                lastAttachmentId = lastAttachmentId,
                types = type,
                offset = offset
            ).collect { response ->
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
        } else _loadMoreAttachmentsFlow.tryEmit(data)

        notifyPageStateWithResponse(
            response = SceytResponse.Success(null),
            wasLoadingMore = response.offset > 0,
            isEmpty = response.data.isEmpty()
        )
    }

    private fun initPaginationServerResponse(response: PaginationResponse.ServerResponse<AttachmentWithUserData>) {
        when (response.data) {
            is SceytResponse.Success -> {
                if (response.hasDiff) {
                    val newMessages = mapToFileListItem(
                        data = response.cacheData,
                        hasPrev = response.hasPrev
                    )
                    _filesFlow.tryEmit(newMessages)
                } else if (response.hasPrev.not())
                    _loadMoreAttachmentsFlow.tryEmit(emptyList())
            }

            is SceytResponse.Error -> {
                if (hasNextDb.not())
                    _loadMoreAttachmentsFlow.tryEmit(emptyList())
            }
        }
        notifyPageStateWithResponse(
            response = response.data,
            wasLoadingMore = response.offset > 0,
            isEmpty = response.cacheData.isEmpty()
        )
    }

    private fun mapToFileListItem(
        data: List<AttachmentWithUserData>?,
        hasPrev: Boolean
    ): List<ChannelFileItem> {
        if (data.isNullOrEmpty()) return arrayListOf()
        val fileItems = arrayListOf<ChannelFileItem>()
        var prevItem: AttachmentWithUserData? = null

        data.sortedByDescending { it.attachment.createdAt }.forEach { item ->
            if (prevItem == null || !DateTimeUtil.isSameDay(
                    epochOne = prevItem.attachment.createdAt,
                    epochTwo = item.attachment.createdAt
                )
            ) {
                fileItems.add(ChannelFileItem.DateSeparator(data = item))
            }

            val type = when (item.attachment.type) {
                AttachmentTypeEnum.Video.value -> ChannelFileItemType.Video
                AttachmentTypeEnum.Image.value -> ChannelFileItemType.Image
                AttachmentTypeEnum.File.value -> ChannelFileItemType.File
                AttachmentTypeEnum.Voice.value -> ChannelFileItemType.Voice
                AttachmentTypeEnum.Link.value -> ChannelFileItemType.Link
                else -> null
            }

            if (type != null) {
                fileItems.add(
                    ChannelFileItem.Item(
                        data = item,
                        type = type,
                        _metadataPayload = item.attachment.getInfoFromMetadata(),
                        _thumbPath = null,
                        _transferData = item.attachment.toTransferData()
                    )
                )

            }
            prevItem = item
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
                    fileTransferService.download(
                        attachment = attachment,
                        transferTask = fileTransferService.findOrCreateTransferTask(attachment)
                    )
                }
            }

            is NeedMediaInfoData.NeedThumb -> {
                viewModelScope.launch(Dispatchers.IO) {
                    fileTransferService.getThumb(attachment.messageTid, attachment, data.thumbData)
                }
            }
        }
    }

    fun pauseOrResumeUpload(item: ChannelFileItem, channelId: Long) {
        viewModelScope.launch {
            pauseOrResumeTransferUseCase(item.attachment, channelId)
        }
    }

    fun observeToUpdateAfterOnResume(fragment: Fragment) {
        FileTransferHelper.onTransferUpdatedLiveData.asFlow().onEach {
            viewModelScope.launch(Dispatchers.Default) {
                if (!fragment.isResumed && it.state != TransferState.Downloading && it.state != TransferState.Uploading)
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