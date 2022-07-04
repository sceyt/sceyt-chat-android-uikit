package com.sceyt.chat.ui.presentation.uicomponents.messageinput.listeners

import android.view.View
import com.sceyt.chat.ui.presentation.uicomponents.messageinput.adapter.AttachmentItem

sealed interface MessageInputClickListeners {

    fun interface SendMsgClickListener : MessageInputClickListeners {
        fun onSendMsgClick(view: View)
    }

    fun interface SendAttachmentClickListener : MessageInputClickListeners {
        fun onSendAttachmentClick(view: View)
    }

    fun interface CloseReplayMessageViewClickListener : MessageInputClickListeners {
        fun onCancelReplayMessageViewClick(view: View)
    }

    fun interface RemoveAttachmentClickListener : MessageInputClickListeners {
        fun onRemoveAttachmentClick(item: AttachmentItem)
    }

    fun interface JoinClickListener : MessageInputClickListeners {
        fun onJoinClick()
    }

    /** User this if you want to implement all callbacks */
    interface ClickListeners :
            SendMsgClickListener,
            SendAttachmentClickListener,
            CloseReplayMessageViewClickListener,
            RemoveAttachmentClickListener,
            JoinClickListener
}