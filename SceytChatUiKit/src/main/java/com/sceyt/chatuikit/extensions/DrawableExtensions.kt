package com.sceyt.chatuikit.extensions

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.IdRes
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit

fun Drawable?.toSpannableString(): SpannableStringBuilder {
    this ?: return SpannableStringBuilder()
    val builder = SpannableStringBuilder(". ")
    setBounds(0, 0, intrinsicWidth, intrinsicHeight)
    builder.setSpan(ImageSpan(this), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    return builder
}

fun Drawable?.applyTint(@ColorInt tintColor: Int): Drawable? {
    this ?: return null
    if (tintColor == 0) return this
    return mutate().apply { setTint(tintColor) }
}

fun Drawable?.applyTint(context: Context, @ColorRes tintColorRes: Int): Drawable? {
    return applyTint(ContextCompat.getColor(context, tintColorRes))
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
        @IdRes bgLayerId: Int,
): Drawable? {
    if (this !is LayerDrawable) return this
    return mutate().apply {
        val backgroundDrawable = findDrawableByLayerId(bgLayerId)
        backgroundDrawable?.setTint(ContextCompat.getColor(context, tintColor))
    }
}

fun SwitchCompat.setColors(
        @ColorInt checkedColor: Int = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor),
        @ColorInt thumbUncheckedColor: Int = context.getCompatColor(R.color.sceyt_switch_thumb_unchecked_color),
        @ColorInt trackUncheckedColor: Int = context.getCompatColor(R.color.sceyt_switch_track_unchecked_color),
) {
    val thumbColor = ColorStateList(
        arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf()
        ),
        intArrayOf(
            checkedColor,
            thumbUncheckedColor
        )
    )
    val trackColor = ColorStateList(
        arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf()
        ),
        intArrayOf(
            checkedColor.withAlpha(0.3f),
            trackUncheckedColor
        )
    )

    thumbTintList = thumbColor
    trackTintList = trackColor
}