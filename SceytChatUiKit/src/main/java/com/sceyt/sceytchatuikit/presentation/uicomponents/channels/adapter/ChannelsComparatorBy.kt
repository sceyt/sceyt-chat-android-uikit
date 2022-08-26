package com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter

import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.sceytconfigs.SceytUIKitConfig

class ChannelsComparatorBy(private val compareBy: SceytUIKitConfig.ChannelSortType) : Comparator<SceytChannel> {

    override fun compare(first: SceytChannel, second: SceytChannel): Int {
        return if (compareBy == SceytUIKitConfig.ChannelSortType.ByLastMsg)
            compareByLastMessageCreatedAt(first, second)
        else compareByChannelCreatedAt(first, second)
    }

    private fun compareByLastMessageCreatedAt(first: SceytChannel?, second: SceytChannel?): Int {
        val firstMsgCreatedAt = first?.lastMessage?.createdAt
        val secondMsgCreatedAt = second?.lastMessage?.createdAt
        return when {
            firstMsgCreatedAt != null && secondMsgCreatedAt != null -> return secondMsgCreatedAt.compareTo(firstMsgCreatedAt)
            firstMsgCreatedAt != null -> -1
            secondMsgCreatedAt != null -> 1
            else -> 0
        }
    }

    private fun compareByChannelCreatedAt(first: SceytChannel, second: SceytChannel): Int {
        return second.createdAt.compareTo(first.createdAt)
    }
}