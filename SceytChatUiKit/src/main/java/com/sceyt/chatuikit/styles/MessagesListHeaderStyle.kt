package com.sceyt.chatuikit.styles

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.formatters.SceytChatUIKitFormatters
import com.sceyt.chatuikit.presentation.components.channel.header.MessagesListHeaderView
import com.sceyt.chatuikit.presentation.custom_views.AvatarView
import com.sceyt.chatuikit.providers.VisualProvider
import com.sceyt.chatuikit.providers.defaults.DefaultChannelDefaultAvatarProvider
import com.sceyt.chatuikit.styles.common.MenuStyle
import com.sceyt.chatuikit.styles.common.SearchInputStyle
import com.sceyt.chatuikit.styles.common.TextStyle
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
 * @property titleTextStyle style for the title, default is [buildTitleTextStyle]
 * @property subTitleStyle style for the subtitle, default is [buildSubTitleTextStyle]
 * @property searchInputStyle style for the search input, default is [buildSearchInputTextStyle]
 * @property messageActionsMenuStyle style for the toolbar menu, default is [buildMessageActionsMenuStyle]
 * @property titleFormatter formatter for the channel title, default is [SceytChatUIKitFormatters.channelNameFormatter]
 * @property subtitleFormatter formatter for the channel subtitle, default is [SceytChatUIKitFormatters.channelSubtitleFormatter]
 * @property defaultAvatarProvider provider for the channel default avatar, default is [DefaultChannelDefaultAvatarProvider]
 * @property typingUserNameFormatter formatter for the typing users, default is [SceytChatUIKitFormatters.userShortNameFormatter]
 * */
data class MessagesListHeaderStyle(
        @ColorInt val backgroundColor: Int,
        @ColorInt val underlineColor: Int,
        val navigationIcon: Drawable?,
        val showUnderline: Boolean,
        val titleTextStyle: TextStyle,
        val subTitleStyle: TextStyle,
        val searchInputStyle: SearchInputStyle,
        val messageActionsMenuStyle: MenuStyle,
        val titleFormatter: Formatter<SceytChannel>,
        val subtitleFormatter: Formatter<SceytChannel>,
        val defaultAvatarProvider: VisualProvider<SceytChannel, AvatarView.DefaultAvatar>,
        val typingUserNameFormatter: Formatter<SceytUser>
) {

    companion object {
        var styleCustomizer = StyleCustomizer<MessagesListHeaderStyle> { _, style -> style }
    }

    internal class Builder(
            internal val context: Context,
            private val attrs: AttributeSet?

    ) {
        fun build(): MessagesListHeaderStyle {
            context.obtainStyledAttributes(attrs, R.styleable.MessagesListHeaderView).use { array ->
                val backgroundColor = array.getColor(R.styleable.MessagesListHeaderView_sceytUiMessagesListHeaderBackground,
                    context.getCompatColor(SceytChatUIKit.theme.colors.primaryColor))

                val navigationIcon = array.getDrawable(R.styleable.MessagesListHeaderView_sceytUiMessagesListHeaderNavigationIcon)
                        ?: context.getCompatDrawable(R.drawable.sceyt_ic_arrow_back)
                            ?.applyTint(context, SceytChatUIKit.theme.colors.accentColor)

                val underlineColor = array.getColor(R.styleable.MessagesListHeaderView_sceytUiMessagesListHeaderUnderlineColor,
                    context.getCompatColor(SceytChatUIKit.theme.colors.borderColor))

                val showUnderline = array.getBoolean(R.styleable.MessagesListHeaderView_sceytUiMessagesListHeaderShowUnderline,
                    true)

                return MessagesListHeaderStyle(
                    backgroundColor = backgroundColor,
                    underlineColor = underlineColor,
                    navigationIcon = navigationIcon,
                    showUnderline = showUnderline,
                    titleTextStyle = buildTitleTextStyle(array),
                    subTitleStyle = buildSubTitleTextStyle(array),
                    searchInputStyle = buildSearchInputTextStyle(array),
                    messageActionsMenuStyle = buildMessageActionsMenuStyle(array),
                    titleFormatter = SceytChatUIKit.formatters.channelNameFormatter,
                    subtitleFormatter = SceytChatUIKit.formatters.channelSubtitleFormatter,
                    defaultAvatarProvider = SceytChatUIKit.providers.channelDefaultAvatarProvider,
                    typingUserNameFormatter = SceytChatUIKit.formatters.typingUserNameFormatter
                ).let { styleCustomizer.apply(context, it) }
            }
        }
    }
}