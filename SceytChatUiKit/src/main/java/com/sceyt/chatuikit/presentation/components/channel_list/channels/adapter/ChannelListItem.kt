package com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter

import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.presentation.common.SelectableItem

sealed class ChannelListItem : SelectableItem() {
    data class ChannelItem(var channel: SceytChannel) : ChannelListItem()
    data object LoadingMoreItem : ChannelListItem()
}
