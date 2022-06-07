package com.sceyt.chat.ui.presentation.uicomponents.channels.adapter

import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig

class ChannelsComparatorBy(private val compareBy: SceytUIKitConfig.ChannelSortType) : Comparator<SceytChannel> {

    override fun compare(next: SceytChannel, prev: SceytChannel): Int {
        return if (compareBy == SceytUIKitConfig.ChannelSortType.ByLastMsg)
            compareByLastMessageCreatedAt(next, prev)
        else compareByChannelCreatedAt(next, prev)
    }

    private fun compareByLastMessageCreatedAt(next: SceytChannel?, prev: SceytChannel?): Int {
        val nextMsgCreatedAt = next?.lastMessage?.createdAt
        val prevMsgCreatedAt = prev?.lastMessage?.createdAt
        return when {
            nextMsgCreatedAt != null && prevMsgCreatedAt != null -> return prevMsgCreatedAt.compareTo(nextMsgCreatedAt)
            nextMsgCreatedAt != null -> -1
            else -> 0
        }
    }

    private fun compareByChannelCreatedAt(next: SceytChannel, prev: SceytChannel): Int {
        return prev.createdAt.compareTo(next.createdAt)
    }
}