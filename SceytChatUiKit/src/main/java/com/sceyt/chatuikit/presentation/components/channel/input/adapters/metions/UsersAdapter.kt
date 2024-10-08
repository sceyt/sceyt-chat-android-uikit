package com.sceyt.chatuikit.presentation.components.channel.input.adapters.metions

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.persistence.extensions.toArrayList
import com.sceyt.chatuikit.presentation.root.BaseViewHolder
import com.sceyt.chatuikit.shared.utils.MyDiffUtil

class UsersAdapter(
        private var usersList: ArrayList<SceytMember>,
        private val factory: MentionUserViewHolderFactory,
) : RecyclerView.Adapter<BaseViewHolder<SceytMember>>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<SceytMember> {
        return factory.createViewHolder(parent)
    }

    override fun onBindViewHolder(holder: BaseViewHolder<SceytMember>, position: Int) {
        holder.bind(usersList[position])
    }

    override fun getItemCount(): Int {
        return usersList.size
    }

    fun notifyUpdate(list: List<SceytMember>) {
        val myDiffUtil = MyDiffUtil(usersList, list)
        val productDiffResult = DiffUtil.calculateDiff(myDiffUtil, true)
        productDiffResult.dispatchUpdatesTo(this)
        usersList = list.toArrayList()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clear() {
        usersList = arrayListOf()
        notifyDataSetChanged()
    }

    fun interface ClickListener {
        fun onClick(user: SceytMember)
    }
}