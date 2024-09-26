package com.sceyt.chatuikit.presentation.components.startchat.adapters.holders

import androidx.core.view.isVisible
import com.sceyt.chatuikit.R.drawable
import com.sceyt.chatuikit.R.string
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemUserBinding
import com.sceyt.chatuikit.extensions.getPresentableName
import com.sceyt.chatuikit.extensions.setTextColorRes
import com.sceyt.chatuikit.presentation.components.select_users.adapters.UserItem
import com.sceyt.chatuikit.presentation.components.startchat.adapters.UsersAdapter
import com.sceyt.chatuikit.presentation.extensions.setUserAvatar
import com.sceyt.chatuikit.presentation.root.BaseViewHolder

class UserViewHolder(
        private val binding: SceytItemUserBinding,
        private val itemClickListener: UsersAdapter.ClickListener
) : BaseViewHolder<UserItem>(binding.root) {
    private lateinit var bindItem: UserItem.User

    init {
        binding.applyStyle()

        itemView.setOnClickListener {
            itemClickListener.onClick(bindItem)
        }
    }

    override fun bind(item: UserItem) {
        bindItem = item as UserItem.User
        val user = item.user

        with(binding) {
            if (user.id == SceytChatUIKit.chatUIFacade.myId) {
                avatar.styleBuilder()
                    .setImageUrl(null)
                    .setAvatarBackgroundColorRes(SceytChatUIKit.theme.accentColor)
                    .setDefaultAvatar(drawable.sceyt_ic_notes)
                    .build()
                userName.text = context.getString(string.sceyt_self_notes)
                tvStatus.isVisible = false
            } else {
                val userPresentableName = user.getPresentableName()
                avatar.setUserAvatar(user)
                userName.text = userPresentableName

                val presence = SceytChatUIKit.formatters.userPresenceDateFormatter.format(context, user)
                tvStatus.isVisible = presence.isNotEmpty()
                tvStatus.text = presence
            }
        }
    }

    private fun SceytItemUserBinding.applyStyle() {
        userName.setTextColorRes(SceytChatUIKit.theme.textPrimaryColor)
        tvStatus.setTextColorRes(SceytChatUIKit.theme.textSecondaryColor)
    }
}