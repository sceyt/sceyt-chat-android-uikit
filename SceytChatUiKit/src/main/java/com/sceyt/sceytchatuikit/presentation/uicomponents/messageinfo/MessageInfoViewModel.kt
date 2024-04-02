package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.Marker
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.persistence.logics.messageslogic.PersistenceMessagesLogic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.inject

sealed interface UIState {
    data class Success(
            val readMarkers: List<Marker>,
            val deliveredMarkers: List<Marker>,
    ) : UIState

    data class Error(val exception: SceytException?) : UIState
    data object Loading : UIState
}


class MessageInfoViewModel : ViewModel(), SceytKoinComponent {
    private val messagesLogic: PersistenceMessagesLogic by inject()

    private val _uiState = MutableStateFlow<UIState>(UIState.Loading)
    val uiState = _uiState.asStateFlow()

    fun getAllMarkers(messageId: Long, offset: Int, limit: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val read = DeliveryStatus.Displayed.name.lowercase()
            val displayed = DeliveryStatus.Received.name.lowercase()

            val readMarkers = async {
                messagesLogic.getMessageMarkers(messageId, read, offset, limit)
            }
            val deliveredMarkers = async {
                messagesLogic.getMessageMarkers(messageId, displayed, offset, limit)
            }

            val readMarkersResult = readMarkers.await()
            val deliveredMarkersResult = deliveredMarkers.await()

            when {
                readMarkersResult is SceytResponse.Success && deliveredMarkersResult is SceytResponse.Success -> {
                    val filteredDeliver = deliveredMarkersResult.data?.filter { deliveredMarker ->
                        readMarkersResult.data?.none { readMarker ->
                            readMarker.user.id == deliveredMarker.user.id
                        } == true
                    } ?: emptyList()

                    _uiState.value = UIState.Success(readMarkersResult.data
                            ?: emptyList(), filteredDeliver)
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