package com.sceyt.chatuikit.styles.channel_info.files

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.providers.VisualProvider
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.MediaLoaderStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.theme.Colors
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme

/**
 * Style for conversation info file item
 * @property backgroundColor - background color, default is [Colors.backgroundColorSections]
 * @property fileNameTextStyle - style for file name
 * @property subtitleTextStyle - style for subtitle
 * @property mediaLoaderStyle - style for media loader
 * @property fileNameFormatter - formatter for file name
 * @property subtitleFormatter - formatter for subtitle
 * @property iconProvider - provider for file icon
 *  */
data class ChannelInfoFileItemStyle(
        @ColorInt val backgroundColor: Int,
        val fileNameTextStyle: TextStyle,
        val subtitleTextStyle: TextStyle,
        val mediaLoaderStyle: MediaLoaderStyle,
        val fileNameFormatter: Formatter<SceytAttachment>,
        val subtitleFormatter: Formatter<SceytAttachment>,
        val iconProvider: VisualProvider<SceytAttachment, Drawable?>,
) {
    companion object {
        var styleCustomizer = StyleCustomizer<ChannelInfoFileItemStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
            private val attributeSet: AttributeSet?
    ) {
        fun build(): ChannelInfoFileItemStyle {
            val backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColorSections)

            val fileNameTextStyle = TextStyle(
                color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor),
                font = R.font.roboto_medium_font
            )

            val subtitleTextStyle = TextStyle(
                color = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor),
            )

            val mediaLoaderStyle = MediaLoaderStyle(
                progressColor = context.getCompatColor(SceytChatUIKitTheme.colors.onPrimaryColor),
                uploadIcon = context.getCompatDrawable(R.drawable.sceyt_ic_upload).applyTint(
                    context.getCompatColor(SceytChatUIKitTheme.colors.onPrimaryColor)
                ),
                downloadIcon = context.getCompatDrawable(R.drawable.sceyt_ic_download).applyTint(
                    context.getCompatColor(SceytChatUIKitTheme.colors.onPrimaryColor)
                ),
                cancelIcon = context.getCompatDrawable(R.drawable.sceyt_ic_cancel_transfer).applyTint(
                    context.getCompatColor(SceytChatUIKitTheme.colors.onPrimaryColor)
                ),
            )

            val fileNameFormatter = Formatter<SceytAttachment> { _, attachment ->
                attachment.name
            }

            return ChannelInfoFileItemStyle(
                backgroundColor = backgroundColor,
                fileNameTextStyle = fileNameTextStyle,
                subtitleTextStyle = subtitleTextStyle,
                mediaLoaderStyle = mediaLoaderStyle,
                fileNameFormatter = fileNameFormatter,
                subtitleFormatter = SceytChatUIKit.formatters.channelInfoFileSubtitleFormatter,
                iconProvider = SceytChatUIKit.providers.attachmentIconProvider
            ).let { styleCustomizer.apply(context, it) }
        }
    }
}
