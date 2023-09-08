package com.sceyt.chat.demo.presentation.newchannel.adapters

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.demo.presentation.addmembers.adapters.UserItem
import com.sceyt.chat.demo.presentation.common.BaseViewHolder
import com.sceyt.sceytchatuikit.persistence.extensions.toArrayList
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