package com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter

import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.persistence.differs.diff
import com.sceyt.chatuikit.presentation.common.SelectableItem

sealed class ChannelListItem : SelectableItem() {
    data class ChannelItem(val channel: SceytChannel) : ChannelListItem() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ChannelItem) return false
            if (channel.id != other.channel.id) return false
            if (selected != other.selected) return false
            return !channel.diff(other.channel).hasDifference()
        }

        override fun hashCode() = channel.id.hashCode()
    }

    data object LoadingMoreItem : ChannelListItem()
}
