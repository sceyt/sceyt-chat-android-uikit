package com.sceyt.sceytchatuikit.extensions

import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible

fun AppCompatTextView.setDrawableEnd(@DrawableRes id: Int, @ColorRes tint: Int = 0) {
    if (tint != 0) {
        val drawable = context.getCompatDrawable(id)
        drawable?.setTint(context.getCompatColor(tint))
        setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null)
    } else
        setCompoundDrawablesWithIntrinsicBounds(0, 0, id, 0)
}

fun AppCompatTextView.setDrawableStart(@DrawableRes id: Int, @ColorRes tint: Int = 0) {
    if (tint != 0) {
        val drawable = context.getCompatDrawable(id)
        drawable?.setTint(context.getCompatColor(tint))
        setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
    } else
        setCompoundDrawablesWithIntrinsicBounds(id, 0, 0, 0)
}

fun TextView.setTextAndVisibility(title: String?) {
    if (title.isNullOrBlank()) {
        isVisible = false
    } else {
        text = title
        isVisible = true
    }
}