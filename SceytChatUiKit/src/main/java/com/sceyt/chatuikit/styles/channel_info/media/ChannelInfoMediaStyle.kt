package com.sceyt.chatuikit.styles.channel_info.media

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.channel_info.ChannelInfoDateSeparatorStyle
import com.sceyt.chatuikit.theme.Colors

/**
 * Style for conversation info media
 * @property backgroundColor - background color, default is [Colors.backgroundColorSections]
 * @property itemStyle - style for media item
 * @property dateSeparatorStyle - style for date separator
 * */
data class ChannelInfoMediaStyle(
        @ColorInt val backgroundColor: Int,
        val itemStyle: ChannelInfoMediaItemStyle,
        val dateSeparatorStyle: ChannelInfoDateSeparatorStyle,
) {

    companion object {
        var styleCustomizer = StyleCustomizer<ChannelInfoMediaStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
            private val attributeSet: AttributeSet?
    ) {
        fun build(): ChannelInfoMediaStyle {
            val backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColor)

            val itemStyle = ChannelInfoMediaItemStyle.Builder(context, attributeSet).build()
            val dateSeparatorStyle = ChannelInfoDateSeparatorStyle.Builder(context, attributeSet)
                .build()

            return ChannelInfoMediaStyle(
                backgroundColor = backgroundColor,
                itemStyle = itemStyle,
                dateSeparatorStyle = dateSeparatorStyle,
            ).let { styleCustomizer.apply(context, it) }
        }
    }
}
