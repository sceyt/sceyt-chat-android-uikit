package com.sceyt.chat.ui.presentation.newchannel.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.presentation.common.BaseViewHolder
import com.sceyt.chat.ui.presentation.addmembers.adapters.UserItem
import com.sceyt.sceytchatuikit.shared.utils.MyDiffUtil

class UsersAdapter(
        private var usersList: ArrayList<UserItem>,
        private val factory: UserViewHolderFactory,
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
        if (usersList.removeIf { it is UserItem.LoadingMore })
            notifyItemRemoved(usersList.lastIndex + 1)
    }

    fun addNewItems(data: List<UserItem>) {
        removeLoading()
        if (data.isEmpty()) return

        usersList.addAll(data)
        if (data.size == usersList.size)
            notifyDataSetChanged()
        else
            notifyItemRangeInserted(usersList.size - data.size, data.size)
    }

    fun notifyUpdate(list: List<UserItem>) {
        val myDiffUtil = MyDiffUtil(usersList, list)
        val productDiffResult = DiffUtil.calculateDiff(myDiffUtil, true)
        usersList = list as ArrayList<UserItem>
        productDiffResult.dispatchUpdatesTo(this)
    }

    fun interface ClickListener {
        fun onClick(user: UserItem.User)
    }
}