package com.sceyt.chatuikit.presentation.components.channel_info.members.adapter.holders

import androidx.core.view.isVisible
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.chatuikit.databinding.SceytItemChannelMembersBinding
import com.sceyt.chatuikit.extensions.firstCharToUppercase
import com.sceyt.chatuikit.extensions.getPresentableNameWithYou
import com.sceyt.chatuikit.extensions.getString
import com.sceyt.chatuikit.extensions.setTextColorRes
import com.sceyt.chatuikit.formatters.UserNameFormatter
import com.sceyt.chatuikit.presentation.components.channel_info.members.adapter.MemberItem
import com.sceyt.chatuikit.presentation.components.channel_info.members.adapter.diff.MemberItemPayloadDiff
import com.sceyt.chatuikit.presentation.components.channel_info.members.adapter.listeners.MemberClickListenersImpl
import com.sceyt.chatuikit.presentation.extensions.setUserAvatar
import com.sceyt.chatuikit.shared.utils.DateTimeUtil
import java.util.Date

class MemberViewHolder(private val binding: SceytItemChannelMembersBinding,
                       private val memberClickListeners: MemberClickListenersImpl,
                       private val userNameFormatter: UserNameFormatter? = null) : BaseMemberViewHolder(binding.root) {

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
            val presentableName = userNameFormatter?.format(member.user)
                    ?: member.getPresentableNameWithYou(itemView.context)

            if (diff.nameChanged || diff.avatarChanged) {
                avatar.setUserAvatar(member.user)
                userName.text = presentableName
            }

            if (diff.onlineStateChanged)
                tvStatus.text = if (member.user.presence?.state == PresenceState.Online)
                    itemView.getString(R.string.sceyt_online)
                else {
                    member.user.presence?.lastActiveAt?.let {
                        if (it != 0L)
                            DateTimeUtil.getPresenceDateFormatData(itemView.context, Date(it))
                        else ""
                    } ?: ""
                }

            if (diff.roleChanged)
                setRole()
        }
    }

    private fun setRole() {
        val role = memberItem.member.role
        val showRole = role.name == RoleTypeEnum.Owner.toString() || role.name == RoleTypeEnum.Admin.toString()

        binding.roleName.apply {
            text = memberItem.member.role.name.firstCharToUppercase()
            isVisible = showRole
        }
    }

    private fun SceytItemChannelMembersBinding.applyStyle() {
        userName.setTextColorRes(SceytChatUIKit.theme.textPrimaryColor)
        tvStatus.setTextColorRes(SceytChatUIKit.theme.textSecondaryColor)
        roleName.setTextColorRes(SceytChatUIKit.theme.textSecondaryColor)
    }
}