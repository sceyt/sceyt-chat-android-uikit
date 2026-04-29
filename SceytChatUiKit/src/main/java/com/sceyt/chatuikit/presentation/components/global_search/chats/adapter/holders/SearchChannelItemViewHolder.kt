package com.sceyt.chatuikit.presentation.components.global_search.chats.adapter.holders

import com.sceyt.chatuikit.databinding.SceytItemChannelBinding
import com.sceyt.chatuikit.persistence.differs.ChannelDiff
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItem
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.holders.ChannelViewHolder
import com.sceyt.chatuikit.presentation.components.channel_list.channels.listeners.click.ChannelClickListeners
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchListItem
import com.sceyt.chatuikit.styles.channel.ChannelItemStyle

open class SearchChannelItemViewHolder(
    binding: SceytItemChannelBinding,
    itemStyle: ChannelItemStyle,
    listeners: ChannelClickListeners.ClickListeners,
    attachDetachListener: ((ChannelListItem?, attached: Boolean) -> Unit)? = null,
) : ChannelViewHolder(binding, itemStyle, listeners, attachDetachListener) {

    fun bind(item: GlobalSearchListItem.ChannelItem, diff: ChannelDiff) {
        bind(ChannelListItem.ChannelItem(item.channel), diff)
    }
}
