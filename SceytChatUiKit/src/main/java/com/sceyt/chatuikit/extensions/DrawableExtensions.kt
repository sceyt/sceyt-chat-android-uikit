package com.sceyt.chatuikit.extensions

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes

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
    return this?.mutate()?.apply { setTint(context.getCompatColor(tintColorRes)) }
}