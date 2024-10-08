package com.sceyt.chatuikit.presentation.components.select_users.adapters.holders

import androidx.core.view.isVisible
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemSelectUserBinding
import com.sceyt.chatuikit.extensions.getPresentableName
import com.sceyt.chatuikit.extensions.setTextColorRes
import com.sceyt.chatuikit.presentation.components.select_users.adapters.SelectableUsersAdapter
import com.sceyt.chatuikit.presentation.components.select_users.adapters.UserItem
import com.sceyt.chatuikit.presentation.extensions.setUserAvatar
import com.sceyt.chatuikit.presentation.root.BaseViewHolder

class SelectableUserViewHolder(
        private val binding: SceytItemSelectUserBinding,
        private val itemClickListener: SelectableUsersAdapter.ClickListener
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
                val userPresentableName = user.getPresentableName()
                avatar.setUserAvatar(user)
                userName.text = userPresentableName
                val presence = SceytChatUIKit.formatters.userPresenceDateFormatter.format(context, user)
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
            userName.setTextColorRes(SceytChatUIKit.theme.colors.textPrimaryColor)
            tvStatus.setTextColorRes(SceytChatUIKit.theme.colors.textSecondaryColor)
        }
    }
}