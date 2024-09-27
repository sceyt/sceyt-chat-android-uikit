package com.sceyt.chatuikit.presentation.components.channel.input.adapters.metions

import androidx.core.view.isVisible
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.databinding.SceytItemMemberBinding
import com.sceyt.chatuikit.presentation.extensions.setUserAvatar
import com.sceyt.chatuikit.presentation.root.BaseViewHolder
import com.sceyt.chatuikit.styles.input.MentionUsersListStyle

class MentionUserViewHolder(
        private val binding: SceytItemMemberBinding,
        private val style: MentionUsersListStyle,
        private val itemClickListener: UsersAdapter.ClickListener
) : BaseViewHolder<SceytMember>(binding.root) {

    init {
        binding.applyStyle()
    }

    override fun bind(item: SceytMember) {
        val user = item.user

        with(binding) {
            avatar.setUserAvatar(user, style.itemStyle.avatarProvider)
            userName.text = style.itemStyle.titleFormatter.format(context, user)
            onlineStatus.isVisible = user.presence?.state == PresenceState.Online

            itemView.setOnClickListener {
                itemClickListener.onClick(item)
            }
        }
    }

    private fun SceytItemMemberBinding.applyStyle() {
        val itemStyle = style.itemStyle
        itemStyle.titleTextStyle.apply(userName)
    }
}