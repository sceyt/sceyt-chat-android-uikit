package com.sceyt.sceytchatuikit.presentation.uicomponents.sharebaleactivity.adapter.viewholders

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.databinding.SceytItemShareChannelBinding
import com.sceyt.sceytchatuikit.databinding.SceytItemLoadingMoreBinding
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.viewholders.BaseChannelViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.viewholders.ChannelLoadingMoreViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.listeners.ChannelClickListeners
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.listeners.ChannelClickListenersImpl
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

open class ShareableChannelViewHolderFactory(context: Context) {
    protected val layoutInflater = LayoutInflater.from(context)
    protected val channelClickListenersImpl = ChannelClickListenersImpl()
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
        val binding = SceytItemShareChannelBinding.inflate(layoutInflater, parent, false)
        return ShareableChannelViewHolder(binding, channelClickListenersImpl, userNameBuilder)
    }

    open fun createLoadingMoreViewHolder(parent: ViewGroup): BaseChannelViewHolder {
        val binding = SceytItemLoadingMoreBinding.inflate(layoutInflater, parent, false)
        return ChannelLoadingMoreViewHolder(binding)
    }

    fun setChannelClickListener(listener: ChannelClickListeners.ChannelClickListener) {
        channelClickListenersImpl.setListener(listener)
    }

    fun setUserNameBuilder(builder: (User) -> String) {
        userNameBuilder = builder
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