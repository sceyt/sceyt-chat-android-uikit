package com.sceyt.chatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners

import android.view.View
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.adapters.attachments.AttachmentItem

sealed interface MessageInputClickListeners {

    fun interface SendMsgClickListener : MessageInputClickListeners {
        fun onSendMsgClick(view: View)
    }

    fun interface SendAttachmentClickListener : MessageInputClickListeners {
        fun onAddAttachmentClick(view: View)
    }

    fun interface VoiceClickListener : MessageInputClickListeners {
        fun onVoiceClick(view: View)
    }

    fun interface VoiceLongClickListener : MessageInputClickListeners {
        fun onVoiceLongClick(view: View)
    }

    fun interface CancelReplyMessageViewClickListener : MessageInputClickListeners {
        fun onCancelReplyMessageViewClick(view: View)
    }

    fun interface CancelLinkPreviewClickListener : MessageInputClickListeners {
        fun onCancelLinkPreviewClick(view: View)
    }

    fun interface RemoveAttachmentClickListener : MessageInputClickListeners {
        fun onRemoveAttachmentClick(item: AttachmentItem)
    }

    fun interface JoinClickListener : MessageInputClickListeners {
        fun onJoinClick()
    }

    fun interface ClearChatClickListener : MessageInputClickListeners {
        fun onClearChatClick()
    }

    fun interface ScrollToNextMessageClickListener : MessageInputClickListeners {
        fun onScrollToNextMessageClick()
    }

    fun interface ScrollToPreviousMessageClickListener : MessageInputClickListeners {
        fun onScrollToPreviousMessageClick()
    }

    /** Use this if you want to implement all callbacks */
    interface ClickListeners :
            SendMsgClickListener,
            SendAttachmentClickListener,
            CancelReplyMessageViewClickListener,
            CancelLinkPreviewClickListener,
            RemoveAttachmentClickListener,
            JoinClickListener,
            VoiceClickListener,
            VoiceLongClickListener,
            ClearChatClickListener,
            ScrollToNextMessageClickListener,
            ScrollToPreviousMessageClickListener
}