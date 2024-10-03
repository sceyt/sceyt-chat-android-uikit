package com.sceyt.chatuikit.styles.messages_list

import android.content.Context
import android.content.res.TypedArray
import androidx.annotation.ColorInt
import androidx.annotation.Px
import androidx.annotation.StyleableRes
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_CORNER_RADIUS
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_SIZE
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import java.util.Date

data class DateSeparatorStyle(
        val backgroundStyle: BackgroundStyle,
        val textStyle: TextStyle,
        val dateFormatter: Formatter<Date>
) {
    companion object {
        var styleCustomizer = StyleCustomizer<DateSeparatorStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
            private val typedArray: TypedArray
    ) {
        @ColorInt
        private var backgroundColor: Int = UNSET_COLOR

        @ColorInt
        private var borderColor: Int = UNSET_COLOR

        @Px
        private var borderWidth: Int = UNSET_SIZE

        @Px
        private var cornerRadius: Float = UNSET_CORNER_RADIUS

        private var textStyle: TextStyle = TextStyle()

        fun backgroundColor(@StyleableRes index: Int, @ColorInt defValue: Int = backgroundColor) = apply {
            this.backgroundColor = typedArray.getColor(index, defValue)
        }

        fun borderColor(@StyleableRes index: Int, @ColorInt defValue: Int = borderColor) = apply {
            this.borderColor = typedArray.getColor(index, defValue)
        }

        fun borderWidth(@StyleableRes index: Int, @Px defValue: Int = borderWidth) = apply {
            this.borderWidth = typedArray.getDimensionPixelSize(index, defValue)
        }

        fun cornerRadius(@StyleableRes index: Int, @Px defValue: Float = cornerRadius) = apply {
            this.cornerRadius = typedArray.getDimension(index, defValue)
        }

        fun textStyle(textStyle: TextStyle) = apply {
            this.textStyle = textStyle
        }

        fun build() = DateSeparatorStyle(
            backgroundStyle = buildBackgroundStyle(),
            textStyle = textStyle,
            dateFormatter = SceytChatUIKit.formatters.messageDateSeparatorFormatter
        ).let { styleCustomizer.apply(context, it) }

        private fun buildBackgroundStyle() = BackgroundStyle(
            backgroundColor = backgroundColor,
            borderColor = borderColor,
            borderWidth = borderWidth,
            cornerRadius = cornerRadius
        )
    }
}