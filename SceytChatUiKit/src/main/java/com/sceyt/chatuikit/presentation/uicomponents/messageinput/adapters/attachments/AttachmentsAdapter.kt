package com.sceyt.chatuikit.presentation.uicomponents.messageinput.adapters.attachments

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.presentation.root.BaseViewHolder

class AttachmentsAdapter(
        data: List<AttachmentItem>,
        private val factory: AttachmentsViewHolderFactory
) : RecyclerView.Adapter<BaseViewHolder<AttachmentItem>>() {

    private val attachments = data.toMutableList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<AttachmentItem> {
        return factory.createViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: BaseViewHolder<AttachmentItem>, position: Int) {
        holder.bind(attachments[position])
    }

    override fun getItemViewType(position: Int): Int {
        return factory.getItemViewType(attachments[position])
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