package com.sceyt.chatuikit.styles.forward

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.core.content.res.use
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.presentation.components.forward.ForwardActivity
import com.sceyt.chatuikit.renderers.AvatarRenderer
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.ButtonStyle
import com.sceyt.chatuikit.styles.common.EmptyStateStyle
import com.sceyt.chatuikit.styles.common.SearchToolbarStyle
import com.sceyt.chatuikit.styles.common.SelectableListItemStyle
import com.sceyt.chatuikit.styles.common.buildEmptyStateStyle
import com.sceyt.chatuikit.styles.extensions.forward.buildActionButtonStyle
import com.sceyt.chatuikit.styles.extensions.forward.buildChannelItemStyle
import com.sceyt.chatuikit.styles.extensions.forward.buildSearchToolbarStyle
import com.sceyt.chatuikit.styles.share.ShareablePageStyle
import com.sceyt.chatuikit.theme.Colors
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme

typealias ForwardChannelItemStyle = SelectableListItemStyle<
        Formatter<SceytChannel>,
        Formatter<SceytChannel>,
        AvatarRenderer<SceytChannel>>

/**
 * Style for [ForwardActivity].
 * @property backgroundColor Background color of the page, default is [Colors.backgroundColor].
 * @property searchToolbarStyle Style for the search toolbar, default is [buildSearchToolbarStyle].
 * @property actionButtonStyle Style for the action button, default is [buildActionButtonStyle].
 * @property channelItemStyle Style for the channel item, default is [buildChannelItemStyle].
 * @property emptyStateStyle Style for the empty state view with icon, title, and subtitle customization.
 * */
data class ForwardStyle(
        @param:ColorInt override val backgroundColor: Int,
        override val searchToolbarStyle: SearchToolbarStyle,
        override val actionButtonStyle: ButtonStyle,
        override val channelItemStyle: ForwardChannelItemStyle,
        override val emptyStateStyle: EmptyStateStyle,
) : ShareablePageStyle(backgroundColor, searchToolbarStyle, actionButtonStyle, channelItemStyle, emptyStateStyle) {

    companion object {
        var styleCustomizer = StyleCustomizer<ForwardStyle> { _, style -> style }
    }

    internal class Builder(
            internal val context: Context,
            private val attributeSet: AttributeSet?
    ) {
        fun build(): ForwardStyle {
            context.obtainStyledAttributes(attributeSet, R.styleable.Forward).use { array ->
                val backgroundColor = context.getCompatColor(SceytChatUIKitTheme.colors.backgroundColor)

                val emptyStateStyle = buildEmptyStateStyle(
                    context = context,
                    iconRes = R.drawable.sceyt_ic_search,
                    titleText = context.getString(R.string.sceyt_ui_channel_list_empty),
                    subtitleText = context.getString(R.string.sceyt_ui_channel_list_empty_desc)
                )

                return ForwardStyle(
                    backgroundColor = backgroundColor,
                    searchToolbarStyle = buildSearchToolbarStyle(array),
                    actionButtonStyle = buildActionButtonStyle(array),
                    channelItemStyle = buildChannelItemStyle(array),
                    emptyStateStyle = emptyStateStyle,
                ).let { styleCustomizer.apply(context, it) }
            }
        }
    }
}