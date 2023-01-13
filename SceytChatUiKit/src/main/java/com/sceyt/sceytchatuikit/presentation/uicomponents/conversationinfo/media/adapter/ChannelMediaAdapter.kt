package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.BaseChannelFileViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.BaseFileViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.ChannelFileItem

class ChannelMediaAdapter(private val attachments: ArrayList<ChannelFileItem>,
                          private val attachmentViewHolderFactory: ChannelAttachmentViewHolderFactory,)
    : RecyclerView.Adapter<BaseChannelFileViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseChannelFileViewHolder {
        return attachmentViewHolderFactory.createViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: BaseChannelFileViewHolder, position: Int) {
        holder.bind(attachments[position])
    }

    override fun getItemCount(): Int {
        return attachments.size
    }

    override fun getItemViewType(position: Int): Int {
        return attachmentViewHolderFactory.getItemViewType(attachments[position])
    }

    private fun removeLoading() {
        if (attachments.remove(ChannelFileItem.LoadingMoreItem))
            notifyItemRemoved(attachments.lastIndex + 1)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addNewItems(items: List<ChannelFileItem>) {
        removeLoading()
        if (items.isEmpty()) return

        attachments.addAll(items)
        if (attachments.size == items.size)
            notifyDataSetChanged()
        else
            notifyItemRangeInserted(attachments.size - items.size, items.size)
    }

    fun getLastMediaItem() = attachments.findLast { it !is ChannelFileItem.LoadingMoreItem }
}