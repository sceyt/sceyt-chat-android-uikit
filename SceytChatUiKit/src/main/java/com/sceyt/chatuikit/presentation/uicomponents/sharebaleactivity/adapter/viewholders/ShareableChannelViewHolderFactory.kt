package com.sceyt.chatuikit.presentation.uicomponents.sharebaleactivity.adapter.viewholders

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.chatuikit.databinding.SceytItemShareChannelBinding
import com.sceyt.chatuikit.databinding.SceytItemLoadingMoreBinding
import com.sceyt.chatuikit.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.chatuikit.presentation.uicomponents.channels.adapter.viewholders.BaseChannelViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.channels.adapter.viewholders.ChannelLoadingMoreViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.channels.listeners.ChannelClickListeners
import com.sceyt.chatuikit.presentation.uicomponents.channels.listeners.ChannelClickListenersImpl
import com.sceyt.chatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.chatuikit.sceytconfigs.UserNameFormatter

open class ShareableChannelViewHolderFactory(context: Context) {
    protected val layoutInflater = LayoutInflater.from(context)
    protected val channelClickListenersImpl = ChannelClickListenersImpl()
    var userNameFormatter: UserNameFormatter? = SceytKitConfig.userNameFormatter
        private set

    open fun createViewHolder(parent: ViewGroup, viewType: Int): BaseChannelViewHolder {
        return when (viewType) {
            ChannelType.Default.ordinal -> createChannelViewHolder(parent)
            ChannelType.Loading.ordinal -> createLoadingMoreViewHolder(parent)
            else -> throw RuntimeException("Not supported view type")
        }
    }

    open fun createChannelViewHolder(parent: ViewGroup): BaseChannelViewHolder {
        val binding = SceytItemShareChannelBinding.inflate(layoutInflater, parent, false)
        return ShareableChannelViewHolder(binding, channelClickListenersImpl, userNameFormatter)
    }

    open fun createLoadingMoreViewHolder(parent: ViewGroup): BaseChannelViewHolder {
        val binding = SceytItemLoadingMoreBinding.inflate(layoutInflater, parent, false)
        return ChannelLoadingMoreViewHolder(binding)
    }

    fun setChannelClickListener(listener: ChannelClickListeners.ChannelClickListener) {
        channelClickListenersImpl.setListener(listener)
    }

    fun setUserNameFormatter(formatter: UserNameFormatter) {
        userNameFormatter = formatter
    }

    protected val clickListeners get() = channelClickListenersImpl as ChannelClickListeners.ClickListeners

    open fun getItemViewType(item: ChannelListItem, position: Int): Int {
        return when (item) {
            is ChannelListItem.ChannelItem -> ChannelType.Default.ordinal
            is ChannelListItem.LoadingMoreItem -> ChannelType.Loading.ordinal
        }
    }

    enum class ChannelType {
        Loading, Default
    }
}