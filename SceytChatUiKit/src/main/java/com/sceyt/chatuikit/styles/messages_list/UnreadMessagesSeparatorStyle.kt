package com.sceyt.chatuikit.styles.messages_list

import android.content.res.TypedArray
import androidx.annotation.ColorInt
import androidx.annotation.StyleableRes
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.common.TextStyle

data class UnreadMessagesSeparatorStyle(
        @ColorInt val backgroundColor: Int,
        val textStyle: TextStyle
) {
    internal class Builder(
            private val typedArray: TypedArray
    ) {
        @ColorInt
        private var backgroundColor: Int = UNSET_COLOR
        private var textStyle: TextStyle = TextStyle()

        fun backgroundColor(@StyleableRes index: Int, @ColorInt defValue: Int = backgroundColor) = apply {
            this.backgroundColor = typedArray.getColor(index, defValue)
        }

        fun textStyle(textStyle: TextStyle) = apply {
            this.textStyle = textStyle
        }

        fun build() = UnreadMessagesSeparatorStyle(
            backgroundColor = backgroundColor,
            textStyle = textStyle
        )
    }
}