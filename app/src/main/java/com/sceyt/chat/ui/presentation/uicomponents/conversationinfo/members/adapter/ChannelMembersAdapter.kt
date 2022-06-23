package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.sceyt.chat.models.member.Member
import com.sceyt.chat.ui.databinding.ItemChannelMemberBinding

class ChannelMembersAdapter : ListAdapter<Member, MemberViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        return MemberViewHolder(ItemChannelMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false).apply {
            onlineStatus.isVisible = false
        })
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {

        holder.binding.onlineStatus.isVisible = position % 2 > 0
        // holder.bind(currentList[position])
    }

    override fun getItemCount(): Int {
        return 4
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Member>() {
            override fun areItemsTheSame(oldItem: Member, newItem: Member) =
                    oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Member, newItem: Member) =
                    oldItem.role.name == newItem.role.name
        }
    }
}