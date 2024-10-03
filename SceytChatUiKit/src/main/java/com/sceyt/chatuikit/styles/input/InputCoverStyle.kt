package com.sceyt.chatuikit.styles.input

import android.content.Context
import android.content.res.TypedArray
import androidx.annotation.ColorInt
import androidx.annotation.StyleableRes
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.TextStyle

data class InputCoverStyle(
        @ColorInt val backgroundColor: Int,
        @ColorInt val dividerColor: Int,
        val textStyle: TextStyle,
) {
    companion object {
        var styleCustomizer = StyleCustomizer<InputCoverStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
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
        ).let { styleCustomizer.apply(context, it) }
    }
}
