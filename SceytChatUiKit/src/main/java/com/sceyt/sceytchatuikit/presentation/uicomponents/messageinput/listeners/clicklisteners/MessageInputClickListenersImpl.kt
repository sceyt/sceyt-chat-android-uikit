package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners

import android.view.View
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.MessageInputView
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.adapters.attachments.AttachmentItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.MessageInputClickListeners.ClickListeners
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.MessageInputClickListeners.CloseReplyMessageViewClickListener
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.MessageInputClickListeners.JoinClickListener
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.MessageInputClickListeners.RemoveAttachmentClickListener
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.MessageInputClickListeners.SendAttachmentClickListener
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.MessageInputClickListeners.SendMsgClickListener
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.MessageInputClickListeners.VoiceClickListener
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.MessageInputClickListeners.VoiceLongClickListener

open class MessageInputClickListenersImpl(view: MessageInputView) : ClickListeners {
    private var defaultListeners: ClickListeners = view
    private var sendMsgClickListener: SendMsgClickListener? = null
    private var addAttachmentClickListener: SendAttachmentClickListener? = null
    private var voiceClickListener: VoiceClickListener? = null
    private var voiceLongClickListener: VoiceLongClickListener? = null
    private var closeReplyMessageViewClickListener: CloseReplyMessageViewClickListener? = null
    private var removeAttachmentClickListener: RemoveAttachmentClickListener? = null
    private var joinClickListener: JoinClickListener? = null

    override fun onSendMsgClick(view: View) {
        defaultListeners.onSendMsgClick(view)
        sendMsgClickListener?.onSendMsgClick(view)
    }

    override fun onAddAttachmentClick(view: View) {
        defaultListeners.onAddAttachmentClick(view)
        addAttachmentClickListener?.onAddAttachmentClick(view)
    }

    override fun onVoiceClick(view: View) {
        defaultListeners.onVoiceClick(view)
        voiceClickListener?.onVoiceClick(view)
    }

    override fun onVoiceLongClick(view: View) {
        defaultListeners.onVoiceLongClick(view)
        voiceLongClickListener?.onVoiceLongClick(view)
    }

    override fun onCancelReplyMessageViewClick(view: View) {
        defaultListeners.onCancelReplyMessageViewClick(view)
        closeReplyMessageViewClickListener?.onCancelReplyMessageViewClick(view)
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
            is ClickListeners -> {
                sendMsgClickListener = listener
                addAttachmentClickListener = listener
                voiceClickListener = listener
                voiceLongClickListener = listener
                closeReplyMessageViewClickListener = listener
                removeAttachmentClickListener = listener
                joinClickListener = listener
            }
            is SendMsgClickListener -> {
                sendMsgClickListener = listener
            }
            is SendAttachmentClickListener -> {
                addAttachmentClickListener = listener
            }
            is VoiceClickListener -> {
                voiceClickListener = listener
            }
            is VoiceLongClickListener -> {
                voiceLongClickListener = listener
            }
            is CloseReplyMessageViewClickListener -> {
                closeReplyMessageViewClickListener = listener
            }
            is RemoveAttachmentClickListener -> {
                removeAttachmentClickListener = listener
            }
            is JoinClickListener -> {
                joinClickListener = listener
            }
        }
    }
}