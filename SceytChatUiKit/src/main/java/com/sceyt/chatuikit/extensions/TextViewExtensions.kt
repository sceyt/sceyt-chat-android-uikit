@file:Suppress("unused")

package com.sceyt.chatuikit.extensions

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible

fun TextView.setDrawableEnd(@DrawableRes id: Int, @ColorRes tint: Int = 0) {
    val drawables = compoundDrawables
    val drawableEnd = context.getCompatDrawable(id)
    if (tint != 0)
        drawableEnd?.mutate()?.setTint(context.getCompatColor(tint))

    setCompoundDrawablesWithIntrinsicBounds(drawables[0], drawables[2], drawableEnd, drawables[3])
}

fun TextView.setDrawableStart(@DrawableRes id: Int, @ColorRes tint: Int = 0) {
    val drawables = compoundDrawables
    val drawableStart = context.getCompatDrawable(id)
    if (tint != 0)
        drawableStart?.mutate()?.setTint(context.getCompatColor(tint))

    setCompoundDrawablesWithIntrinsicBounds(drawableStart, drawables[1], drawables[2], drawables[3])
}

fun TextView.setDrawableStart(drawable: Drawable?, @ColorInt tint: Int = 0) {
    val drawables = compoundDrawables
    if (tint != 0)
        drawable?.mutate()?.setTint(tint)

    setCompoundDrawablesWithIntrinsicBounds(drawable, drawables[1], drawables[2], drawables[3])
}

fun TextView.setDrawableTop(@DrawableRes id: Int, @ColorRes tint: Int = 0) {
    val drawables = compoundDrawables
    val drawableTop = context.getCompatDrawable(id)
    if (tint != 0)
        drawableTop?.mutate()?.setTint(context.getCompatColor(tint))

    setCompoundDrawablesWithIntrinsicBounds(drawables[0], drawableTop, drawables[2], drawables[3])
}


fun TextView.setTextAndVisibility(title: String?) {
    if (title.isNullOrBlank()) {
        isVisible = false
    } else {
        text = title.trim()
        isVisible = true
    }
}

fun TextView.setTextAndDrawableByColorId(@ColorRes colorId: Int) {
    if (colorId != 0) {
        val color = context.getCompatColor(colorId)
        setTextColor(color)
        setTextViewDrawableColor(color)
    }
}

fun TextView.setTextAndDrawableByColor(@ColorInt color: Int) {
    if (color != 0) {
        setTextColor(color)
        setTextViewDrawableColor(color)
    }
}

fun TextView.setTextViewDrawableColor(@ColorInt color: Int) {
    for (drawable in compoundDrawables) {
        if (drawable != null)
            drawable.mutate().colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
    }
    for (drawable in compoundDrawablesRelative) {
        if (drawable != null)
            drawable.mutate().colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
    }
}

fun setTextViewsDrawableColor(texts: List<TextView>, @ColorInt color: Int) {
    texts.forEach {
        it.compoundDrawables.forEach { drawable ->
            drawable?.mutate()?.let {
                drawable.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
            }
        }
        it.compoundDrawablesRelative.forEach { drawable ->
            drawable?.mutate()?.let {
                drawable.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
            }
        }
    }
}

fun List<TextView>.setTextViewsTextColor(@ColorInt color: Int) {
    forEach {
        it.setTextColor(color)
    }
}

fun List<TextView>.setTextViewsTextColorRes(@ColorRes colorId: Int) {
    if (isEmpty()) return
    val color = first().context.getCompatColor(colorId)
    forEach {
        it.setTextColor(color)
    }
}

fun List<TextView>.setTextViewsHintTextColorRes(@ColorRes colorId: Int) {
    if (isEmpty()) return
    val color = first().context.getCompatColor(colorId)
    forEach {
        it.setHintTextColor(color)
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
fun TextView.setCursorAndHandleColor(@ColorInt color: Int = 0) {
    fun Drawable.applyTint(color: Int) = mutate().apply {
        setTint(color)
    }
    textSelectHandleRight?.let {
        setTextSelectHandleRight(it.applyTint(color))
    }
    textSelectHandleLeft?.let {
        setTextSelectHandleLeft(it.applyTint(color))
    }
    textSelectHandle?.let {
        setTextSelectHandle(it.applyTint(color))
    }
    textCursorDrawable?.let {
        setTextCursorDrawable(it.applyTint(color))
    }
    highlightColor = color.withAlpha(0.5f)
}

@RequiresApi(Build.VERSION_CODES.Q)
fun TextView.setCursorAndHandleColorRes(@ColorRes color: Int = 0) {
    val colorInt = context.getCompatColor(color)
    setCursorAndHandleColor(colorInt)
}