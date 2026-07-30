package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.core.util.Predicate
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytMessageType
import com.sceyt.chatuikit.extensions.dispatchUpdatesToSafety
import com.sceyt.chatuikit.extensions.dispatchUpdatesToSafetySuspend
import com.sceyt.chatuikit.extensions.findIndexed
import com.sceyt.chatuikit.extensions.isLastItemDisplaying
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.persistence.differs.diff
import com.sceyt.chatuikit.presentation.extensions.getUpdateMessage
import com.sceyt.chatuikit.presentation.helpers.DebounceHelper
import com.sceyt.chatuikit.presentation.common.collections.SyncArrayList
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem.MessageItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.comporators.MessageItemComparator
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
    private val debounceHelper by lazy { DebounceHelper(300) }
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

    private fun updateDateAndState(
        newItem: MessageListItem,
        prevItem: MessageListItem?,
        dateItem: MessageListItem?
    ) {
        if (newItem is MessageItem && prevItem is MessageItem) {
            val prevMessage = prevItem.message
            if (prevItem.message.isGroup) {
                val prevIndex = messages.indexOf(prevItem)
                messages[prevIndex] = prevItem.copy(
                    message = prevMessage.copy(
                        shouldShowAvatarAndName = prevMessage.incoming
                                && prevMessage.user?.id != newItem.message.user?.id
                    )
                )
                notifyItemChanged(prevIndex, Unit)
            }

            val needShowDate = !DateTimeUtil.isSameDay(
                epochOne = prevMessage.createdAt,
                epochTwo = newItem.message.createdAt
            )
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
        val itemsToAdd = mergeAndFilterAlreadyExistingItems(items)
        if (itemsToAdd.isEmpty()) return

        val firstItem = getFirstMessageItem()
        val dateItem = messages.find { item ->
            item is MessageListItem.DateSeparatorItem && item.messageTid == firstItem?.message?.tid
        }
        messages.addAll(0, itemsToAdd)
        notifyItemRangeInserted(0, itemsToAdd.size)
        updateDateAndState(itemsToAdd.last(), firstItem, dateItem)
        onListCommittedListener?.invoke()
    }

    fun addNextPageMessagesList(items: List<MessageListItem>) {
        removeLoadingNext()
        if (items.isEmpty()) return
        val itemsToAdd = mergeAndFilterAlreadyExistingItems(items)
        if (itemsToAdd.isEmpty()) return

        val insertPosition = messages.size
        messages.addAll(itemsToAdd)
        notifyItemRangeInserted(insertPosition, itemsToAdd.size)
        onListCommittedListener?.invoke()
    }

    fun addNewMessages(items: List<MessageListItem>) {
        removeLoadingNext()
        if (items.isEmpty()) return
        val itemsToAdd = mergeAndFilterAlreadyExistingItems(items)
        if (itemsToAdd.isEmpty()) return

        val insertPosition = messages.size
        messages.addAll(itemsToAdd)
        notifyItemRangeInserted(insertPosition, itemsToAdd.size)
        onListCommittedListener?.invoke()
    }

    /**
     * Messages are identified by tid in the whole UI layer, so the same message must never be
     * inserted twice. It can arrive again from a pagination page or from a sync response, e.g. a
     * message sent while offline is already displayed as pending, and comes back from the server
     * with its id after reconnect.
     *
     * Instead of appending a second row, the already displayed item is updated in place with the
     * newer data, and the incoming item (together with its date separator) is dropped.
     */
    private fun mergeAndFilterAlreadyExistingItems(
        items: List<MessageListItem>,
    ): List<MessageListItem> {
        val existingIndexes = HashMap<Long, Int>()
        messages.forEachIndexed { index, item ->
            if (item is MessageItem) existingIndexes[item.message.tid] = index
        }
        if (existingIndexes.isEmpty()) return items

        val duplicatedTids = items.filterIsInstance<MessageItem>()
            .map { it.message.tid }
            .filterTo(HashSet()) { existingIndexes.containsKey(it) }
        if (duplicatedTids.isEmpty()) return items

        items.filterIsInstance<MessageItem>().forEach { newItem ->
            val index = existingIndexes[newItem.message.tid] ?: return@forEach
            val existingItem = messages.getOrNull(index) as? MessageItem ?: return@forEach
            val updatedMessage = existingItem.message.getUpdateMessage(newItem.message)
            val diff = existingItem.message.diff(updatedMessage)
            if (diff.hasDifference()) {
                messages[index] = existingItem.copy(message = updatedMessage)
                notifyItemChanged(index, diff)
            }
        }

        return items.filter { item ->
            when (item) {
                is MessageItem -> !duplicatedTids.contains(item.message.tid)
                is MessageListItem.DateSeparatorItem -> !duplicatedTids.contains(item.messageTid)
                else -> true
            }
        }
    }

    fun replaceMessageItem(
        updatedItem: MessageItem,
        positionHint: Int = RecyclerView.NO_POSITION,
    ): Int {
        val currentIndex = resolveMessageIndex(positionHint, updatedItem.message.tid)
        if (currentIndex != RecyclerView.NO_POSITION)
            messages[currentIndex] = updatedItem
        return currentIndex
    }

    private fun resolveMessageIndex(positionHint: Int, tid: Long): Int {
        val hintedItem = messages.getOrNull(positionHint) as? MessageItem
        if (hintedItem?.message?.tid == tid)
            return positionHint

        return messages.indexOfFirst {
            it is MessageItem && it.message.tid == tid
        }.takeIf { it >= 0 } ?: RecyclerView.NO_POSITION
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

    fun sort(recyclerView: RecyclerView) {
        debounceHelper.submit {
            val sortedList = messages.sortedWith(MessageItemComparator())
            val myDiffUtil = MessagesDiffUtil(
                oldList = ArrayList(this@MessagesAdapter.messages),
                newList = sortedList
            )
            val productDiffResult = DiffUtil.calculateDiff(myDiffUtil, true)

            val isLastItemVisible = recyclerView.isLastItemDisplaying()
            this@MessagesAdapter.messages = SyncArrayList(sortedList)
            productDiffResult.dispatchUpdatesToSafety(recyclerView)
            if (isLastItemVisible)
                recyclerView.scrollToPosition(itemCount - 1)
        }
    }

    fun setMultiSelectableMode(enable: Boolean) {
        isMultiSelectableMode = enable
    }

    fun isMultiSelectableMode() = isMultiSelectableMode

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