package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.extensions.findIndexed
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members.adapter.diff.MemberDiffUtil
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members.adapter.diff.MemberItemPayloadDiff
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members.viewmodel.BaseMemberViewHolder
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members.viewmodel.ChannelMembersViewHolderFactory

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

        members.addAll(items)
        if (members.size == items.size)
            notifyDataSetChanged()
        else
            notifyItemRangeInserted(members.size - items.size, items.size)
    }

    fun addNewItemsToStart(items: List<MemberItem>?) {
        if (items.isNullOrEmpty()) return

        members.addAll(0, items)
        notifyItemRangeInserted(0, items.size)
    }

    fun getMembers(): List<MemberItem.Member> = members.filterIsInstance<MemberItem.Member>()

    fun getData() = members

    fun getMemberItemById(memberId: String) = members.findIndexed { it is MemberItem.Member && it.member.id == memberId }

    fun getMemberItemByRole(role: String) = members.findIndexed { it is MemberItem.Member && it.member.role.name == role }

    fun showHideMoreItem(show: Boolean) {
        if (show == showMoreIcon) return
        val myDiffUtil = MemberDiffUtil(members, members, true)
        val productDiffResult = DiffUtil.calculateDiff(myDiffUtil, true)
        showMoreIcon = show
        productDiffResult.dispatchUpdatesTo(this)
    }
}