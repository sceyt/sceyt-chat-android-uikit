package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners

import android.view.View
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.MessageInputView
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.adapter.AttachmentItem

open class MessageInputClickListenersImpl(view: MessageInputView) : MessageInputClickListeners.ClickListeners {
    private var defaultListeners: MessageInputClickListeners.ClickListeners = view
    private var sendMsgClickListener: MessageInputClickListeners.SendMsgClickListener? = null
    private var sendAttachmentClickListener: MessageInputClickListeners.SendAttachmentClickListener? = null
    private var closeReplayMessageViewClickListener: MessageInputClickListeners.CloseReplayMessageViewClickListener? = null
    private var removeAttachmentClickListener: MessageInputClickListeners.RemoveAttachmentClickListener? = null
    private var joinClickListener: MessageInputClickListeners.JoinClickListener? = null

    override fun onSendMsgClick(view: View) {
        defaultListeners.onSendMsgClick(view)
        sendMsgClickListener?.onSendMsgClick(view)
    }

    override fun onSendAttachmentClick(view: View) {
        defaultListeners.onSendAttachmentClick(view)
        sendAttachmentClickListener?.onSendAttachmentClick(view)
    }

    override fun onCancelReplayMessageViewClick(view: View) {
        defaultListeners.onCancelReplayMessageViewClick(view)
        closeReplayMessageViewClickListener?.onCancelReplayMessageViewClick(view)
    }

    override fun onRemoveAttachmentClick(item: AttachmentItem) {
        defaultListeners.onRemoveAttachmentClick(item)
        removeAttachmentClickListener?.onRemoveAttachmentClick(item)
    }

    override fun onJoinClick() {
        defaultListeners.onJoinClick()
        joinClickListener?.onJoinClick()
    }

    fun setListener(listener: MessageInputClickListeners) {
        when (listener) {
            is MessageInputClickListeners.ClickListeners -> {
                sendMsgClickListener = listener
                sendAttachmentClickListener = listener
                closeReplayMessageViewClickListener = listener
                removeAttachmentClickListener = listener
                joinClickListener = listener
            }
            is MessageInputClickListeners.SendMsgClickListener -> {
                sendMsgClickListener = listener
            }
            is MessageInputClickListeners.SendAttachmentClickListener -> {
                sendAttachmentClickListener = listener
            }
            is MessageInputClickListeners.CloseReplayMessageViewClickListener -> {
                closeReplayMessageViewClickListener = listener
            }
            is MessageInputClickListeners.RemoveAttachmentClickListener -> {
                removeAttachmentClickListener = listener
            }
            is MessageInputClickListeners.JoinClickListener -> {
                joinClickListener = listener
            }
        }
    }
}