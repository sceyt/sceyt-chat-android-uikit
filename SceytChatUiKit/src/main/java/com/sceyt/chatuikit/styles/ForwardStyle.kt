package com.sceyt.chatuikit.styles

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
import com.sceyt.chatuikit.styles.common.ButtonStyle
import com.sceyt.chatuikit.styles.common.SearchToolbarStyle
import com.sceyt.chatuikit.styles.common.SelectableListItemStyle
import com.sceyt.chatuikit.styles.extensions.forward.buildActionButtonStyle
import com.sceyt.chatuikit.styles.extensions.forward.buildChannelItemStyle
import com.sceyt.chatuikit.styles.extensions.forward.buildSearchToolbarStyle
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
 * */
data class ForwardStyle(
        @ColorInt override val backgroundColor: Int,
        override val searchToolbarStyle: SearchToolbarStyle,
        override val actionButtonStyle: ButtonStyle,
        override val channelItemStyle: ForwardChannelItemStyle,
) : ShareablePageStyle(backgroundColor, searchToolbarStyle, actionButtonStyle, channelItemStyle) {

    companion object {
        var styleCustomizer = StyleCustomizer<ForwardStyle> { _, style -> style }
    }

    internal class Builder(
            internal val context: Context,
            private val attributeSet: AttributeSet?
    ) {
        fun build(): ForwardStyle {
            context.obtainStyledAttributes(attributeSet, R.styleable.ChannelListView).use { array ->
                val backgroundColor = context.getCompatColor(SceytChatUIKitTheme.colors.backgroundColor)

                return ForwardStyle(
                    backgroundColor = backgroundColor,
                    searchToolbarStyle = buildSearchToolbarStyle(array),
                    actionButtonStyle = buildActionButtonStyle(array),
                    channelItemStyle = buildChannelItemStyle(array),
                ).let { styleCustomizer.apply(context, it) }
            }
        }
    }
}