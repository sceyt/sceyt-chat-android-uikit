package com.sceyt.chatuikit.presentation.components.channel.input.listeners.action

import android.text.Editable
import com.sceyt.chat.models.message.Message
import com.sceyt.chatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.presentation.components.channel.input.mention.Mention
import com.sceyt.chatuikit.presentation.components.channel.input.format.BodyStyleRange

open class InputActionsListenerImpl(
        private var defaultListeners: InputActionsListener.InputActionListeners?
) : InputActionsListener.InputActionListeners {
    private var sendMessageListener: InputActionsListener.SendMessageListener? = null
    private var sendMessagesListener: InputActionsListener.SendMessagesListener? = null
    private var sendEditMessageListener: InputActionsListener.SendEditMessageListener? = null
    private var typingListener: InputActionsListener.TypingListener? = null
    private var updateDraftMessageListener: InputActionsListener.UpdateDraftMessageListener? = null

    override fun sendMessage(message: Message, linkDetails: LinkPreviewDetails?) {
        defaultListeners?.sendMessage(message, linkDetails)
        sendMessageListener?.sendMessage(message, linkDetails)
    }

    override fun sendMessages(message: List<Message>, linkDetails: LinkPreviewDetails?) {
        defaultListeners?.sendMessages(message, linkDetails)
        sendMessagesListener?.sendMessages(message, linkDetails)
    }

    override fun sendEditMessage(message: SceytMessage, linkDetails: LinkPreviewDetails?) {
        defaultListeners?.sendEditMessage(message, linkDetails)
        sendEditMessageListener?.sendEditMessage(message, linkDetails)
    }

    override fun sendTyping(typing: Boolean) {
        defaultListeners?.sendTyping(typing)
        typingListener?.sendTyping(typing)
    }

    override fun updateDraftMessage(text: Editable?, mentionUserIds: List<Mention>, styling: List<BodyStyleRange>?, replyOrEditMessage: SceytMessage?, isReply: Boolean) {
        defaultListeners?.updateDraftMessage(text, mentionUserIds, styling, replyOrEditMessage, isReply)
        updateDraftMessageListener?.updateDraftMessage(text, mentionUserIds, styling, replyOrEditMessage, isReply)
    }

    fun setListener(listener: InputActionsListener) {
        when (listener) {
            is InputActionsListener.InputActionListeners -> {
                defaultListeners = listener
                sendMessageListener = listener
                sendMessagesListener = listener
                sendEditMessageListener = listener
                typingListener = listener
                updateDraftMessageListener = listener
            }

            is InputActionsListener.SendMessageListener -> sendMessageListener = listener
            is InputActionsListener.SendMessagesListener -> sendMessagesListener = listener
            is InputActionsListener.SendEditMessageListener -> sendEditMessageListener = listener
            is InputActionsListener.TypingListener -> typingListener = listener
            is InputActionsListener.UpdateDraftMessageListener -> updateDraftMessageListener = listener
        }
    }
}