package com.sceyt.chatuikit.styles

import android.content.Context
import android.graphics.Color
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
import com.sceyt.chatuikit.formatters.defaults.DefaultChannelUnreadCountFormatter
import com.sceyt.chatuikit.presentation.custom_views.AvatarView
import com.sceyt.chatuikit.providers.SceytChatUIKitProviders
import com.sceyt.chatuikit.providers.VisualProvider
import com.sceyt.chatuikit.styles.common.MessageDeliveryStatusIcons
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.extensions.buildDateTextStyle
import com.sceyt.chatuikit.styles.extensions.buildDeletedTextStyle
import com.sceyt.chatuikit.styles.extensions.buildDraftPrefixTextStyle
import com.sceyt.chatuikit.styles.extensions.buildLastMessageTextStyle
import com.sceyt.chatuikit.styles.extensions.buildMentionMutedTextStyle
import com.sceyt.chatuikit.styles.extensions.buildMentionTextStyle
import com.sceyt.chatuikit.styles.extensions.buildMessageSenderNameStyle
import com.sceyt.chatuikit.styles.extensions.buildSubjectTextStyle
import com.sceyt.chatuikit.styles.extensions.buildTypingTextStyle
import com.sceyt.chatuikit.styles.extensions.buildUnreadCountMutedTextStyle
import com.sceyt.chatuikit.styles.extensions.buildUnreadCountTextStyle
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
 * @property subjectTextStyle - Style for channel subject, default is [buildSubjectTextStyle].
 * @property lastMessageTextStyle - Style for last message, default is [buildLastMessageTextStyle].
 * @property dateTextStyle - Style for date, default is [buildDateTextStyle].
 * @property messageSenderNameStyle - Style for sender name, default is [buildMessageSenderNameStyle].
 * @property deletedTextStyle - Style for deleted message, default is [buildDeletedTextStyle].
 * @property draftPrefixTextStyle - Style for draft message, default is [buildDraftPrefixTextStyle].
 * @property typingTextStyle - Style for typing message, default is [buildTypingTextStyle].
 * @property unreadCountTextStyle - Style for unread count, default is [buildUnreadCountTextStyle].
 * @property unreadCountMutedStateTextStyle - Style for unread count in muted channel, default is [buildUnreadCountMutedTextStyle].
 * @property mentionTextStyle - Style for mention message, default is [buildMentionTextStyle].
 * @property mentionMutedStateTextStyle - Style for mention message in muted channel, default is [buildMentionMutedTextStyle].
 * @property channelNameFormatter - Formatter for channel name, default is [SceytChatUIKitFormatters.channelNameFormatter].
 * @property channelDateFormatter - Date format for channel, default is [SceytChatUIKitFormatters.channelDateFormatter].
 * @property userNameFormatter - Formatter for user name, default is [SceytChatUIKitFormatters.userNameFormatter].
 * @property messageSenderNameFormatter - Formatter for user name, default is [SceytChatUIKitFormatters.messageSenderNameFormatter].
 * @property mentionUserNameFormatter - Formatter for user name, default is [SceytChatUIKitFormatters.mentionUserNameFormatter].
 * @property reactedUserNameFormatter - Formatter for user name, default is [SceytChatUIKitFormatters.reactedUserNameFormatter].
 * @property typingUserNameFormatter - Formatter for user name, default is [SceytChatUIKitFormatters.typingUserNameFormatter].
 * @property channelUnreadCountFormatter - Formatter for unread count, default is [DefaultChannelUnreadCountFormatter].
 * @property attachmentIconProvider - Provider for attachment icon, default is [SceytChatUIKitProviders.attachmentIconProvider].
 * @property channelDefaultAvatarProvider - Provider for channel default avatar, default is [SceytChatUIKitProviders.channelDefaultAvatarProvider].
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
        val channelUnreadCountFormatter: Formatter<SceytChannel>,
        val attachmentIconProvider: VisualProvider<SceytAttachment, Drawable?>,
        val channelDefaultAvatarProvider: VisualProvider<SceytChannel, AvatarView.DefaultAvatar>
) {

    companion object {
        @JvmField
        var styleCustomizer = StyleCustomizer<ChannelItemStyle> { _, style -> style }
    }

    internal class Builder(
            internal val context: Context,
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
                    subjectTextStyle = buildSubjectTextStyle(array),
                    lastMessageTextStyle = buildLastMessageTextStyle(array),
                    dateTextStyle = buildDateTextStyle(array),
                    messageSenderNameStyle = buildMessageSenderNameStyle(array),
                    deletedTextStyle = buildDeletedTextStyle(array),
                    draftPrefixTextStyle = buildDraftPrefixTextStyle(array),
                    typingTextStyle = buildTypingTextStyle(array),
                    unreadCountTextStyle = buildUnreadCountTextStyle(array),
                    unreadCountMutedStateTextStyle = buildUnreadCountMutedTextStyle(array),
                    mentionTextStyle = buildMentionTextStyle(array),
                    mentionMutedStateTextStyle = buildMentionMutedTextStyle(array),
                    channelNameFormatter = SceytChatUIKit.formatters.channelNameFormatter,
                    channelDateFormatter = SceytChatUIKit.formatters.channelDateFormatter,
                    userNameFormatter = SceytChatUIKit.formatters.userNameFormatterNew,
                    messageSenderNameFormatter = SceytChatUIKit.formatters.messageSenderNameFormatter,
                    mentionUserNameFormatter = SceytChatUIKit.formatters.mentionUserNameFormatterNew,
                    reactedUserNameFormatter = SceytChatUIKit.formatters.reactedUserNameFormatter,
                    typingUserNameFormatter = SceytChatUIKit.formatters.typingUserNameFormatter,
                    attachmentIconProvider = SceytChatUIKit.providers.channelListAttachmentIconProvider,
                    channelUnreadCountFormatter = SceytChatUIKit.formatters.channelUnreadCountFormatter,
                    channelDefaultAvatarProvider = SceytChatUIKit.providers.channelDefaultAvatarProvider
                ).let { styleCustomizer.apply(context, it) }
            }
        }
    }
}