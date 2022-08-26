package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.BaseFileViewHolder

class ChannelMediaAdapter(private val attachments: ArrayList<FileListItem>,
                          private val attachmentViewHolderFactory: ChannelAttachmentViewHolderFactory)
    : RecyclerView.Adapter<BaseFileViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseFileViewHolder {
        return attachmentViewHolderFactory.createViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: BaseFileViewHolder, position: Int) {
        holder.bind(attachments[position])
    }

    override fun getItemCount(): Int {
        return attachments.size
    }

    override fun getItemViewType(position: Int): Int {
        return attachmentViewHolderFactory.getItemViewType(attachments[position])
    }

    private fun removeLoading() {
        if (attachments.removeIf { it is FileListItem.LoadingMoreItem })
            notifyItemRemoved(attachments.lastIndex + 1)
    }

    fun addNewItems(items: List<FileListItem>) {
        removeLoading()
        if (items.isEmpty()) return

        attachments.addAll(items)
        if (attachments.size == items.size)
            notifyDataSetChanged()
        else
            notifyItemRangeInserted(attachments.size - items.size, items.size)
    }

    fun getLastMediaItem() = attachments.findLast { it != FileListItem.LoadingMoreItem }
}