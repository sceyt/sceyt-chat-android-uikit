package com.sceyt.chatuikit.presentation.components.channel_info.groups.adapter

import com.sceyt.chatuikit.data.models.channels.SceytChannel

sealed class CommonGroupListItem {
    data class GroupItem(val channel: SceytChannel) : CommonGroupListItem()
    data object LoadingMore : CommonGroupListItem()
}