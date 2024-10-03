package com.sceyt.chatuikit.styles.channel_info.voice

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.styles.common.MediaLoaderStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme

/**
 * Style for voice item.
 * @property backgroundColor - background color, default is [Color.TRANSPARENT]
 * @property playIcon - icon for play button, default is [R.drawable.sceyt_ic_play]
 * @property pauseIcon - icon for pause button, default is [R.drawable.sceyt_ic_pause]
 * @property userNameTextStyle - style for user name
 * @property subtitleTextStyle - style for date
 * @property durationTextStyle - style for duration
 * @property mediaLoaderStyle - style for media loader
 * @property userNameFormatter - formatter for user name
 * @property durationFormatter - formatter for duration
 * @property subtitleFormatter - formatter for date
 * */
data class ChannelInfoVoiceItemStyle(
        @ColorInt val backgroundColor: Int,
        val playIcon: Drawable?,
        val pauseIcon: Drawable?,
        val userNameTextStyle: TextStyle,
        val subtitleTextStyle: TextStyle,
        val durationTextStyle: TextStyle,
        val mediaLoaderStyle: MediaLoaderStyle,
        val userNameFormatter: Formatter<SceytUser>,
        val durationFormatter: Formatter<Long>,
        val subtitleFormatter: Formatter<SceytAttachment>
) {
    internal class Builder(
            private val context: Context,
            private val attributeSet: AttributeSet?
    ) {
        fun build(): ChannelInfoVoiceItemStyle {
            val backgroundColor = Color.TRANSPARENT

            val playIcon = context.getCompatDrawable(R.drawable.sceyt_ic_play).applyTint(
                context.getCompatColor(SceytChatUIKitTheme.colors.onPrimaryColor)
            )

            val pauseIcon = context.getCompatDrawable(R.drawable.sceyt_ic_pause).applyTint(
                context.getCompatColor(SceytChatUIKitTheme.colors.onPrimaryColor)
            )

            val titleTextStyle = TextStyle(
                color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor),
                font = R.font.roboto_medium_font
            )

            val dateTextStyle = TextStyle(
                color = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor),
            )

            val durationTextStyle = TextStyle(
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

            return ChannelInfoVoiceItemStyle(
                backgroundColor = backgroundColor,
                playIcon = playIcon,
                pauseIcon = pauseIcon,
                userNameTextStyle = titleTextStyle,
                subtitleTextStyle = dateTextStyle,
                durationTextStyle = durationTextStyle,
                mediaLoaderStyle = mediaLoaderStyle,
                userNameFormatter = SceytChatUIKit.formatters.userNameFormatter,
                durationFormatter = SceytChatUIKit.formatters.mediaDurationFormatter,
                subtitleFormatter = SceytChatUIKit.formatters.channelInfoVoiceSubtitleFormatter
            )
        }
    }
}