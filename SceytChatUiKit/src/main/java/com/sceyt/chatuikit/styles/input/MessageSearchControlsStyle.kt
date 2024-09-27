package com.sceyt.chatuikit.styles.input

import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.StyleableRes
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.common.TextStyle

data class MessageSearchControlsStyle(
        @ColorInt var backgroundColor: Int,
        var previousIcon: Drawable?,
        var nextIcon: Drawable?,
        var resultTextStyle: TextStyle
) {
    internal class Builder(
            private val typedArray: TypedArray
    ) {
        @ColorInt
        private var backgroundColor: Int = UNSET_COLOR
        private var previousIcon: Drawable? = null
        private var nextIcon: Drawable? = null
        private var resultTextStyle: TextStyle = TextStyle()

        fun backgroundColor(@StyleableRes index: Int, defValue: Int = backgroundColor) = apply {
            this.backgroundColor = typedArray.getColor(index, defValue)
        }

        fun previousIcon(@StyleableRes index: Int, defValue: Drawable? = previousIcon) = apply {
            this.previousIcon = typedArray.getDrawable(index) ?: defValue
        }

        fun nextIcon(@StyleableRes index: Int, defValue: Drawable? = nextIcon) = apply {
            this.nextIcon = typedArray.getDrawable(index) ?: defValue
        }

        fun resultTextStyle(resultTextStyle: TextStyle) = apply {
            this.resultTextStyle = resultTextStyle
        }

        fun build() = MessageSearchControlsStyle(
            backgroundColor = backgroundColor,
            previousIcon = previousIcon,
            nextIcon = nextIcon,
            resultTextStyle = resultTextStyle
        )
    }
}