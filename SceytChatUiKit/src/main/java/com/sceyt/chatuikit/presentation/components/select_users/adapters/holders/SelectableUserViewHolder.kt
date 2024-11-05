package com.sceyt.chatuikit.presentation.components.select_users.adapters.holders

import androidx.core.view.isVisible
import com.sceyt.chatuikit.databinding.SceytItemSelectUserBinding
import com.sceyt.chatuikit.presentation.components.select_users.adapters.SelectableUsersAdapter
import com.sceyt.chatuikit.presentation.components.select_users.adapters.UserItem
import com.sceyt.chatuikit.presentation.root.BaseViewHolder
import com.sceyt.chatuikit.styles.UsersListItemsStyle

class SelectableUserViewHolder(
        private val binding: SceytItemSelectUserBinding,
        private val style: UsersListItemsStyle,
        private val itemClickListener: SelectableUsersAdapter.ClickListener,
) : BaseViewHolder<UserItem>(binding.root) {
    private lateinit var bindItem: UserItem.User

    init {
        binding.applyStyle()
    }

    override fun bind(item: UserItem) {
        bindItem = item as UserItem.User
        val user = item.user

        with(binding) {
            checkbox.isChecked = item.chosen

            with(layoutDetails) {
                root.background = null
                style.avatarRenderer.render(context, user, style.avatarStyle, avatar)
                userName.text = style.titleFormatter.format(context, user)
                val presence = style.subtitleFormatter.format(context, user)
                tvStatus.isVisible = presence.isNotEmpty()
                tvStatus.text = presence
            }

            itemView.setOnClickListener {
                item.chosen = !item.chosen
                checkbox.isChecked = item.chosen
                itemClickListener.onClick(bindItem)
            }
        }
    }

    private fun SceytItemSelectUserBinding.applyStyle() {
        with(layoutDetails) {
            style.avatarStyle.apply(avatar)
            style.titleTextStyle.apply(userName)
            style.subtitleTextStyle.apply(tvStatus)
            style.checkboxStyle.apply(checkbox)
        }
    }
}