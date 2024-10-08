package com.sceyt.chatuikit.styles.messages_list

import android.content.Context
import android.content.res.TypedArray
import androidx.annotation.ColorInt
import androidx.annotation.StyleableRes
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.TextStyle

data class UnreadMessagesSeparatorStyle(
        @ColorInt val backgroundColor: Int,
        val unreadText: String,
        val textStyle: TextStyle,
) {
    companion object {
        var styleCustomizer = StyleCustomizer<UnreadMessagesSeparatorStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
            private val typedArray: TypedArray
    ) {
        @ColorInt
        private var backgroundColor: Int = UNSET_COLOR
        private var textStyle: TextStyle = TextStyle()
        private var unreadText: String = ""

        fun backgroundColor(@StyleableRes index: Int, @ColorInt defValue: Int = backgroundColor) = apply {
            this.backgroundColor = typedArray.getColor(index, defValue)
        }

        fun unreadText(@StyleableRes index: Int, defValue: String = unreadText) = apply {
            this.unreadText = typedArray.getString(index) ?: defValue
        }

        fun textStyle(textStyle: TextStyle) = apply {
            this.textStyle = textStyle
        }

        fun build() = UnreadMessagesSeparatorStyle(
            backgroundColor = backgroundColor,
            unreadText = unreadText,
            textStyle = textStyle
        ).let { styleCustomizer.apply(context, it) }
    }
}