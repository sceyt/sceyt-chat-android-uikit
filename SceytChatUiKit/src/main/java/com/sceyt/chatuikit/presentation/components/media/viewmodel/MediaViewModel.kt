package com.sceyt.chatuikit.presentation.components.media.viewmodel

import androidx.lifecycle.viewModelScope
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNear
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNext
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadPrev
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.AttachmentWithUserData
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.persistence.interactor.AttachmentInteractor
import com.sceyt.chatuikit.persistence.interactor.MessageInteractor
import com.sceyt.chatuikit.persistence.mappers.getInfoFromMetadata
import com.sceyt.chatuikit.persistence.mappers.toTransferData
import com.sceyt.chatuikit.presentation.components.media.MediaPreviewTransferHolder
import com.sceyt.chatuikit.presentation.components.media.adapter.MediaItem
import com.sceyt.chatuikit.presentation.components.media.adapter.MediaItemType
import com.sceyt.chatuikit.presentation.root.BaseViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject

class MediaViewModel(
    val reversed: Boolean,
    private val channelId: Long,
    private val mediaTypes: List<String>,
    openedAttachmentData: AttachmentWithUserData? = null,
    preloadedData: MediaPreviewTransferHolder.PreloadedData? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BaseViewModel(), SceytKoinComponent {

    private val messageInteractor: MessageInteractor by inject()
    private val attachmentInteractor: AttachmentInteractor by inject()
    private val fileTransferService: FileTransferService by inject()

    val openedWithAttachment = openedAttachmentData?.attachment
    var initialScrollIndex: Int = preloadedData?.initialIndex ?: 0
        private set

    // One-time scroll correction for LoadNear. -1 = nothing pending.
    // initialScrollSet prevents repeated correction scheduling across DB/server responses.
    private var pendingInitialScrollIndex: Int = -1
    private var initialScrollSet = false

    fun consumePendingScrollIndex(): Int {
        val idx = pendingInitialScrollIndex
        pendingInitialScrollIndex = -1
        return idx
    }

    private val isPreloaded = preloadedData != null

    private val initialItems: List<MediaItem> =
        preloadedData?.items?.mapNotNull { it.toMediaItem() }
            ?: openedAttachmentData?.toMediaItem()?.let(::listOf).orEmpty()

    private val _mediaItems = MutableStateFlow(initialItems)
    val mediaItems: StateFlow<List<MediaItem>> = _mediaItems.asStateFlow()

    init {
        if (!isPreloaded) loadInitial()
    }

    private fun loadInitial() {
        val attachment = openedWithAttachment
        if (attachment == null || attachment.id == null || attachment.id == 0L) {
            loadPrevAttachments(
                lastAttachmentId = 0,
                isLoadingMore = false,
                offset = 0,
            )
        } else {
            loadNearAttachments(lastAttachmentId = attachment.id)
        }
    }

    fun checkAndLoadPrev() {
        if (isPreloaded) return
        if (!canLoadPrev()) return
        val items = _mediaItems.value
        val anchor = if (reversed) items.lastOrNull() else items.firstOrNull()
        val anchorId = anchor?.data?.attachment?.id ?: return
        loadPrevAttachments(
            lastAttachmentId = anchorId,
            isLoadingMore = true,
            offset = items.size,
        )
    }

    fun checkAndLoadNext() {
        if (isPreloaded) return
        if (!canLoadNext()) return
        val items = _mediaItems.value
        val anchor = if (reversed) items.firstOrNull() else items.lastOrNull()
        val anchorId = anchor?.data?.attachment?.id ?: return
        loadNextAttachments(
            lastAttachmentId = anchorId,
            offset = items.size,
        )
    }

    private fun loadPrevAttachments(
        lastAttachmentId: Long,
        isLoadingMore: Boolean,
        offset: Int,
    ) {
        loadAttachments(
            loadType = LoadPrev,
            isLoadingMore = isLoadingMore,
        ) {
            attachmentInteractor.getPrevAttachments(
                conversationId = channelId,
                lastAttachmentId = lastAttachmentId,
                types = mediaTypes,
                offset = offset,
            )
        }
    }

    private fun loadNextAttachments(
        lastAttachmentId: Long,
        offset: Int,
    ) {
        loadAttachments(
            loadType = LoadNext,
            isLoadingMore = true,
        ) {
            attachmentInteractor.getNextAttachments(
                conversationId = channelId,
                lastAttachmentId = lastAttachmentId,
                types = mediaTypes,
                offset = offset,
            )
        }
    }

    private fun loadNearAttachments(
        lastAttachmentId: Long,
    ) {
        loadAttachments(
            loadType = LoadNear,
            isLoadingMore = false,
        ) {
            attachmentInteractor.getNearAttachments(
                conversationId = channelId,
                attachmentId = lastAttachmentId,
                types = mediaTypes,
                offset = 0,
            )
        }
    }

    private fun loadAttachments(
        loadType: LoadType,
        isLoadingMore: Boolean,
        request: suspend () -> Flow<PaginationResponse<AttachmentWithUserData>>,
    ) {
        setPagingLoadingStarted(loadType)
        notifyPageLoadingState(isLoadingMore)

        viewModelScope.launch(ioDispatcher) {
            request().collect(::handlePaginationResponse)
        }
    }

    private suspend fun handlePaginationResponse(
        response: PaginationResponse<AttachmentWithUserData>,
    ) {
        when (response) {
            is PaginationResponse.DBResponse -> {
                if (!checkIgnoreDatabasePagingResponse(response)) {
                    handlePaginationDbResponse(response)
                }
            }

            is PaginationResponse.ServerResponse -> {
                handlePaginationServerResponse(response)
            }

            else -> return
        }

        pagingResponseReceived(response)
    }

    private suspend fun handlePaginationDbResponse(
        response: PaginationResponse.DBResponse<AttachmentWithUserData>,
    ) {
        val newItems = mapToMediaItems(response.data)

        withContext(Dispatchers.Main) {
            updateList(
                newItems = newItems,
                loadType = response.loadType,
            )

            notifyPageStateWithResponse(
                response = SceytResponse.Success(null),
                wasLoadingMore = response.offset > 0,
                isEmpty = response.data.isEmpty(),
            )
        }
    }

    private suspend fun handlePaginationServerResponse(
        response: PaginationResponse.ServerResponse<AttachmentWithUserData>,
    ) {
        val newItems = mapToMediaItems(response.cacheData)

        withContext(Dispatchers.Main) {
            if (response.hasDiff) {
                val ordered = newItems.applyDisplayOrder()
                if (response.loadType == LoadNear && !initialScrollSet) {
                    val pos = ordered.indexOfFirst { it.attachment.id == openedWithAttachment?.id }
                    if (pos >= 0) {
                        setPendingInitialScroll(pos, source = "server")
                    }
                }
                _mediaItems.update { ordered }
            }

            notifyPageStateWithResponse(
                response = response.data,
                wasLoadingMore = response.offset > 0,
                isEmpty = response.cacheData.isEmpty(),
            )
        }
    }

    private fun updateList(
        newItems: List<MediaItem>,
        loadType: LoadType,
    ) {
        when (loadType) {
            LoadNear -> {
                val ordered = newItems.applyDisplayOrder()
                val openedId = openedWithAttachment?.id
                val posInNewList = ordered.indexOfFirst { it.attachment.id == openedId }
                if (posInNewList >= 0 && !initialScrollSet) {
                    setPendingInitialScroll(posInNewList, source = "db")
                }
                _mediaItems.update { ordered }
            }

            LoadPrev -> {
                val orderedItems = newItems.applyDisplayOrder()

                _mediaItems.update { current ->
                    val mergedItems = removeInitialPlaceholderDuplicateIfNeeded(
                        current = current,
                        incoming = orderedItems,
                    )

                    if (mergedItems.isEmpty()) return@update current

                    if (reversed) current + mergedItems else mergedItems + current
                }
            }

            LoadNext -> {
                val orderedItems = newItems.applyDisplayOrder()

                _mediaItems.update { current ->
                    if (orderedItems.isEmpty()) return@update current
                    if (reversed) orderedItems + current else current + orderedItems
                }
            }

            else -> return
        }
    }

    private fun removeInitialPlaceholderDuplicateIfNeeded(
        current: List<MediaItem>,
        incoming: List<MediaItem>,
    ): List<MediaItem> {
        if (current.size != 1) return incoming

        val currentItem = current.first()
        val isPlaceholderItem = currentItem.attachment.id == 0L
        if (!isPlaceholderItem) return incoming

        return incoming.filterNot { it.attachment.url == currentItem.attachment.url }
    }

    private fun List<MediaItem>.applyDisplayOrder(): List<MediaItem> {
        return if (reversed) reversed() else this
    }

    private fun mapToMediaItems(
        data: List<AttachmentWithUserData>?,
    ): List<MediaItem> {
        if (data.isNullOrEmpty()) return emptyList()
        return data.mapNotNull { it.toMediaItem() }
    }

    private fun AttachmentWithUserData?.toMediaItem(): MediaItem? {
        this ?: return null

        val mediaType = when (attachment.type) {
            AttachmentTypeEnum.Image.value -> MediaItemType.Image
            AttachmentTypeEnum.Video.value -> MediaItemType.Video
            else -> return null
        }

        return MediaItem(
            data = AttachmentWithUserData(
                attachment = attachment,
                user = user,
            ),
            _dataFromJson = attachment.getInfoFromMetadata(),
            _thumbPath = null,
            _transferData = attachment.toTransferData(),
            type = mediaType,
        )
    }

    fun needMediaInfo(data: NeedMediaInfoData) {
        val attachment = data.item

        when (data) {
            is NeedMediaInfoData.NeedDownload -> {
                viewModelScope.launch(Dispatchers.IO) {
                    fileTransferService.download(attachment)
                }
            }

            is NeedMediaInfoData.NeedThumb -> {
                viewModelScope.launch(Dispatchers.IO) {
                    fileTransferService.getThumb(
                        messageTid = attachment.messageTid,
                        attachment = attachment,
                        thumbData = data.thumbData,
                    )
                }
            }
        }
    }

    suspend fun getMessageById(messageId: Long): SceytMessage? {
        return messageInteractor.getMessageFromDbById(messageId)
    }

    private fun setPendingInitialScroll(
        position: Int,
        source: String,
    ) {
        initialScrollIndex = position
        pendingInitialScrollIndex = position
        initialScrollSet = true
        SceytLog.i(
            LOG_TAG,
            "Set pending initial scroll source=$source index=$position openedAttachment=${openedWithAttachment.toLogString()}"
        )
    }

    private fun SceytAttachment?.toLogString(): String {
        this ?: return "null"
        return "id=$id, messageId=$messageId, messageTid=$messageTid, type=$type"
    }

    companion object {
        private const val LOG_TAG = "MediaPreviewTag"
    }
}
