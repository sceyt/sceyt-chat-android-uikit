package com.sceyt.sceytchatuikit.extensions

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible

fun AppCompatTextView.setDrawableEnd(@DrawableRes id: Int, @ColorRes tint: Int = 0) {
    val drawables = compoundDrawables
    val drawableEnd = context.getCompatDrawable(id)
    if (tint != 0)
        drawableEnd?.setTint(context.getCompatColor(tint))

    setCompoundDrawablesWithIntrinsicBounds(drawables[0], drawables[2], drawableEnd, drawables[3])
}

fun AppCompatTextView.setDrawableStart(@DrawableRes id: Int, @ColorRes tint: Int = 0) {
    val drawables = compoundDrawables
    val drawableStart = context.getCompatDrawable(id)
    if (tint != 0)
        drawableStart?.setTint(context.getCompatColor(tint))

    setCompoundDrawablesWithIntrinsicBounds(drawableStart, drawables[1], drawables[2], drawables[3])
}

fun AppCompatTextView.setDrawableTop(@DrawableRes id: Int, @ColorRes tint: Int = 0) {
    val drawables = compoundDrawables
    val drawableTop = context.getCompatDrawable(id)
    if (tint != 0)
        drawableTop?.setTint(context.getCompatColor(tint))

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

fun TextView.setTextAndDrawableColor(@ColorRes colorId: Int) {
    if (colorId != 0) {
        setTextColor(context.getCompatColor(colorId))
        setTextViewDrawableColor(colorId)
    }
}

fun TextView.setTextViewDrawableColor(@ColorRes color: Int) {
    for (drawable in compoundDrawables) {
        if (drawable != null)
            drawable.colorFilter = PorterDuffColorFilter(context.getCompatColor(color), PorterDuff.Mode.SRC_IN)
    }
    for (drawable in compoundDrawablesRelative) {
        if (drawable != null)
            drawable.colorFilter = PorterDuffColorFilter(context.getCompatColor(color), PorterDuff.Mode.SRC_IN)
    }
}

fun setTextViewsDrawableColor(texts: List<TextView>, @ColorInt color: Int) {
    texts.forEach {
        it.compoundDrawables.forEach { drawable ->
            drawable?.let {
                drawable.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
            }
        }
        it.compoundDrawablesRelative.forEach { drawable ->
            drawable?.let {
                drawable.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
            }
        }
    }
}