package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.adapters.metions

import androidx.core.view.isVisible
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.databinding.SceytItemMemberBinding
import com.sceyt.sceytchatuikit.extensions.getPresentableName
import com.sceyt.sceytchatuikit.presentation.root.BaseViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.MentionUserHelper
import com.sceyt.sceytchatuikit.sceytconfigs.UserStyle

class MentionUserViewHolder(private val binding: SceytItemMemberBinding,
                            private val itemClickListener: UsersAdapter.ClickListener) : BaseViewHolder<SceytMember>(binding.root) {
    private lateinit var bindItem: SceytMember

    override fun bind(item: SceytMember) {
        bindItem = item
        val user = item.user

        with(binding) {
            val userPresentableName = MentionUserHelper.userNameBuilder?.invoke(item.user)
                    ?: user.getPresentableName()
            avatar.setNameAndImageUrl(userPresentableName, user.avatarURL, UserStyle.userDefaultAvatar)
            userName.text = userPresentableName
            onlineStatus.isVisible = user.presence?.state == PresenceState.Online

            itemView.setOnClickListener {
                itemClickListener.onClick(bindItem)
            }
        }
    }
}