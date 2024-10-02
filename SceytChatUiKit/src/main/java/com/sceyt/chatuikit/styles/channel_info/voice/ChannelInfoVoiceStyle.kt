package com.sceyt.chatuikit.styles.channel_info.voice

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.presentation.components.channel_info.media.ChannelInfoMediaFragment
import com.sceyt.chatuikit.styles.channel_info.ChannelInfoDateSeparatorStyle
import com.sceyt.chatuikit.theme.Colors

/**
 * Style for [ChannelInfoMediaFragment] page.
 * @property backgroundColor - background color, default is [Colors.backgroundColor]
 * @property dateSeparatorStyle - style for date separator
 * @property itemStyle - style for voice item
 * */
data class ChannelInfoVoiceStyle(
        @ColorInt var backgroundColor: Int,
        var dateSeparatorStyle: ChannelInfoDateSeparatorStyle,
        var itemStyle: ChannelInfoVoiceItemStyle,
) {

    internal class Builder(
            private val context: Context,
            private val attributeSet: AttributeSet?
    ) {
        fun build(): ChannelInfoVoiceStyle {
            val backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColorSections)


            val dateSeparatorStyle = ChannelInfoDateSeparatorStyle.Builder(context, attributeSet)
                .build()

            val itemStyle = ChannelInfoVoiceItemStyle.Builder(context, attributeSet)
                .build()

            return ChannelInfoVoiceStyle(
                backgroundColor = backgroundColor,
                dateSeparatorStyle = dateSeparatorStyle,
                itemStyle = itemStyle
            )
        }
    }
}
