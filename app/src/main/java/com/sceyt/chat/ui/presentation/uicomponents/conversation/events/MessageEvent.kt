package com.sceyt.chat.ui.presentation.uicomponents.conversation.events

import com.sceyt.chat.ui.data.models.messages.SceytUiMessage

sealed class MessageEvent {

    data class DeleteMessage(
            val message: SceytUiMessage,
    ) : MessageEvent()

    data class EditMessage(
            val message: SceytUiMessage,
    ) : MessageEvent()

    data class Replay(
            val message: SceytUiMessage,
    ) : MessageEvent()

    data class ReplayInThread(
            val message: SceytUiMessage,
    ) : MessageEvent()
}