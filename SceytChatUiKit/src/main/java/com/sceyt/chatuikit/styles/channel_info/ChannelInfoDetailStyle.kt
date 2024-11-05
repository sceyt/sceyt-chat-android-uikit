package com.sceyt.chatuikit.styles.channel_info

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.renderers.AvatarRenderer
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.AvatarStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme

data class ChannelInfoDetailStyle(
        @ColorInt val backgroundColor: Int,
        val titleTextStyle: TextStyle,
        val subtitleTextStyle: TextStyle,
        val avatarStyle: AvatarStyle,
        val channelNameFormatter: Formatter<SceytChannel>,
        val channelSubtitleFormatter: Formatter<SceytChannel>,
        val channelAvatarRenderer: AvatarRenderer<SceytChannel>
) {
    companion object {
        var styleCustomizer = StyleCustomizer<ChannelInfoDetailStyle> { _, style -> style }
    }

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
                avatarStyle = AvatarStyle(),
                channelNameFormatter = SceytChatUIKit.formatters.channelNameFormatter,
                channelSubtitleFormatter = SceytChatUIKit.formatters.channelSubtitleFormatter,
                channelAvatarRenderer = SceytChatUIKit.renderers.channelAvatarRenderer
            ).let { styleCustomizer.apply(context, it) }
        }
    }
}
