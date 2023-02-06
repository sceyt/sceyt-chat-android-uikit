package com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter

import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.presentation.common.SelectableItem

sealed class ChannelListItem : SelectableItem() {
    data class ChannelItem(var channel: SceytChannel) : ChannelListItem()
    object LoadingMoreItem : ChannelListItem()

    override fun equals(other: Any?): Boolean {
        return when {
            other is ChannelItem && this is ChannelItem -> {
                other.channel == channel
            }
            other is LoadingMoreItem && this is LoadingMoreItem -> true
            else -> false
        }
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
