package com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.holders

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemChannelBinding
import com.sceyt.chatuikit.databinding.SceytItemLoadingMoreBinding
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItem
import com.sceyt.chatuikit.presentation.components.channel_list.channels.listeners.click.ChannelClickListeners
import com.sceyt.chatuikit.presentation.components.channel_list.channels.listeners.click.ChannelClickListenersImpl
import com.sceyt.chatuikit.formatters.UserNameFormatter
import com.sceyt.chatuikit.styles.ChannelListViewStyle

open class ChannelViewHolderFactory(context: Context) {
    protected val layoutInflater: LayoutInflater = LayoutInflater.from(context)
    protected open val channelClickListenersImpl = ChannelClickListenersImpl()
    protected lateinit var channelStyle: ChannelListViewStyle
    private var attachDetachListener: ((ChannelListItem?, Boolean) -> Unit)? = null
    var userNameFormatter: UserNameFormatter? = SceytChatUIKit.formatters.userNameFormatter
        private set

    internal fun setStyle(channelStyle: ChannelListViewStyle) {
        this.channelStyle = channelStyle
    }

    open fun createViewHolder(parent: ViewGroup, viewType: Int): BaseChannelViewHolder {
        return when (viewType) {
            ChannelType.Default.ordinal -> createChannelViewHolder(parent)
            ChannelType.Loading.ordinal -> createLoadingMoreViewHolder(parent)
            else -> throw RuntimeException("Not supported view type")
        }
    }

    open fun createChannelViewHolder(parent: ViewGroup): BaseChannelViewHolder {
        val binding = SceytItemChannelBinding.inflate(layoutInflater, parent, false)
        return ChannelViewHolder(binding, channelStyle.itemStyle,
            channelClickListenersImpl, attachDetachListener, userNameFormatter)
    }

    open fun createLoadingMoreViewHolder(parent: ViewGroup): BaseChannelViewHolder {
        val binding = SceytItemLoadingMoreBinding.inflate(layoutInflater, parent, false)
        return ChannelLoadingMoreViewHolder(binding)
    }

    fun setChannelListener(listener: ChannelClickListeners) {
        channelClickListenersImpl.setListener(listener)
    }

    fun setChannelAttachDetachListener(listener: (ChannelListItem?, attached: Boolean) -> Unit) {
        attachDetachListener = listener
    }

    fun setUserNameFormatter(formatter: UserNameFormatter) {
        userNameFormatter = formatter
    }

    protected val clickListeners get() = channelClickListenersImpl as ChannelClickListeners.ClickListeners

    protected fun getAttachDetachListener() = attachDetachListener

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