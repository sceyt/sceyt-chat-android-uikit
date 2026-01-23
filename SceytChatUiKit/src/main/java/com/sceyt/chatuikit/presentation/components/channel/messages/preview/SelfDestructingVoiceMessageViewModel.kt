package com.sceyt.chatuikit.presentation.components.channel.messages.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sceyt.chatuikit.data.models.messages.MarkerType
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.interactor.MessageInteractor
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class SelfDestructingVoiceMessageViewModel(
    message: SceytMessage
) : ViewModel(), SceytKoinComponent {

    private val messageInteractor: MessageInteractor by inject()
    private val channelId: Long = message.channelId
    private val _messageUpdatedFlow = MutableSharedFlow<SceytMessage>(replay = 1)
    val messageUpdatedFlow = _messageUpdatedFlow.asSharedFlow()

    private val currentMessage: SceytMessage?
        get() = _messageUpdatedFlow.replayCache.firstOrNull()

    init {
        _messageUpdatedFlow.tryEmit(message)

        MessagesCache.messageUpdatedFlow
            .onEach { (updatedChannelId, messages) ->
                if (updatedChannelId == channelId) {
                    messages.find { it.id == message.id || it.tid == message.tid }?.let { updatedMessage ->
                        updateMessage(updatedMessage)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun updateMessage(updatedMessage: SceytMessage) {
        val current = currentMessage ?: return
        if (updatedMessage.deliveryStatus >= current.deliveryStatus) {
            _messageUpdatedFlow.tryEmit(updatedMessage)
        }
    }

    fun sendOpenedMarker(message: SceytMessage) {
        if (!message.incoming) return
        if (message.userMarkers?.any { it.name == MarkerType.Opened.value } == true) return

        viewModelScope.launch {
            messageInteractor.addMessagesMarker(
                channelId = message.channelId,
                marker = MarkerType.Opened.value,
                message.id,
            )
        }
    }
}