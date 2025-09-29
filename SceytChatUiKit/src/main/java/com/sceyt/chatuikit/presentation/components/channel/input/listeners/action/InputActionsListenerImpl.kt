package com.sceyt.chatuikit.presentation.components.channel.input.listeners.action

import android.text.Editable
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.message.Message
import com.sceyt.chatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.media.audio.AudioRecordData
import com.sceyt.chatuikit.presentation.components.channel.input.data.InputUserAction
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
    private var channelEventListener: InputActionsListener.ChannelEventListener? = null
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

    override fun sendChannelEvent(state: InputUserAction) {
        defaultListeners?.sendChannelEvent(state)
        channelEventListener?.sendChannelEvent(state)
    }

    override fun updateDraftMessage(
            text: Editable?,
            attachments: List<Attachment>,
            audioRecordData: AudioRecordData?,
            mentionUserIds: List<Mention>,
            styling: List<BodyStyleRange>?,
            replyOrEditMessage: SceytMessage?,
            isReply: Boolean,
    ) {
        defaultListeners?.updateDraftMessage(
            text = text,
            attachments = attachments,
            audioRecordData = audioRecordData,
            mentionUserIds = mentionUserIds,
            styling = styling,
            replyOrEditMessage = replyOrEditMessage,
            isReply = isReply
        )
        updateDraftMessageListener?.updateDraftMessage(
            text = text,
            attachments = attachments,
            audioRecordData = audioRecordData,
            mentionUserIds = mentionUserIds,
            styling = styling,
            replyOrEditMessage = replyOrEditMessage,
            isReply = isReply
        )
    }

    fun setListener(listener: InputActionsListener) {
        when (listener) {
            is InputActionsListener.InputActionListeners -> {
                defaultListeners = listener
                sendMessageListener = listener
                sendMessagesListener = listener
                sendEditMessageListener = listener
                channelEventListener = listener
                updateDraftMessageListener = listener
            }

            is InputActionsListener.SendMessageListener -> sendMessageListener = listener
            is InputActionsListener.SendMessagesListener -> sendMessagesListener = listener
            is InputActionsListener.SendEditMessageListener -> sendEditMessageListener = listener
            is InputActionsListener.ChannelEventListener -> channelEventListener = listener
            is InputActionsListener.UpdateDraftMessageListener -> updateDraftMessageListener = listener
        }
    }

    internal fun withDefaultListeners(
            listener: InputActionsListener.InputActionListeners,
    ): InputActionsListenerImpl {
        defaultListeners = listener
        return this
    }
}