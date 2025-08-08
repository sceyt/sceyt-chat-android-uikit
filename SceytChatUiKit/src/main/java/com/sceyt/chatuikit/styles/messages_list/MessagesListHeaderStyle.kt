package com.sceyt.chatuikit.styles.messages_list

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.content.res.use
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.formatters.SceytChatUIKitFormatters
import com.sceyt.chatuikit.formatters.attributes.ChannelEventTitleFormatterAttributes
import com.sceyt.chatuikit.presentation.components.channel.header.MessagesListHeaderView
import com.sceyt.chatuikit.renderers.AvatarRenderer
import com.sceyt.chatuikit.renderers.SceytChatUIKitRenderers
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.messages_list.MessagesListHeaderStyle.Companion.styleCustomizer
import com.sceyt.chatuikit.styles.common.AvatarStyle
import com.sceyt.chatuikit.styles.common.MenuStyle
import com.sceyt.chatuikit.styles.common.SearchInputStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.extensions.messages_list_header.buildAvatarStyle
import com.sceyt.chatuikit.styles.extensions.messages_list_header.buildMessageActionsMenuStyle
import com.sceyt.chatuikit.styles.extensions.messages_list_header.buildSearchInputTextStyle
import com.sceyt.chatuikit.styles.extensions.messages_list_header.buildSubTitleTextStyle
import com.sceyt.chatuikit.styles.extensions.messages_list_header.buildTitleTextStyle
import com.sceyt.chatuikit.theme.Colors

/**
 * Style for [MessagesListHeaderView] component.
 * @property backgroundColor background color of the header, default is [Colors.primaryColor]
 * @property underlineColor color of the underline, default is [Colors.borderColor]
 * @property navigationIcon icon for back button, default is [R.drawable.sceyt_ic_arrow_back]
 * @property showUnderline enable underline, default is true
 * @property showChannelEventsInSequence Whether to show users activity one at a time in sequence, default is true
 * @property enableChannelEventIndicator Whether to show a user activity animation at the end of the active users' title. Default is true.
 * @property titleTextStyle style for the title, default is [buildTitleTextStyle]
 * @property subTitleStyle style for the subtitle, default is [buildSubTitleTextStyle]
 * @property searchInputStyle style for the search input, default is [buildSearchInputTextStyle]
 * @property messageActionsMenuStyle style for the toolbar menu, default is [buildMessageActionsMenuStyle]
 * @property titleFormatter formatter for the channel title, default is [SceytChatUIKitFormatters.channelNameFormatter]
 * @property subtitleFormatter formatter for the channel subtitle, default is [SceytChatUIKitFormatters.channelSubtitleFormatter]
 * @property channelEventTitleFormatter formatter for the channel event title, default is [SceytChatUIKitFormatters.channelEventTitleFormatter]
 * @property channelAvatarRenderer renderer for the channel avatar, default is [SceytChatUIKitRenderers.channelAvatarRenderer]
 * */
data class MessagesListHeaderStyle(
        @param:ColorInt val backgroundColor: Int,
        @param:ColorInt val underlineColor: Int,
        val navigationIcon: Drawable?,
        val showUnderline: Boolean,
        val showChannelEventsInSequence: Boolean,
        val enableChannelEventIndicator: Boolean,
        val titleTextStyle: TextStyle,
        val subTitleStyle: TextStyle,
        val avatarStyle: AvatarStyle,
        val searchInputStyle: SearchInputStyle,
        val messageActionsMenuStyle: MenuStyle,
        val titleFormatter: Formatter<SceytChannel>,
        val subtitleFormatter: Formatter<SceytChannel>,
        val channelEventTitleFormatter: Formatter<ChannelEventTitleFormatterAttributes>,
        val channelAvatarRenderer: AvatarRenderer<SceytChannel>
) {

    companion object {
        var styleCustomizer = StyleCustomizer<MessagesListHeaderStyle> { _, style -> style }

        /**
         * Use this method if you are using [MessagesListHeaderView] in multiple places,
         * and want to customize the style for each view.
         * @param viewId - Id of the current [MessagesListHeaderView] which you want to customize.
         * @param customizer - Customizer for [MessagesListHeaderStyle].
         *
         * Note: If you have already set the [styleCustomizer], it will be overridden by this customizer.
         * */
        @Suppress("unused")
        @JvmStatic
        fun setStyleCustomizerForViewId(viewId: Int, customizer: StyleCustomizer<MessagesListHeaderStyle>) {
            styleCustomizers[viewId] = customizer
        }

        private var styleCustomizers: HashMap<Int, StyleCustomizer<MessagesListHeaderStyle>> = hashMapOf()
    }

    internal class Builder(
            internal val context: Context,
            private val attrs: AttributeSet?

    ) {
        fun build(): MessagesListHeaderStyle {
            context.obtainStyledAttributes(attrs, R.styleable.MessagesListHeaderView).use { array ->
                val viewId = array.getResourceId(R.styleable.MessagesListHeaderView_android_id, View.NO_ID)

                val backgroundColor = array.getColor(
                    R.styleable.MessagesListHeaderView_sceytUiMessagesListHeaderBackground,
                    context.getCompatColor(SceytChatUIKit.theme.colors.primaryColor))

                val navigationIcon = array.getDrawable(
                    R.styleable.MessagesListHeaderView_sceytUiMessagesListHeaderNavigationIcon)
                        ?: context.getCompatDrawable(R.drawable.sceyt_ic_arrow_back)
                            ?.applyTint(context, SceytChatUIKit.theme.colors.accentColor)

                val underlineColor = array.getColor(
                    R.styleable.MessagesListHeaderView_sceytUiMessagesListHeaderUnderlineColor,
                    context.getCompatColor(SceytChatUIKit.theme.colors.borderColor))

                val showUnderline = array.getBoolean(
                    R.styleable.MessagesListHeaderView_sceytUiMessagesListHeaderShowUnderline, true
                )

                val showUsersActivityInSequence = array.getBoolean(
                    R.styleable.MessagesListHeaderView_sceytUiMessagesListHeaderShowChannelEventsInSequence,
                    true
                )

                val enableChannelEventIndicator = array.getBoolean(
                    R.styleable.MessagesListHeaderView_sceytUiMessagesListHeaderEnableChannelEventIndicator,
                    true
                )

                return MessagesListHeaderStyle(
                    backgroundColor = backgroundColor,
                    underlineColor = underlineColor,
                    navigationIcon = navigationIcon,
                    showUnderline = showUnderline,
                    showChannelEventsInSequence = showUsersActivityInSequence,
                    enableChannelEventIndicator = enableChannelEventIndicator,
                    titleTextStyle = buildTitleTextStyle(array),
                    subTitleStyle = buildSubTitleTextStyle(array),
                    avatarStyle = buildAvatarStyle(array),
                    searchInputStyle = buildSearchInputTextStyle(array),
                    messageActionsMenuStyle = buildMessageActionsMenuStyle(array),
                    titleFormatter = SceytChatUIKit.formatters.channelNameFormatter,
                    subtitleFormatter = SceytChatUIKit.formatters.channelSubtitleFormatter,
                    channelEventTitleFormatter = SceytChatUIKit.formatters.channelEventTitleFormatter,
                    channelAvatarRenderer = SceytChatUIKit.renderers.channelAvatarRenderer
                ).let {
                    (styleCustomizers[viewId] ?: styleCustomizer).apply(context, it)
                }
            }
        }
    }
}