package com.sceyt.chat.ui.presentation.uicomponents.channels.adapter

import androidx.recyclerview.widget.DiffUtil
import com.sceyt.chat.ui.presentation.common.diff

class ChannelsDiffUtil(private var oldList: List<ChannelListItem>,
                       private var newList: List<ChannelListItem>) : DiffUtil.Callback() {

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
        return oldItem == newItem
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        if (oldItem is ChannelListItem.ChannelItem && newItem is ChannelListItem.ChannelItem)
            return oldItem.channel.diff(newItem.channel)
        return null
    }
}