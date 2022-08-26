package com.sceyt.chat.ui.presentation.newchannel.adapters

import androidx.core.view.isVisible
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.ui.databinding.ItemUserBinding
import com.sceyt.chat.ui.presentation.common.BaseViewHolder
import com.sceyt.chat.ui.presentation.addmembers.adapters.UserItem
import com.sceyt.sceytchatuikit.extensions.getPresentableName

class UserViewHolder(private val binding: ItemUserBinding,
                     private val itemClickListener: UsersAdapter.ClickListener) : BaseViewHolder<UserItem>(binding.root) {
    private lateinit var bindItem: UserItem.User

    override fun bind(item: UserItem) {
        bindItem = item as UserItem.User
        val user = item.user

        with(binding) {
            val userPresentableName = user.getPresentableName()
            avatar.setNameAndImageUrl(userPresentableName, user.avatarURL)
            userName.text = userPresentableName
            onlineStatus.isVisible = user.presence?.state == PresenceState.Online

            itemView.setOnClickListener {
                itemClickListener.onClick(bindItem)
            }
        }
    }
}