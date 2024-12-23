package com.sceyt.chatuikit.presentation.components.channel.input.listeners.click

import android.view.View
import com.sceyt.chatuikit.presentation.components.channel.input.adapters.attachments.AttachmentItem

sealed interface AttachmentClickListeners {
    fun interface RemoveAttachmentClickListener : AttachmentClickListeners {
        fun onRemoveAttachmentClick(view: View, item: AttachmentItem)
    }

    fun interface AttachmentClickListener : AttachmentClickListeners {
        fun onAttachmentClick(view: View, item: AttachmentItem)
    }

    /** Use this if you want to implement all callbacks */
    interface ClickListeners : RemoveAttachmentClickListener, AttachmentClickListener
}