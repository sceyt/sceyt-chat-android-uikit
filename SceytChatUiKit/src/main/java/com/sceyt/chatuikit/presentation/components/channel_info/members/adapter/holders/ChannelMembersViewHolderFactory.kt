package com.sceyt.chatuikit.presentation.components.channel_info.members.adapter.holders

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemChannelMembersBinding
import com.sceyt.chatuikit.databinding.SceytItemLoadingMoreBinding
import com.sceyt.chatuikit.extensions.setProgressColor
import com.sceyt.chatuikit.presentation.components.channel_info.members.adapter.MemberItem
import com.sceyt.chatuikit.presentation.components.channel_info.members.adapter.diff.MemberItemPayloadDiff
import com.sceyt.chatuikit.presentation.components.channel_info.members.adapter.listeners.MemberClickListeners
import com.sceyt.chatuikit.presentation.components.channel_info.members.adapter.listeners.MemberClickListenersImpl
import com.sceyt.chatuikit.styles.channel_members.ChannelMemberListItemStyle

open class ChannelMembersViewHolderFactory(
        context: Context,
        private val style: ChannelMemberListItemStyle
) {
    private val layoutInflater = LayoutInflater.from(context)
    private val clickListeners = MemberClickListenersImpl()

    fun createViewHolder(parent: ViewGroup, viewType: Int): BaseMemberViewHolder {
        return when (viewType) {
            ItemType.Member.ordinal -> createMemberViewHolder(parent)
            ItemType.Loading.ordinal -> createLoadingMoreViewHolder(parent)
            else -> throw RuntimeException("Not supported view type")
        }
    }

    open fun createMemberViewHolder(parent: ViewGroup): BaseMemberViewHolder {
        return MemberViewHolder(
            SceytItemChannelMembersBinding.inflate(layoutInflater, parent, false),
            style, clickListeners)
    }

    open fun createLoadingMoreViewHolder(parent: ViewGroup): BaseMemberViewHolder {
        val binding = SceytItemLoadingMoreBinding.inflate(layoutInflater, parent, false)
        return object : BaseMemberViewHolder(binding.root) {

            override fun bind(item: MemberItem, diff: MemberItemPayloadDiff) {
                binding.applyStyle()
            }

            private fun SceytItemLoadingMoreBinding.applyStyle() {
                adapterListLoadingProgressBar.setProgressColor(SceytChatUIKit.theme.colors.accentColor)
            }
        }
    }

    open fun getItemViewType(item: MemberItem): Int {
        return when (item) {
            is MemberItem.Member -> ItemType.Member.ordinal
            is MemberItem.LoadingMore -> ItemType.Loading.ordinal
        }
    }

    fun setOnClickListener(listeners: MemberClickListeners) {
        clickListeners.setListener(listeners)
    }

    enum class ItemType {
        Member, Loading
    }
}