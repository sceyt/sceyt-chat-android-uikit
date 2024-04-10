package com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.viewholders

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.databinding.SceytItemChannelBinding
import com.sceyt.sceytchatuikit.databinding.SceytItemLoadingMoreBinding
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.listeners.ChannelClickListeners
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.listeners.ChannelClickListenersImpl
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

open class ChannelViewHolderFactory(context: Context) {
    protected val layoutInflater: LayoutInflater = LayoutInflater.from(context)
    protected val channelClickListenersImpl = ChannelClickListenersImpl()
    private var attachDetachListener: ((ChannelListItem?, Boolean) -> Unit)? = null
    var userNameBuilder: ((User) -> String)? = SceytKitConfig.userNameBuilder
        private set

    open fun createViewHolder(parent: ViewGroup, viewType: Int): BaseChannelViewHolder {
        return when (viewType) {
            ChannelType.Default.ordinal -> createChannelViewHolder(parent)
            ChannelType.Loading.ordinal -> createLoadingMoreViewHolder(parent)
            else -> throw RuntimeException("Not supported view type")
        }
    }

    open fun createChannelViewHolder(parent: ViewGroup): BaseChannelViewHolder {
        val binding = SceytItemChannelBinding.inflate(layoutInflater, parent, false)
        return ChannelViewHolder(binding, channelClickListenersImpl, attachDetachListener, userNameBuilder)
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