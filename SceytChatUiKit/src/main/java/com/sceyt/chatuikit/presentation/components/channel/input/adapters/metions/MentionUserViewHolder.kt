package com.sceyt.chatuikit.presentation.components.channel.input.adapters.metions

import androidx.core.view.isVisible
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.databinding.SceytItemMemberBinding
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
            style.itemStyle.avatarRenderer.render(
                context, user, style.itemStyle.avatarStyle, avatar
            )

            userName.text = style.itemStyle.titleFormatter.format(context, user)

            val indicatorColor = SceytChatUIKit.providers.presenceStateColorProvider.provide(
                context, user.presence?.state ?: PresenceState.Offline
            )

            onlineStatus.setBackgroundColor(indicatorColor)
            onlineStatus.isVisible = user.presence?.state == PresenceState.Online

            itemView.setOnClickListener {
                itemClickListener.onClick(item)
            }
        }
    }

    private fun SceytItemMemberBinding.applyStyle() {
        val itemStyle = style.itemStyle
        itemStyle.titleTextStyle.apply(userName)
        itemStyle.avatarStyle.apply(avatar)
    }
}