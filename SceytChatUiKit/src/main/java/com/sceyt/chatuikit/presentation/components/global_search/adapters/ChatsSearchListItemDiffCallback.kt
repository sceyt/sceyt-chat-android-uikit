package com.sceyt.chatuikit.presentation.components.global_search.adapters

import androidx.recyclerview.widget.DiffUtil
import com.sceyt.chatuikit.persistence.differs.diff
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchListItem

class ChatsSearchListItemDiffCallback : DiffUtil.ItemCallback<GlobalSearchListItem>() {

    override fun areItemsTheSame(
        oldItem: GlobalSearchListItem,
        newItem: GlobalSearchListItem,
    ): Boolean = when (oldItem) {
        is GlobalSearchListItem.SectionHeader if newItem is GlobalSearchListItem.SectionHeader ->
            oldItem.titleRes == newItem.titleRes

        is GlobalSearchListItem.ChannelItem if newItem is GlobalSearchListItem.ChannelItem ->
            oldItem.channel.id == newItem.channel.id

        is GlobalSearchListItem.MessageItem if newItem is GlobalSearchListItem.MessageItem ->
            oldItem.result.message.id == newItem.result.message.id

        else -> false
    }

    override fun areContentsTheSame(
        oldItem: GlobalSearchListItem,
        newItem: GlobalSearchListItem,
    ): Boolean = when (oldItem) {
        is GlobalSearchListItem.SectionHeader if newItem is GlobalSearchListItem.SectionHeader ->
            true

        is GlobalSearchListItem.ChannelItem if newItem is GlobalSearchListItem.ChannelItem ->
            !oldItem.channel.diff(newItem.channel).hasDifference()

        is GlobalSearchListItem.MessageItem if newItem is GlobalSearchListItem.MessageItem ->
            oldItem == newItem

        else -> false
    }

    override fun getChangePayload(
        oldItem: GlobalSearchListItem,
        newItem: GlobalSearchListItem,
    ): Any? {
        if (oldItem is GlobalSearchListItem.ChannelItem && newItem is GlobalSearchListItem.ChannelItem)
            return oldItem.channel.diff(newItem.channel)
        return null
    }
}
