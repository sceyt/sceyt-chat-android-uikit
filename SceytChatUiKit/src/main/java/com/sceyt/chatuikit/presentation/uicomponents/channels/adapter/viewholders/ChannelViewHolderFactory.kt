package com.sceyt.chatuikit.presentation.uicomponents.channels.adapter.viewholders

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.databinding.SceytItemChannelBinding
import com.sceyt.chatuikit.databinding.SceytItemLoadingMoreBinding
import com.sceyt.chatuikit.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.chatuikit.presentation.uicomponents.channels.listeners.ChannelClickListeners
import com.sceyt.chatuikit.presentation.uicomponents.channels.listeners.ChannelClickListenersImpl
import com.sceyt.chatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.chatuikit.sceytstyles.ChannelListViewStyle

open class ChannelViewHolderFactory(context: Context) {
    protected val layoutInflater: LayoutInflater = LayoutInflater.from(context)
    protected open val channelClickListenersImpl = ChannelClickListenersImpl()
    protected lateinit var channelStyle: ChannelListViewStyle
    private var attachDetachListener: ((ChannelListItem?, Boolean) -> Unit)? = null
    var userNameBuilder: ((User) -> String)? = SceytKitConfig.userNameBuilder
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
        return ChannelViewHolder(binding, channelStyle, channelClickListenersImpl, attachDetachListener, userNameBuilder)
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

    fun setUserNameBuilder(builder: (User) -> String) {
        userNameBuilder = builder
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