package com.sceyt.chatuikit.styles.messages_list

import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.StyleableRes
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.styles.common.TextStyle

data class ScrollDownButtonStyle(
        @ColorInt val backgroundColor: Int,
        var icon: Drawable?,
        var unreadCountTextStyle: TextStyle,
        var unreadCountFormatter: Formatter<Long>
) {
    internal class Builder(
            private val typedArray: TypedArray
    ) {
        @ColorInt
        private var backgroundColor: Int = 0
        private var unreadCountTextStyle: TextStyle = TextStyle()
        private var icon: Drawable? = null

        fun backgroundColor(@StyleableRes index: Int, @ColorInt defValue: Int = backgroundColor) = apply {
            this.backgroundColor = typedArray.getColor(index, defValue)
        }

        fun icon(@StyleableRes index: Int, defValue: Drawable? = icon) = apply {
            this.icon = typedArray.getDrawable(index) ?: defValue
        }

        fun unreadCountTextStyle(unreadCountTextStyle: TextStyle) = apply {
            this.unreadCountTextStyle = unreadCountTextStyle
        }

        fun build() = ScrollDownButtonStyle(
            backgroundColor = backgroundColor,
            icon = icon,
            unreadCountTextStyle = unreadCountTextStyle,
            unreadCountFormatter = SceytChatUIKit.formatters.unreadCountFormatter
        )
    }
}