package com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.viewholders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelItemPayloadDiff
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelListItem

abstract class BaseChannelViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    abstract fun bind(item: ChannelListItem, diff: ChannelItemPayloadDiff)
}