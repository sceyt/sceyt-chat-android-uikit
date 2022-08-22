package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.extensions.SyncArrayList
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.viewholders.BaseMsgViewHolder
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.viewholders.MessageViewHolderFactory
import com.sceyt.chat.ui.shared.utils.DateTimeUtil

class MessagesAdapter(private var messages: SyncArrayList<MessageListItem>,
                      private val viewHolderFactory: MessageViewHolderFactory) :
        RecyclerView.Adapter<BaseMsgViewHolder>() {
    private val mLoadingItem by lazy { MessageListItem.LoadingMoreItem }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseMsgViewHolder {
        return viewHolderFactory.createViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: BaseMsgViewHolder, position: Int) {
        holder.bind(item = messages[position], diff = MessageItemPayloadDiff.DEFAULT)
    }

    override fun onBindViewHolder(holder: BaseMsgViewHolder, position: Int, payloads: MutableList<Any>) {
        val diff = payloads.find { it is MessageItemPayloadDiff } as? MessageItemPayloadDiff
                ?: MessageItemPayloadDiff.DEFAULT
        holder.bind(item = messages[position], diff)
    }

    override fun getItemViewType(position: Int): Int {
        return viewHolderFactory.getItemViewType(messages[position])
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    override fun onViewAttachedToWindow(holder: BaseMsgViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.onViewAttachedToWindow()
    }

    override fun onViewDetachedFromWindow(holder: BaseMsgViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.onViewDetachedFromWindow()
    }

    fun getSkip() = messages.filterIsInstance<MessageListItem.MessageItem>().size

    fun getFirstMessageItem() = messages.find { it is MessageListItem.MessageItem } as? MessageListItem.MessageItem

    fun getLastMessageItem() = messages.findLast { it is MessageListItem.MessageItem } as? MessageListItem.MessageItem

    fun removeLoading() {
        if (messages.remove(mLoadingItem))
            notifyItemRemoved(0)
    }

    private fun updateDateAndState(newItem: MessageListItem, prevItem: MessageListItem?, dateItem: MessageListItem?) {
        if (newItem is MessageListItem.MessageItem && prevItem is MessageListItem.MessageItem) {
            val prevMessage = prevItem.message
            if (prevItem.message.isGroup) {
                val prevIndex = messages.indexOf(prevItem)
                prevMessage.canShowAvatarAndName = prevMessage.from?.id != newItem.message.from?.id
                notifyItemChanged(prevIndex, Unit)
            }

            val needShowDate = !DateTimeUtil.isSameDay(prevMessage.createdAt, newItem.message.createdAt)
            if (!needShowDate) {
                val dateIndex = messages.indexOf(dateItem)
                messages.removeAt(dateIndex)
                notifyItemRemoved(dateIndex)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addNextPageMessagesList(items: List<MessageListItem>) {
        removeLoading()
        if (items.isEmpty()) return

        val firstItem = getFirstMessageItem()
        val dateItem = messages.find { it is MessageListItem.DateSeparatorItem && it.msgId == firstItem?.message?.id }
        messages.addAll(0, items)
        notifyItemRangeInserted(0, items.size)
        updateDateAndState(items.last(), firstItem, dateItem)
    }

    fun addNewMessages(items: List<MessageListItem>) {
        if (items.isEmpty()) return
        messages.addAll(items)
        notifyItemRangeInserted(messages.lastIndex, items.size)
    }

    fun notifyUpdate(messages: List<MessageListItem>) {
        val myDiffUtil = MessagesDiffUtil(this.messages, messages)
        val productDiffResult = DiffUtil.calculateDiff(myDiffUtil, true)
        this.messages.clear()
        this.messages.addAll(messages as ArrayList)
        productDiffResult.dispatchUpdatesTo(this)
    }

    fun getData() = messages

    fun needTopOffset(position: Int): Boolean {
        try {
            val prevItem = (messages.getOrNull(position - 1) as? MessageListItem.MessageItem)
            val currentItem = (messages.getOrNull(position) as? MessageListItem.MessageItem)
            if (prevItem != null && currentItem != null)
                return prevItem.message.incoming != currentItem.message.incoming
        } catch (ex: Exception) {
        }
        return false
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearData() {
        messages.clear()
        notifyDataSetChanged()
    }
}