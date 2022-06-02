package com.sceyt.chat.ui.presentation.uicomponents.messageinput.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.databinding.SceytItemInputAttachmentBinding
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.viewholders.BaseViewHolder

class AttachmentsAdapter(private val attachments: ArrayList<AttachmentItem>,
                         private val viewCallbacks: AttachmentFileViewHolder.Callbacks) :
        RecyclerView.Adapter<BaseViewHolder<AttachmentItem>>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<AttachmentItem> {
        val itemView = SceytItemInputAttachmentBinding.inflate(LayoutInflater.from(parent.context),
            parent, false)
        return AttachmentFileViewHolder(itemView, viewCallbacks)
    }

    override fun onBindViewHolder(holder: BaseViewHolder<AttachmentItem>, position: Int) {
        val attachment = attachments[position]
        holder.bindViews(attachment)
    }

    override fun getItemCount(): Int {
        return attachments.size
    }

    fun addItems(data: List<AttachmentItem>) {
        attachments.addAll(data)
        notifyItemInserted(attachments.size - data.size)
    }

    fun removeItem(attachment: AttachmentItem) {
        val index = attachments.indexOf(attachment)
        if (index >= 0) {
            attachments.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clear() {
        attachments.clear()
        notifyDataSetChanged()
    }
}