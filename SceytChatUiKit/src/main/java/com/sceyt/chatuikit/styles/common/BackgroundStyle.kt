package com.sceyt.chatuikit.styles.common

import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.Px
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_CORNER_RADIUS
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_SIZE

data class BackgroundStyle(
        @ColorInt val backgroundColor: Int = UNSET_COLOR,
        @ColorInt val borderColor: Int = UNSET_COLOR,
        @Px val borderWidth: Int = UNSET_SIZE,
        @Px val cornerRadius: Float = UNSET_CORNER_RADIUS,
) {

    fun apply(view: View) {
        if (!shouldApplyBackground) return

        val background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE

            if (this@BackgroundStyle.cornerRadius != UNSET_CORNER_RADIUS)
                cornerRadius = this@BackgroundStyle.cornerRadius

            if (borderWidth != UNSET_SIZE)
                setStroke(borderWidth, borderColor)

            if (backgroundColor != UNSET_COLOR) {
                view.backgroundTintList = null
                setColor(backgroundColor)
            }
        }
        view.background = background
    }

    private val shouldApplyBackground: Boolean
        get() = backgroundColor != UNSET_COLOR ||
                cornerRadius != UNSET_CORNER_RADIUS || borderWidth != UNSET_SIZE
}