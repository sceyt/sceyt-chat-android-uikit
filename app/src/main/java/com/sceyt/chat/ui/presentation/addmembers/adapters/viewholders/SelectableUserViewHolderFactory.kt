package com.sceyt.chat.ui.presentation.addmembers.adapters.viewholders

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.chat.ui.databinding.ItemSelectUserBinding
import com.sceyt.chat.ui.presentation.common.BaseViewHolder
import com.sceyt.chat.ui.presentation.addmembers.adapters.UserItem
import com.sceyt.chat.ui.presentation.addmembers.adapters.SelectableUsersAdapter
import com.sceyt.sceytchatuikit.databinding.SceytItemLoadingMoreBinding

class SelectableUserViewHolderFactory(context: Context, private val listeners: SelectableUsersAdapter.ClickListener) {

    private val layoutInflater = LayoutInflater.from(context)

    fun createViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<UserItem> {
        return when (viewType) {
            ItemViewType.User.ordinal -> {
                SelectableUserViewHolder(ItemSelectUserBinding.inflate(layoutInflater, parent, false),
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