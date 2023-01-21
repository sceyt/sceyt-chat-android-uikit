package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.viewholders

import android.content.res.ColorStateList
import androidx.annotation.ColorRes
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.sceytchatuikit.databinding.ItemChannelMembersBinding
import com.sceyt.sceytchatuikit.extensions.firstCharToUppercase
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.getPresentableNameWithYou
import com.sceyt.sceytchatuikit.extensions.getString
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.MemberItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.diff.MemberItemPayloadDiff
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.listeners.MemberClickListenersImpl
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.sceytconfigs.UserStyle
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil
import java.util.*

class MemberViewHolder(private val binding: ItemChannelMembersBinding,
                       private val currentUserId: String?,
                       private val memberClickListeners: MemberClickListenersImpl,
                       private val userNameBuilder: ((User) -> String)? = null) : BaseMemberViewHolder(binding.root) {

    private lateinit var memberItem: MemberItem.Member

    init {
        binding.root.setOnLongClickListener {
            memberClickListeners.onMemberLongClick(it, memberItem)
            return@setOnLongClickListener true
        }
    }

    override fun bind(item: MemberItem, diff: MemberItemPayloadDiff) {
        memberItem = ((item as? MemberItem.Member) ?: return)
        val member = memberItem.member

        with(binding) {

            val presentableName = userNameBuilder?.invoke(member.user)
                    ?: member.getPresentableNameWithYou(itemView.context)

            if (diff.nameChanged || diff.avatarChanged) {
                avatar.setNameAndImageUrl(presentableName, member.user.avatarURL, UserStyle.userDefaultAvatar)

                userName.text = presentableName
            }

            if (diff.onlineStateChanged)
                tvStatus.text = if (member.user.presence?.state == PresenceState.Online)
                    itemView.getString(R.string.sceyt_online)
                else DateTimeUtil.getPresenceDateFormatData(itemView.context, Date(member.user.presence?.lastActiveAt
                        ?: 0))

            if (diff.roleChanged)
                setRole()
        }
    }

    private fun setRole() {
        val role = memberItem.member.role
        val showRole = role.name == RoleTypeEnum.Owner.toString() || role.name == RoleTypeEnum.Admin.toString()
        if (showRole)
            setRoleNameColor()
        binding.roleName.apply {
            text = memberItem.member.role.name.firstCharToUppercase()
            isVisible = showRole
        }
    }

    private fun setRoleNameColor() {
        val role = memberItem.member.role
        when (role.name) {
            RoleTypeEnum.Owner.toString() -> {
                setRoleNameColor(SceytKitConfig.sceytColorAccent)
            }
            RoleTypeEnum.Admin.toString() -> {
                setRoleNameColor(R.color.sceyt_color_admin_role)
            }
        }
    }

    private fun setRoleNameColor(@ColorRes colorRes: Int) {
        binding.roleName.apply {
            val colorAccent = context.getCompatColor(colorRes)
            val bgColorTint = ColorUtils.setAlphaComponent(itemView.context.getCompatColor(colorRes), (0.1 * 255).toInt())
            backgroundTintList = ColorStateList.valueOf(bgColorTint)
            setTextColor(colorAccent)
        }
    }
}