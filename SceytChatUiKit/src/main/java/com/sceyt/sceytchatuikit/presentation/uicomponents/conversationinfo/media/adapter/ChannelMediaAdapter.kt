package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.sceytchatuikit.databinding.SceytItemChannelMediaDateBinding
import com.sceyt.sceytchatuikit.extensions.dispatchUpdatesToSafety
import com.sceyt.sceytchatuikit.persistence.extensions.toArrayList
import com.sceyt.sceytchatuikit.presentation.common.SyncArrayList
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.AttachmentsDiffUtil
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.BaseFileViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.ChannelFileItem
import com.sceyt.sceytchatuikit.sceytstyles.ConversationInfoMediaStyle
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil

class ChannelMediaAdapter(
        private var attachments: SyncArrayList<ChannelFileItem>,
        private val attachmentViewHolderFactory: ChannelAttachmentViewHolderFactory,
) : RecyclerView.Adapter<BaseFileViewHolder<ChannelFileItem>>(), MediaStickHeaderItemDecoration.StickyHeaderInterface {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseFileViewHolder<ChannelFileItem> {
        return attachmentViewHolderFactory.createViewHolder(parent, viewType)
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
        return attachmentViewHolderFactory.getItemViewType(attachments[position])
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
        val myDiffUtil = AttachmentsDiffUtil(attachments, data)
        val productDiffResult = DiffUtil.calculateDiff(myDiffUtil, true)
        productDiffResult.dispatchUpdatesToSafety(recyclerView)
        attachments = SyncArrayList(data)
    }

    override fun bindHeaderData(header: SceytItemChannelMediaDateBinding, headerPosition: Int) {
        val date = DateTimeUtil.getDateTimeStringWithDateFormatter(
            context = header.root.context,
            time = attachments.getOrNull(headerPosition)?.getCreatedAt(),
            dateFormatter = ConversationInfoMediaStyle.mediaDateSeparatorFormat)

        header.tvDate.text = date
    }

    override fun isHeader(itemPosition: Int): Boolean {
        return attachments.getOrNull(itemPosition) is ChannelFileItem.MediaDate
    }
}