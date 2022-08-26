package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter

import android.util.Log
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.extensions.findIndexed
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.diff.MemberDiffUtil
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.diff.MemberItemPayloadDiff
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.viewholders.BaseMemberViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.viewholders.ChannelMembersViewHolderFactory

class ChannelMembersAdapter(
        private var members: ArrayList<MemberItem>,
        var showMoreIcon: Boolean,
        private val viewHolderFactory: ChannelMembersViewHolderFactory) : RecyclerView.Adapter<BaseMemberViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseMemberViewHolder {
        return viewHolderFactory.createViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: BaseMemberViewHolder, position: Int) {
        holder.bind(members[position], diff = MemberItemPayloadDiff.DEFAULT)
    }

    override fun onBindViewHolder(holder: BaseMemberViewHolder, position: Int, payloads: MutableList<Any>) {
        val diff = payloads.find { it is MemberItemPayloadDiff } as? MemberItemPayloadDiff
                ?: MemberItemPayloadDiff.DEFAULT
        holder.bind(item = members[position], diff)
    }

    override fun getItemCount(): Int {
        return members.size
    }

    override fun getItemViewType(position: Int): Int {
        return viewHolderFactory.getItemViewType(members[position])
    }

    private fun removeLoading() {
        if (members.removeIf { it is MemberItem.LoadingMore }) {
            notifyItemRemoved(members.lastIndex + 1)
        }
    }

    fun addNewItems(items: List<MemberItem>) {
        removeLoading()
        if (items.isEmpty()) return

        val filteredItems = items.minus(ArrayList(members).toSet())
        members.addAll(filteredItems)

        if (members.size == items.size)
            notifyDataSetChanged()
        else
            notifyItemRangeInserted(members.size - filteredItems.size, filteredItems.size)
    }

    fun addNewItemsToStart(items: List<MemberItem>?) {
        if (items.isNullOrEmpty()) return

        members.addAll(0, items)
        notifyItemRangeInserted(0, items.size)
    }

    fun notifyUpdate(data: List<MemberItem>, showMoreIconChanged: Boolean = false) {
        try {
            val myDiffUtil = MemberDiffUtil(members, data, showMoreIconChanged)
            val productDiffResult = DiffUtil.calculateDiff(myDiffUtil, true)
            members = data as ArrayList
            productDiffResult.dispatchUpdatesTo(this)
        } catch (ex: java.lang.IllegalStateException) {
            Log.e(TAG, ex.message.toString())
        }
    }

    fun getMembers(): List<MemberItem.Member> = members.filterIsInstance<MemberItem.Member>()

    fun getData() = members

    fun getSkip() = members.filterIsInstance<MemberItem.Member>().size

    fun getMemberItemById(memberId: String) = members.findIndexed { it is MemberItem.Member && it.member.id == memberId }

    fun getMemberItemByRole(role: String) = members.findIndexed { it is MemberItem.Member && it.member.role.name == role }

    fun showHideMoreIcon(show: Boolean) {
        if (show == showMoreIcon) return
        notifyUpdate(members, true)
        showMoreIcon = show
    }
}