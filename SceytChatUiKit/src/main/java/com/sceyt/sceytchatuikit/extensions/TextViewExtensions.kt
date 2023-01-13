package com.sceyt.sceytchatuikit.extensions

import android.graphics.drawable.Drawable
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible

fun AppCompatTextView.setDrawableEnd(@DrawableRes id: Int, @ColorRes tint: Int = 0) {
    val drawables = compoundDrawables
    var drawableEnd: Drawable? = null
    if (tint != 0) {
        drawableEnd = context.getCompatDrawable(id)
        drawableEnd?.setTint(context.getCompatColor(tint))
    }
    setCompoundDrawablesWithIntrinsicBounds(drawables[0], drawables[2], drawableEnd, drawables[3])
}

fun AppCompatTextView.setDrawableStart(@DrawableRes id: Int, @ColorRes tint: Int = 0) {
    val drawables = compoundDrawables
    var drawableStart: Drawable? = null
    if (tint != 0) {
        drawableStart = context.getCompatDrawable(id)
        drawableStart?.setTint(context.getCompatColor(tint))
    }
    setCompoundDrawablesWithIntrinsicBounds(drawableStart, drawables[1], drawables[2], drawables[3])
}

fun AppCompatTextView.setDrawableTop(@DrawableRes id: Int, @ColorRes tint: Int = 0) {
    val drawables = compoundDrawables
    var drawableTop: Drawable? = null
    if (tint != 0) {
        drawableTop = context.getCompatDrawable(id)
        drawableTop?.setTint(context.getCompatColor(tint))
    }
    setCompoundDrawablesWithIntrinsicBounds(drawables[0], drawableTop, drawables[2], drawables[3])
}


fun TextView.setTextAndVisibility(title: String?) {
    if (title.isNullOrBlank()) {
        isVisible = false
    } else {
        text = title
        isVisible = true
    }
}