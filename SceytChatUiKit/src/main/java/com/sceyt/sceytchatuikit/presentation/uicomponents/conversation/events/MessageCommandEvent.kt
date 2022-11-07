package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.events

import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.ScrollToDownView

sealed class MessageCommandEvent {

    data class DeleteMessage(
            val message: SceytMessage,
            val onlyForMe: Boolean
    ) : MessageCommandEvent()

    data class EditMessage(
            val message: SceytMessage,
    ) : MessageCommandEvent()

    data class Replay(
            val message: SceytMessage,
    ) : MessageCommandEvent()

    data class ScrollToDown(
            val view: ScrollToDownView
    ) : MessageCommandEvent()
}