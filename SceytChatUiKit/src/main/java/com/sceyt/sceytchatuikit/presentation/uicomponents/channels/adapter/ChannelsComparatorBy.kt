package com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter

import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

class ChannelsComparatorBy(private val compareBy: SceytKitConfig.ChannelSortType) : Comparator<SceytChannel> {

    override fun compare(first: SceytChannel, second: SceytChannel): Int {
        return if (compareBy == SceytKitConfig.ChannelSortType.ByLastMsg)
            compareByLastMessageCreatedAt(first, second)
        else compareByChannelCreatedAt(first, second)
    }

    private fun compareByLastMessageCreatedAt(first: SceytChannel?, second: SceytChannel?): Int {
        //Todo need review sorting
        val firstMsgCreatedAt = first?.lastMessage?.createdAt?.run { this * 1000 }
                ?: second?.createdAt
        val secondMsgCreatedAt = second?.lastMessage?.createdAt?.run { this * 1000 }
                ?: second?.createdAt


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