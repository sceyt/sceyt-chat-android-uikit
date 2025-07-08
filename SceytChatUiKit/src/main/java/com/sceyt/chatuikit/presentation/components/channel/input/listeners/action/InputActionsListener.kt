package com.sceyt.chatuikit.presentation.components.channel.input.listeners.action

import android.text.Editable
import com.sceyt.chat.models.message.Message
import com.sceyt.chatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.presentation.components.channel.input.data.InputUserAction
import com.sceyt.chatuikit.presentation.components.channel.input.mention.Mention
import com.sceyt.chatuikit.presentation.components.channel.input.format.BodyStyleRange

sealed interface InputActionsListener {

    fun interface SendMessageListener : InputActionsListener {
        fun sendMessage(message: Message, linkDetails: LinkPreviewDetails?)
    }

    fun interface SendMessagesListener : InputActionsListener {
        fun sendMessages(message: List<Message>, linkDetails: LinkPreviewDetails?)
    }

    fun interface SendEditMessageListener : InputActionsListener {
        fun sendEditMessage(message: SceytMessage, linkDetails: LinkPreviewDetails?)
    }

    fun interface ChannelEventListener : InputActionsListener {
        fun sendChannelEvent(state: InputUserAction)
    }

    fun interface UpdateDraftMessageListener {
        fun updateDraftMessage(text: Editable?, mentionUserIds: List<Mention>,
                               styling: List<BodyStyleRange>?,
                               replyOrEditMessage: SceytMessage?, isReply: Boolean)
    }

    /** Use this if you want to implement all callbacks */
    interface InputActionListeners : SendMessageListener, SendMessagesListener,
            SendEditMessageListener, ChannelEventListener, UpdateDraftMessageListener
}

internal fun InputActionsListener.setListener(listener: InputActionsListener) {
    (this as? InputActionsListenerImpl)?.setListener(listener)
}