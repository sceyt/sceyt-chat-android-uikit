package com.sceyt.chatuikit.presentation.components.shareable.adapter.holders

import androidx.core.view.isVisible
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytItemShareChannelBinding
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.persistence.differs.ChannelDiff
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItem
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.holders.BaseChannelViewHolder
import com.sceyt.chatuikit.presentation.components.channel_list.channels.listeners.click.ChannelClickListeners
import com.sceyt.chatuikit.presentation.custom_views.AvatarView
import com.sceyt.chatuikit.presentation.extensions.setChannelAvatar
import com.sceyt.chatuikit.providers.VisualProvider
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.common.SelectableListItemStyle

open class ShareableChannelViewHolder(
        protected val binding: SceytItemShareChannelBinding,
        protected val itemStyle: SelectableListItemStyle<Formatter<SceytChannel>,
                Formatter<SceytChannel>, VisualProvider<SceytChannel, AvatarView.DefaultAvatar>>,
        protected val clickListener: ChannelClickListeners.ChannelClickListener
) : BaseChannelViewHolder(binding.root) {

    init {
        binding.applyStyle()
    }

    override fun bind(item: ChannelListItem, diff: ChannelDiff) {
        super.bind(item, diff)

        val channel = (item as? ChannelListItem.ChannelItem)?.channel ?: return

        setAvatar(channel)
        setTitle(channel)
        setSubtitle(channel)

        binding.checkbox.isChecked = item.selected

        binding.root.setOnClickListener {
            clickListener.onChannelClick(item)
        }
    }

    open fun setAvatar(channel: SceytChannel) {
        binding.avatar.setChannelAvatar(channel)
    }

    open fun setTitle(channel: SceytChannel) {
        binding.tvTitle.text = itemStyle.titleFormatter.format(context, channel)
    }

    open fun setSubtitle(channel: SceytChannel) {
        if (channel.isGroup) {
            binding.tvSubtitle.text = itemStyle.subtitleFormatter.format(context, channel)
        }
        binding.tvSubtitle.isVisible = channel.isGroup
    }

    private fun SceytItemShareChannelBinding.applyStyle() {
        if (itemStyle.backgroundColor != UNSET_COLOR) {
            root.setBackgroundColor(itemStyle.backgroundColor)
        }
        if (itemStyle.dividerColor != UNSET_COLOR) {
            divider.setBackgroundColor(itemStyle.dividerColor)
        }
        itemStyle.checkboxStyle.apply(checkbox)
        itemStyle.titleTextStyle.apply(tvTitle)
        itemStyle.subtitleTextStyle.apply(tvSubtitle)
    }
}