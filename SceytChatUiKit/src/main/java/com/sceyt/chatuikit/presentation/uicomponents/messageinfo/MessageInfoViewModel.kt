package com.sceyt.chatuikit.presentation.uicomponents.messageinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chatuikit.data.channeleventobserver.ChannelEventsObserver
import com.sceyt.chatuikit.data.channeleventobserver.MessageMarkerEventData
import com.sceyt.chatuikit.data.connectionobserver.ConnectionEventsObserver
import com.sceyt.chatuikit.data.messageeventobserver.MessageStatusChangeData
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.MarkerTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytMarker
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.toFileListItem
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.filetransfer.FileTransferService
import com.sceyt.chatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.chatuikit.persistence.interactor.MessageInteractor
import com.sceyt.chatuikit.persistence.interactor.MessageMarkerInteractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject

sealed interface UIState {
    data class Success(
            val readMarkers: List<SceytMarker>,
            val deliveredMarkers: List<SceytMarker>,
            val playedMarkers: List<SceytMarker>,
            val message: SceytMessage?,
            val isDataFromDatabase: Boolean
    ) : UIState

    data class Error(val exception: SceytException?) : UIState
    data object Loading : UIState
}

class MessageInfoViewModel(
        private val messageId: Long,
        private val channelId: Long
) : ViewModel(), SceytKoinComponent {
    private val markerInteractor: MessageMarkerInteractor by inject()
    private val messageInteractor: MessageInteractor by inject()
    private val fileTransferService: FileTransferService by inject()

    private val _uiState = MutableStateFlow<UIState>(UIState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _messageFlow = MutableSharedFlow<SceytMessage>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val messageFlow = _messageFlow.asSharedFlow()

    private val limit = 100
    private var message: SceytMessage? = null

    init {
        ChannelEventsObserver.onMessageStatusFlow
            .filter { it.marker.messageIds.contains(messageId) }
            .onEach(::onMessageStatusChange)
            .launchIn(viewModelScope)

        ChannelEventsObserver.onMarkerReceivedFlow
            .filter { it.marker.messageIds.contains(messageId) }
            .onEach(::onMarkerReceived)
            .launchIn(viewModelScope)

        getMessage()
        getAllMarkers()
    }

    private fun getMessage() {
        viewModelScope.launch(Dispatchers.IO) {
            (messageInteractor.getMessageDbById(messageId)
                    ?: messageInteractor.getMessageFromServerById(channelId, messageId).run {
                        if (this is SceytResponse.Success) this.data else null
                    })?.let { message ->

                this@MessageInfoViewModel.message = initMessageFiles(message)
                _messageFlow.tryEmit(message)
            }
        }
    }

    private fun onMessageStatusChange(data: MessageStatusChangeData) {
        viewModelScope.launch(Dispatchers.Default) {
            if ((message?.deliveryStatus ?: DeliveryStatus.Pending) < data.status)
                message?.deliveryStatus = data.status

            when (data.status) {
                DeliveryStatus.Displayed -> {
                    val state = _uiState.value
                    if (state is UIState.Success) {
                        if (state.readMarkers.any { it.userId == data.from.id }) return@launch
                        val newReadMarkers = state.readMarkers.toMutableList()
                        val deliveredMarkers = state.deliveredMarkers.filter { it.userId != data.from.id }
                        newReadMarkers.add(0, SceytMarker(messageId, data.from, data.status.name, data.marker.createdAt))
                        _uiState.update {
                            state.copy(
                                readMarkers = newReadMarkers,
                                deliveredMarkers = deliveredMarkers
                            )
                        }
                    }
                }

                DeliveryStatus.Received -> {
                    val state = _uiState.value
                    if (state is UIState.Success) {
                        if (state.deliveredMarkers.any { it.userId == data.from.id }) return@launch
                        val newDeliveredMarkers = state.deliveredMarkers.toMutableList()
                        newDeliveredMarkers.add(0, SceytMarker(messageId, data.from, data.status.name, data.marker.createdAt))
                        _uiState.update {
                            state.copy(deliveredMarkers = newDeliveredMarkers)
                        }
                    }
                }

                else -> return@launch
            }
        }
    }

    private fun onMarkerReceived(data: MessageMarkerEventData) {
        if (data.marker.name == MarkerTypeEnum.Played.value()) {
            val state = _uiState.value
            if (state is UIState.Success) {
                if (state.playedMarkers.any { it.userId == data.user.id }) return
                val playedMarkers = state.playedMarkers.toMutableList()
                playedMarkers.add(0, SceytMarker(messageId, data.user, data.marker.name, data.marker.createdAt))
                _uiState.update {
                    state.copy(playedMarkers = playedMarkers)
                }
            }
        }
    }

    private fun getAllMarkers() {
        viewModelScope.launch(Dispatchers.IO) {
            val read = DeliveryStatus.Displayed.name.lowercase()
            val displayed = DeliveryStatus.Received.name.lowercase()
            val played = MarkerTypeEnum.Played.value()

            markerInteractor.getMessageMarkersDb(messageId, listOf(read, displayed, played), 0, limit).let { markers ->
                markers.groupBy { it.name }.let {
                    onMarkerDbResponse(it[read].orEmpty(), it[displayed].orEmpty(), it[played].orEmpty())
                }
            }

            ConnectionEventsObserver.awaitToConnectSceyt()

            val readMarkers = async {
                markerInteractor.getMessageMarkers(messageId, read, 0, limit)
            }
            val deliveredMarkers = async {
                markerInteractor.getMessageMarkers(messageId, displayed, 0, limit)
            }

            val playedMarkers = async {
                markerInteractor.getMessageMarkers(messageId, played, 0, limit)
            }

            val readMarkersResult = readMarkers.await()
            val deliveredMarkersResult = deliveredMarkers.await()
            val playedMarkersResult = playedMarkers.await()

            onMarkersServerResponse(readMarkersResult, deliveredMarkersResult, playedMarkersResult)
        }
    }

    private suspend fun onMarkerDbResponse(readMarkersResult: List<SceytMarker>,
                                           deliveredMarkersResult: List<SceytMarker>,
                                           playedMarkersResult: List<SceytMarker>) {

        val filteredDeliver = deliveredMarkersResult.filter { deliveredMarker ->
            readMarkersResult.none { readMarker ->
                readMarker.userId == deliveredMarker.userId
            }
        }
        withContext(Dispatchers.Main) {
            _uiState.update {
                UIState.Success(
                    readMarkers = readMarkersResult,
                    deliveredMarkers = filteredDeliver,
                    playedMarkers = playedMarkersResult,
                    message = message, true)
            }
        }
    }

    private suspend fun onMarkersServerResponse(readMarkersResult: SceytResponse<List<SceytMarker>>,
                                                deliveredMarkersResult: SceytResponse<List<SceytMarker>>,
                                                playedMarkersResult: SceytResponse<List<SceytMarker>>) {
        when {
            readMarkersResult is SceytResponse.Success && deliveredMarkersResult is SceytResponse.Success
                    && playedMarkersResult is SceytResponse.Success -> {
                val filteredDeliver = deliveredMarkersResult.data?.filter { deliveredMarker ->
                    readMarkersResult.data?.none { readMarker ->
                        readMarker.userId == deliveredMarker.userId
                    } == true
                } ?: emptyList()

                withContext(Dispatchers.Main) {
                    _uiState.update {
                        UIState.Success(
                            readMarkers = readMarkersResult.data.orEmpty(),
                            deliveredMarkers = filteredDeliver,
                            playedMarkers = playedMarkersResult.data.orEmpty(),
                            message = message, false)
                    }
                }
            }

            readMarkersResult is SceytResponse.Error -> {
                _uiState.update { UIState.Error(readMarkersResult.exception) }
            }

            deliveredMarkersResult is SceytResponse.Error -> {
                _uiState.update { UIState.Error(deliveredMarkersResult.exception) }
            }
        }
    }

    private fun initMessageFiles(sceytMessage: SceytMessage): SceytMessage {
        return sceytMessage.apply {
            files = attachments?.map { it.toFileListItem(this) }
        }
    }

    fun getMessageAttachmentSizeIfExist(message: SceytMessage): Long? {
        return message.attachments?.find {
            it.type != AttachmentTypeEnum.Link.value() && it.type != AttachmentTypeEnum.File.value()
        }?.fileSize
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
}