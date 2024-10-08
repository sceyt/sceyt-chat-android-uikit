package com.sceyt.chatuikit.styles.input

import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.StyleableRes
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.TextStyle

data class MessageSearchControlsStyle(
        @ColorInt val backgroundColor: Int,
        val previousIcon: Drawable?,
        val nextIcon: Drawable?,
        val resultTextStyle: TextStyle
) {
    companion object {
        var styleCustomizer = StyleCustomizer<MessageSearchControlsStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
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
        ).let { styleCustomizer.apply(context, it) }
    }
}