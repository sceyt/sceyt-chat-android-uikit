package com.sceyt.chat.demo.presentation.newchannel.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.chat.demo.databinding.ItemUserBinding
import com.sceyt.chat.demo.presentation.common.BaseViewHolder
import com.sceyt.chat.demo.presentation.addmembers.adapters.UserItem
import com.sceyt.chatuikit.databinding.SceytItemLoadingMoreBinding

class UserViewHolderFactory (context: Context, private val listeners: UsersAdapter.ClickListener) {

    private val layoutInflater = LayoutInflater.from(context)

    fun createViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<UserItem> {
        return when (viewType) {
            ItemViewType.User.ordinal -> {
                UserViewHolder(ItemUserBinding.inflate(layoutInflater, parent, false),
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