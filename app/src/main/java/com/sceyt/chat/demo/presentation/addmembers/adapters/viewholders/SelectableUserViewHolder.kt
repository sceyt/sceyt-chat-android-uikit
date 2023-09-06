package com.sceyt.chat.demo.presentation.addmembers.adapters.viewholders

import androidx.core.view.isVisible
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.demo.databinding.ItemSelectUserBinding
import com.sceyt.chat.demo.presentation.common.BaseViewHolder
import com.sceyt.chat.demo.presentation.addmembers.adapters.UserItem
import com.sceyt.chat.demo.presentation.addmembers.adapters.SelectableUsersAdapter
import com.sceyt.sceytchatuikit.extensions.getPresentableName

class SelectableUserViewHolder(private val binding: ItemSelectUserBinding,
                               private val itemClickListener: SelectableUsersAdapter.ClickListener) : BaseViewHolder<UserItem>(binding.root) {
    private lateinit var bindItem: UserItem.User

    override fun bind(item: UserItem) {
        bindItem = item as UserItem.User
        val user = item.user

        with(binding) {
            val userPresentableName = user.getPresentableName()
            avatar.setNameAndImageUrl(userPresentableName, user.avatarURL)
            userName.text = userPresentableName
            onlineStatus.isVisible = user.presence?.state == PresenceState.Online
            checkbox.isChecked = item.chosen

            itemView.setOnClickListener {
                item.chosen = !item.chosen
                checkbox.isChecked = item.chosen
                itemClickListener.onClick(bindItem)
            }
        }
    }
}