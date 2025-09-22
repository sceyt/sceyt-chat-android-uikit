package com.sceyt.chatuikit.styles.channel_info.link

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.presentation.components.channel_info.links.ChannelInfoLinksFragment
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.messages_list.item.LinkPreviewStyle
import com.sceyt.chatuikit.theme.Colors

/**
 * Style for link item in [ChannelInfoLinksFragment]
 * @property backgroundColor - background color, default is [Colors.backgroundColorSections]
 * @property linkTextStyle - style for link text
 * @property linkPreviewStyle - style for link preview
 * */
data class ChannelInfoLinkItemStyle(
        @param:ColorInt val backgroundColor: Int,
        val linkTextStyle: TextStyle,
        val linkPreviewStyle: LinkPreviewStyle,
) {
    companion object {
        var styleCustomizer = StyleCustomizer<ChannelInfoLinkItemStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
            private val attributeSet: AttributeSet?
    ) {
        fun build(): ChannelInfoLinkItemStyle {
            val backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColorSections)

            val linkTitleTextStyle = TextStyle(
                color = context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor),
                font = R.font.roboto_medium
            )

            val linkDescriptionTextStyle = TextStyle(
                color = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor),
            )

            val placeholder = context.getCompatDrawable(R.drawable.sceyt_ic_link).applyTint(
                context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
            )

            val linkTextStyle = TextStyle(
                color = context.getCompatColor(R.color.sceyt_auto_link_color),
            )

            val linkStyle = LinkPreviewStyle(
                titleStyle = linkTitleTextStyle,
                descriptionStyle = linkDescriptionTextStyle,
                placeHolder = placeholder,
            )

            return ChannelInfoLinkItemStyle(
                backgroundColor = backgroundColor,
                linkTextStyle = linkTextStyle,
                linkPreviewStyle = linkStyle
            ).let { styleCustomizer.apply(context, it) }
        }
    }
}
