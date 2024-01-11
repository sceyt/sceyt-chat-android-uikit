package com.sceyt.chat.demo.presentation.newchannel.adapters

import androidx.core.view.isVisible
import com.sceyt.chat.demo.databinding.ItemUserBinding
import com.sceyt.chat.demo.presentation.addmembers.adapters.UserItem
import com.sceyt.chat.demo.presentation.common.BaseViewHolder
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.sceytchatuikit.extensions.getPresentableName
import com.sceyt.sceytchatuikit.extensions.getString
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil
import java.util.Date

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

            if (user.presence == null || user.presence!!.lastActiveAt == 0L)
                tvStatus.isVisible = false
            else
                tvStatus.text = if (user.presence?.state == PresenceState.Online)
                    itemView.getString(com.sceyt.sceytchatuikit.R.string.sceyt_online)
                else DateTimeUtil.getPresenceDateFormatData(itemView.context, Date(user.presence?.lastActiveAt
                        ?: 0))

            itemView.setOnClickListener {
                itemClickListener.onClick(bindItem)
            }
        }
    }
}