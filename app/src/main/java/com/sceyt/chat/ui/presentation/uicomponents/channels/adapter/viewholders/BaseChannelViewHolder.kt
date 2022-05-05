package com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.viewholders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.ChannelListItem

abstract class BaseChannelViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    abstract fun bindViews(item: ChannelListItem)
    open fun onViewDetachedFromWindow() {}
    open fun onViewAttachedFromWindow() {}
}