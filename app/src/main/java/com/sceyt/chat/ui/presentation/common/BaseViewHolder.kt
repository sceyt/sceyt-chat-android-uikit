package com.sceyt.chat.ui.presentation.common

import android.view.View
import androidx.recyclerview.widget.RecyclerView

abstract class BaseViewHolder<I>(view: View) : RecyclerView.ViewHolder(view) {
    abstract fun bind(item: I)
    open fun onViewDetachedFromWindow() {}
    open fun onViewAttachedToWindow() {}
}