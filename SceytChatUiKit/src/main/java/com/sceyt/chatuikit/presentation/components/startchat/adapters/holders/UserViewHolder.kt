package com.sceyt.chatuikit.presentation.components.startchat.adapters.holders

import androidx.core.view.isVisible
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.databinding.SceytItemUserBinding
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.presentation.components.select_users.adapters.UserItem
import com.sceyt.chatuikit.presentation.components.startchat.adapters.UsersAdapter
import com.sceyt.chatuikit.presentation.root.BaseViewHolder
import com.sceyt.chatuikit.renderers.AvatarRenderer
import com.sceyt.chatuikit.styles.common.ListItemStyle

class UserViewHolder(
        private val binding: SceytItemUserBinding,
        private val style: ListItemStyle<Formatter<SceytUser>, Formatter<SceytUser>, AvatarRenderer<SceytUser>>,
        private val itemClickListener: UsersAdapter.ClickListener,
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
            style.avatarRenderer.render(context, user, style.avatarStyle, avatar)
            userName.text = style.titleFormatter.format(context, user)
            val presence = style.subtitleFormatter.format(context, user)
            tvStatus.text = presence
            tvStatus.isVisible = presence.isNotEmpty() && user.id != SceytChatUIKit.chatUIFacade.myId
        }
    }

    private fun SceytItemUserBinding.applyStyle() {
        style.avatarStyle.apply(avatar)
        style.titleTextStyle.apply(userName)
        style.subtitleTextStyle.apply(tvStatus)
    }
}