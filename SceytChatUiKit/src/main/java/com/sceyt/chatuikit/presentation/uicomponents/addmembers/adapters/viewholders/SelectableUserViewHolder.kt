package com.sceyt.chatuikit.presentation.uicomponents.addmembers.adapters.viewholders

import androidx.core.view.isVisible
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemSelectUserBinding
import com.sceyt.chatuikit.extensions.getPresentableName
import com.sceyt.chatuikit.extensions.getString
import com.sceyt.chatuikit.extensions.setTextColorRes
import com.sceyt.chatuikit.presentation.root.BaseViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.addmembers.adapters.SelectableUsersAdapter
import com.sceyt.chatuikit.presentation.uicomponents.addmembers.adapters.UserItem
import com.sceyt.chatuikit.shared.utils.DateTimeUtil
import java.util.Date

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

    private fun SceytItemSelectUserBinding.applyStyle() {
        with(layoutDetails) {
            userName.setTextColorRes(SceytChatUIKit.theme.textPrimaryColor)
            tvStatus.setTextColorRes(SceytChatUIKit.theme.textSecondaryColor)
        }
    }
}