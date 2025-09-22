package com.sceyt.chatuikit.styles.preview

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.core.content.res.use
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.formatters.SceytChatUIKitFormatters
import com.sceyt.chatuikit.presentation.components.media.MediaPreviewActivity
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.MediaLoaderStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.common.ToolbarStyle
import com.sceyt.chatuikit.styles.extensions.media_preview.buildMediaLoaderStyle
import com.sceyt.chatuikit.styles.extensions.media_preview.buildTimelineTextStyle
import com.sceyt.chatuikit.styles.extensions.media_preview.buildToolbarStyle
import com.sceyt.chatuikit.theme.Colors
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme
import java.util.Date

/** Style for [MediaPreviewActivity].
 * @property backgroundColor Background color of the media preview, default is [Color.BLACK].
 * @property videoControllerBackgroundColor Color of the video controller, default is [R.color.sceyt_media_primary_color].
 * @property trackColor Color of the track, default is [Colors.iconSecondaryColor].
 * @property progressColor Color of the progress, default is [Colors.primaryColor].
 * @property thumbColor Color of the thumb, default is [Colors.primaryColor].
 * @property pauseIcon Icon for the pause button, default is [R.drawable.sceyt_ic_pause].
 * @property playIcon Icon for the play button, default is [R.drawable.sceyt_ic_play].
 * @property timelineTextStyle Style for the timeline text, default is [buildTimelineTextStyle] with primary color.
 * @property toolbarStyle Style for the toolbar, default is [buildToolbarStyle] with primary color.
 * @property mediaLoaderStyle Style for the media loader, default is [buildMediaLoaderStyle].
 * @property userNameFormatter Formatter for the user name, default is [SceytChatUIKitFormatters.userNameFormatter].
 * @property mediaDateFormatter Formatter for the media date, default is [SceytChatUIKitFormatters.mediaPreviewDateFormatter].
 * */

data class MediaPreviewStyle(
        @param:ColorInt val backgroundColor: Int,
        @param:ColorInt val videoControllerBackgroundColor: Int,
        @param:ColorInt val trackColor: Int,
        @param:ColorInt val progressColor: Int,
        @param:ColorInt val thumbColor: Int,
        val pauseIcon: Drawable?,
        val playIcon: Drawable?,
        val timelineTextStyle: TextStyle,
        val toolbarStyle: ToolbarStyle,
        val mediaLoaderStyle: MediaLoaderStyle,
        val userNameFormatter: Formatter<SceytUser>,
        val mediaDateFormatter: Formatter<Date>
) {
    companion object {
        var styleCustomizer = StyleCustomizer<MediaPreviewStyle> { _, style -> style }
    }

    internal class Builder(
            internal val context: Context,
            private val attributeSet: AttributeSet?
    ) {
        fun build(): MediaPreviewStyle {
            context.obtainStyledAttributes(attributeSet, R.styleable.MediaPreview).use { array ->
                val opPrimaryColor = context.getCompatColor(SceytChatUIKitTheme.colors.onPrimaryColor)

                val backgroundColor = Color.BLACK
                val videoControllerBackgroundColor = context.getCompatColor(R.color.sceyt_media_primary_color)

                val pauseIcon = context.getCompatDrawable(R.drawable.sceyt_ic_pause).applyTint(
                    context.getCompatColor(SceytChatUIKitTheme.colors.onPrimaryColor)
                )
                val playIcon = context.getCompatDrawable(R.drawable.sceyt_ic_play).applyTint(
                    context.getCompatColor(SceytChatUIKitTheme.colors.onPrimaryColor)
                )
                val trackColor = context.getCompatColor(SceytChatUIKit.theme.colors.iconSecondaryColor)

                return MediaPreviewStyle(
                    backgroundColor = backgroundColor,
                    videoControllerBackgroundColor = videoControllerBackgroundColor,
                    pauseIcon = pauseIcon,
                    playIcon = playIcon,
                    trackColor = trackColor,
                    progressColor = opPrimaryColor,
                    thumbColor = opPrimaryColor,
                    timelineTextStyle = buildTimelineTextStyle(array),
                    toolbarStyle = buildToolbarStyle(array),
                    mediaLoaderStyle = buildMediaLoaderStyle(array),
                    userNameFormatter = SceytChatUIKit.formatters.userNameFormatter,
                    mediaDateFormatter = SceytChatUIKit.formatters.mediaPreviewDateFormatter
                ).let { styleCustomizer.apply(context, it) }
            }
        }
    }
}
