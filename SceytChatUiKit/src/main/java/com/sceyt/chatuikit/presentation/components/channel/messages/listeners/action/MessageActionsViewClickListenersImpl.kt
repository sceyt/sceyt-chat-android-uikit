package com.sceyt.chatuikit.presentation.components.channel.messages.listeners.action

import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.presentation.components.channel.messages.MessagesListView

open class MessageActionsViewClickListenersImpl : MessageActionsViewClickListeners.ActionsViewClickListeners {
    @Suppress("unused")
    constructor()

    internal constructor(view: MessagesListView) {
        defaultListeners = view
    }

    private var defaultListeners: MessageActionsViewClickListeners.ActionsViewClickListeners? = null
    private var copyMessageListener: MessageActionsViewClickListeners.CopyMessage? = null
    private var deleteMessageListener: MessageActionsViewClickListeners.DeleteMessage? = null
    private var editMessageListener: MessageActionsViewClickListeners.EditMessage? = null
    private var messageInfoListener: MessageActionsViewClickListeners.MessageInfo? = null
    private var forwardMessageListener: MessageActionsViewClickListeners.ForwardMessage? = null
    private var reactMessageListener: MessageActionsViewClickListeners.ReactMessage? = null
    private var replyMessageListener: MessageActionsViewClickListeners.ReplyMessage? = null
    private var replyInThreadMessageListener: MessageActionsViewClickListeners.ReplyInThreadMessage? = null
    private var retractVoteListener: MessageActionsViewClickListeners.RetractVote? = null
    private var endVoteListener: MessageActionsViewClickListeners.EndVote? = null

    override fun onCopyMessagesClick(vararg messages: SceytMessage) {
        defaultListeners?.onCopyMessagesClick(*messages)
        copyMessageListener?.onCopyMessagesClick(*messages)
    }

    override fun onDeleteMessageClick(vararg messages: SceytMessage, requireForMe: Boolean, actionFinish: () -> Unit) {
        defaultListeners?.onDeleteMessageClick(*messages, requireForMe = requireForMe, actionFinish = actionFinish)
        deleteMessageListener?.onDeleteMessageClick(*messages, requireForMe = requireForMe, actionFinish = actionFinish)
    }

    override fun onEditMessageClick(message: SceytMessage) {
        defaultListeners?.onEditMessageClick(message)
        editMessageListener?.onEditMessageClick(message)
    }

    override fun onMessageInfoClick(message: SceytMessage) {
        defaultListeners?.onMessageInfoClick(message)
        messageInfoListener?.onMessageInfoClick(message)
    }

    override fun onReactMessageClick(message: SceytMessage) {
        defaultListeners?.onReactMessageClick(message)
        reactMessageListener?.onReactMessageClick(message)
    }

    override fun onForwardMessageClick(vararg messages: SceytMessage) {
        defaultListeners?.onForwardMessageClick(*messages)
        forwardMessageListener?.onForwardMessageClick(*messages)
    }

    override fun onReplyMessageClick(message: SceytMessage) {
        defaultListeners?.onReplyMessageClick(message)
        replyMessageListener?.onReplyMessageClick(message)
    }

    override fun onReplyMessageInThreadClick(message: SceytMessage) {
        defaultListeners?.onReplyMessageInThreadClick(message)
        replyInThreadMessageListener?.onReplyMessageInThreadClick(message)
    }

    override fun onRetractVoteClick(message: SceytMessage) {
        defaultListeners?.onRetractVoteClick(message)
        retractVoteListener?.onRetractVoteClick(message)
    }

    override fun onEndVoteClick(message: SceytMessage) {
        defaultListeners?.onEndVoteClick(message)
        endVoteListener?.onEndVoteClick(message)
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
                retractVoteListener = listener
                endVoteListener = listener
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

            is MessageActionsViewClickListeners.RetractVote -> {
                retractVoteListener = listener
            }

            is MessageActionsViewClickListeners.EndVote -> {
                endVoteListener = listener
            }
        }
    }

    internal fun withDefaultListeners(
            listener: MessageActionsViewClickListeners.ActionsViewClickListeners
    ): MessageActionsViewClickListenersImpl {
        defaultListeners = listener
        return this
    }
}