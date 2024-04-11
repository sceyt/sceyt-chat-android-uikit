package com.sceyt.chat.demo.presentation.newchannel.adapters

import androidx.core.view.isVisible
import com.sceyt.chat.demo.databinding.ItemUserBinding
import com.sceyt.chat.demo.presentation.addmembers.adapters.UserItem
import com.sceyt.chat.demo.presentation.common.BaseViewHolder
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chatuikit.R.*
import com.sceyt.chatuikit.SceytKitClient
import com.sceyt.chatuikit.extensions.getPresentableName
import com.sceyt.chatuikit.extensions.getString
import com.sceyt.chatuikit.shared.utils.DateTimeUtil
import java.util.Date

class UserViewHolder(private val binding: ItemUserBinding,
                     private val itemClickListener: UsersAdapter.ClickListener) : BaseViewHolder<UserItem>(binding.root) {
    private lateinit var bindItem: UserItem.User

    override fun bind(item: UserItem) {
        bindItem = item as UserItem.User
        val user = item.user

        with(binding) {
            if (user.id == SceytKitClient.myId) {
                avatar.setNameAndImageUrl("", null, drawable.sceyt_ic_notes_with_paddings)
                userName.text = context.getString(string.self_notes)
                tvStatus.isVisible = false
            } else {
                val userPresentableName = user.getPresentableName()
                avatar.setNameAndImageUrl(userPresentableName, user.avatarURL, drawable.sceyt_ic_default_avatar)
                userName.text = userPresentableName

                if (user.presence == null || user.presence!!.lastActiveAt == 0L)
                    tvStatus.isVisible = false
                else {
                    tvStatus.text = if (user.presence?.state == PresenceState.Online)
                        itemView.getString(string.sceyt_online)
                    else DateTimeUtil.getPresenceDateFormatData(itemView.context, Date(user.presence?.lastActiveAt
                            ?: 0))
                    tvStatus.isVisible = true
                }
            }

            itemView.setOnClickListener {
                itemClickListener.onClick(bindItem)
            }
        }
    }
}