package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners

import android.view.View
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.adapters.attachments.AttachmentItem

sealed interface AttachmentClickListeners {
    fun interface RemoveAttachmentClickListener : AttachmentClickListeners {
        fun onRemoveAttachmentClick(view: View, item: AttachmentItem)
    }

    /** Use this if you want to implement all callbacks */
    interface ClickListeners : RemoveAttachmentClickListener
}