package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.listeners

import android.view.View
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.ChannelFileItem

sealed interface AttachmentClickListeners {

    fun interface AttachmentClickListener : AttachmentClickListeners {
        fun onAttachmentClick(view: View, item: ChannelFileItem)
    }

    /** Use this if you want to implement all callbacks */
    interface ClickListeners : AttachmentClickListener
}