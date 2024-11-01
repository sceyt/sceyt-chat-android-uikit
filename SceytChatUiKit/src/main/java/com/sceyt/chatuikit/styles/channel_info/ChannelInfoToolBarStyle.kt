package com.sceyt.chatuikit.styles.channel_info

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.renderers.ChannelAvatarRenderer
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.AvatarStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme

data class ChannelInfoToolBarStyle(
        @ColorInt val backgroundColor: Int,
        val expandedStateTitle: String,
        val navigationIcon: Drawable?,
        val editIcon: Drawable?,
        val moreIcon: Drawable?,
        val expandedStateTitleTextStyle: TextStyle,
        val collapsedStateTitleTextStyle: TextStyle,
        val collapsedStateSubtitleTextStyle: TextStyle,
        val avatarStyle: AvatarStyle,
        val channelNameFormatter: Formatter<SceytChannel>,
        val channelSubtitleFormatter: Formatter<SceytChannel>,
        val channelAvatarRenderer: ChannelAvatarRenderer
) {
    companion object {
        var styleCustomizer = StyleCustomizer<ChannelInfoToolBarStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
            private val attributeSet: AttributeSet?
    ) {
        fun build(): ChannelInfoToolBarStyle {
            val backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.primaryColor)

            val expandedStateTitle = context.getString(R.string.sceyt_info)
            val navigationIcon = context.getCompatDrawable(R.drawable.sceyt_ic_arrow_back).applyTint(
                context.getCompatColor(SceytChatUIKitTheme.colors.accentColor)
            )
            val editIcon = context.getCompatDrawable(R.drawable.sceyt_ic_edit_stroked).applyTint(
                context.getCompatColor(SceytChatUIKitTheme.colors.accentColor)
            )
            val moreIcon = context.getCompatDrawable(R.drawable.sceyt_ic_more_24).applyTint(
                context.getCompatColor(SceytChatUIKitTheme.colors.accentColor)
            )
            val expandedStateTitleTextStyle = TextStyle(
                color = context.getCompatColor(SceytChatUIKitTheme.colors.textPrimaryColor),
                font = R.font.roboto_medium
            )

            val collapsedStateTitleTextStyle = TextStyle(
                color = context.getCompatColor(SceytChatUIKitTheme.colors.textPrimaryColor),
                font = R.font.roboto_medium
            )

            val collapsedStateSubtitleTextStyle = TextStyle(
                color = context.getCompatColor(SceytChatUIKitTheme.colors.textSecondaryColor),
                font = R.font.roboto_regular
            )

            return ChannelInfoToolBarStyle(
                backgroundColor = backgroundColor,
                expandedStateTitle = expandedStateTitle,
                navigationIcon = navigationIcon,
                editIcon = editIcon,
                moreIcon = moreIcon,
                expandedStateTitleTextStyle = expandedStateTitleTextStyle,
                collapsedStateTitleTextStyle = collapsedStateTitleTextStyle,
                collapsedStateSubtitleTextStyle = collapsedStateSubtitleTextStyle,
                avatarStyle = AvatarStyle(),
                channelNameFormatter = SceytChatUIKit.formatters.channelNameFormatter,
                channelSubtitleFormatter = SceytChatUIKit.formatters.channelSubtitleFormatter,
                channelAvatarRenderer = SceytChatUIKit.renderers.channelAvatarRenderer
            ).let { styleCustomizer.apply(context, it) }
        }
    }
}
