package com.sceyt.chat.ui.presentation.uicomponents.channels.adapter

import com.sceyt.chat.ui.data.models.SceytUiChannel

sealed class ChannelListItem {
    data class ChannelItem(val channel: SceytUiChannel) : ChannelListItem()
    object LoadingMoreItem : ChannelListItem()

    override fun equals(other: Any?): Boolean {
        return when {
            other == null -> false
            other !is ChannelListItem -> false
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
