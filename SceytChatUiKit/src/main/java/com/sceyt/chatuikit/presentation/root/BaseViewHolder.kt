package com.sceyt.chatuikit.presentation.root

import android.content.Context
import android.view.View
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.RecyclerView

abstract class BaseViewHolder<I>(view: View) : RecyclerView.ViewHolder(view) {
    protected val context: Context by lazy { view.context }
    protected var isAttachedToWindow = false

    abstract fun bind(item: I)

    @CallSuper
    open fun onViewAttachedToWindow() {
        isAttachedToWindow = true
    }

    @CallSuper
    open fun onViewDetachedFromWindow() {
        isAttachedToWindow = false
    }
}