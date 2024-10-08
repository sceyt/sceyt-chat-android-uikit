package com.sceyt.chatuikit.presentation.components.select_users.adapters.holders

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.chatuikit.databinding.SceytItemLoadingMoreBinding
import com.sceyt.chatuikit.databinding.SceytItemSelectUserBinding
import com.sceyt.chatuikit.presentation.root.BaseViewHolder
import com.sceyt.chatuikit.presentation.components.select_users.adapters.SelectableUsersAdapter
import com.sceyt.chatuikit.presentation.components.select_users.adapters.UserItem

class SelectableUserViewHolderFactory(context: Context, private val listeners: SelectableUsersAdapter.ClickListener) {

    private val layoutInflater = LayoutInflater.from(context)

    fun createViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<UserItem> {
        return when (viewType) {
            ItemViewType.User.ordinal -> {
                SelectableUserViewHolder(SceytItemSelectUserBinding.inflate(layoutInflater, parent, false),
                    listeners)
            }
            ItemViewType.Loading.ordinal -> {
                return object : BaseViewHolder<UserItem>(SceytItemLoadingMoreBinding.inflate(layoutInflater, parent, false).root) {
                    override fun bind(item: UserItem) {}
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