package com.sceyt.chatuikit.styles.common

import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.widget.Button
import androidx.annotation.ColorInt
import androidx.annotation.Px
import androidx.annotation.StyleableRes
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_CORNER_RADIUS
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_SIZE

data class ButtonStyle(
        val textStyle: TextStyle = TextStyle(),
        val icon: Drawable? = null,
        val backgroundStyle: BackgroundStyle = BackgroundStyle()
) {

    fun apply(button: Button) {
        textStyle.apply(button)
        backgroundStyle.apply(button)
    }

    fun apply(button: FloatingActionButton) {
        backgroundStyle.apply(button)
        button.setImageDrawable(icon)
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
            backgroundStyle = BackgroundStyle(
                backgroundColor = backgroundColor,
                borderColor = borderColor,
                borderWidth = borderWidth,
                cornerRadius = cornerRadius
            ),
            textStyle = textStyle
        )
    }
}
