package com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter

import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItem.LoadingMoreItem
import com.sceyt.chatuikit.config.ChannelSortType

class ChannelsItemComparatorBy(private val compareBy: ChannelSortType = SceytChatUIKit.config.sortChannelsBy) : Comparator<ChannelListItem> {
    private val channelsComparatorDescBy = ChannelsComparatorDescBy(compareBy)

    override fun compare(first: ChannelListItem, second: ChannelListItem): Int {
        return if (compareBy == ChannelSortType.ByLastMsg)
            compareByLastMessageCreatedAt(first, second)
        else compareByChannelCreatedAt(first, second)
    }

    private fun compareByLastMessageCreatedAt(first: ChannelListItem, second: ChannelListItem): Int {
        return when {
            first is ChannelListItem.ChannelItem && second is ChannelListItem.ChannelItem -> {
                channelsComparatorDescBy.compare(first.channel, second.channel)
            }

            else -> sortWithLoading(first, second)
        }
    }

    private fun compareByChannelCreatedAt(first: ChannelListItem, second: ChannelListItem): Int {
        return when {
            first is ChannelListItem.ChannelItem && second is ChannelListItem.ChannelItem -> {
                channelsComparatorDescBy.compare(first.channel, second.channel)
            }

            else -> sortWithLoading(first, second)
        }
    }

    private fun sortWithLoading(first: ChannelListItem, second: ChannelListItem): Int {
        return when {
            first is LoadingMoreItem && second is LoadingMoreItem -> 0
            first is LoadingMoreItem && second !is LoadingMoreItem -> 1
            second is LoadingMoreItem -> -1
            else -> 0
        }
    }
}