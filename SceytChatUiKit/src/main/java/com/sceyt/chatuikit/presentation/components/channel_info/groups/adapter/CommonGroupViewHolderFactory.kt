package com.sceyt.chatuikit.presentation.components.channel_info.groups.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.chatuikit.databinding.SceytItemCommonGroupBinding
import com.sceyt.chatuikit.databinding.SceytItemLoadingMoreBinding
import com.sceyt.chatuikit.presentation.components.channel_info.groups.adapter.listeners.CommonGroupClickListeners
import com.sceyt.chatuikit.presentation.components.channel_info.groups.adapter.listeners.CommonGroupClickListenersImpl
import com.sceyt.chatuikit.presentation.components.channel_info.groups.adapter.viewholders.CommonGroupViewHolder
import com.sceyt.chatuikit.presentation.components.channel_info.groups.adapter.viewholders.LoadingMoreViewHolder
import com.sceyt.chatuikit.presentation.root.BaseViewHolder
import com.sceyt.chatuikit.styles.channel_info.common_groups.ChannelInfoCommonGroupsStyle

open class CommonGroupViewHolderFactory(
    context: Context,
    private val style: ChannelInfoCommonGroupsStyle
) {
    protected val layoutInflater: LayoutInflater = LayoutInflater.from(context)
    protected open val clickListeners = CommonGroupClickListenersImpl()

    open fun createViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<CommonGroupListItem> {
        return when (viewType) {
            ItemType.Group.ordinal -> createGroupViewHolder(parent)
            ItemType.Loading.ordinal -> createLoadingMoreViewHolder(parent)
            else -> throw RuntimeException("Not supported view type: $viewType")
        }
    }

    open fun createGroupViewHolder(parent: ViewGroup): BaseViewHolder<CommonGroupListItem> {
        val binding = SceytItemCommonGroupBinding.inflate(layoutInflater, parent, false)
        return CommonGroupViewHolder(
            binding = binding,
            style = style.itemStyle,
            clickListeners = clickListeners
        )
    }

    open fun createLoadingMoreViewHolder(parent: ViewGroup): BaseViewHolder<CommonGroupListItem> {
        val binding = SceytItemLoadingMoreBinding.inflate(layoutInflater, parent, false)
        return LoadingMoreViewHolder(
            binding = binding,
            loadMoreProgressColor = style.loadMoreProgressColor
        )
    }

    fun setOnClickListener(listener: CommonGroupClickListeners.ClickListener) {
        clickListeners.setListener(listener)
    }

    open fun getItemViewType(item: CommonGroupListItem): Int {
        return when (item) {
            is CommonGroupListItem.GroupItem -> ItemType.Group.ordinal
            is CommonGroupListItem.LoadingMore -> ItemType.Loading.ordinal
        }
    }

    enum class ItemType {
        Loading, Group
    }
}