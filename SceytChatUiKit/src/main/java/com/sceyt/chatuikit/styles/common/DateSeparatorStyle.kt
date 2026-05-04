package com.sceyt.chatuikit.styles.common

import android.content.Context
import android.content.res.TypedArray
import androidx.annotation.ColorInt
import androidx.annotation.Px
import androidx.annotation.StyleableRes
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.styles.StyleConstants
import com.sceyt.chatuikit.styles.StyleCustomizer
import java.util.Date

/** Style for date separator in channel info.
 * @property backgroundStyle - background style for date separator
 * @property textStyle - style for date separator text
 * @property dateFormatter - formatter for date separator text
 * */
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
        private var backgroundColor: Int = StyleConstants.UNSET_COLOR

        @ColorInt
        private var borderColor: Int = StyleConstants.UNSET_COLOR

        @Px
        private var borderWidth: Int = StyleConstants.UNSET_SIZE

        @Px
        private var cornerRadius: Float = StyleConstants.UNSET_CORNER_RADIUS

        private var textStyle: TextStyle = TextStyle()

        fun backgroundColor(
            @StyleableRes index: Int, @ColorInt defValue: Int = backgroundColor
        ) = apply {
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
            shape = if (cornerRadius != StyleConstants.UNSET_CORNER_RADIUS)
                Shape.RoundedCornerShape(cornerRadius) else Shape.UnsetShape
        )
    }
}