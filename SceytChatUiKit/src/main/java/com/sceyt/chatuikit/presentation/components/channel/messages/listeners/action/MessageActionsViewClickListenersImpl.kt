package com.sceyt.chatuikit.presentation.components.channel.messages.listeners.action

import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.presentation.components.channel.messages.MessagesListView

open class MessageActionsViewClickListenersImpl(view: MessagesListView) : MessageActionsViewClickListeners.ActionsViewClickListeners {
    private var defaultListeners: MessageActionsViewClickListeners.ActionsViewClickListeners = view
    private var copyMessageListener: MessageActionsViewClickListeners.CopyMessage? = null
    private var deleteMessageListener: MessageActionsViewClickListeners.DeleteMessage? = null
    private var editMessageListener: MessageActionsViewClickListeners.EditMessage? = null
    private var messageInfoListener: MessageActionsViewClickListeners.MessageInfo? = null
    private var forwardMessageListener: MessageActionsViewClickListeners.ForwardMessage? = null
    private var reactMessageListener: MessageActionsViewClickListeners.ReactMessage? = null
    private var replyMessageListener: MessageActionsViewClickListeners.ReplyMessage? = null
    private var replyInThreadMessageListener: MessageActionsViewClickListeners.ReplyInThreadMessage? = null

    override fun onCopyMessagesClick(vararg messages: SceytMessage) {
        defaultListeners.onCopyMessagesClick(*messages)
        copyMessageListener?.onCopyMessagesClick(*messages)
    }

    override fun onDeleteMessageClick(vararg messages: SceytMessage, requireForMe: Boolean, actionFinish: () -> Unit) {
        defaultListeners.onDeleteMessageClick(*messages, requireForMe = requireForMe, actionFinish = actionFinish)
        deleteMessageListener?.onDeleteMessageClick(*messages, requireForMe = requireForMe, actionFinish = actionFinish)
    }

    override fun onEditMessageClick(message: SceytMessage) {
        defaultListeners.onEditMessageClick(message)
        editMessageListener?.onEditMessageClick(message)
    }

    override fun onMessageInfoClick(message: SceytMessage) {
        defaultListeners.onMessageInfoClick(message)
        messageInfoListener?.onMessageInfoClick(message)
    }

    override fun onReactMessageClick(message: SceytMessage) {
        defaultListeners.onReactMessageClick(message)
        reactMessageListener?.onReactMessageClick(message)
    }

    override fun onForwardMessageClick(vararg messages: SceytMessage) {
        defaultListeners.onForwardMessageClick(*messages)
        forwardMessageListener?.onForwardMessageClick(*messages)
    }

    override fun onReplyMessageClick(message: SceytMessage) {
        defaultListeners.onReplyMessageClick(message)
        replyMessageListener?.onReplyMessageClick(message)
    }

    override fun onReplyMessageInThreadClick(message: SceytMessage) {
        defaultListeners.onReplyMessageInThreadClick(message)
        replyInThreadMessageListener?.onReplyMessageInThreadClick(message)
    }

    fun setListener(listener: MessageActionsViewClickListeners) {
        when (listener) {
            is MessageActionsViewClickListeners.ActionsViewClickListeners -> {
                copyMessageListener = listener
                deleteMessageListener = listener
                editMessageListener = listener
                messageInfoListener = listener
                forwardMessageListener = listener
                reactMessageListener = listener
                replyMessageListener = listener
                replyInThreadMessageListener = listener
            }

            is MessageActionsViewClickListeners.CopyMessage -> {
                copyMessageListener = listener
            }

            is MessageActionsViewClickListeners.DeleteMessage -> {
                deleteMessageListener = listener
            }

            is MessageActionsViewClickListeners.EditMessage -> {
                editMessageListener = listener
            }

            is MessageActionsViewClickListeners.MessageInfo -> {
                messageInfoListener = listener
            }

            is MessageActionsViewClickListeners.ForwardMessage -> {
                forwardMessageListener = listener
            }

            is MessageActionsViewClickListeners.ReactMessage -> {
                reactMessageListener = listener
            }

            is MessageActionsViewClickListeners.ReplyMessage -> {
                replyMessageListener = listener
            }

            is MessageActionsViewClickListeners.ReplyInThreadMessage -> {
                replyInThreadMessageListener = listener
            }
        }
    }
}