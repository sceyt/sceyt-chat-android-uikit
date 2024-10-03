package com.sceyt.chatuikit.styles.channel_info

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.presentation.custom_views.AvatarView.DefaultAvatar
import com.sceyt.chatuikit.providers.VisualProvider
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme

data class ChannelInfoDetailStyle(
        @ColorInt val backgroundColor: Int,
        val titleTextStyle: TextStyle,
        val subtitleTextStyle: TextStyle,
        val channelNameFormatter: Formatter<SceytChannel>,
        val channelSubtitleFormatter: Formatter<SceytChannel>,
        val channelDefaultAvatarProvider: VisualProvider<SceytChannel, DefaultAvatar>
) {
    internal class Builder(
            private val context: Context,
            private val attributeSet: AttributeSet?
    ) {
        fun build(): ChannelInfoDetailStyle {
            val backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColorSections)

            val titleTextStyle = TextStyle(
                color = context.getCompatColor(SceytChatUIKitTheme.colors.textPrimaryColor),
                font = R.font.roboto_medium
            )

            val descriptionTextStyle = TextStyle(
                color = context.getCompatColor(SceytChatUIKitTheme.colors.textSecondaryColor),
            )

            return ChannelInfoDetailStyle(
                backgroundColor = backgroundColor,
                titleTextStyle = titleTextStyle,
                subtitleTextStyle = descriptionTextStyle,
                channelNameFormatter = SceytChatUIKit.formatters.channelNameFormatter,
                channelSubtitleFormatter = SceytChatUIKit.formatters.channelSubtitleFormatter,
                channelDefaultAvatarProvider = SceytChatUIKit.providers.channelDefaultAvatarProvider
            )
        }
    }
}
