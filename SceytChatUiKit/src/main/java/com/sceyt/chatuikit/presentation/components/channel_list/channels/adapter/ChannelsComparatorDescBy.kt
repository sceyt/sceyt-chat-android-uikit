package com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter

import com.sceyt.chat.models.channel.ChannelListQuery.ChannelListOrder
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel

class ChannelsComparatorDescBy(
        private val sortBy: ChannelListOrder = SceytChatUIKit.config.channelListOrder
) : Comparator<SceytChannel> {

    override fun compare(first: SceytChannel, second: SceytChannel): Int {
        return if (sortBy == ChannelListOrder.ListQueryChannelOrderLastMessage)
            compareByLastMessageCreatedAtDesc(first, second)
        else compareByChannelCreatedAtDesc(first, second)
    }

    private fun compareByLastMessageCreatedAtDesc(first: SceytChannel, second: SceytChannel): Int {
        // Last Message created at
        val firstMsgCreatedAt = first.lastMessage?.createdAt
        val secondMsgCreatedAt = second.lastMessage?.createdAt

        // Channel created at
        val firstCreatedAt = first.createdAt
        val secondCreatedAt = second.createdAt

        // Pinned channels always come first
        val (isPinned, result) = compareWithPinnedAtDesc(first, second)
        if (isPinned)
            return result

        return when {
            firstMsgCreatedAt != null && secondMsgCreatedAt != null -> secondMsgCreatedAt.compareTo(firstMsgCreatedAt)
            firstMsgCreatedAt != null && secondMsgCreatedAt == null -> -1
            firstMsgCreatedAt == null && secondMsgCreatedAt != null -> 1
            else -> secondCreatedAt.compareTo(firstCreatedAt)
        }
    }

    private fun compareByChannelCreatedAtDesc(first: SceytChannel, second: SceytChannel): Int {
        // Pinned channels always come first
        val (isPinned, result) = compareWithPinnedAtDesc(first, second)
        if (isPinned)
            return result

        return second.createdAt.compareTo(first.createdAt)
    }

    private fun compareWithPinnedAtDesc(first: SceytChannel, second: SceytChannel): Pair<Boolean, Int> {
        if (first.pinned && !second.pinned) return true to -1
        if (!first.pinned && second.pinned) return true to 1
        if (first.pinned && second.pinned)
            return true to (second.pinnedAt ?: 0).compareTo(first.pinnedAt ?: 0)
        return false to 0
    }
}
