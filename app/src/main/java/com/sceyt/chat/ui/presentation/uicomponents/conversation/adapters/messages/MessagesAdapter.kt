package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.extensions.addRVScrollListener
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.viewholders.BaseViewHolder
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.viewholders.MessageViewHolderFactory
import com.sceyt.chat.ui.utils.DateTimeUtil
import com.sceyt.chat.ui.utils.MyDiffUtil

class MessagesAdapter(private val messages: ArrayList<MessageListItem>,
                      private val viewHolderFactory: MessageViewHolderFactory,
                      private val recyclerView: RecyclerView) :
        RecyclerView.Adapter<BaseViewHolder<MessageListItem>>() {

    private val mLoadingItem by lazy { MessageListItem.LoadingMoreItem }
    private var state = RecyclerView.SCROLL_STATE_IDLE

    init {
        recyclerView.addRVScrollListener(onScrollStateChanged = { _, newState ->
            state = newState
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<MessageListItem> {
        return viewHolderFactory.createViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: BaseViewHolder<MessageListItem>, position: Int) {
        holder.bindViews(item = messages[position])
    }

    override fun getItemViewType(position: Int): Int {
        return viewHolderFactory.getItemViewType(messages[position])
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    override fun onViewAttachedToWindow(holder: BaseViewHolder<MessageListItem>) {
        super.onViewAttachedToWindow(holder)
        holder.onViewAttachedFromWindow()
    }

    override fun onViewDetachedFromWindow(holder: BaseViewHolder<MessageListItem>) {
        super.onViewDetachedFromWindow(holder)
        holder.onViewDetachedFromWindow()
    }

    fun getSkip() = messages.filter { it !is MessageListItem.LoadingMoreItem }.size

    fun getFirstItem() = messages.find { it is MessageListItem.MessageItem } as? MessageListItem.MessageItem


    fun getLastItem() = messages.findLast { it is MessageListItem.MessageItem } as? MessageListItem.MessageItem

    private fun removeLoading() {
        if (messages.remove(mLoadingItem))
            notifyItemRemoved(0)
    }

    private fun updateDateAndState(newItem: MessageListItem, prevItem: MessageListItem?) {
        if (newItem is MessageListItem.MessageItem) {
            (prevItem as? MessageListItem.MessageItem)?.message?.apply {
                showDate = !DateTimeUtil.isSameDay(createdAt, newItem.message.createdAt)
                canShowAvatarAndName = from?.id != newItem.message.from?.id && isGroup
            }
        }
    }

    fun notifyUpdate(messages: List<MessageListItem>) {
        val myDiffUtil = MyDiffUtil(this.messages, messages)
        val productDiffResult = DiffUtil.calculateDiff(myDiffUtil, true)
        this.messages.clear()
        this.messages.addAll(messages)
        productDiffResult.dispatchUpdatesTo(this)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addNextPageMessagesList(items: List<MessageListItem>) {
        removeLoading()
        if (items.isEmpty()) return

        val firstItem = getFirstItem()
        messages.addAll(0, items)
        if (messages.size == items.size)
            notifyDataSetChanged()
        else {
            notifyItemRangeInserted(0, items.size)
            updateDateAndState(items.last(), firstItem)
        }
    }

    fun addNewMessages(items: List<MessageListItem>) {
        if (items.isEmpty()) return
        // todo from typing item id needed

        /*  if (messages.isNotEmpty() && messages.last().type == Typing) {
              messages.addAll(messages.lastIndex, items.toList())
              notifyItemRangeInserted(messages.lastIndex - 1, items.size)
          } else {*/
        messages.addAll(items)
        notifyItemRangeInserted(messages.lastIndex, items.size)
        //}
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