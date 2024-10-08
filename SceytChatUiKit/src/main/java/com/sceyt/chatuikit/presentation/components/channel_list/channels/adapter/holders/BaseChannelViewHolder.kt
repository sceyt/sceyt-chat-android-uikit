package com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.holders

import android.content.Context
import android.view.View
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.persistence.differs.ChannelDiff
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItem

abstract class BaseChannelViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    protected lateinit var channelItem: ChannelListItem
    protected val context: Context by lazy { view.context }

    @CallSuper
    open fun bind(item: ChannelListItem, diff: ChannelDiff) {
        channelItem = item
    }

    fun rebind(diff: ChannelDiff = ChannelDiff.DEFAULT): Boolean {
        return if (::channelItem.isInitialized) {
            bind(channelItem, diff)
            true
        } else false
    }

    protected fun getChannelListItem() = if (::channelItem.isInitialized) channelItem else null

    @CallSuper
    open fun onViewDetachedFromWindow() {
    }

    @CallSuper
    open fun onViewAttachedToWindow() {
    }
}