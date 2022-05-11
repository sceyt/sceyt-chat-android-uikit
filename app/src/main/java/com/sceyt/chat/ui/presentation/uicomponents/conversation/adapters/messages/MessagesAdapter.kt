package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.viewholders.BaseViewHolder
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.viewholders.MessageViewHolderFactory
import com.sceyt.chat.ui.utils.DateTimeUtil
import com.sceyt.chat.ui.utils.MyDiffUtil

class MessagesAdapter(private val messages: ArrayList<MessageListItem>,
                      private val viewHolderFactory: MessageViewHolderFactory) :
        RecyclerView.Adapter<BaseViewHolder<MessageListItem>>() {

    private val mLoadingItem by lazy { MessageListItem.LoadingMoreItem }

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

    fun getSkip() = messages.filter { it !is MessageListItem.LoadingMoreItem }.size

    fun getFirstItem(): MessageListItem.MessageItem = messages.find {
        it !is MessageListItem.LoadingMoreItem
    } as MessageListItem.MessageItem

    private fun removeLoading() {
        if (messages.remove(mLoadingItem))
            notifyItemRemoved(0)
    }

    private fun updateDate(newItem: MessageListItem, firstItem: MessageListItem) {
        if (newItem is MessageListItem.MessageItem) {
            (firstItem as MessageListItem.MessageItem).message.apply {
                showDate = !DateTimeUtil.isSameDay(createdAt, newItem.message.createdAt)
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
    fun addList(items: List<MessageListItem>) {
        removeLoading()
        val firstItem = getFirstItem()
        messages.addAll(0, items)
        if (messages.size == items.size)
            notifyDataSetChanged()
        else {
            notifyItemRangeInserted(0, items.size)
            updateDate(items.last(), firstItem)
        }
    }

    fun needTopOffset(position: Int): Boolean {
        try {
            val prevItem = (messages.getOrNull(position - 1) as? MessageListItem.MessageItem)
            val currentItem = (messages.getOrNull(position) as? MessageListItem.MessageItem)
            if (prevItem != null && currentItem != null)
                return prevItem.message.incoming != currentItem.message.incoming
        } catch (ex: Exception) {
        }
        return true
    }

    /*  fun needToShowName(position: Int): Boolean {
          try {
              val prevItem = getItem(position - 1)
              val currentItem = getItem(position)
              if (prevItem != null && currentItem != null) {
                  return prevItem.incoming != currentItem.incoming ||
                          prevItem.from.id != currentItem.from.id
              }
          } catch (ex: Exception) {
          }
          return true
      }*/
}