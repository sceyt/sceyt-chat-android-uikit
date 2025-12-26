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
import com.sceyt.chatuikit.presentation.components.channel.messages.preview.SelfDestructingMediaPreviewActivity
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.common.ToolbarStyle
import com.sceyt.chatuikit.styles.extensions.self_destructing_media_preview.buildMessageBodyBackgroundStyle
import com.sceyt.chatuikit.styles.extensions.self_destructing_media_preview.buildMessageBodyTextStyle
import com.sceyt.chatuikit.styles.extensions.self_destructing_media_preview.buildTimelineTextStyle
import com.sceyt.chatuikit.styles.extensions.self_destructing_media_preview.buildToolbarStyle
import com.sceyt.chatuikit.theme.Colors
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme
import java.util.Date

/** Style for [SelfDestructingMediaPreviewActivity].
 * @property backgroundColor Background color of the media preview, default is [Color.BLACK].
 * @property toolbarStyle Style for the toolbar with self-destruct indicator.
 * @property userNameFormatter Formatter for the user name, default is [SceytChatUIKitFormatters.userNameFormatter].
 * @property mediaDateFormatter Formatter for the media date, default is [SceytChatUIKitFormatters.mediaPreviewDateFormatter].
 * @property messageBodyBackgroundStyle Background style for the message body container (supports color, gradient, borders, etc).
 * @property messageBodyTextStyle Style for the message body text.
 * @property videoControllerBackgroundColor Color of the video controller, default is [R.color.sceyt_media_primary_color].
 * @property trackColor Color of the timeline track, default is [Colors.iconSecondaryColor].
 * @property progressColor Color of the timeline progress, default is [Colors.onPrimaryColor].
 * @property thumbColor Color of the timeline thumb, default is [Colors.onPrimaryColor].
 * @property pauseIcon Icon for the pause button, default is [R.drawable.sceyt_ic_pause].
 * @property playIcon Icon for the play button, default is [R.drawable.sceyt_ic_play].
 * @property timelineTextStyle Style for the timeline text, default is [buildTimelineTextStyle] with primary color.
 * */

data class SelfDestructingMediaPreviewStyle(
    @param:ColorInt val backgroundColor: Int,
    val toolbarStyle: ToolbarStyle,
    val userNameFormatter: Formatter<SceytUser>,
    val mediaDateFormatter: Formatter<Date>,
    val messageBodyBackgroundStyle: BackgroundStyle,
    val messageBodyTextStyle: TextStyle,
    @param:ColorInt val videoControllerBackgroundColor: Int,
    @param:ColorInt val trackColor: Int,
    @param:ColorInt val progressColor: Int,
    @param:ColorInt val thumbColor: Int,
    val pauseIcon: Drawable?,
    val playIcon: Drawable?,
    val timelineTextStyle: TextStyle
) {
    companion object {
        var styleCustomizer =
            StyleCustomizer<SelfDestructingMediaPreviewStyle> { _, style -> style }
    }

    internal class Builder(
        internal val context: Context,
        private val attributeSet: AttributeSet?
    ) {
        fun build(): SelfDestructingMediaPreviewStyle {
            context.obtainStyledAttributes(attributeSet, R.styleable.SelfDestructingMediaPreview)
                .use { array ->
                    val opPrimaryColor =
                        context.getCompatColor(SceytChatUIKitTheme.colors.onPrimaryColor)

                    val backgroundColor = Color.BLACK
                    val videoControllerBackgroundColor =
                        context.getCompatColor(R.color.sceyt_media_primary_color)

                    val pauseIcon = context.getCompatDrawable(R.drawable.sceyt_ic_pause).applyTint(
                        context.getCompatColor(SceytChatUIKitTheme.colors.onPrimaryColor)
                    )
                    val playIcon = context.getCompatDrawable(R.drawable.sceyt_ic_play).applyTint(
                        context.getCompatColor(SceytChatUIKitTheme.colors.onPrimaryColor)
                    )
                    val trackColor =
                        context.getCompatColor(SceytChatUIKit.theme.colors.iconSecondaryColor)

                    return SelfDestructingMediaPreviewStyle(
                        backgroundColor = backgroundColor,
                        toolbarStyle = buildToolbarStyle(array),
                        userNameFormatter = SceytChatUIKit.formatters.userNameFormatter,
                        mediaDateFormatter = SceytChatUIKit.formatters.mediaPreviewDateFormatter,
                        messageBodyBackgroundStyle = buildMessageBodyBackgroundStyle(array),
                        messageBodyTextStyle = buildMessageBodyTextStyle(array),
                        videoControllerBackgroundColor = videoControllerBackgroundColor,
                        trackColor = trackColor,
                        progressColor = opPrimaryColor,
                        thumbColor = opPrimaryColor,
                        pauseIcon = pauseIcon,
                        playIcon = playIcon,
                        timelineTextStyle = buildTimelineTextStyle(array)
                    ).let { styleCustomizer.apply(context, it) }
                }
        }
    }
}