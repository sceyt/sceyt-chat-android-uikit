package com.sceyt.chatuikit.presentation.components.channel_info.groups.adapter.listeners

import android.view.View
import com.sceyt.chatuikit.data.models.channels.SceytChannel

open class CommonGroupClickListenersImpl : CommonGroupClickListeners.ClickListener {
    private var clickListener: CommonGroupClickListeners.ClickListener? = null

    override fun onGroupClick(view: View, channel: SceytChannel) {
        clickListener?.onGroupClick(view, channel)
    }

    fun setListener(listener: CommonGroupClickListeners.ClickListener) {
        clickListener = listener
    }
}