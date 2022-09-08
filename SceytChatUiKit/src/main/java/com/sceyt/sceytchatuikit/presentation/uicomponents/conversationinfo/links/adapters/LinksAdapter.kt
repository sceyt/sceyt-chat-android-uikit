package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.links.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.sceytchatuikit.presentation.root.BaseViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.links.adapters.viewholders.ChannelLinkViewHolderFactory

class LinksAdapter(private val attachments: ArrayList<LinkItem>,
                   private val viewHolderFactory: ChannelLinkViewHolderFactory)
    : RecyclerView.Adapter<BaseViewHolder<LinkItem>>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<LinkItem> {
        return viewHolderFactory.createViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: BaseViewHolder<LinkItem>, position: Int) {
        holder.bind(attachments[position])
    }

    override fun getItemCount(): Int {
        return attachments.size
    }

    override fun getItemViewType(position: Int): Int {
        return viewHolderFactory.getItemViewType(attachments[position])
    }

    private fun removeLoading() {
        if (attachments.removeIf { it is LinkItem.LoadingMore })
            notifyItemRemoved(attachments.lastIndex + 1)
    }

    fun addNewItems(items: List<LinkItem>) {
        removeLoading()
        if (items.isEmpty()) return

        attachments.addAll(items)
        if (attachments.size == items.size)
            notifyDataSetChanged()
        else
            notifyItemRangeInserted(attachments.size - items.size, items.size)
    }

    fun getLastMediaItem() = attachments.findLast { it is LinkItem.Link }
            as? LinkItem.Link
}