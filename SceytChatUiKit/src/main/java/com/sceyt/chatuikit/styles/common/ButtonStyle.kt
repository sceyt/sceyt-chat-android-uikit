package com.sceyt.chatuikit.styles.common

import android.graphics.drawable.GradientDrawable
import android.widget.Button
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.styles.StyleConstants

data class ButtonStyle(
        val textStyle: TextStyle = TextStyle(),
        @ColorInt val backgroundColor: Int = StyleConstants.UNSET_COLOR,
        val cornerRadius: Float = StyleConstants.UNSET_CORNER_RADIUS
) {

    fun apply(button: Button) {
        textStyle.apply(button)
        button.setBackgroundColor(backgroundColor)

        if (backgroundColor == StyleConstants.UNSET_COLOR)
            return

        if (cornerRadius == StyleConstants.UNSET_CORNER_RADIUS) {
            button.setBackgroundColor(backgroundColor)
        } else {
            button.setBackgroundWithCornerRadius(cornerRadius, backgroundColor)
        }
    }

    private fun Button.setBackgroundWithCornerRadius(radius: Float, backgroundColor: Int) {
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = this@ButtonStyle.cornerRadius
            setColor(backgroundColor)
        }
        drawable.cornerRadius = radius
        background = drawable
    }
}
