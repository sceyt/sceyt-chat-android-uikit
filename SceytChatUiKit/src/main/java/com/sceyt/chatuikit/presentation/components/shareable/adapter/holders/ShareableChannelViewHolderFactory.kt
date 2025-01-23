package com.sceyt.chatuikit.presentation.components.shareable.adapter.holders

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.chatuikit.databinding.SceytItemLoadingMoreBinding
import com.sceyt.chatuikit.databinding.SceytItemShareChannelBinding
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItem
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.holders.BaseChannelViewHolder
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.holders.ChannelLoadingMoreViewHolder
import com.sceyt.chatuikit.presentation.components.channel_list.channels.listeners.click.ChannelClickListeners
import com.sceyt.chatuikit.presentation.components.channel_list.channels.listeners.click.ChannelClickListenersImpl
import com.sceyt.chatuikit.styles.ShareablePageStyle

@Suppress("MemberVisibilityCanBePrivate")
open class ShareableChannelViewHolderFactory(
        context: Context,
        private val style: ShareablePageStyle
) {
    protected val layoutInflater: LayoutInflater = LayoutInflater.from(context)
    protected val clickListeners = ChannelClickListenersImpl()

    open fun createViewHolder(parent: ViewGroup, viewType: Int): BaseChannelViewHolder {
        return when (viewType) {
            ChannelType.Channel.ordinal -> createChannelViewHolder(parent)
            ChannelType.Loading.ordinal -> createLoadingMoreViewHolder(parent)
            else -> throw RuntimeException("Not supported view type")
        }
    }

    open fun createChannelViewHolder(parent: ViewGroup): BaseChannelViewHolder {
        val binding = SceytItemShareChannelBinding.inflate(layoutInflater, parent, false)
        return ShareableChannelViewHolder(binding, style.channelItemStyle, clickListeners)
    }

    open fun createLoadingMoreViewHolder(parent: ViewGroup): BaseChannelViewHolder {
        val binding = SceytItemLoadingMoreBinding.inflate(layoutInflater, parent, false)
        return ChannelLoadingMoreViewHolder(binding)
    }

    fun setChannelClickListener(listener: ChannelClickListeners.ChannelClickListener) {
        clickListeners.setListener(listener)
    }

    open fun getItemViewType(item: ChannelListItem, position: Int): Int {
        return when (item) {
            is ChannelListItem.ChannelItem -> ChannelType.Channel.ordinal
            is ChannelListItem.LoadingMoreItem -> ChannelType.Loading.ordinal
        }
    }

    enum class ChannelType {
        Loading, Channel
    }
}