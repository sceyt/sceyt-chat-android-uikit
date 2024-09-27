package com.sceyt.chatuikit.styles.input

import android.content.res.TypedArray
import androidx.annotation.ColorInt
import androidx.annotation.StyleableRes
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.common.TextStyle

data class InputCoverStyle(
        @ColorInt var backgroundColor: Int,
        @ColorInt var dividerColor: Int,
        var textStyle: TextStyle,
) {
    internal class Builder(
            private val typedArray: TypedArray
    ) {
        @ColorInt
        private var backgroundColor: Int = UNSET_COLOR

        @ColorInt
        private var dividerColor: Int = UNSET_COLOR
        private var textStyle: TextStyle = TextStyle()

        fun backgroundColor(@StyleableRes index: Int, defValue: Int = backgroundColor) = apply {
            this.backgroundColor = typedArray.getColor(index, defValue)
        }

        fun dividerColor(@StyleableRes index: Int, defValue: Int = dividerColor) = apply {
            this.dividerColor = typedArray.getColor(index, defValue)
        }

        fun textStyle(textStyle: TextStyle) = apply {
            this.textStyle = textStyle
        }

        fun build() = InputCoverStyle(
            backgroundColor = backgroundColor,
            dividerColor = dividerColor,
            textStyle = textStyle
        )
    }
}
