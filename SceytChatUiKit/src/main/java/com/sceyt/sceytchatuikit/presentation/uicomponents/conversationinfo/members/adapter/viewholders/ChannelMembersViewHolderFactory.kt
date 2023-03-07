package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.viewholders

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.databinding.SceytItemChannelMembersBinding
import com.sceyt.sceytchatuikit.databinding.SceytItemLoadingMoreBinding
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.MemberItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.diff.MemberItemPayloadDiff
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.listeners.MemberClickListeners
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.listeners.MemberClickListenersImpl
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

open class ChannelMembersViewHolderFactory(context: Context) {
    private val layoutInflater = LayoutInflater.from(context)
    private val clickListeners = MemberClickListenersImpl()
    private var userNameBuilder: ((User) -> String)? = SceytKitConfig.userNameBuilder

    fun createViewHolder(parent: ViewGroup, viewType: Int): BaseMemberViewHolder {
        return when (viewType) {
            ItemType.Member.ordinal -> createMemberViewHolder(parent)
            ItemType.Loading.ordinal -> createLoadingMoreViewHolder(parent)
            else -> throw RuntimeException("Not supported view type")
        }
    }

    open fun createMemberViewHolder(parent: ViewGroup): BaseMemberViewHolder {
        return MemberViewHolder(SceytItemChannelMembersBinding.inflate(layoutInflater, parent, false),
            clickListeners, userNameBuilder)
    }

    open fun createLoadingMoreViewHolder(parent: ViewGroup): BaseMemberViewHolder {
        val binding = SceytItemLoadingMoreBinding.inflate(layoutInflater, parent, false)
        return object : BaseMemberViewHolder(binding.root) {
            override fun bind(item: MemberItem, diff: MemberItemPayloadDiff) {}
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

    fun setUserNameBuilder(builder: (User) -> String) {
        userNameBuilder = builder
    }

    enum class ItemType {
        Member, Loading
    }
}