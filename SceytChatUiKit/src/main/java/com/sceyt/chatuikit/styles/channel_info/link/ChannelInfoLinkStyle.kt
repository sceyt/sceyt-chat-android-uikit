package com.sceyt.chatuikit.styles.channel_info.link

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.presentation.components.channel_info.links.ChannelInfoLinksFragment
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.channel_info.ChannelInfoDateSeparatorStyle
import com.sceyt.chatuikit.theme.Colors

/**
 * Style for [ChannelInfoLinksFragment] page.
 * @property backgroundColor - background color, default is [Colors.backgroundColor]
 * @property itemStyle - style for link item
 * @property dateSeparatorStyle - style for date separator
 * */
data class ChannelInfoLinkStyle(
        @ColorInt val backgroundColor: Int,
        val itemStyle: ChannelInfoLinkItemStyle,
        val dateSeparatorStyle: ChannelInfoDateSeparatorStyle
) {
    companion object {
        var styleCustomizer = StyleCustomizer<ChannelInfoLinkStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
            private val attributeSet: AttributeSet?
    ) {
        fun build(): ChannelInfoLinkStyle {
            val backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColor)

            val itemStyle = ChannelInfoLinkItemStyle.Builder(context, attributeSet).build()
            val dateSeparatorStyle = ChannelInfoDateSeparatorStyle.Builder(context, attributeSet)
                .build()


            return ChannelInfoLinkStyle(
                backgroundColor = backgroundColor,
                itemStyle = itemStyle,
                dateSeparatorStyle = dateSeparatorStyle
            ).let { styleCustomizer.apply(context, it) }
        }
    }
}
