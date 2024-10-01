package com.sceyt.chatuikit.styles

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.core.content.res.use
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.formatters.SceytChatUIKitFormatters
import com.sceyt.chatuikit.presentation.custom_views.AvatarView
import com.sceyt.chatuikit.providers.SceytChatUIKitProviders
import com.sceyt.chatuikit.providers.VisualProvider
import com.sceyt.chatuikit.styles.common.MessageDeliveryStatusIcons
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.extensions.channel.buildDateTextStyle
import com.sceyt.chatuikit.styles.extensions.channel.buildDeletedTextStyle
import com.sceyt.chatuikit.styles.extensions.channel.buildDraftPrefixTextStyle
import com.sceyt.chatuikit.styles.extensions.channel.buildLastMessageSenderNameStyle
import com.sceyt.chatuikit.styles.extensions.channel.buildLastMessageTextStyle
import com.sceyt.chatuikit.styles.extensions.channel.buildUnreadMentionMutedTextStyle
import com.sceyt.chatuikit.styles.extensions.channel.buildMentionTextStyle
import com.sceyt.chatuikit.styles.extensions.channel.buildSubjectTextStyle
import com.sceyt.chatuikit.styles.extensions.channel.buildTypingTextStyle
import com.sceyt.chatuikit.styles.extensions.channel.buildUnreadCountMutedTextStyle
import com.sceyt.chatuikit.styles.extensions.channel.buildUnreadCountTextStyle
import com.sceyt.chatuikit.styles.extensions.channel.buildUnreadMentionTextStyle
import com.sceyt.chatuikit.theme.Colors
import java.util.Date

/**
 * Style for [ChannelItemStyle] component.
 * @property pinnedChannelBackgroundColor - Background color of the pinned channel, default is [Colors.surface1Color].
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
 * @property lastMessageSenderNameTextStyle - Style for sender name, default is [buildLastMessageSenderNameStyle].
 * @property deletedTextStyle - Style for deleted message, default is [buildDeletedTextStyle].
 * @property draftPrefixTextStyle - Style for draft message, default is [buildDraftPrefixTextStyle].
 * @property typingTextStyle - Style for typing message, default is [buildTypingTextStyle].
 * @property unreadCountTextStyle - Style for unread count, default is [buildUnreadCountTextStyle].
 * @property unreadCountMutedStateTextStyle - Style for unread count in muted channel, default is [buildUnreadCountMutedTextStyle].
 * @property mentionTextStyle - Style for mention message, default is [buildMentionTextStyle].
 * @property unreadMentionMutedStateTextStyle - Style for mention message in muted channel, default is [buildUnreadMentionMutedTextStyle].
 * @property channelNameFormatter - Formatter for channel name, default is [SceytChatUIKitFormatters.channelNameFormatter].
 * @property channelDateFormatter - Date format for channel, default is [SceytChatUIKitFormatters.channelDateFormatter].
 * @property lastMessageSenderNameFormatter - Formatter for user name, default is [SceytChatUIKitFormatters.channelLastMessageSenderNameFormatter].
 * @property mentionUserNameFormatter - Formatter for user name, default is [SceytChatUIKitFormatters.mentionUserNameFormatter].
 * @property reactedUserNameFormatter - Formatter for user name, default is [SceytChatUIKitFormatters.reactedUserNameFormatter].
 * @property typingUserNameFormatter - Formatter for user name, default is [SceytChatUIKitFormatters.typingUserNameFormatter].
 * @property unreadCountFormatter - Formatter for unread count, default is [SceytChatUIKitFormatters.unreadCountFormatter].
 * @property attachmentNameFormatter - Formatter for attachment name, default is [SceytChatUIKitFormatters.attachmentNameFormatter].
 * @property attachmentIconProvider - Provider for attachment icon, default is [SceytChatUIKitProviders.attachmentIconProvider].
 * @property channelDefaultAvatarProvider - Provider for channel default avatar, default is [SceytChatUIKitProviders.channelDefaultAvatarProvider].
 * */
data class ChannelItemStyle(
        @ColorInt var pinnedChannelBackgroundColor: Int,
        @ColorInt var dividerColor: Int,
        @ColorInt var linkTextColor: Int,
        var mutedIcon: Drawable?,
        var pinIcon: Drawable?,
        var messageDeliveryStatusIcons: MessageDeliveryStatusIcons,
        var autoDeletedChannelIcon: Drawable?,
        var deliveryStatusIndicatorSize: Int,
        var subjectTextStyle: TextStyle,
        var lastMessageTextStyle: TextStyle,
        var dateTextStyle: TextStyle,
        var lastMessageSenderNameTextStyle: TextStyle,
        var deletedTextStyle: TextStyle,
        var draftPrefixTextStyle: TextStyle,
        var typingTextStyle: TextStyle,
        var unreadCountTextStyle: TextStyle,
        var unreadCountMutedStateTextStyle: TextStyle,
        var mentionTextStyle: TextStyle,
        var unreadMentionTextStyle: TextStyle,
        var unreadMentionMutedStateTextStyle: TextStyle,
        var channelNameFormatter: Formatter<SceytChannel>,
        var channelDateFormatter: Formatter<Date>,
        var lastMessageSenderNameFormatter: Formatter<SceytChannel>,
        var mentionUserNameFormatter: Formatter<SceytUser>,
        var reactedUserNameFormatter: Formatter<SceytUser>,
        var typingUserNameFormatter: Formatter<SceytUser>,
        var unreadCountFormatter: Formatter<Long>,
        var attachmentNameFormatter: Formatter<SceytAttachment>,
        var attachmentIconProvider: VisualProvider<SceytAttachment, Drawable?>,
        var channelDefaultAvatarProvider: VisualProvider<SceytChannel, AvatarView.DefaultAvatar>,
        val presenceStateColorProvider: VisualProvider<PresenceState, Int>
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
                    context.getCompatColor(SceytChatUIKit.theme.colors.surface1Color))

                val dividerColor = array.getColor(R.styleable.ChannelListView_sceytUiChannelListDividerColor, Color.TRANSPARENT)

                val linkTextColor = array.getColor(R.styleable.ChannelListView_sceytUiChannelListLinkTextColor,
                    context.getCompatColor(R.color.sceyt_auto_link_color))

                val mutedIcon = array.getDrawable(R.styleable.ChannelListView_sceytUiChannelListMutedIcon)
                        ?: context.getCompatDrawable(R.drawable.sceyt_ic_muted)?.apply {
                            mutate().setTint(context.getCompatColor(SceytChatUIKit.theme.colors.iconSecondaryColor))
                        }

                val pinIcon = array.getDrawable(R.styleable.ChannelListView_sceytUiChannelListPinnedIcon)
                        ?: context.getCompatDrawable(R.drawable.sceyt_ic_pin_filled)?.apply {
                            mutate().setTint(context.getCompatColor(SceytChatUIKit.theme.colors.iconSecondaryColor))
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
                    lastMessageSenderNameTextStyle = buildLastMessageSenderNameStyle(array),
                    deletedTextStyle = buildDeletedTextStyle(array),
                    draftPrefixTextStyle = buildDraftPrefixTextStyle(array),
                    typingTextStyle = buildTypingTextStyle(array),
                    unreadCountTextStyle = buildUnreadCountTextStyle(array),
                    unreadCountMutedStateTextStyle = buildUnreadCountMutedTextStyle(array),
                    mentionTextStyle = buildMentionTextStyle(array),
                    unreadMentionTextStyle = buildUnreadMentionTextStyle(array),
                    unreadMentionMutedStateTextStyle = buildUnreadMentionMutedTextStyle(array),
                    channelNameFormatter = SceytChatUIKit.formatters.channelNameFormatter,
                    channelDateFormatter = SceytChatUIKit.formatters.channelDateFormatter,
                    lastMessageSenderNameFormatter = SceytChatUIKit.formatters.channelLastMessageSenderNameFormatter,
                    mentionUserNameFormatter = SceytChatUIKit.formatters.mentionUserNameFormatterNew,
                    reactedUserNameFormatter = SceytChatUIKit.formatters.reactedUserNameFormatter,
                    typingUserNameFormatter = SceytChatUIKit.formatters.typingUserNameFormatter,
                    unreadCountFormatter = SceytChatUIKit.formatters.unreadCountFormatter,
                    attachmentNameFormatter = SceytChatUIKit.formatters.attachmentNameFormatter,
                    attachmentIconProvider = SceytChatUIKit.providers.channelListAttachmentIconProvider,
                    channelDefaultAvatarProvider = SceytChatUIKit.providers.channelDefaultAvatarProvider,
                    presenceStateColorProvider = SceytChatUIKit.providers.presenceStateColorProvider
                ).let { styleCustomizer.apply(context, it) }
            }
        }
    }
}