package com.sceyt.chatuikit.presentation.components.channel_info.media.adapter

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemChannelMediaDateBinding
import com.sceyt.chatuikit.extensions.dispatchUpdatesToSafety
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.setTextColorRes
import com.sceyt.chatuikit.persistence.extensions.toArrayList
import com.sceyt.chatuikit.presentation.common.SyncArrayList
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.holders.BaseFileViewHolder
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelFileItem
import com.sceyt.chatuikit.shared.utils.DateTimeUtil

class ChannelMediaAdapter(
        private var attachments: SyncArrayList<ChannelFileItem>,
        private val factory: ChannelAttachmentViewHolderFactory,
) : RecyclerView.Adapter<BaseFileViewHolder<ChannelFileItem>>(), MediaStickHeaderItemDecoration.StickyHeaderInterface {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseFileViewHolder<ChannelFileItem> {
        return factory.createViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: BaseFileViewHolder<ChannelFileItem>, position: Int) {
        holder.bind(attachments[position])
    }

    override fun onViewAttachedToWindow(holder: BaseFileViewHolder<ChannelFileItem>) {
        super.onViewAttachedToWindow(holder)
        holder.onViewAttachedToWindow()
    }

    override fun onViewDetachedFromWindow(holder: BaseFileViewHolder<ChannelFileItem>) {
        super.onViewDetachedFromWindow(holder)
        holder.onViewDetachedFromWindow()
    }

    override fun getItemCount(): Int {
        return attachments.size
    }

    override fun getItemViewType(position: Int): Int {
        return factory.getItemViewType(attachments[position])
    }

    private fun removeLoading() {
        attachments.indexOf(ChannelFileItem.LoadingMoreItem).takeIf { it != -1 }?.let {
            attachments.removeAt(it)
            notifyItemRemoved(it)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addNewItems(items: List<ChannelFileItem>) {
        removeLoading()
        if (items.isEmpty()) return

        val updatedItems = checkMaybeShouldRemoveDateItem(items)

        attachments.addAll(updatedItems)
        if (attachments.size == updatedItems.size)
            notifyDataSetChanged()
        else
            notifyItemRangeInserted(attachments.size - updatedItems.size, updatedItems.size)
    }

    fun updateItemAt(index: Int, item: ChannelFileItem) {
        try {
            attachments[index] = item
            notifyItemChanged(index, Unit)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkMaybeShouldRemoveDateItem(itemsToAdd: List<ChannelFileItem>): List<ChannelFileItem> {
        return attachments.findLast { it is ChannelFileItem.MediaDate }?.let { date1 ->
            itemsToAdd.find { item -> item is ChannelFileItem.MediaDate }?.let { date2 ->
                if (DateTimeUtil.isSameDay(date1.getCreatedAt(), date2.getCreatedAt())) {
                    val newItems = itemsToAdd.toArrayList()
                    newItems.remove(date2)
                    newItems
                } else itemsToAdd
            }
        } ?: itemsToAdd
    }

    fun getLastMediaItem() = attachments.findLast { it.isMediaItem() }

    fun getFileItems() = attachments.filter { it.isMediaItem() }

    fun getData() = attachments

    @SuppressLint("NotifyDataSetChanged")
    fun clearData() {
        attachments = SyncArrayList()
        notifyDataSetChanged()
    }

    fun notifyUpdate(data: List<ChannelFileItem>, recyclerView: RecyclerView) {
        val myDiffUtil = com.sceyt.chatuikit.presentation.components.channel.messages.adapters.AttachmentsDiffUtil(attachments, data)
        val productDiffResult = DiffUtil.calculateDiff(myDiffUtil, true)
        productDiffResult.dispatchUpdatesToSafety(recyclerView)
        attachments = SyncArrayList(data)
    }

    override fun bindHeaderData(header: SceytItemChannelMediaDateBinding, headerPosition: Int) {
        val date = DateTimeUtil.getDateTimeStringWithDateFormatter(
            context = header.root.context,
            time = attachments.getOrNull(headerPosition)?.getCreatedAt(),
            dateFormatter = factory.getMediaStyle().mediaDateSeparatorFormat)

        header.tvDate.text = date
        header.tvDate.setTextColorRes(SceytChatUIKit.theme.textSecondaryColor)
        header.root.setBackgroundColor(header.root.context.getCompatColor(SceytChatUIKit.theme.backgroundColor))
    }

    override fun isHeader(itemPosition: Int): Boolean {
        return attachments.getOrNull(itemPosition) is ChannelFileItem.MediaDate
    }
}