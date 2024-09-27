package com.sceyt.chatuikit.styles.common

import android.content.res.TypedArray
import android.graphics.drawable.GradientDrawable
import android.widget.Button
import androidx.annotation.ColorInt
import androidx.annotation.Px
import androidx.annotation.StyleableRes
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_CORNER_RADIUS
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_SIZE

data class ButtonStyle(
        val textStyle: TextStyle = TextStyle(),
        @ColorInt val backgroundColor: Int = UNSET_COLOR,
        val cornerRadius: Float = UNSET_CORNER_RADIUS,
        @Px val borderWidth: Int = UNSET_SIZE,
        @ColorInt val borderColor: Int = UNSET_COLOR
) {

    fun apply(button: Button) {
        textStyle.apply(button)
        button.setBackgroundColor(backgroundColor)

        if (backgroundColor == UNSET_COLOR)
            return

        if (cornerRadius == UNSET_CORNER_RADIUS) {
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

    internal class Builder(private val typedArray: TypedArray) {
        @ColorInt
        private var backgroundColor: Int = UNSET_COLOR

        @ColorInt
        private var borderColor: Int = UNSET_COLOR

        @Px
        private var borderWidth: Int = UNSET_SIZE

        @Px
        private var cornerRadius: Float = UNSET_CORNER_RADIUS

        private var textStyle: TextStyle = TextStyle()

        fun setBackgroundColor(@StyleableRes index: Int, defValue: Int = backgroundColor) = apply {
            backgroundColor = typedArray.getColor(index, defValue)
        }

        fun setBorderColor(@StyleableRes index: Int, defValue: Int = borderColor) = apply {
            borderColor = typedArray.getColor(index, defValue)
        }

        fun setBorderWidth(@StyleableRes index: Int, defValue: Int = borderWidth) = apply {
            borderWidth = typedArray.getDimensionPixelSize(index, defValue)
        }

        fun setCornerRadius(@StyleableRes index: Int, defValue: Float = cornerRadius) = apply {
            cornerRadius = typedArray.getDimension(index, defValue)
        }

        fun setTextStyle(textStyle: TextStyle) = apply {
            this.textStyle = textStyle
        }

        fun build() = ButtonStyle(
            backgroundColor = backgroundColor,
            borderColor = borderColor,
            borderWidth = borderWidth,
            cornerRadius = cornerRadius,
            textStyle = textStyle
        )
    }
}
