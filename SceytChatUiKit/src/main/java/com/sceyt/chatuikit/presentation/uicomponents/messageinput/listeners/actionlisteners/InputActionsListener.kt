package com.sceyt.chatuikit.presentation.uicomponents.messageinput.listeners.actionlisteners

import android.text.Editable
import com.sceyt.chat.models.message.Message
import com.sceyt.chatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.mention.Mention
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.style.BodyStyleRange

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

    fun interface TypingListener : InputActionsListener {
        fun sendTyping(typing: Boolean)
    }

    fun interface UpdateDraftMessageListener {
        fun updateDraftMessage(text: Editable?, mentionUserIds: List<Mention>,
                               styling: List<BodyStyleRange>?,
                               replyOrEditMessage: SceytMessage?, isReply: Boolean)
    }

    /** Use this if you want to implement all callbacks */
    interface InputActionListeners : SendMessageListener, SendMessagesListener,
            SendEditMessageListener, TypingListener, UpdateDraftMessageListener
}