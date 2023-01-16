package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.viewholders

import android.text.SpannableStringBuilder
import androidx.core.text.color
import androidx.core.view.isVisible
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.sceytchatuikit.databinding.ItemChannelMembersBinding
import com.sceyt.sceytchatuikit.extensions.firstCharToUppercase
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.getPresentableName
import com.sceyt.sceytchatuikit.extensions.getString
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.MemberItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.diff.MemberItemPayloadDiff
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.listeners.MemberClickListenersImpl
import com.sceyt.sceytchatuikit.sceytconfigs.UserStyle
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil
import java.util.*

class MemberViewHolder(private val binding: ItemChannelMembersBinding,
                       private val currentUserId: String?,
                       private val memberClickListeners: MemberClickListenersImpl,
                       private val userNameBuilder: ((User) -> String)? = null) : BaseMemberViewHolder(binding.root) {

    private lateinit var memberItem: MemberItem.Member
    private val youColor = itemView.context.getCompatColor(R.color.sceyt_color_gray_400)

    init {
        binding.roleName.setOnClickListener {
            if (enabledActions())
                memberClickListeners.onMemberLongClick(it, memberItem)
        }
    }

    override fun bind(item: MemberItem, diff: MemberItemPayloadDiff) {
        memberItem = ((item as? MemberItem.Member) ?: return)
        val member = memberItem.member

        with(binding) {

            val presentableName = userNameBuilder?.invoke(member.user)
                    ?: member.getPresentableName()

            if (diff.nameChanged || diff.avatarChanged) {
                avatar.setNameAndImageUrl(presentableName, member.user.avatarURL, UserStyle.userDefaultAvatar)

                userName.text = if (member.id == currentUserId) {
                    val text = SpannableStringBuilder()
                        .append(presentableName)
                        .color(youColor) { append(" " + itemView.context.getString(R.string.sceyt_member_name_you)) }
                    text
                } else presentableName
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
        val showRole = enabledActions()
        binding.roleName.apply {
            text = memberItem.member.role.name.firstCharToUppercase()
            isVisible = showRole
        }
    }

    private fun enabledActions(): Boolean {
        val role = memberItem.member.role
        return role.name == RoleTypeEnum.Owner.toString() || role.name == RoleTypeEnum.Admin.toString()
    }
}