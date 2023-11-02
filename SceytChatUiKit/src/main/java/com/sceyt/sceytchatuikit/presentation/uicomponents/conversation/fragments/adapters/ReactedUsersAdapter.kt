package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.fragments.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.databinding.SceytItemLoadingMoreBinding
import com.sceyt.sceytchatuikit.databinding.SceytItemReactedUserBinding
import com.sceyt.sceytchatuikit.extensions.getPresentableName
import com.sceyt.sceytchatuikit.presentation.root.BaseViewHolder
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.sceytstyles.UserStyle

class ReactedUsersAdapter(private var clickListener: OnItemClickListener) : ListAdapter<ReactedUserItem, BaseViewHolder<ReactedUserItem>>(DIFF_UTIL) {

    companion object {
        val DIFF_UTIL = object : DiffUtil.ItemCallback<ReactedUserItem>() {
            override fun areItemsTheSame(oldItem: ReactedUserItem, newItem: ReactedUserItem): Boolean {
                return when {
                    oldItem is ReactedUserItem.Item && newItem is ReactedUserItem.Item -> oldItem.reaction.id == newItem.reaction.id
                    oldItem is ReactedUserItem.LoadingMore && newItem is ReactedUserItem.LoadingMore -> true
                    else -> false
                }
            }

            override fun areContentsTheSame(oldItem: ReactedUserItem, newItem: ReactedUserItem): Boolean {
                return true
            }

            override fun getChangePayload(oldItem: ReactedUserItem, newItem: ReactedUserItem): Any {
                return Any()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<ReactedUserItem> {
        when (viewType) {
            ViewType.Loading.ordinal -> {
                val binding = SceytItemLoadingMoreBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return LoadingMoreViewHolder(binding)
            }
        }

        val binding = SceytItemReactedUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UsersViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BaseViewHolder<ReactedUserItem>, position: Int) {
        holder.bind(currentList[position])
    }

    override fun getItemViewType(position: Int): Int {
        return when (currentList[position]) {
            is ReactedUserItem.Item -> ViewType.Item.ordinal
            is ReactedUserItem.LoadingMore -> ViewType.Loading.ordinal
        }
    }

    override fun getItemCount(): Int {
        return currentList.size
    }

    inner class UsersViewHolder(val binding: SceytItemReactedUserBinding) : BaseViewHolder<ReactedUserItem>(binding.root) {

        override fun bind(item: ReactedUserItem) {
            with(binding) {
                val user: User? = (item as ReactedUserItem.Item).reaction.user
                val userPresentableName = user?.let { SceytKitConfig.userNameBuilder?.invoke(it) }
                        ?: user?.getPresentableName()
                avatar.setNameAndImageUrl(userPresentableName, user?.avatarURL, UserStyle.userDefaultAvatar)
                userName.text = userPresentableName

                reaction.setSmileText(item.reaction.key)

                root.setOnClickListener {
                    clickListener.onItemClick(item)
                }
            }
        }
    }

    inner class LoadingMoreViewHolder(val binding: SceytItemLoadingMoreBinding) : BaseViewHolder<ReactedUserItem>(binding.root) {
        override fun bind(item: ReactedUserItem) {}
    }

    fun interface OnItemClickListener {
        fun onItemClick(item: ReactedUserItem.Item)
    }

    fun getSkip(): Int {
        return ArrayList(currentList).apply { remove(ReactedUserItem.LoadingMore) }.size
    }

    enum class ViewType {
        Item, Loading
    }
}