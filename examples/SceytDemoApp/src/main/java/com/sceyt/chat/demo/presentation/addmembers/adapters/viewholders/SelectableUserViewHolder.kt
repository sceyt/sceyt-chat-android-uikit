package com.sceyt.chat.demo.presentation.addmembers.adapters.viewholders

import androidx.core.view.isVisible
import com.sceyt.chat.demo.databinding.ItemSelectUserBinding
import com.sceyt.chat.demo.presentation.addmembers.adapters.SelectableUsersAdapter
import com.sceyt.chat.demo.presentation.addmembers.adapters.UserItem
import com.sceyt.chat.demo.presentation.common.BaseViewHolder
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chatuikit.extensions.getPresentableName
import com.sceyt.chatuikit.extensions.getString
import com.sceyt.chatuikit.shared.utils.DateTimeUtil
import java.util.Date

class SelectableUserViewHolder(private val binding: ItemSelectUserBinding,
                               private val itemClickListener: SelectableUsersAdapter.ClickListener) : BaseViewHolder<UserItem>(binding.root) {
    private lateinit var bindItem: UserItem.User

    override fun bind(item: UserItem) {
        bindItem = item as UserItem.User
        val user = item.user

        with(binding) {
            checkbox.isChecked = item.chosen

            with(layoutDetails) {
                root.background = null
                val userPresentableName = user.getPresentableName()
                avatar.setNameAndImageUrl(userPresentableName, user.avatarURL)
                userName.text = userPresentableName
                tvStatus.isVisible = user.presence?.state == PresenceState.Online || (user.presence?.lastActiveAt
                        ?: 0) > 0L

                tvStatus.text = if (user.presence?.state == PresenceState.Online) {
                    itemView.getString(com.sceyt.chatuikit.R.string.sceyt_online)
                } else {
                    if (user.presence?.lastActiveAt == 0L) {
                        ""
                    } else DateTimeUtil.getPresenceDateFormatData(itemView.context, Date(user.presence?.lastActiveAt
                            ?: 0))
                }
            }

            itemView.setOnClickListener {
                item.chosen = !item.chosen
                checkbox.isChecked = item.chosen
                itemClickListener.onClick(bindItem)
            }
        }
    }
}