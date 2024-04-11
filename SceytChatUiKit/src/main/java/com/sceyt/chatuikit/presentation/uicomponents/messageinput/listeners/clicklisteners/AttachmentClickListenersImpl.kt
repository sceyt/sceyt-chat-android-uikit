package com.sceyt.chatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners

import android.view.View
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.adapters.attachments.AttachmentItem

open class AttachmentClickListenersImpl : AttachmentClickListeners.ClickListeners {
    private var removeClickListener: AttachmentClickListeners.RemoveAttachmentClickListener? = null

    override fun onRemoveAttachmentClick(view: View, item: AttachmentItem) {
        removeClickListener?.onRemoveAttachmentClick(view, item)
    }

    fun setListener(listener: AttachmentClickListeners) {
        when (listener) {
            is AttachmentClickListeners.ClickListeners -> {
                removeClickListener = listener
            }
            is AttachmentClickListeners.RemoveAttachmentClickListener -> {
                removeClickListener = listener
            }
        }
    }
}