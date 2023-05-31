package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.core.util.Predicate
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.sceytchatuikit.data.models.messages.MessageTypeEnum
import com.sceyt.sceytchatuikit.extensions.*
import com.sceyt.sceytchatuikit.presentation.common.SyncArrayList
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem.MessageItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.comporators.MessageItemComparator
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.root.BaseMsgViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.searchinput.DebounceHelper
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MessagesAdapter(private var messages: SyncArrayList<MessageListItem>,
                      private val viewHolderFactory: MessageViewHolderFactory) :
        RecyclerView.Adapter<BaseMsgViewHolder>() {
    private val loadingPrevItem by lazy { MessageListItem.LoadingPrevItem }
    private val loadingNextItem by lazy { MessageListItem.LoadingNextItem }
    private val debounceHelper by lazy { DebounceHelper(300) }

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

    fun getSkip() = messages.filterIsInstance<MessageItem>().size

    fun getFirstMessageItem() = messages.find { it is MessageItem } as? MessageItem

    fun getLastMessageItem() = messages.findLast { it is MessageItem } as? MessageItem

    fun getFirstMessageBy(predicate: (MessageListItem) -> Boolean) = messages.find(predicate)

    fun getLastMessageBy(predicate: (MessageListItem) -> Boolean) = messages.findLast(predicate)

    fun removeLoadingPrev() {
        if (messages.remove(loadingPrevItem))
            notifyItemRemoved(0)
    }

    fun removeLoadingNext() {
        messages.findIndexed { it is MessageListItem.LoadingNextItem }?.let {
            if (messages.remove(loadingNextItem))
                notifyItemRemoved(it.first)
        }
    }

    private fun updateDateAndState(newItem: MessageListItem, prevItem: MessageListItem?, dateItem: MessageListItem?) {
        if (newItem is MessageItem && prevItem is MessageItem) {
            val prevMessage = prevItem.message
            if (prevItem.message.isGroup) {
                val prevIndex = messages.indexOf(prevItem)
                prevMessage.canShowAvatarAndName = prevMessage.incoming && prevMessage.from?.id != newItem.message.from?.id
                notifyItemChanged(prevIndex, Unit)
            }

            val needShowDate = !DateTimeUtil.isSameDay(prevMessage.createdAt, newItem.message.createdAt)
            if (!needShowDate) {
                val dateIndex = messages.indexOf(dateItem)
                if (dateIndex != -1) {
                    messages.removeAt(dateIndex)
                    notifyItemRemoved(dateIndex)
                }
            }
        }
    }

    fun addPrevPageMessagesList(items: List<MessageListItem>) {
        removeLoadingPrev()
        if (items.isEmpty()) return

        val firstItem = getFirstMessageItem()
        val dateItem = messages.find { it is MessageListItem.DateSeparatorItem && it.msgTid == firstItem?.message?.tid }
        messages.addAll(0, items)
        notifyItemRangeInserted(0, items.size)
        updateDateAndState(items.last(), firstItem, dateItem)
    }

    fun addNextPageMessagesList(items: List<MessageListItem>) {
        if (items.isEmpty()) return
        val filteredItems = items.toSet().minus(messages.toSet())
        removeLoadingNext()
        addNewMessages(filteredItems.toList())
    }

    fun addNewMessages(items: List<MessageListItem>) {
        if (items.isEmpty()) return
        val filteredItems = items.toSet().minus(messages.toSet())
        if (filteredItems.isEmpty()) return

        messages.addAll(filteredItems)
        notifyItemRangeInserted(messages.lastIndex, filteredItems.size)
    }

    fun notifyUpdate(messages: List<MessageListItem>, recyclerView: RecyclerView) {
        updateJob?.cancel()
        updateJob = recyclerView.context.asComponentActivity().lifecycleScope.launch(Dispatchers.Default) {
            val myDiffUtil = MessagesDiffUtil(ArrayList(this@MessagesAdapter.messages), messages)
            val productDiffResult = DiffUtil.calculateDiff(myDiffUtil, true)

            withContext(Dispatchers.Main) {
                productDiffResult.dispatchUpdatesToSafety(recyclerView)
                this@MessagesAdapter.messages = SyncArrayList(messages)
            }
        }
    }

    fun getData() = messages

    fun needTopOffset(position: Int): Boolean {
        try {
            val prev = (messages.getOrNull(position - 1) as? MessageItem)?.message
            val current = (messages.getOrNull(position) as? MessageItem)?.message
            if (prev != null && current != null)
                return prev.incoming != current.incoming || current.type == MessageTypeEnum.System.value()
                        || prev.type == MessageTypeEnum.System.value()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return true
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearData() {
        messages.clear()
        notifyDataSetChanged()
    }

    fun deleteMessageByTid(tid: Long) {
        messages.findIndexed { it is MessageItem && it.message.tid == tid }?.let {
            messages.removeAt(it.first)
            notifyItemRemoved(it.first)
        }
        messages.findIndexed { it is MessageListItem.DateSeparatorItem && it.msgTid == tid }?.let {
            messages.removeAt(it.first)
            notifyItemRemoved(it.first)
        }
    }

    fun deleteAllMessagesBefore(predicate: Predicate<MessageListItem>) {
        ArrayList(messages).forEach { item ->
            if (predicate.test(item)) {
                messages.findIndexed { it == item }?.let {
                    messages.removeAt(it.first)
                    notifyItemRemoved(it.first)
                }
            }
        }
    }

    fun sort(recyclerView: RecyclerView) {
        debounceHelper.submit {
            val myDiffUtil = MessagesDiffUtil(ArrayList(this@MessagesAdapter.messages), messages.apply {
                sortWith(MessageItemComparator())
            })
            val productDiffResult = DiffUtil.calculateDiff(myDiffUtil, true)

            val isLastItemVisible = recyclerView.isLastItemDisplaying()
            productDiffResult.dispatchUpdatesToSafety(recyclerView)
            if (isLastItemVisible)
                recyclerView.scrollToPosition(itemCount - 1)
        }
    }

    companion object {
        private var updateJob: Job? = null

        fun awaitUpdating(cb: () -> Unit) {
            if (updateJob == null || updateJob?.isCompleted == true || updateJob?.isCompleted == true)
                cb.invoke()
            else {
                updateJob?.invokeOnCompletion { cb.invoke() }
            }
        }
    }
}