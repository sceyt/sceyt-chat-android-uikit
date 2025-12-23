package com.sceyt.chatuikit.presentation.components.channel_info.groups.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.persistence.differs.diff
import com.sceyt.chatuikit.presentation.root.BaseViewHolder

class CommonGroupsAdapter(
    private val viewHolderFactory: CommonGroupViewHolderFactory
) : RecyclerView.Adapter<BaseViewHolder<CommonGroupListItem>>() {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<CommonGroupListItem>() {
            override fun areItemsTheSame(oldItem: CommonGroupListItem, newItem: CommonGroupListItem): Boolean {
                return when {
                    oldItem is CommonGroupListItem.GroupItem && newItem is CommonGroupListItem.GroupItem ->
                        oldItem.channel.id == newItem.channel.id
                    oldItem is CommonGroupListItem.LoadingMore && newItem is CommonGroupListItem.LoadingMore -> true
                    else -> false
                }
            }

            override fun areContentsTheSame(oldItem: CommonGroupListItem, newItem: CommonGroupListItem): Boolean {
                return when {
                    oldItem is CommonGroupListItem.GroupItem && newItem is CommonGroupListItem.GroupItem ->
                        !oldItem.channel.diff(newItem.channel).hasDifference()
                    oldItem is CommonGroupListItem.LoadingMore && newItem is CommonGroupListItem.LoadingMore -> true
                    else -> false
                }
            }
        }
    }

    private val differ = AsyncListDiffer(this, DIFF_CALLBACK)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<CommonGroupListItem> {
        return viewHolderFactory.createViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: BaseViewHolder<CommonGroupListItem>, position: Int) {
        holder.bind(differ.currentList[position])
    }

    override fun getItemCount() = differ.currentList.size

    override fun getItemViewType(position: Int): Int {
        return viewHolderFactory.getItemViewType(differ.currentList[position])
    }

    fun submitList(items: List<CommonGroupListItem>) {
        differ.submitList(items)
    }

    fun addItems(items: List<CommonGroupListItem>) {
        val newList = currentList.filterNot { it is CommonGroupListItem.LoadingMore } + items
        differ.submitList(newList)
    }

    val currentList get() = differ.currentList
}