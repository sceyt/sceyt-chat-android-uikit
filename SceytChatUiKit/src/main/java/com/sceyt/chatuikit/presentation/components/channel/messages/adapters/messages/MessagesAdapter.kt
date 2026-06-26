package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytMessageType
import com.sceyt.chatuikit.extensions.dispatchUpdatesToSafetySuspend
import com.sceyt.chatuikit.extensions.findIndexed
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.presentation.common.collections.SyncArrayList
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem.MessageItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root.BaseMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.sticky_date.StickyDateHeaderView
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.sticky_date.StickyHeaderInterface
import com.sceyt.chatuikit.shared.utils.DateTimeUtil
import com.sceyt.chatuikit.styles.messages_list.MessagesListViewStyle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class MessagesAdapter(
    private var messages: SyncArrayList<MessageListItem>,
    private val viewHolderFactory: MessageViewHolderFactory,
    private val style: MessagesListViewStyle,
    private val scope: LifecycleCoroutineScope,
    private val recyclerView: RecyclerView,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : RecyclerView.Adapter<BaseMessageViewHolder>(), StickyHeaderInterface {
    private val loadingPrevItem by lazy { MessageListItem.LoadingPrevItem }
    private val loadingNextItem by lazy { MessageListItem.LoadingNextItem }
    private var updateJob: Job? = null
    private var isMultiSelectableMode = false
    private var lastHeaderPosition = -1

    // Called after the backing list is committed/rebuilt.
    // Useful for retrying operations that require the latest list state.
    var onListCommittedListener: (() -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseMessageViewHolder {
        return viewHolderFactory.createViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: BaseMessageViewHolder, position: Int) {
        holder.bind(item = messages[position], diff = MessageDiff.DEFAULT)
    }

    override fun onBindViewHolder(
        holder: BaseMessageViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        val diff = payloads.find { it is MessageDiff } as? MessageDiff
        holder.bind(item = messages[position], diff ?: MessageDiff.DEFAULT)
    }

    override fun getItemViewType(position: Int): Int {
        return viewHolderFactory.getItemViewType(messages[position])
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    override fun getItemId(position: Int): Long {
        return messages[position].getItemId()
    }

    override fun onViewAttachedToWindow(holder: BaseMessageViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.onViewAttachedToWindow()
    }

    override fun onViewDetachedFromWindow(holder: BaseMessageViewHolder) {
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

    fun addNextPageMessagesList(items: List<MessageListItem>) {
        removeLoadingNext()
        if (items.isEmpty()) return

        val insertStart = messages.size
        messages.addAll(items)
        notifyItemRangeInserted(insertStart, items.size)
        onListCommittedListener?.invoke()
    }

    fun addPreparedNewMessages(items: List<MessageListItem>) {
        removeLoadingNext()
        if (items.isEmpty()) return

        val insertStart = messages.size
        messages.addAll(items)
        notifyItemRangeInserted(insertStart, items.size)
        onListCommittedListener?.invoke()
    }

    fun updateItemAt(index: Int, updatedItem: MessageItem) {
        try {
            messages[index] = updatedItem
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun notifyUpdate(messages: List<MessageListItem>) {
        updateJob?.cancel()
        updateJob = scope.launch {
            var productDiffResult: DiffUtil.DiffResult
            withContext(backgroundDispatcher) {
                val myDiffUtil =
                    MessagesDiffUtil(ArrayList(this@MessagesAdapter.messages), messages)
                productDiffResult = DiffUtil.calculateDiff(myDiffUtil, true)
            }
            withContext(mainDispatcher) {
                this@MessagesAdapter.messages = SyncArrayList(messages)
                productDiffResult.dispatchUpdatesToSafetySuspend(recyclerView)
                onListCommittedListener?.invoke()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun forceUpdate(data: List<MessageListItem>) {
        updateJob?.cancel()
        messages = SyncArrayList(data)
        notifyDataSetChanged()
        onListCommittedListener?.invoke()
    }

    fun getData() = messages.toList()

    fun needTopOffset(position: Int): Boolean {
        try {
            if (position == 0) return true
            val prev = (messages.getOrNull(position - 1) as? MessageItem)?.message
            val current = (messages.getOrNull(position) as? MessageItem)?.message
            if (prev != null && current != null)
                return prev.incoming != current.incoming || current.type == SceytMessageType.System.value
                        || prev.type == SceytMessageType.System.value
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

    fun deleteMessageByTIds(tid: List<Long>) {
        tid.forEach { messageTid ->
            deleteMessageByTid(messageTid)
        }
    }

    fun deleteMessageByTid(tid: Long) {
        messages.findIndexed { it is MessageItem && it.message.tid == tid }?.let {
            messages.removeAt(it.first)
            notifyItemRemoved(it.first)
        }
        messages.findIndexed { item ->
            item is MessageListItem.DateSeparatorItem && item.messageTid == tid
        }?.let {
            messages.removeAt(it.first)
            notifyItemRemoved(it.first)
        }
    }

    fun removeUnreadMessagesSeparator() {
        messages.findIndexed { item ->
            item is MessageListItem.UnreadMessagesSeparatorItem
        }?.let { (index, _) ->
            messages.removeAt(index)
            notifyItemRemoved(index)
            // Hide avatar and name after removing unread separator, if the previous message is from the same user
            messages.getOrNull(index)?.let { item ->
                if (item is MessageItem && item.message.shouldShowAvatarAndName) {
                    messages.getOrNull(index - 1)?.let { prevItem ->
                        if (prevItem is MessageItem && prevItem.message.user?.id == item.message.user?.id
                            && !shouldShowDate(item.message, prevItem.message)
                        ) {
                            messages[index] = item.copy(
                                message = item.message.copy(shouldShowAvatarAndName = false)
                            )
                            notifyItemChanged(index, Unit)
                        }
                    }
                }
            }
        }
    }

    private fun shouldShowDate(sceytMessage: SceytMessage, prevMessage: SceytMessage): Boolean {
        return !DateTimeUtil.isSameDay(sceytMessage.createdAt, prevMessage.createdAt)
    }

    fun setMultiSelectableMode(enable: Boolean) {
        isMultiSelectableMode = enable
    }

    fun isMultiSelectableMode() = isMultiSelectableMode

    fun awaitUpdating(cb: () -> Unit) {
        val job = updateJob
        if (job == null || job.isCompleted)
            cb()
        else
            job.invokeOnCompletion { cb() }
    }

    override fun bindHeaderData(header: StickyDateHeaderView, headerPosition: Int) {
        if (lastHeaderPosition == headerPosition) return
        val dateAt =
            messages.getOrNull(headerPosition)?.getMessageCreatedAtForDateHeader() ?: return
        header.setDate(
            date = style.dateSeparatorStyle.dateFormatter.format(
                context = header.context,
                from = Date(dateAt)
            )
        )
        lastHeaderPosition = headerPosition
    }

    override fun isHeader(itemPosition: Int): Boolean {
        return messages.getOrNull(itemPosition) is MessageListItem.DateSeparatorItem
    }
}
