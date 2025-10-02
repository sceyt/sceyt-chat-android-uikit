package com.sceyt.chatuikit.styles.share

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.core.content.res.use
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.presentation.components.share.ShareActivity
import com.sceyt.chatuikit.renderers.AvatarRenderer
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.ButtonStyle
import com.sceyt.chatuikit.styles.common.SearchToolbarStyle
import com.sceyt.chatuikit.styles.common.SelectableListItemStyle
import com.sceyt.chatuikit.styles.common.TextInputStyle
import com.sceyt.chatuikit.styles.extensions.share.buildActionButtonStyle
import com.sceyt.chatuikit.styles.extensions.share.buildChannelItemStyle
import com.sceyt.chatuikit.styles.extensions.share.buildMessageInputStyle
import com.sceyt.chatuikit.styles.extensions.share.buildSearchToolbarStyle
import com.sceyt.chatuikit.theme.Colors
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme

typealias ShareChannelItemStyle = SelectableListItemStyle<
        Formatter<SceytChannel>,
        Formatter<SceytChannel>,
        AvatarRenderer<SceytChannel>>

/**
 * Style for [ShareActivity].
 * @property backgroundColor Background color of the page, default is [Colors.backgroundColor].
 * @property messageInputStyle Style for the message input, default is [buildMessageInputStyle].
 * @property searchToolbarStyle Style for the search toolbar, default is [buildSearchToolbarStyle].
 * @property actionButtonStyle Style for the action button, default is [buildActionButtonStyle].
 * @property channelItemStyle Style for the channel item, default is [buildChannelItemStyle].
 * */
data class ShareStyle(
        @param:ColorInt override val backgroundColor: Int,
        val messageInputStyle: TextInputStyle,
        override val searchToolbarStyle: SearchToolbarStyle,
        override val actionButtonStyle: ButtonStyle,
        override val channelItemStyle: ShareChannelItemStyle,
) : ShareablePageStyle(backgroundColor, searchToolbarStyle, actionButtonStyle, channelItemStyle) {

    companion object {
        var styleCustomizer = StyleCustomizer<ShareStyle> { _, style -> style }
    }

    internal class Builder(
            internal val context: Context,
            private val attributeSet: AttributeSet?
    ) {
        fun build(): ShareStyle {
            context.obtainStyledAttributes(attributeSet, R.styleable.Share).use { array ->
                val backgroundColor = context.getCompatColor(SceytChatUIKitTheme.colors.backgroundColor)

                return ShareStyle(
                    backgroundColor = backgroundColor,
                    messageInputStyle = buildMessageInputStyle(array),
                    searchToolbarStyle = buildSearchToolbarStyle(array),
                    actionButtonStyle = buildActionButtonStyle(array),
                    channelItemStyle = buildChannelItemStyle(array),
                ).let { styleCustomizer.apply(context, it) }
            }
        }
    }
}