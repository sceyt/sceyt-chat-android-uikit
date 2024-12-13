package com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.holders

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.chatuikit.databinding.SceytItemChannelBinding
import com.sceyt.chatuikit.databinding.SceytItemLoadingMoreBinding
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItem
import com.sceyt.chatuikit.presentation.components.channel_list.channels.listeners.click.ChannelClickListeners
import com.sceyt.chatuikit.presentation.components.channel_list.channels.listeners.click.ChannelClickListenersImpl
import com.sceyt.chatuikit.styles.ChannelListViewStyle

open class ChannelViewHolderFactory(context: Context) {
    protected val layoutInflater: LayoutInflater = LayoutInflater.from(context)
    protected open val clickListeners = ChannelClickListenersImpl()
    protected lateinit var channelStyle: ChannelListViewStyle
    private var attachDetachListener: ((ChannelListItem?, Boolean) -> Unit)? = null

    internal fun setStyle(channelStyle: ChannelListViewStyle) {
        this.channelStyle = channelStyle
    }

    open fun createViewHolder(parent: ViewGroup, viewType: Int): BaseChannelViewHolder {
        return when (viewType) {
            ChannelType.Channel.ordinal -> createChannelViewHolder(parent)
            ChannelType.Loading.ordinal -> createLoadingMoreViewHolder(parent)
            else -> throw RuntimeException("Not supported view type")
        }
    }

    open fun createChannelViewHolder(parent: ViewGroup): BaseChannelViewHolder {
        val binding = SceytItemChannelBinding.inflate(layoutInflater, parent, false)
        return ChannelViewHolder(binding, channelStyle.itemStyle,
            clickListeners, attachDetachListener)
    }

    open fun createLoadingMoreViewHolder(parent: ViewGroup): BaseChannelViewHolder {
        val binding = SceytItemLoadingMoreBinding.inflate(layoutInflater, parent, false)
        return ChannelLoadingMoreViewHolder(binding)
    }

    fun setChannelListener(listener: ChannelClickListeners) {
        clickListeners.setListener(listener)
    }

    fun setChannelAttachDetachListener(listener: (ChannelListItem?, attached: Boolean) -> Unit) {
        attachDetachListener = listener
    }

    protected fun getAttachDetachListener() = attachDetachListener

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