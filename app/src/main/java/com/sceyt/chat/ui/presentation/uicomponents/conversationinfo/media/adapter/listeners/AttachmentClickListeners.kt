package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.media.adapter.listeners

import android.view.View
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem

sealed interface AttachmentClickListeners {

    fun interface AttachmentClickListener : AttachmentClickListeners {
        fun onAttachmentClick(view: View, item: FileListItem)
    }

    /** User this if you want to implement all callbacks */
    interface ClickListeners : AttachmentClickListener
}