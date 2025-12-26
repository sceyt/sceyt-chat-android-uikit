package com.sceyt.chatuikit.presentation.components.channel.messages.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sceyt.chatuikit.data.models.messages.MarkerType
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.interactor.MessageInteractor
import kotlinx.coroutines.launch

class SelfDestructingMediaPreviewViewModel(
    private val messageInteractor: MessageInteractor
) : ViewModel() {

    fun sendOpenedMarker(message: SceytMessage) {
        if (!message.incoming) return
        if (message.userMarkers?.any { it.name == MarkerType.Opened.value } == true) return

        viewModelScope.launch {
                messageInteractor.addMessagesMarker(message.channelId, MarkerType.Opened.value, message.id)
        }
    }
}