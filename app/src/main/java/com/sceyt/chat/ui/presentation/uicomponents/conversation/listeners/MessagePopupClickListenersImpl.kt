package com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners

import android.view.View
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.presentation.uicomponents.conversation.MessagesListView

open class MessagePopupClickListenersImpl(view: MessagesListView) : MessagePopupClickListeners.PopupClickListeners {
    private var defaultListeners: MessagePopupClickListeners.PopupClickListeners = view
    private var copyMessageListener: MessagePopupClickListeners.CopyMessage? = null
    private var deleteMessageListener: MessagePopupClickListeners.DeleteMessage? = null
    private var editMessageListener: MessagePopupClickListeners.EditMessage? = null
    private var reactMessageListener: MessagePopupClickListeners.ReactMessage? = null
    private var replayMessageListener: MessagePopupClickListeners.ReplayMessage? = null
    private var replayInThreadMessageListener: MessagePopupClickListeners.ReplayInThreadMessage? = null

    override fun onCopyMessageClick(message: SceytMessage) {
        defaultListeners.onCopyMessageClick(message)
        copyMessageListener?.onCopyMessageClick(message)
    }

    override fun onDeleteMessageClick(message: SceytMessage) {
        defaultListeners.onDeleteMessageClick(message)
        deleteMessageListener?.onDeleteMessageClick(message)
    }

    override fun onEditMessageClick(message: SceytMessage) {
        defaultListeners.onEditMessageClick(message)
        editMessageListener?.onEditMessageClick(message)
    }

    override fun onReactMessageClick(view: View, message: SceytMessage) {
        defaultListeners.onReactMessageClick(view, message)
        reactMessageListener?.onReactMessageClick(view, message)
    }

    override fun onReplayMessageClick(message: SceytMessage) {
        defaultListeners.onReplayMessageClick(message)
        replayMessageListener?.onReplayMessageClick(message)
    }

    override fun onReplayInThreadMessageClick(message: SceytMessage) {
        defaultListeners.onReplayInThreadMessageClick(message)
        replayInThreadMessageListener?.onReplayInThreadMessageClick(message)
    }

    fun setListener(listener: MessagePopupClickListeners) {
        when (listener) {
            is MessagePopupClickListeners.PopupClickListeners -> {
                copyMessageListener = listener
                deleteMessageListener = listener
                editMessageListener = listener
                reactMessageListener = listener
                replayMessageListener = listener
                replayInThreadMessageListener = listener
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
            is MessagePopupClickListeners.ReplayMessage -> {
               replayMessageListener = listener
            }
            is MessagePopupClickListeners.ReplayInThreadMessage -> {
               replayInThreadMessageListener = listener
            }
        }
    }
}