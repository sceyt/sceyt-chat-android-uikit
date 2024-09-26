package com.sceyt.chatuikit.styles.common

import android.content.res.TypedArray
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.EditText
import androidx.annotation.ColorInt
import androidx.annotation.Px
import androidx.annotation.StyleableRes
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_CORNER_RADIUS
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_SIZE

data class TextInputStyle(
        @ColorInt val backgroundColor: Int = UNSET_COLOR,
        @ColorInt val borderColor: Int = UNSET_COLOR,
        @Px val borderWidth: Int = UNSET_SIZE,
        @Px val cornerRadius: Float = UNSET_CORNER_RADIUS,
        val textStyle: TextStyle = TextStyle(),
        val hintStyle: HintStyle = HintStyle(),
) {
    fun apply(textInput: EditText, inputRoot: View?) {
        textStyle.apply(textInput)
        hintStyle.apply(textInput)
        if (backgroundColor != UNSET_COLOR) {
            val background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = this@TextInputStyle.cornerRadius
                setStroke(borderWidth, borderColor)
                setColor(backgroundColor)
            }
            if (inputRoot != null)
                inputRoot.background = background
            else
                textInput.background = background
        }
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
        private var hintStyle: HintStyle = HintStyle()

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

        fun setHintStyle(hintStyle: HintStyle) = apply {
            this.hintStyle = hintStyle
        }

        fun build() = TextInputStyle(
            backgroundColor = backgroundColor,
            borderColor = borderColor,
            borderWidth = borderWidth,
            cornerRadius = cornerRadius,
            textStyle = textStyle,
            hintStyle = hintStyle
        )
    }
}
