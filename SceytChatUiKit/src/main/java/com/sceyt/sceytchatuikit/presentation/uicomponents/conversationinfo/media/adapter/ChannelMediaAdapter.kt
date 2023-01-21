package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.sceytchatuikit.extensions.dispatchUpdatesToSafety
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.AttachmentsDiffUtil
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.BaseChannelFileViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.ChannelFileItem

class ChannelMediaAdapter(
        private var attachments: ArrayList<ChannelFileItem>,
        private val attachmentViewHolderFactory: ChannelAttachmentViewHolderFactory,
) : RecyclerView.Adapter<BaseChannelFileViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseChannelFileViewHolder {
        return attachmentViewHolderFactory.createViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: BaseChannelFileViewHolder, position: Int) {
        holder.bind(attachments[position])
    }

    override fun onViewAttachedToWindow(holder: BaseChannelFileViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.onViewAttachedToWindow()
    }

    override fun onViewDetachedFromWindow(holder: BaseChannelFileViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.onViewDetachedFromWindow()
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

    fun getFileItems() = attachments.filter { it !is ChannelFileItem.LoadingMoreItem }

    @SuppressLint("NotifyDataSetChanged")
    fun clearData() {
        attachments = arrayListOf()
        notifyDataSetChanged()
    }

    fun notifyUpdate(data: List<ChannelFileItem>, recyclerView: RecyclerView) {
        val myDiffUtil = AttachmentsDiffUtil(attachments, data)
        val productDiffResult = DiffUtil.calculateDiff(myDiffUtil, true)
        productDiffResult.dispatchUpdatesToSafety(recyclerView)
        this.attachments.clear()
        this.attachments.addAll(data)
    }
}