package com.sceyt.chatuikit.presentation.uicomponents.messageinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.Marker
import com.sceyt.chatuikit.data.channeleventobserver.ChannelEventsObserver
import com.sceyt.chatuikit.data.messageeventobserver.MessageStatusChangeData
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.MarkerTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.di.SceytKoinComponent
import com.sceyt.chatuikit.extensions.orEmptyList
import com.sceyt.chatuikit.persistence.logics.messageslogic.PersistenceMessagesLogic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.inject

sealed interface UIState {
    data class Success(
            val readMarkers: List<Marker>,
            val deliveredMarkers: List<Marker>,
            val playedMarkers: List<Marker>,
            val message: SceytMessage
    ) : UIState

    data class Error(val exception: SceytException?) : UIState
    data object Loading : UIState
}

class MessageInfoViewModel(private val message: SceytMessage) : ViewModel(), SceytKoinComponent {
    private val messagesLogic: PersistenceMessagesLogic by inject()
    private val limit = 100

    private val _uiState = MutableStateFlow<UIState>(UIState.Loading)
    val uiState = _uiState.asStateFlow()

    private val messageId = message.id

    init {
        ChannelEventsObserver.onMessageStatusFlow
            .filter { it.messageIds.contains(messageId) }
            .onEach(::onMessageStatusChange)
            .launchIn(viewModelScope)

        getAllMarkers()
    }

    private fun onMessageStatusChange(data: MessageStatusChangeData) {
        viewModelScope.launch(Dispatchers.Default) {
            if (message.deliveryStatus < data.status)
                message.deliveryStatus = data.status

            when (data.status) {
                DeliveryStatus.Displayed -> {
                    val state = _uiState.value
                    if (state is UIState.Success) {
                        if (state.readMarkers.any { it.user.id == data.from?.id }) return@launch
                        val newReadMarkers = state.readMarkers.toMutableList()
                        val deliveredMarkers = state.deliveredMarkers.filter { it.user.id != data.from?.id }
                        newReadMarkers.add(Marker(messageId, data.from, data.status.name, System.currentTimeMillis()))
                        _uiState.update {
                            UIState.Success(
                                readMarkers = newReadMarkers,
                                deliveredMarkers = deliveredMarkers,
                                playedMarkers = state.playedMarkers,
                                message = message)
                        }
                    }
                }

                DeliveryStatus.Received -> {
                    val state = _uiState.value
                    if (state is UIState.Success) {
                        if (state.deliveredMarkers.any { it.user.id == data.from?.id }) return@launch
                        val newDeliveredMarkers = state.deliveredMarkers.toMutableList()
                        newDeliveredMarkers.add(Marker(messageId, data.from, data.status.name, System.currentTimeMillis()))
                        _uiState.update {
                            UIState.Success(
                                readMarkers = state.readMarkers,
                                deliveredMarkers = newDeliveredMarkers,
                                playedMarkers = state.playedMarkers,
                                message = message)
                        }
                    }
                }

                else -> return@launch
            }
        }
    }

    fun getAllMarkers() {
        viewModelScope.launch(Dispatchers.IO) {
            val read = DeliveryStatus.Displayed.name.lowercase()
            val displayed = DeliveryStatus.Received.name.lowercase()
            val played = MarkerTypeEnum.Played.value()

            val readMarkers = async {
                messagesLogic.getMessageMarkers(messageId, read, 0, limit)
            }
            val deliveredMarkers = async {
                messagesLogic.getMessageMarkers(messageId, displayed, 0, limit)
            }

            val playedMarkers = async {
                messagesLogic.getMessageMarkers(messageId, played, 0, limit)
            }

            val readMarkersResult = readMarkers.await()
            val deliveredMarkersResult = deliveredMarkers.await()
            val playedMarkersResult = playedMarkers.await()

            when {
                readMarkersResult is SceytResponse.Success && deliveredMarkersResult is SceytResponse.Success
                        && playedMarkersResult is SceytResponse.Success -> {
                    val filteredDeliver = deliveredMarkersResult.data?.filter { deliveredMarker ->
                        readMarkersResult.data?.none { readMarker ->
                            readMarker.user.id == deliveredMarker.user.id
                        } == true
                    } ?: emptyList()

                    _uiState.value = UIState.Success(
                        readMarkers = readMarkersResult.data.orEmptyList(),
                        deliveredMarkers = filteredDeliver,
                        playedMarkers = playedMarkersResult.data.orEmptyList(),
                        message = message)
                }

                readMarkersResult is SceytResponse.Error -> {
                    _uiState.value = UIState.Error(readMarkersResult.exception)
                }

                deliveredMarkersResult is SceytResponse.Error -> {
                    _uiState.value = UIState.Error(deliveredMarkersResult.exception)
                }
            }
        }
    }

    fun getMessageAttachmentSizeIfExist(message: SceytMessage): Long? {
        return message.attachments?.find {
            it.type != AttachmentTypeEnum.Link.value() && it.type != AttachmentTypeEnum.File.value()
        }?.fileSize
    }
}