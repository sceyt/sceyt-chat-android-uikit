package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners

import android.view.View
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.adapters.attachments.AttachmentItem

sealed interface MessageInputClickListeners {

    fun interface SendMsgClickListener : MessageInputClickListeners {
        fun onSendMsgClick(view: View)
    }

    fun interface SendAttachmentClickListener : MessageInputClickListeners {
        fun onSendAttachmentClick(view: View)
    }

    fun interface VoiceClickListener : MessageInputClickListeners {
        fun onVoiceClick(view: View)
    }

    fun interface VoiceLongClickListener : MessageInputClickListeners {
        fun onVoiceLongClick(view: View)
    }

    fun interface CloseReplyMessageViewClickListener : MessageInputClickListeners {
        fun onCancelReplyMessageViewClick(view: View)
    }

    fun interface RemoveAttachmentClickListener : MessageInputClickListeners {
        fun onRemoveAttachmentClick(item: AttachmentItem)
    }

    fun interface JoinClickListener : MessageInputClickListeners {
        fun onJoinClick()
    }

    /** Use this if you want to implement all callbacks */
    interface ClickListeners :
            SendMsgClickListener,
            SendAttachmentClickListener,
            CloseReplyMessageViewClickListener,
            RemoveAttachmentClickListener,
            JoinClickListener,
            VoiceClickListener,
            VoiceLongClickListener
}