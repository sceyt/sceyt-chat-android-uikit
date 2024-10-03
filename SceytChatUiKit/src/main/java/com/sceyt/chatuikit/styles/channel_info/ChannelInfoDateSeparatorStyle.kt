package com.sceyt.chatuikit.styles.channel_info

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.theme.Colors
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme
import java.util.Date

/** Style for date separator in channel info.
 * @property backgroundColor - background color, default is [Colors.backgroundColor]
 * @property textStyle - style for date separator text
 * @property dateFormatter - formatter for date separator text
 * */
data class ChannelInfoDateSeparatorStyle(
        @ColorInt val backgroundColor: Int,
        val textStyle: TextStyle,
        val dateFormatter: Formatter<Date>
) {
    companion object {
        var styleCustomizer = StyleCustomizer<ChannelInfoDateSeparatorStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
            private val attributeSet: AttributeSet?
    ) {
        fun build(): ChannelInfoDateSeparatorStyle {
            val backgroundColor = context.getCompatColor(SceytChatUIKitTheme.colors.backgroundColor)

            val textStyle = TextStyle(
                color = context.getCompatColor(SceytChatUIKitTheme.colors.textSecondaryColor),
                font = R.font.roboto_medium
            )

            return ChannelInfoDateSeparatorStyle(
                backgroundColor = backgroundColor,
                textStyle = textStyle,
                dateFormatter = SceytChatUIKit.formatters.channelInfoDateSeparatorFormatter
            ).let { styleCustomizer.apply(context, it) }
        }
    }
}
