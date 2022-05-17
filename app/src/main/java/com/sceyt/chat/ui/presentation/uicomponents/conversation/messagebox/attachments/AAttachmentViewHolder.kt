package com.sceyt.chat.ui.presentation.uicomponents.conversation.messagebox.attachments

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.attachment.Attachment

abstract class AAttachmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    open fun bindTo(attachment: Attachment?) {

    }
}