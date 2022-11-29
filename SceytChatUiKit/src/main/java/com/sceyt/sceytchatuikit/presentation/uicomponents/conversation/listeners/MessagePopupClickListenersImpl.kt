package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners

import android.view.View
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.MessagesListView

open class MessagePopupClickListenersImpl(view: MessagesListView) : MessagePopupClickListeners.PopupClickListeners {
    private var defaultListeners: MessagePopupClickListeners.PopupClickListeners = view
    private var copyMessageListener: MessagePopupClickListeners.CopyMessage? = null
    private var deleteMessageListener: MessagePopupClickListeners.DeleteMessage? = null
    private var editMessageListener: MessagePopupClickListeners.EditMessage? = null
    private var reactMessageListener: MessagePopupClickListeners.ReactMessage? = null
    private var replyMessageListener: MessagePopupClickListeners.ReplyMessage? = null
    private var replyInThreadMessageListener: MessagePopupClickListeners.ReplyInThreadMessage? = null

    override fun onCopyMessageClick(message: SceytMessage) {
        defaultListeners.onCopyMessageClick(message)
        copyMessageListener?.onCopyMessageClick(message)
    }

    override fun onDeleteMessageClick(message: SceytMessage, onlyForMe: Boolean) {
        defaultListeners.onDeleteMessageClick(message, onlyForMe)
        deleteMessageListener?.onDeleteMessageClick(message, onlyForMe)
    }

    override fun onEditMessageClick(message: SceytMessage) {
        defaultListeners.onEditMessageClick(message)
        editMessageListener?.onEditMessageClick(message)
    }

    override fun onReactMessageClick(view: View, message: SceytMessage) {
        defaultListeners.onReactMessageClick(view, message)
        reactMessageListener?.onReactMessageClick(view, message)
    }

    override fun onReplyMessageClick(message: SceytMessage) {
        defaultListeners.onReplyMessageClick(message)
        replyMessageListener?.onReplyMessageClick(message)
    }

    override fun onReplyMessageInThreadClick(message: SceytMessage) {
        defaultListeners.onReplyMessageInThreadClick(message)
        replyInThreadMessageListener?.onReplyMessageInThreadClick(message)
    }

    fun setListener(listener: MessagePopupClickListeners) {
        when (listener) {
            is MessagePopupClickListeners.PopupClickListeners -> {
                copyMessageListener = listener
                deleteMessageListener = listener
                editMessageListener = listener
                reactMessageListener = listener
                replyMessageListener = listener
                replyInThreadMessageListener = listener
            }
            is MessagePopupClickListeners.CopyMessage -> {
                copyMessageListener = listener
            }
            is MessagePopupClickListeners.DeleteMessage -> {
                deleteMessageListener = listener
            }
            is MessagePopupClickListeners.EditMessage -> {
                editMessageListener = listener
            }
            is MessagePopupClickListeners.ReactMessage -> {
                reactMessageListener = listener
            }
            is MessagePopupClickListeners.ReplyMessage -> {
                replyMessageListener = listener
            }
            is MessagePopupClickListeners.ReplyInThreadMessage -> {
                replyInThreadMessageListener = listener
            }
        }
    }
}