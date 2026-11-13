package com.sceyt.chatuikit.styles.channel_info.media

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.MediaLoaderStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.theme.Colors

/**
 * Style for conversation info media item
 * @property backgroundColor - background color, default is [Colors.backgroundColorSections]
 * @property videoDurationIcon - icon for video duration, default is [R.drawable.sceyt_ic_video]
 * @property videoDurationTextStyle - style for video duration
 * @property mediaLoaderStyle - style for media loader
 * @property durationFormatter - formatter for duration
 * */
data class ChannelInfoMediaItemStyle(
    @param:ColorInt val backgroundColor: Int,
    val videoDurationIcon: Drawable?,
    val videoDurationTextStyle: TextStyle,
    val mediaLoaderStyle: MediaLoaderStyle,
    val durationFormatter: Formatter<Long>
) {

    companion object {
        var styleCustomizer = StyleCustomizer<ChannelInfoMediaItemStyle> { _, style -> style }
    }

    @Suppress("unused")
    internal class Builder(
        private val context: Context,
        private val attributeSet: AttributeSet?
    ) {
        fun build(): ChannelInfoMediaItemStyle {
            @ColorInt
            val backgroundColor =
                context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColorSections)
            val videoDurationIcon = context.getCompatDrawable(R.drawable.sceyt_ic_video).applyTint(
                context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor)
            )
            val videoDurationTextStyle = TextStyle(
                color = context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor)
            )
            val mediaLoaderStyle = MediaLoaderStyle(
                progressColor = context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor),
                uploadIcon = context.getCompatDrawable(R.drawable.sceyt_ic_upload).applyTint(
                    context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor)
                ),
                downloadIcon = context.getCompatDrawable(R.drawable.sceyt_ic_download).applyTint(
                    context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor)
                ),
                cancelIcon = context.getCompatDrawable(R.drawable.sceyt_ic_cancel_transfer)
                    .applyTint(
                        context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor)
                    ),
            )

            return ChannelInfoMediaItemStyle(
                backgroundColor = backgroundColor,
                videoDurationIcon = videoDurationIcon,
                videoDurationTextStyle = videoDurationTextStyle,
                mediaLoaderStyle = mediaLoaderStyle,
                durationFormatter = SceytChatUIKit.formatters.mediaDurationFormatter,
            ).let { styleCustomizer.apply(context, it) }
        }
    }
}