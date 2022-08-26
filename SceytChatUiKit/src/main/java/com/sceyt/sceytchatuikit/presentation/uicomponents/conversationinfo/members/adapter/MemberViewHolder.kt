package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter

import android.text.SpannableStringBuilder
import androidx.core.text.color
import androidx.core.view.isVisible
import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.databinding.ItemChannelMemberBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.getPresentableName
import com.sceyt.sceytchatuikit.extensions.setDrawableEnd
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.diff.MemberItemPayloadDiff
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.listeners.MemberClickListenersImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.viewholder.BaseMemberViewHolder

class MemberViewHolder(private val binding: ItemChannelMemberBinding,
                       private val memberClickListeners: MemberClickListenersImpl) : BaseMemberViewHolder(binding.root) {

    private lateinit var memberItem: MemberItem.Member
    private val youColor = itemView.context.getCompatColor(R.color.sceyt_color_gray_400)

    override fun bind(item: MemberItem, diff: MemberItemPayloadDiff) {
        memberItem = ((item as? MemberItem.Member) ?: return)
        val member = memberItem.member

        with(binding) {

            val presentableName = member.getPresentableName()

            if (diff.nameChanged || diff.avatarChanged) {
                avatar.setNameAndImageUrl(presentableName, member.user.avatarURL)

                memberName.text = if (member.id == ChatClient.getClient().user.id) {
                    val text = SpannableStringBuilder()
                        .append(presentableName)
                        .color(youColor) { append(" " + itemView.context.getString(R.string.member_name_you)) }
                    text
                } else presentableName
            }

            if (diff.onlineStateChanged)
                onlineStatus.isVisible = member.user.presence?.state == PresenceState.Online

            if (diff.roleChanged)
                roleName.text = member.role.name

            if (diff.showMorIconChanged || diff.roleChanged)
                setMoreItem()
        }
    }

    private fun setMoreItem() {
        if (memberItem.member.role.name == "owner") {
            binding.roleName.setDrawableEnd(0)
            return
        }
        val showMoreIcon = (bindingAdapter as ChannelMembersAdapter).showMoreIcon
        binding.roleName.setDrawableEnd(if (showMoreIcon) R.drawable.sceyt_ic_more_dots else 0)

        if (showMoreIcon) {
            binding.roleName.setOnClickListener {
                memberClickListeners.onMoreClick(it, memberItem)
            }
        }
    }
}