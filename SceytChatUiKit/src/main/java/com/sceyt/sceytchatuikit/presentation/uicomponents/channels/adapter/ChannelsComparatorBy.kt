package com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter

import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

class ChannelsComparatorBy(private val compareBy: SceytKitConfig.ChannelSortType = SceytKitConfig.sortChannelsBy)
    : Comparator<SceytChannel> {

    override fun compare(first: SceytChannel, second: SceytChannel): Int {
        return if (compareBy == SceytKitConfig.ChannelSortType.ByLastMsg)
            compareByLastMessageCreatedAt(first, second)
        else compareByChannelCreatedAt(first, second)
    }

    private fun compareByLastMessageCreatedAt(first: SceytChannel?, second: SceytChannel?): Int {
        // Last Message created at
        val firstMsgCreatedAt = first?.lastMessage?.createdAt
        val secondMsgCreatedAt = second?.lastMessage?.createdAt

        // Channel created at
        val firstCreatedAt = first?.createdAt
        val secondCreatedAt = second?.createdAt


        return when {
            firstMsgCreatedAt != null && secondMsgCreatedAt != null -> secondMsgCreatedAt.compareTo(firstMsgCreatedAt)
            firstMsgCreatedAt != null && secondMsgCreatedAt == null -> -1
            secondMsgCreatedAt != null -> 1

            firstCreatedAt != null && secondCreatedAt != null -> secondCreatedAt.compareTo(firstCreatedAt)
            else -> 0
        }
    }

    private fun compareByChannelCreatedAt(first: SceytChannel, second: SceytChannel): Int {
        return second.createdAt.compareTo(first.createdAt)
    }
}