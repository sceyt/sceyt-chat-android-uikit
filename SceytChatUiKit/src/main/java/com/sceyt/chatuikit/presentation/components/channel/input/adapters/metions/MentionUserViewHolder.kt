package com.sceyt.chatuikit.presentation.components.channel.input.adapters.metions

import androidx.core.view.isVisible
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.databinding.SceytItemMemberBinding
import com.sceyt.chatuikit.extensions.getPresentableName
import com.sceyt.chatuikit.presentation.extensions.setUserAvatar
import com.sceyt.chatuikit.presentation.root.BaseViewHolder

class MentionUserViewHolder(
        private val binding: SceytItemMemberBinding,
        private val itemClickListener: UsersAdapter.ClickListener
) : BaseViewHolder<SceytMember>(binding.root) {
    private lateinit var bindItem: SceytMember

    override fun bind(item: SceytMember) {
        bindItem = item
        val user = item.user

        with(binding) {
            avatar.setUserAvatar(user)
            val userPresentableName = SceytChatUIKit.formatters.mentionUserNameFormatter?.format(item.user)
                    ?: user.getPresentableName()
            userName.text = userPresentableName
            onlineStatus.isVisible = user.presence?.state == PresenceState.Online

            itemView.setOnClickListener {
                itemClickListener.onClick(bindItem)
            }
        }
    }
}