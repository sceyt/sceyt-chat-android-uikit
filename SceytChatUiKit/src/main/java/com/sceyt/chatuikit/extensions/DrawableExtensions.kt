package com.sceyt.chatuikit.extensions

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat

fun Drawable?.toSpannableString(): SpannableStringBuilder {
    this ?: return SpannableStringBuilder()
    val builder = SpannableStringBuilder(". ")
    setBounds(0, 0, intrinsicWidth, intrinsicHeight)
    builder.setSpan(ImageSpan(this), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    return builder
}

fun Drawable?.applyTint(@ColorInt tintColor: Int): Drawable? {
    return this?.mutate()?.apply { setTint(tintColor) }
}

fun Drawable?.applyTint(context: Context, @ColorRes tintColorRes: Int): Drawable? {
    return this?.mutate()?.apply { setTint(ContextCompat.getColor(context, tintColorRes)) }
}

fun Drawable?.applyTintBackgroundLayer(@ColorInt tintColor: Int, @IdRes bgLayerId: Int): Drawable? {
    if (this !is LayerDrawable) return this
    return mutate().apply {
        val backgroundDrawable = findDrawableByLayerId(bgLayerId)
        backgroundDrawable?.setTint(tintColor)
    }
}

fun Drawable?.applyTintBackgroundLayer(
        context: Context,
        @ColorRes tintColor: Int,
        @IdRes bgLayerId: Int
): Drawable? {
    if (this !is LayerDrawable) return this
    return mutate().apply {
        val backgroundDrawable = findDrawableByLayerId(bgLayerId)
        backgroundDrawable?.setTint(ContextCompat.getColor(context, tintColor))
    }
}