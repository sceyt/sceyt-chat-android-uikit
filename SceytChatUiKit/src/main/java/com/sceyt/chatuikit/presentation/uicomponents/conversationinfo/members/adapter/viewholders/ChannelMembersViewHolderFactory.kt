package com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.members.adapter.viewholders

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemChannelMembersBinding
import com.sceyt.chatuikit.databinding.SceytItemLoadingMoreBinding
import com.sceyt.chatuikit.extensions.setProgressColor
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.members.adapter.MemberItem
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.members.adapter.diff.MemberItemPayloadDiff
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.members.adapter.listeners.MemberClickListeners
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.members.adapter.listeners.MemberClickListenersImpl
import com.sceyt.chatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.chatuikit.sceytconfigs.UserNameFormatter

open class ChannelMembersViewHolderFactory(context: Context) {
    private val layoutInflater = LayoutInflater.from(context)
    private val clickListeners = MemberClickListenersImpl()
    private var userNameFormatter: UserNameFormatter? = SceytKitConfig.userNameFormatter

    fun createViewHolder(parent: ViewGroup, viewType: Int): BaseMemberViewHolder {
        return when (viewType) {
            ItemType.Member.ordinal -> createMemberViewHolder(parent)
            ItemType.Loading.ordinal -> createLoadingMoreViewHolder(parent)
            else -> throw RuntimeException("Not supported view type")
        }
    }

    open fun createMemberViewHolder(parent: ViewGroup): BaseMemberViewHolder {
        return MemberViewHolder(SceytItemChannelMembersBinding.inflate(layoutInflater, parent, false),
            clickListeners, userNameFormatter)
    }

    open fun createLoadingMoreViewHolder(parent: ViewGroup): BaseMemberViewHolder {
        val binding = SceytItemLoadingMoreBinding.inflate(layoutInflater, parent, false)
        return object : BaseMemberViewHolder(binding.root) {

            override fun bind(item: MemberItem, diff: MemberItemPayloadDiff) {
                binding.applyStyle()
            }

            private fun SceytItemLoadingMoreBinding.applyStyle() {
                adapterListLoadingProgressBar.setProgressColor(SceytChatUIKit.theme.accentColor)
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

    fun setUserNameFormatter(formatter: UserNameFormatter) {
        userNameFormatter = formatter
    }

    enum class ItemType {
        Member, Loading
    }
}