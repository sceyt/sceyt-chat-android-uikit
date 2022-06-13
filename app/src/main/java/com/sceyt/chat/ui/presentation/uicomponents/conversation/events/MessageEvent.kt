package com.sceyt.chat.ui.presentation.uicomponents.conversation.events

import android.content.Context
import com.sceyt.chat.ui.data.models.messages.SceytMessage

sealed class MessageEvent {

    data class DeleteMessage(
            val message: SceytMessage,
    ) : MessageEvent()

    data class EditMessage(
            val message: SceytMessage,
    ) : MessageEvent()

    data class Replay(
            val message: SceytMessage,
    ) : MessageEvent()

    data class ReplayInThread(
            val message: SceytMessage,
            val context: Context
    ) : MessageEvent()
}