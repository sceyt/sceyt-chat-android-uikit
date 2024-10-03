package com.sceyt.chatuikit.styles.channel_info.files

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.presentation.components.channel_info.files.ChannelInfoFilesFragment
import com.sceyt.chatuikit.styles.channel_info.ChannelInfoDateSeparatorStyle
import com.sceyt.chatuikit.theme.Colors

/**
 * Style for [ChannelInfoFilesFragment] page.
 * @property backgroundColor - background color, default is [Colors.backgroundColorSections]
 * @property dateSeparatorStyle - style for date separator
 * @property itemStyle - style for file item
 * */
data class ChannelInfoFilesStyle(
        @ColorInt val backgroundColor: Int,
        val dateSeparatorStyle: ChannelInfoDateSeparatorStyle,
        val itemStyle: ChannelInfoFileItemStyle
) {
    internal class Builder(
            private val context: Context,
            private val attributeSet: AttributeSet?
    ) {
        fun build(): ChannelInfoFilesStyle {
            val backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColorSections)

            val itemStyle = ChannelInfoFileItemStyle.Builder(context, attributeSet).build()

            val dateSeparatorStyle = ChannelInfoDateSeparatorStyle.Builder(context, attributeSet)
                .build()

            return ChannelInfoFilesStyle(
                backgroundColor = backgroundColor,
                dateSeparatorStyle = dateSeparatorStyle,
                itemStyle = itemStyle
            )
        }
    }
}
