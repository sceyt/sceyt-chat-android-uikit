package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.viewholders

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.sceytchatuikit.data.SceytSharedPreference
import com.sceyt.sceytchatuikit.databinding.ItemChannelMemberBinding
import com.sceyt.sceytchatuikit.databinding.SceytItemLoadingMoreBinding
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.MemberItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.diff.MemberItemPayloadDiff
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.listeners.MemberClickListeners
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.adapter.listeners.MemberClickListenersImpl
import org.koin.core.component.inject

open class ChannelMembersViewHolderFactory(context: Context) : SceytKoinComponent {
    private val layoutInflater = LayoutInflater.from(context)
    private val clickListeners = MemberClickListenersImpl()
    private val preferences by inject<SceytSharedPreference>()

    fun createViewHolder(parent: ViewGroup, viewType: Int): BaseMemberViewHolder {
        return when (viewType) {
            ItemType.Member.ordinal -> createMemberViewHolder(parent)
            ItemType.Loading.ordinal -> createLoadingMoreViewHolder(parent)
            else -> throw RuntimeException("Not supported view type")
        }
    }

    open fun createMemberViewHolder(parent: ViewGroup): BaseMemberViewHolder {
        val currentUserId = preferences.getUserId()
        return MemberViewHolder(ItemChannelMemberBinding.inflate(layoutInflater, parent, false), currentUserId,
            clickListeners)
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

    enum class ItemType {
        Member, Loading
    }
}