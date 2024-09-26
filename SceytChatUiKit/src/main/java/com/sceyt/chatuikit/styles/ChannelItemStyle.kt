package com.sceyt.chatuikit.styles

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.core.content.res.use
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.formatters.SceytChatUIKitFormatters
import com.sceyt.chatuikit.providers.VisualProvider
import com.sceyt.chatuikit.providers.defaults.DefaultChannelListAttachmentIconProvider
import com.sceyt.chatuikit.styles.common.MessageDeliveryStatusIcons
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme
import java.util.Date

/**
 * Style for [ChannelItemStyle] component.
 * @property pinnedChannelBackgroundColor - Background color of the pinned channel, default is [SceytChatUIKitTheme.surface1Color].
 * @property onlineStateColor - Color of the online status indicator, default is [R.color.sceyt_color_green].
 * @property dividerColor - Color of the divider, default is [Color.TRANSPARENT].
 * @property linkTextColor - Color of the link text, default is [R.color.sceyt_auto_link_color].
 * @property mutedIcon - Icon for muted channel, default is [R.drawable.sceyt_ic_muted].
 * @property pinIcon - Icon for pinned channel, default is [R.drawable.sceyt_ic_pin_filled].
 * @property messageDeliveryStatusIcons - Icons for message delivery status, default is [MessageDeliveryStatusIcons].
 * @property autoDeletedChannelIcon - Icon for auto deleted channel, default is [R.drawable.sceyt_ic_auto_deleted_channel].
 * @property deliveryStatusIndicatorSize - Size of the status icon, default is 16dp.
 * @property subjectTextStyle - Style for channel subject, default is [TextStyle].
 * @property lastMessageTextStyle - Style for last message, default is [TextStyle].
 * @property dateTextStyle - Style for date, default is [TextStyle].
 * @property messageSenderNameStyle - Style for sender name, default is [TextStyle].
 * @property deletedTextStyle - Style for deleted message, default is [TextStyle].
 * @property draftPrefixTextStyle - Style for draft message, default is [TextStyle].
 * @property typingTextStyle - Style for typing message, default is [TextStyle].
 * @property unreadCountTextStyle - Style for unread count, default is [TextStyle].
 * @property unreadCountMutedStateTextStyle - Style for unread count in muted channel, default is [TextStyle].
 * @property mentionTextStyle - Style for mention message, default is [TextStyle].
 * @property mentionMutedStateTextStyle - Style for mention message in muted channel, default is [TextStyle].
 * @property channelNameFormatter - Formatter for channel name, default is [SceytChatUIKitFormatters.channelNameFormatter].
 * @property channelDateFormatter - Date format for channel, default is [SceytChatUIKitFormatters.channelDateFormatter].
 * @property userNameFormatter - Formatter for user name, default is [SceytChatUIKitFormatters.userNameFormatter].
 * @property messageSenderNameFormatter - Formatter for user name, default is [SceytChatUIKitFormatters.messageSenderNameFormatter].
 * @property mentionUserNameFormatter - Formatter for user name, default is [SceytChatUIKitFormatters.mentionUserNameFormatter].
 * @property reactedUserNameFormatter - Formatter for user name, default is [SceytChatUIKitFormatters.reactedUserNameFormatter].
 * @property typingUserNameFormatter - Formatter for user name, default is [SceytChatUIKitFormatters.typingUserNameFormatter].
 * @property attachmentIconProvider - Provider for attachment icon, default is [DefaultChannelListAttachmentIconProvider].
 * */
data class ChannelItemStyle(
        @ColorInt val pinnedChannelBackgroundColor: Int,
        @ColorInt val onlineStateColor: Int,
        @ColorInt val dividerColor: Int,
        @ColorInt val linkTextColor: Int,
        val mutedIcon: Drawable?,
        val pinIcon: Drawable?,
        val messageDeliveryStatusIcons: MessageDeliveryStatusIcons,
        val autoDeletedChannelIcon: Drawable?,
        val deliveryStatusIndicatorSize: Int,
        val subjectTextStyle: TextStyle,
        val lastMessageTextStyle: TextStyle,
        val dateTextStyle: TextStyle,
        val messageSenderNameStyle: TextStyle,
        val deletedTextStyle: TextStyle,
        val draftPrefixTextStyle: TextStyle,
        val typingTextStyle: TextStyle,
        val unreadCountTextStyle: TextStyle,
        val unreadCountMutedStateTextStyle: TextStyle,
        val mentionTextStyle: TextStyle,
        val mentionMutedStateTextStyle: TextStyle,
        val channelNameFormatter: Formatter<SceytChannel>,
        val channelDateFormatter: Formatter<Date>,
        val userNameFormatter: Formatter<User>,
        val messageSenderNameFormatter: Formatter<User>,
        val mentionUserNameFormatter: Formatter<User>,
        val reactedUserNameFormatter: Formatter<User>,
        val typingUserNameFormatter: Formatter<User>,
        val attachmentIconProvider: VisualProvider<SceytAttachment, Drawable?>
) {

    companion object {
        @JvmField
        var styleCustomizer = StyleCustomizer<ChannelItemStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
            private val attrs: AttributeSet?
    ) {
        fun build(): ChannelItemStyle {
            context.obtainStyledAttributes(attrs, R.styleable.ChannelListView).use { array ->
                val pinnedChannelBackgroundColor = array.getColor(R.styleable.ChannelListView_sceytUiChannelListPinnedBackgroundColor,
                    context.getCompatColor(SceytChatUIKit.theme.surface1Color))

                val onlineStateColor = array.getColor(R.styleable.ChannelListView_sceytUiChannelListOnlineStateColor,
                    context.getCompatColor(R.color.sceyt_color_green))

                val dividerColor = array.getColor(R.styleable.ChannelListView_sceytUiChannelListDividerColor, Color.TRANSPARENT)

                val linkTextColor = array.getColor(R.styleable.ChannelListView_sceytUiChannelListLinkTextColor,
                    context.getCompatColor(R.color.sceyt_auto_link_color))

                val mutedIcon = array.getDrawable(R.styleable.ChannelListView_sceytUiChannelListMutedIcon)
                        ?: context.getCompatDrawable(R.drawable.sceyt_ic_muted)?.apply {
                            mutate().setTint(context.getCompatColor(SceytChatUIKit.theme.iconSecondaryColor))
                        }

                val pinIcon = array.getDrawable(R.styleable.ChannelListView_sceytUiChannelListPinnedIcon)
                        ?: context.getCompatDrawable(R.drawable.sceyt_ic_pin_filled)?.apply {
                            mutate().setTint(context.getCompatColor(SceytChatUIKit.theme.iconSecondaryColor))
                        }

                val autoDeletedChannelIcon = array.getDrawable(R.styleable.ChannelListView_sceytUiChannelListAutoDeletedChannelIcon)
                        ?: context.getCompatDrawable(R.drawable.sceyt_ic_auto_deleted_channel)

                val messageDeliveryStatusIcons = MessageDeliveryStatusIcons.Builder(context, array)
                    .setPendingIconFromStyle(R.styleable.ChannelListView_sceytUiChannelListStatusPendingIndicator)
                    .setSentIconFromStyle(R.styleable.ChannelListView_sceytUiChannelListStatusSentIndicator)
                    .setReceivedIconIconFromStyle(R.styleable.ChannelListView_sceytUiChannelListStatusReceivedIndicator)
                    .setDisplayedIconFromStyle(R.styleable.ChannelListView_sceytUiChannelListStatusDisplayedIndicator)
                    .build()

                val statusIconSize = array.getDimensionPixelSize(R.styleable.ChannelListView_sceytUiChannelListStatusIndicatorSize,
                    dpToPx(16f))

                val subjectTextStyle = TextStyle.Builder(array)
                    .setColor(
                        index = R.styleable.ChannelListView_sceytUiChannelListChannelSubjectTextColor,
                        defValue = context.getCompatColor(SceytChatUIKit.theme.textPrimaryColor)
                    )
                    .setSize(
                        index = R.styleable.ChannelListView_sceytUiChannelListChannelSubjectTextSize,
                    )
                    .setStyle(
                        index = R.styleable.ChannelListView_sceytUiChannelListChannelSubjectTextStyle,
                        defValue = Typeface.NORMAL
                    )
                    .setFont(
                        index = R.styleable.ChannelListView_sceytUiChannelListChannelSubjectTextFont,
                        defValue = R.font.roboto_medium
                    )
                    .build()


                val lastMessageTextStyle = TextStyle.Builder(array)
                    .setColor(
                        index = R.styleable.ChannelListView_sceytUiChannelListLastMessageTextColor,
                        defValue = context.getCompatColor(SceytChatUIKit.theme.textSecondaryColor)
                    )
                    .setSize(
                        index = R.styleable.ChannelListView_sceytUiChannelListLastMessageTextSize,
                    )
                    .setStyle(
                        index = R.styleable.ChannelListView_sceytUiChannelListLastMessageTextStyle,
                        defValue = Typeface.NORMAL
                    )
                    .setFont(
                        index = R.styleable.ChannelListView_sceytUiChannelListLastMessageTextFont
                    )
                    .build()

                val dateTextStyle = TextStyle.Builder(array)
                    .setColor(
                        index = R.styleable.ChannelListView_sceytUiChannelListDateTextColor,
                        defValue = context.getCompatColor(SceytChatUIKit.theme.textSecondaryColor)
                    )
                    .setSize(
                        index = R.styleable.ChannelListView_sceytUiChannelListDateTextSize,
                    )
                    .setStyle(
                        index = R.styleable.ChannelListView_sceytUiChannelListDateTextStyle,
                        defValue = Typeface.NORMAL
                    )
                    .setFont(
                        index = R.styleable.ChannelListView_sceytUiChannelListDateTextFont
                    )
                    .build()

                val messageSenderNameStyle = TextStyle.Builder(array)
                    .setColor(
                        index = R.styleable.ChannelListView_sceytUiChannelListMessageSenderNameTextColor,
                        defValue = context.getCompatColor(SceytChatUIKit.theme.textPrimaryColor)
                    )
                    .setSize(
                        index = R.styleable.ChannelListView_sceytUiChannelListMessageSenderNameTextSize,
                    )
                    .setStyle(
                        index = R.styleable.ChannelListView_sceytUiChannelListMessageSenderNameTextStyle,
                        defValue = Typeface.NORMAL
                    )
                    .setFont(
                        index = R.styleable.ChannelListView_sceytUiChannelListMessageSenderNameTextFont,
                    )
                    .build()

                val deletedTextStyle = TextStyle.Builder(array)
                    .setColor(
                        index = R.styleable.ChannelListView_sceytUiChannelListDeletedTextColor,
                        defValue = context.getCompatColor(SceytChatUIKit.theme.textSecondaryColor)
                    )
                    .setSize(
                        index = R.styleable.ChannelListView_sceytUiChannelListDeletedTextSize,
                    )
                    .setStyle(
                        index = R.styleable.ChannelListView_sceytUiChannelListDeletedTextStyle,
                        defValue = Typeface.ITALIC
                    )
                    .setFont(
                        index = R.styleable.ChannelListView_sceytUiChannelListDeletedTextFont
                    )
                    .build()

                val draftPrefixTextStyle = TextStyle.Builder(array)
                    .setColor(
                        index = R.styleable.ChannelListView_sceytUiChannelListDraftPrefixTextColor,
                        defValue = context.getCompatColor(R.color.sceyt_color_red)
                    )
                    .setSize(
                        index = R.styleable.ChannelListView_sceytUiChannelListDraftPrefixTextSize,
                    )
                    .setStyle(
                        index = R.styleable.ChannelListView_sceytUiChannelListDraftPrefixTextStyle,
                        defValue = Typeface.NORMAL
                    )
                    .setFont(
                        index = R.styleable.ChannelListView_sceytUiChannelListDraftTextFont
                    )
                    .build()

                val typingTextStyle = TextStyle.Builder(array)
                    .setColor(
                        index = R.styleable.ChannelListView_sceytUiChannelListTypingTextColor,
                        defValue = context.getCompatColor(SceytChatUIKit.theme.textSecondaryColor)
                    )
                    .setSize(
                        index = R.styleable.ChannelListView_sceytUiChannelListTypingTextSize,
                    )
                    .setStyle(
                        index = R.styleable.ChannelListView_sceytUiChannelListTypingTextStyle,
                        defValue = Typeface.ITALIC
                    )
                    .setFont(
                        index = R.styleable.ChannelListView_sceytUiChannelListTypingTextFont
                    )
                    .build()

                val unreadCountTextStyle = TextStyle.Builder(array)
                    .setBackgroundColor(
                        index = R.styleable.ChannelListView_sceytUiChannelListUnreadCountBackgroundColor,
                        defValue = context.getCompatColor(SceytChatUIKit.theme.accentColor)
                    )
                    .setColor(
                        index = R.styleable.ChannelListView_sceytUiChannelListUnreadCountTextColor,
                        defValue = context.getCompatColor(SceytChatUIKit.theme.onPrimaryColor)
                    )
                    .setSize(
                        index = R.styleable.ChannelListView_sceytUiChannelListUnreadCountTextSize,
                    )
                    .setStyle(
                        index = R.styleable.ChannelListView_sceytUiChannelListUnreadCountTextStyle,
                        defValue = Typeface.NORMAL
                    )
                    .setFont(
                        index = R.styleable.ChannelListView_sceytUiChannelListUnreadCountTextFont
                    )
                    .build()

                val unreadCountMutedTextStyle = TextStyle.Builder(array)
                    .setBackgroundColor(
                        index = R.styleable.ChannelListView_sceytUiChannelListUnreadCountMutedStateBackgroundColor,
                        defValue = context.getCompatColor(SceytChatUIKit.theme.surface3Color)
                    )
                    .setColor(
                        index = R.styleable.ChannelListView_sceytUiChannelListUnreadCountMutedStateTextColor,
                        defValue = context.getCompatColor(SceytChatUIKit.theme.onPrimaryColor)
                    )
                    .setSize(
                        index = R.styleable.ChannelListView_sceytUiChannelListUnreadCountMutedStateTextSize,
                    )
                    .setStyle(
                        index = R.styleable.ChannelListView_sceytUiChannelListUnreadCountMutedStateTextStyle,
                        defValue = Typeface.NORMAL
                    )
                    .setFont(
                        index = R.styleable.ChannelListView_sceytUiChannelListUnreadCountMutedTextFont
                    )
                    .build()

                val mentionTextStyle = TextStyle.Builder(array)
                    .setBackgroundColor(
                        index = R.styleable.ChannelListView_sceytUiChannelListMentionBackgroundColor,
                        defValue = context.getCompatColor(SceytChatUIKit.theme.accentColor)
                    )
                    .setColor(
                        index = R.styleable.ChannelListView_sceytUiChannelListMentionTextColor,
                        defValue = context.getCompatColor(SceytChatUIKit.theme.onPrimaryColor)
                    )
                    .setSize(
                        index = R.styleable.ChannelListView_sceytUiChannelListMentionTextSize,
                    )
                    .setStyle(
                        index = R.styleable.ChannelListView_sceytUiChannelListMentionTextStyle,
                        defValue = Typeface.BOLD
                    )
                    .setFont(
                        index = R.styleable.ChannelListView_sceytUiChannelListMentionTextFont
                    )
                    .build()

                val mentionMutedTextStyle = TextStyle.Builder(array)
                    .setBackgroundColor(
                        index = R.styleable.ChannelListView_sceytUiChannelListMentionMutedStateBackgroundColor,
                        defValue = context.getCompatColor(SceytChatUIKit.theme.surface3Color)
                    )
                    .setColor(
                        index = R.styleable.ChannelListView_sceytUiChannelListMentionMutedStateTextColor,
                        defValue = context.getCompatColor(SceytChatUIKit.theme.onPrimaryColor)
                    )
                    .setSize(
                        index = R.styleable.ChannelListView_sceytUiChannelListMentionMutedStateTextSize,
                    )
                    .setStyle(
                        index = R.styleable.ChannelListView_sceytUiChannelListMentionMutedStateTextStyle,
                        defValue = Typeface.NORMAL
                    )
                    .setFont(
                        index = R.styleable.ChannelListView_sceytUiChannelListMentionMutedTextFont
                    )
                    .build()



                return ChannelItemStyle(
                    pinnedChannelBackgroundColor = pinnedChannelBackgroundColor,
                    onlineStateColor = onlineStateColor,
                    dividerColor = dividerColor,
                    linkTextColor = linkTextColor,
                    mutedIcon = mutedIcon,
                    pinIcon = pinIcon,
                    messageDeliveryStatusIcons = messageDeliveryStatusIcons,
                    autoDeletedChannelIcon = autoDeletedChannelIcon,
                    deliveryStatusIndicatorSize = statusIconSize,
                    subjectTextStyle = subjectTextStyle,
                    lastMessageTextStyle = lastMessageTextStyle,
                    dateTextStyle = dateTextStyle,
                    messageSenderNameStyle = messageSenderNameStyle,
                    deletedTextStyle = deletedTextStyle,
                    draftPrefixTextStyle = draftPrefixTextStyle,
                    typingTextStyle = typingTextStyle,
                    unreadCountTextStyle = unreadCountTextStyle,
                    unreadCountMutedStateTextStyle = unreadCountMutedTextStyle,
                    mentionTextStyle = mentionTextStyle,
                    mentionMutedStateTextStyle = mentionMutedTextStyle,
                    channelNameFormatter = SceytChatUIKit.formatters.channelNameFormatter,
                    channelDateFormatter = SceytChatUIKit.formatters.channelDateFormatter,
                    userNameFormatter = SceytChatUIKit.formatters.userNameFormatterNew,
                    messageSenderNameFormatter = SceytChatUIKit.formatters.messageSenderNameFormatter,
                    mentionUserNameFormatter = SceytChatUIKit.formatters.mentionUserNameFormatterNew,
                    reactedUserNameFormatter = SceytChatUIKit.formatters.reactedUserNameFormatter,
                    typingUserNameFormatter = SceytChatUIKit.formatters.typingUserNameFormatter,
                    attachmentIconProvider = SceytChatUIKit.providers.channelListAttachmentIconProvider,
                ).let { styleCustomizer.apply(context, it) }
            }
        }
    }
}