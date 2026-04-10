package com.sceyt.chatuikit.presentation.common.recyclerview

import androidx.recyclerview.widget.DiffUtil
import com.sceyt.chatuikit.persistence.differs.diff
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchListItem

class GlobalSearchListItemDiffCallback : DiffUtil.ItemCallback<GlobalSearchListItem>() {

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
            !oldItem.result.message.diff(newItem.result.message).hasDifference()
                    && oldItem.query == newItem.query

        else -> false
    }

    override fun getChangePayload(
        oldItem: GlobalSearchListItem,
        newItem: GlobalSearchListItem,
    ): Any? {
        return when (oldItem) {
            is GlobalSearchListItem.ChannelItem if newItem is GlobalSearchListItem.ChannelItem ->
                oldItem.channel.diff(newItem.channel)

            is GlobalSearchListItem.MessageItem if newItem is GlobalSearchListItem.MessageItem ->
                oldItem.result.message.diff(newItem.result.message)

            else -> null
        }
    }
}