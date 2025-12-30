package com.sceyt.chatuikit.presentation.components.channel.messages.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sceyt.chatuikit.data.models.messages.MarkerType
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.interactor.MessageInteractor
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class SelfDestructingMediaPreviewViewModel(
    message: SceytMessage
) : ViewModel(), SceytKoinComponent {

    private val messageInteractor: MessageInteractor by inject()

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