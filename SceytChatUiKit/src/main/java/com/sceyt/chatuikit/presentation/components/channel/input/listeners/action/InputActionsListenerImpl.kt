package com.sceyt.chatuikit.presentation.components.channel.input.listeners.action

import android.text.Editable
import com.sceyt.chat.models.message.Message
import com.sceyt.chatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.presentation.components.channel.input.data.InputUserActivity
import com.sceyt.chatuikit.presentation.components.channel.input.format.BodyStyleRange
import com.sceyt.chatuikit.presentation.components.channel.input.mention.Mention

open class InputActionsListenerImpl : InputActionsListener.InputActionListeners {
    @Suppress("unused")
    constructor()

    internal constructor(listener: InputActionsListener.InputActionListeners) {
        defaultListeners = listener
    }

    private var defaultListeners: InputActionsListener.InputActionListeners? = null
    private var sendMessageListener: InputActionsListener.SendMessageListener? = null
    private var sendMessagesListener: InputActionsListener.SendMessagesListener? = null
    private var sendEditMessageListener: InputActionsListener.SendEditMessageListener? = null
    private var userActivityListener: InputActionsListener.UserActivityListener? = null
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

    override fun sendUserActivity(state: InputUserActivity) {
        defaultListeners?.sendUserActivity(state)
        userActivityListener?.sendUserActivity(state)
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
                userActivityListener = listener
                updateDraftMessageListener = listener
            }

            is InputActionsListener.SendMessageListener -> sendMessageListener = listener
            is InputActionsListener.SendMessagesListener -> sendMessagesListener = listener
            is InputActionsListener.SendEditMessageListener -> sendEditMessageListener = listener
            is InputActionsListener.UserActivityListener -> userActivityListener = listener
            is InputActionsListener.UpdateDraftMessageListener -> updateDraftMessageListener = listener
        }
    }

    internal fun withDefaultListeners(
            listener: InputActionsListener.InputActionListeners
    ): InputActionsListenerImpl {
        defaultListeners = listener
        return this
    }
}