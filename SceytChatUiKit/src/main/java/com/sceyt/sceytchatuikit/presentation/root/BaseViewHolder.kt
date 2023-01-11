package com.sceyt.sceytchatuikit.presentation.root

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView

abstract class BaseViewHolder<I>(view: View) : RecyclerView.ViewHolder(view) {
    protected val context: Context by lazy { view.context }

    abstract fun bind(item: I)
    open fun onViewDetachedFromWindow() {}
    open fun onViewAttachedToWindow() {}
}