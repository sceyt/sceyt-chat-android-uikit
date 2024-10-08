package com.sceyt.chatuikit.presentation.components.channel_info.members.adapter.holders

import androidx.core.view.isVisible
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.chatuikit.databinding.SceytItemChannelMembersBinding
import com.sceyt.chatuikit.extensions.firstCharToUppercase
import com.sceyt.chatuikit.presentation.components.channel_info.members.adapter.MemberItem
import com.sceyt.chatuikit.presentation.components.channel_info.members.adapter.diff.MemberItemPayloadDiff
import com.sceyt.chatuikit.presentation.components.channel_info.members.adapter.listeners.MemberClickListeners
import com.sceyt.chatuikit.presentation.extensions.setUserAvatar
import com.sceyt.chatuikit.styles.channel_members.ChannelMemberListItemStyle

class MemberViewHolder(
        private val binding: SceytItemChannelMembersBinding,
        private val style: ChannelMemberListItemStyle,
        private val memberClickListeners: MemberClickListeners.ClickListeners,
) : BaseMemberViewHolder(binding.root) {

    private lateinit var memberItem: MemberItem.Member

    init {
        binding.applyStyle()
        binding.root.setOnClickListener {
            memberClickListeners.onMemberClick(it, memberItem)
        }

        binding.root.setOnLongClickListener {
            memberClickListeners.onMemberLongClick(it, memberItem)
            return@setOnLongClickListener true
        }
    }

    override fun bind(item: MemberItem, diff: MemberItemPayloadDiff) {
        memberItem = ((item as? MemberItem.Member) ?: return)
        val member = memberItem.member

        with(binding) {
            if (diff.nameChanged || diff.avatarChanged) {
                avatar.setUserAvatar(member.user, style.listItemStyle.avatarProvider)
                userName.text = style.listItemStyle.titleFormatter.format(context, member.user)
            }

            if (diff.presenceStateChanged)
                tvStatus.text = SceytChatUIKit.formatters.userPresenceDateFormatter.format(
                    context = context, from = member.user)

            if (diff.roleChanged)
                setRole()
        }
    }

    private fun setRole() {
        val role = memberItem.member.role
        val showRole = role.name == RoleTypeEnum.Owner.value || role.name == RoleTypeEnum.Admin.value

        binding.roleName.apply {
            text = memberItem.member.role.name.firstCharToUppercase()
            isVisible = showRole
        }
    }

    private fun SceytItemChannelMembersBinding.applyStyle() {
        style.listItemStyle.titleTextStyle.apply(userName)
        style.listItemStyle.subtitleTextStyle.apply(tvStatus)
        style.roleTextStyle.apply(roleName)
    }
}