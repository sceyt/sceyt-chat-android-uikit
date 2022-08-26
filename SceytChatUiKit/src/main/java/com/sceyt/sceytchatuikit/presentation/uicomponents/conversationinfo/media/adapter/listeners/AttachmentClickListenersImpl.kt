package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.listeners

import android.view.View
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem

class AttachmentClickListenersImpl : AttachmentClickListeners.ClickListeners {
    private var attachmentClickListener: AttachmentClickListeners.AttachmentClickListener? = null

    override fun onAttachmentClick(view: View, item: FileListItem) {
        attachmentClickListener?.onAttachmentClick(view, item)
    }

    fun setListener(listener: AttachmentClickListeners) {
        attachmentClickListener = when (listener) {
            is AttachmentClickListeners.ClickListeners -> {
                listener
            }
            is AttachmentClickListeners.AttachmentClickListener -> {
                listener
            }
        }
    }
}