package com.sceyt.chatuikit.presentation.components.select_users.adapters

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.extensions.findIndexed
import com.sceyt.chatuikit.persistence.extensions.toArrayList
import com.sceyt.chatuikit.presentation.root.BaseViewHolder
import com.sceyt.chatuikit.presentation.components.select_users.adapters.holders.SelectableUserViewHolderFactory
import com.sceyt.chatuikit.shared.utils.MyDiffUtil

class SelectableUsersAdapter(
        private var usersList: ArrayList<UserItem>,
        private val factory: SelectableUserViewHolderFactory,
) : RecyclerView.Adapter<BaseViewHolder<UserItem>>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<UserItem> {
        return factory.createViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: BaseViewHolder<UserItem>, position: Int) {
        holder.bind(usersList[position])
    }

    override fun getItemViewType(position: Int): Int {
        return factory.getItemViewType(usersList[position])
    }

    override fun getItemCount(): Int {
        return usersList.size
    }

    private fun removeLoading() {
        if (usersList.remove(UserItem.LoadingMore))
            notifyItemRemoved(usersList.lastIndex + 1)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addNewItems(data: List<UserItem>) {
        removeLoading()
        if (data.isEmpty()) return

        usersList.addAll(data)
        if (data.size == usersList.size)
            notifyDataSetChanged()
        else
            notifyItemRangeInserted(usersList.size - data.size, data.size)
    }

    fun uncheckItem(id: String) {
        usersList.findIndexed { it is UserItem.User && it.user.id == id }?.let {
            (it.second as UserItem.User).chosen = false
            notifyItemChanged(it.first, Unit)
        }
    }

    fun notifyUpdate(list: List<UserItem>) {
        val myDiffUtil = MyDiffUtil(usersList, list)
        val productDiffResult = DiffUtil.calculateDiff(myDiffUtil, true)
        productDiffResult.dispatchUpdatesTo(this)
        usersList = list.toArrayList()
    }

    fun interface ClickListener {
        fun onClick(user: UserItem.User)
    }
}