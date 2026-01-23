package com.sceyt.chatuikit.styles.common

import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.widget.ImageView
import androidx.annotation.ColorInt

/**
 * Style for the view-once badge that appears on voice messages.
 * @property icon Icon drawable for the badge.
 * @property backgroundColor Background color for the circular badge.
 * @property strokeColor Stroke color for the badge border.
 * @property strokeWidth Stroke width in pixels.
 */
data class ViewOnceBadgeStyle(
    val icon: Drawable?,
    @param:ColorInt val backgroundColor: Int,
    @param:ColorInt val strokeColor: Int,
    val strokeWidth: Int
) {
    /**
     * Applies this style to an ImageView, setting both the background and icon.
     */
    fun apply(imageView: ImageView) {
        imageView.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(backgroundColor)
            setStroke(strokeWidth, strokeColor)
        }
        imageView.setImageDrawable(icon)
    }
}

