package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages

import androidx.recyclerview.widget.DiffUtil
import com.sceyt.chatuikit.persistence.differs.diff

class MessagesDiffUtil(
    private var oldList: List<MessageListItem>,
    private var newList: List<MessageListItem>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return when {
            oldItem is MessageListItem.MessageItem && newItem is MessageListItem.MessageItem ->
                oldItem.message.tid == newItem.message.tid

            oldItem is MessageListItem.DateSeparatorItem && newItem is MessageListItem.DateSeparatorItem ->
                oldItem.messageTid == newItem.messageTid

            oldItem is MessageListItem.UnreadMessagesSeparatorItem &&
                    newItem is MessageListItem.UnreadMessagesSeparatorItem ->
                oldItem.msgId == newItem.msgId

            oldItem is MessageListItem.LoadingPrevItem && newItem is MessageListItem.LoadingPrevItem -> true
            oldItem is MessageListItem.LoadingNextItem && newItem is MessageListItem.LoadingNextItem -> true
            else -> false
        }
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return if (oldItem is MessageListItem.MessageItem && newItem is MessageListItem.MessageItem)
            oldItem.message.diff(newItem.message).hasDifference().not()
        else oldItem == newItem
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        if (oldItem is MessageListItem.MessageItem && newItem is MessageListItem.MessageItem)
            return oldItem.message.diff(newItem.message)
        return null
    }
}
