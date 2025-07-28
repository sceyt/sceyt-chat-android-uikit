package com.sceyt.chatuikit.styles.messages_list

import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.StyleableRes
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.TextStyle

data class ScrollButtonStyle(
        @ColorInt val backgroundColor: Int,
        val icon: Drawable?,
        val unreadCountTextStyle: TextStyle,
        val unreadCountFormatter: Formatter<Long>
) {
    companion object {
        var styleCustomizer = StyleCustomizer<ScrollButtonStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
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

        fun build() = ScrollButtonStyle(
            backgroundColor = backgroundColor,
            icon = icon,
            unreadCountTextStyle = unreadCountTextStyle,
            unreadCountFormatter = SceytChatUIKit.formatters.unreadCountFormatter
        ).let { styleCustomizer.apply(context, it) }
    }
}