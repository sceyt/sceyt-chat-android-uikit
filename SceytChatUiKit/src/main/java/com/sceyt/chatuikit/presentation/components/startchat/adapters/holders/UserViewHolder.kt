package com.sceyt.chatuikit.presentation.components.startchat.adapters.holders

import androidx.core.view.isVisible
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chatuikit.R.drawable
import com.sceyt.chatuikit.R.string
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemUserBinding
import com.sceyt.chatuikit.extensions.getPresentableName
import com.sceyt.chatuikit.extensions.getString
import com.sceyt.chatuikit.extensions.setTextColorRes
import com.sceyt.chatuikit.presentation.root.BaseViewHolder
import com.sceyt.chatuikit.presentation.components.select_users.adapters.UserItem
import com.sceyt.chatuikit.presentation.components.startchat.adapters.UsersAdapter
import com.sceyt.chatuikit.shared.utils.DateTimeUtil
import java.util.Date

class UserViewHolder(private val binding: SceytItemUserBinding,
                     private val itemClickListener: UsersAdapter.ClickListener) : BaseViewHolder<UserItem>(binding.root) {
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
                avatar.setAvatarColorRes(SceytChatUIKit.theme.accentColor)
                avatar.setNameAndImageUrl("", null, drawable.sceyt_ic_notes)
                userName.text = context.getString(string.sceyt_self_notes)
                tvStatus.isVisible = false
            } else {
                val userPresentableName = user.getPresentableName()
                avatar.setAvatarColor(0)
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
        }
    }

    private fun SceytItemUserBinding.applyStyle() {
        userName.setTextColorRes(SceytChatUIKit.theme.textPrimaryColor)
        tvStatus.setTextColorRes(SceytChatUIKit.theme.textSecondaryColor)
    }
}