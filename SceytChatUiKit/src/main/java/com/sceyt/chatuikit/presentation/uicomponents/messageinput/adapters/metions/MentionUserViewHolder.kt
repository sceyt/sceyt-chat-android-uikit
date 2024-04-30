package com.sceyt.chatuikit.presentation.uicomponents.messageinput.adapters.metions

import androidx.core.view.isVisible
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.databinding.SceytItemMemberBinding
import com.sceyt.chatuikit.extensions.getPresentableName
import com.sceyt.chatuikit.presentation.root.BaseViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.mention.MentionUserHelper

class MentionUserViewHolder(private val binding: SceytItemMemberBinding,
                            private val itemClickListener: UsersAdapter.ClickListener) : BaseViewHolder<SceytMember>(binding.root) {
    private lateinit var bindItem: SceytMember

    override fun bind(item: SceytMember) {
        bindItem = item
        val user = item.user

        with(binding) {
            val userPresentableName = MentionUserHelper.userNameFormatter?.format(item.user)
                    ?: user.getPresentableName()
            avatar.setNameAndImageUrl(userPresentableName, user.avatarURL, SceytChatUIKit.theme.userDefaultAvatar)
            userName.text = userPresentableName
            onlineStatus.isVisible = user.presence?.state == PresenceState.Online

            itemView.setOnClickListener {
                itemClickListener.onClick(bindItem)
            }
        }
    }
}