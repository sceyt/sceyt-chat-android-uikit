package com.sceyt.chatuikit.presentation.components.channel_info.groups.adapter.viewholders

import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytItemCommonGroupBinding
import com.sceyt.chatuikit.presentation.components.channel_info.groups.adapter.CommonGroupListItem
import com.sceyt.chatuikit.presentation.components.channel_info.groups.adapter.listeners.CommonGroupClickListeners
import com.sceyt.chatuikit.presentation.root.BaseViewHolder
import com.sceyt.chatuikit.styles.channel_info.common_groups.CommonGroupItemStyle

class CommonGroupViewHolder(
    private val binding: SceytItemCommonGroupBinding,
    private val style: CommonGroupItemStyle,
    private val clickListeners: CommonGroupClickListeners.ClickListeners?
) : BaseViewHolder<CommonGroupListItem>(binding.root) {

    private lateinit var channel: SceytChannel

    init {
        binding.applyStyle()

        binding.root.setOnClickListener {
            clickListeners?.onChannelClick(it, channel)
        }

        binding.root.setOnLongClickListener {
            clickListeners?.onChannelLongClick(it, channel)
            true
        }

        binding.avatar.setOnClickListener {
            clickListeners?.onAvatarClick(it, channel)
        }
    }

    override fun bind(item: CommonGroupListItem) {
        if (item is CommonGroupListItem.GroupItem) {
            channel = item.channel
            bindGroup(channel)
        }
    }

    private fun bindGroup(channel: SceytChannel) {
        with(binding) {
            tvChannelName.text = style.titleFormatter.format(
                context = root.context,
                from = channel
            )

            tvChannelSubtitle.text = style.membersCountFormatter.format(
                context = root.context,
                from = channel
            )

            style.avatarRenderer.render(
                context = root.context,
                from = channel,
                style = style.avatarStyle,
                avatarView = avatar
            )
        }
    }

    private fun SceytItemCommonGroupBinding.applyStyle() {
        style.titleStyle.apply(tvChannelName)
        style.membersCountStyle.apply(tvChannelSubtitle)
        style.avatarStyle.apply(avatar)
    }
}