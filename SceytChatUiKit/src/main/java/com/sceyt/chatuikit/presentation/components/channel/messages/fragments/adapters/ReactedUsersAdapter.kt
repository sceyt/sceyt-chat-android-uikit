package com.sceyt.chatuikit.presentation.components.channel.messages.fragments.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.sceyt.chatuikit.presentation.root.BaseViewHolder

class ReactedUsersAdapter(
        private val factory: ReactedUserViewHolderFactory
) : ListAdapter<ReactedUserItem, BaseViewHolder<ReactedUserItem>>(DIFF_UTIL) {

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
        return factory.onCreateViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: BaseViewHolder<ReactedUserItem>, position: Int) {
        holder.bind(currentList[position])
    }

    override fun getItemViewType(position: Int): Int {
        return factory.getItemViewType(currentList[position])
    }

    override fun getItemCount(): Int {
        return currentList.size
    }

    fun getSkip(): Int {
        return currentList.filterIsInstance<ReactedUserItem.Item>().size
    }
}