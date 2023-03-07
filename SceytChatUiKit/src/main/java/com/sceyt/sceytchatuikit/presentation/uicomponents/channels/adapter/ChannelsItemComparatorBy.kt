package com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter

import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelListItem.LoadingMoreItem
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

class ChannelsItemComparatorBy(private val compareBy: SceytKitConfig.ChannelSortType = SceytKitConfig.sortChannelsBy) : Comparator<ChannelListItem> {

    override fun compare(first: ChannelListItem, second: ChannelListItem): Int {
        return if (compareBy == SceytKitConfig.ChannelSortType.ByLastMsg)
            compareByLastMessageCreatedAt(first, second)
        else compareByChannelCreatedAt(first, second)
    }

    private fun compareByLastMessageCreatedAt(first: ChannelListItem, second: ChannelListItem): Int {
        return when {
            first is ChannelListItem.ChannelItem && second is ChannelListItem.ChannelItem -> {
                // Last Message created at
                val firstMsgCreatedAt = first.channel.lastMessage?.createdAt
                val secondMsgCreatedAt = second.channel.lastMessage?.createdAt

                // Channel created at
                val firstCreatedAt = first.channel.createdAt
                val secondCreatedAt = second.channel.createdAt

                when {
                    firstMsgCreatedAt != null && secondMsgCreatedAt != null -> secondMsgCreatedAt.compareTo(firstMsgCreatedAt)
                    firstMsgCreatedAt != null && secondMsgCreatedAt == null -> -1
                    secondMsgCreatedAt != null -> 1
                    else -> secondCreatedAt.compareTo(firstCreatedAt)
                }
            }
            else -> sortWithLoading(first, second)
        }
    }


    private fun compareByChannelCreatedAt(first: ChannelListItem, second: ChannelListItem): Int {
        return when {
            first is ChannelListItem.ChannelItem && second is ChannelListItem.ChannelItem -> {
                // Channel created at
                val firstCreatedAt = first.channel.createdAt
                val secondCreatedAt = second.channel.createdAt

                secondCreatedAt.compareTo(firstCreatedAt)
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