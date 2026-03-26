package com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter

import androidx.recyclerview.widget.DiffUtil
import com.sceyt.chatuikit.persistence.differs.diff

object ChannelListItemDiffCallback : DiffUtil.ItemCallback<ChannelListItem>() {
    override fun areItemsTheSame(oldItem: ChannelListItem, newItem: ChannelListItem): Boolean {
        if (oldItem is ChannelListItem.ChannelItem && newItem is ChannelListItem.ChannelItem)
            return oldItem.channel.id == newItem.channel.id
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: ChannelListItem, newItem: ChannelListItem): Boolean {
        return when (oldItem) {
            is ChannelListItem.ChannelItem if newItem is ChannelListItem.ChannelItem ->
                !oldItem.channel.diff(newItem.channel).hasDifference()

            is ChannelListItem.LoadingMoreItem if newItem is ChannelListItem.LoadingMoreItem -> true
            else -> false
        }
    }

    override fun getChangePayload(oldItem: ChannelListItem, newItem: ChannelListItem): Any? {
        if (oldItem is ChannelListItem.ChannelItem && newItem is ChannelListItem.ChannelItem)
            return oldItem.channel.diff(newItem.channel)
        return null
    }
}
