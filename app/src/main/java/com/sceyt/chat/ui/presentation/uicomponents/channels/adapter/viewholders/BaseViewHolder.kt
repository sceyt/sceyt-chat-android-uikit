package com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.viewholders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.ChannelListItem

abstract class BaseViewHolder<T>(view: View) : RecyclerView.ViewHolder(view) {
    abstract fun bind(item: T)
    open fun onViewDetachedFromWindow() {}
    open fun onViewAttachedToWindow() {}
}