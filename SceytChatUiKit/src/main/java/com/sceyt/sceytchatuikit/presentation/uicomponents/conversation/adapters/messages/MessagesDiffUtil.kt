package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages

import androidx.recyclerview.widget.DiffUtil
import com.sceyt.sceytchatuikit.persistence.differs.diff

class MessagesDiffUtil(private var oldList: List<MessageListItem>,
                       private var newList: List<MessageListItem>) : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return (oldItem == newItem)
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return if (oldItem is MessageListItem.MessageItem && newItem is MessageListItem.MessageItem)
            oldItem.message.diff(newItem.message).hasDifference().not()
        else oldItem == newItem
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        if (oldItem is MessageListItem.MessageItem && newItem is MessageListItem.MessageItem)
            return oldItem.message.diff(newItem.message)
        return Unit
    }
}