package com.sceyt.chatuikit.presentation.components.select_users.adapters.holders

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemLoadingMoreBinding
import com.sceyt.chatuikit.databinding.SceytItemSelectUserBinding
import com.sceyt.chatuikit.extensions.setProgressColor
import com.sceyt.chatuikit.presentation.components.select_users.adapters.SelectableUsersAdapter
import com.sceyt.chatuikit.presentation.components.select_users.adapters.UserItem
import com.sceyt.chatuikit.presentation.root.BaseViewHolder
import com.sceyt.chatuikit.styles.select_users.UsersListItemsStyle

class SelectableUserViewHolderFactory(
        context: Context,
        private val style: UsersListItemsStyle,
        private val listeners: SelectableUsersAdapter.ClickListener,
) {

    private val layoutInflater = LayoutInflater.from(context)

    fun createViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<UserItem> {
        return when (viewType) {
            ItemViewType.User.ordinal -> {
                SelectableUserViewHolder(SceytItemSelectUserBinding.inflate(layoutInflater, parent, false),
                    style, listeners)
            }

            ItemViewType.Loading.ordinal -> {
                val binding = SceytItemLoadingMoreBinding.inflate(layoutInflater, parent, false)
                return object : BaseViewHolder<UserItem>(binding.root) {
                    override fun bind(item: UserItem) {
                        binding.adapterListLoadingProgressBar.setProgressColor(SceytChatUIKit.theme.colors.accentColor)
                    }
                }
            }

            else -> throw RuntimeException("Not supported view type")
        }
    }

    fun getItemViewType(item: UserItem): Int {
        return when (item) {
            is UserItem.User -> ItemViewType.User.ordinal
            is UserItem.LoadingMore -> ItemViewType.Loading.ordinal
        }
    }

    enum class ItemViewType {
        User, Loading
    }
}