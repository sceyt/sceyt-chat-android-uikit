package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.adapters.metions

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.persistence.extensions.toArrayList
import com.sceyt.sceytchatuikit.presentation.root.BaseViewHolder
import com.sceyt.sceytchatuikit.shared.utils.MyDiffUtil

class UsersAdapter(
        private var usersList: ArrayList<SceytMember>,
        private val factory: UserViewHolderFactory,
) : RecyclerView.Adapter<BaseViewHolder<SceytMember>>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<SceytMember> {
        return factory.createViewHolder(parent, viewType)
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