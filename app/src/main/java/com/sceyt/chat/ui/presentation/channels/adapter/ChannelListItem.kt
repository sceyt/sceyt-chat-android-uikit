package com.sceyt.chat.ui.presentation.channels.adapter

import com.sceyt.chat.ui.data.models.SceytUiChannel

sealed class ChannelListItem {
    data class ChannelItem(val channel: SceytUiChannel) : ChannelListItem()
    object LoadingMoreItem : ChannelListItem()
}
