package com.sceyt.chatuikit.presentation.components.startchat.adapters.holders

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.chatuikit.databinding.SceytItemLoadingMoreBinding
import com.sceyt.chatuikit.databinding.SceytItemUserBinding
import com.sceyt.chatuikit.formatters.UserFormatter
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.holders.LoadingMoreViewHolder
import com.sceyt.chatuikit.presentation.components.select_users.adapters.UserItem
import com.sceyt.chatuikit.presentation.components.startchat.adapters.UsersAdapter
import com.sceyt.chatuikit.presentation.root.BaseViewHolder
import com.sceyt.chatuikit.renderers.UserAvatarRenderer
import com.sceyt.chatuikit.styles.common.ListItemStyle

class UserViewHolderFactory(
        context: Context,
        private val style: ListItemStyle<UserFormatter, UserFormatter, UserAvatarRenderer>,
        private val listeners: UsersAdapter.ClickListener,
) {

    private val layoutInflater = LayoutInflater.from(context)

    fun createViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<UserItem> {
        return when (viewType) {
            ItemViewType.User.ordinal -> {
                UserViewHolder(
                    binding = SceytItemUserBinding.inflate(layoutInflater, parent, false),
                    style = style,
                    itemClickListener = listeners
                )
            }

            ItemViewType.Loading.ordinal -> {
                val binding = SceytItemLoadingMoreBinding.inflate(layoutInflater, parent, false)
                return LoadingMoreViewHolder(binding)
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