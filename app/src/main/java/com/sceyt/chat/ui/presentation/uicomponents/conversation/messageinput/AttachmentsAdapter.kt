package com.sceyt.chat.ui.presentation.uicomponents.conversation.messageinput

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.ui.databinding.RecyclerviewAttachmentFileItemBinding
import com.sceyt.chat.ui.presentation.uicomponents.conversation.messageinput.attachments.AAttachmentViewHolder
import com.sceyt.chat.ui.presentation.uicomponents.conversation.messageinput.attachments.AttachmentFileViewHolder

class AttachmentsAdapter(private val viewCallbacks: AttachmentFileViewHolder.Callbacks) :
        ListAdapter<Attachment, AAttachmentViewHolder>(DIFF_CALLBACK) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AAttachmentViewHolder {

        val itemView = RecyclerviewAttachmentFileItemBinding.inflate(LayoutInflater.from(parent.context),
            parent, false)
        return AttachmentFileViewHolder(itemView, viewCallbacks)
    }

    override fun onBindViewHolder(holder: AAttachmentViewHolder, position: Int) {
        val attachment: Attachment? = getItem(position)

        holder.bindTo(attachment)
    }

    companion object {

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Attachment>() {
            override fun areItemsTheSame(oldItem: Attachment, newItem: Attachment) =
                    oldItem.url == newItem.url

            override fun areContentsTheSame(
                    oldItem: Attachment, newItem: Attachment
            ) = areItemsTheSame(oldItem, newItem)
        }
    }
}