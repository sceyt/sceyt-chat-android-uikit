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
    private val clickListeners: CommonGroupClickListeners.ClickListener?
) : BaseViewHolder<CommonGroupListItem>(binding.root) {

    private lateinit var channel: SceytChannel

    init {
        binding.applyStyle()
        binding.root.setOnClickListener {
            clickListeners?.onGroupClick(it, channel)
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
            tvChannelName.text = style.commonGroupTitleFormatter.format(
                context = root.context,
                from = channel
            )

            tvChannelSubtitle.text = style.commonGroupMembersCountFormatter.format(
                context = root.context,
                from = channel
            )

            style.channelAvatarRenderer.render(
                context = root.context,
                from = channel,
                style = style.avatarStyle,
                avatarView = avatar
            )
        }
    }

    private fun SceytItemCommonGroupBinding.applyStyle() {
        root.setBackgroundColor(style.backgroundColor)
        style.commonGroupTitleStyle.apply(tvChannelName)
        style.commonGroupMembersCountStyle.apply(tvChannelSubtitle)
        style.avatarStyle.apply(avatar)
    }
}