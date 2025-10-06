package com.sceyt.chatuikit.presentation.components.shareable.adapter.holders

import androidx.core.view.isVisible
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytItemShareChannelBinding
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.persistence.differs.ChannelDiff
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItem
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.holders.BaseChannelViewHolder
import com.sceyt.chatuikit.presentation.components.channel_list.channels.listeners.click.ChannelClickListeners
import com.sceyt.chatuikit.renderers.ChannelAvatarRenderer
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.common.SelectableListItemStyle

open class ShareableChannelViewHolder(
        private val binding: SceytItemShareChannelBinding,
        private val itemStyle: SelectableListItemStyle<Formatter<SceytChannel>,
                Formatter<SceytChannel>, ChannelAvatarRenderer>,
        private val clickListener: ChannelClickListeners.ChannelClickListener,
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
            clickListener.onChannelClick(view = it, item)
        }
    }

    open fun setAvatar(channel: SceytChannel) {
        itemStyle.avatarRenderer.render(context, channel, itemStyle.avatarStyle, binding.avatar)
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
        itemStyle.avatarStyle.apply(avatar)
    }
}