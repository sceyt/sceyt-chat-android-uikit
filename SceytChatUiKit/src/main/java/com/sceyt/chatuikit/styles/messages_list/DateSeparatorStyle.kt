package com.sceyt.chatuikit.styles.messages_list

import android.content.res.TypedArray
import androidx.annotation.ColorInt
import androidx.annotation.StyleableRes
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.styles.common.TextStyle
import java.util.Date

data class DateSeparatorStyle(
        @ColorInt val backgroundColor: Int,
        val textStyle: TextStyle,
        val dateFormatter: Formatter<Date>
){
    internal class Builder(
            private val typedArray: TypedArray
    ) {
        @ColorInt
        private var backgroundColor: Int = 0
        private var textStyle: TextStyle = TextStyle()

        fun backgroundColor(@StyleableRes index: Int, @ColorInt defValue: Int = backgroundColor) = apply {
            this.backgroundColor = typedArray.getColor(index, defValue)
        }

        fun textStyle(textStyle: TextStyle) = apply {
            this.textStyle = textStyle
        }

        fun build() = DateSeparatorStyle(
            backgroundColor = backgroundColor,
            textStyle = textStyle,
            dateFormatter = SceytChatUIKit.formatters.messageDateSeparatorFormatter
        )
    }
}