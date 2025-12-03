package com.sceyt.chatuikit.styles.channel

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
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.applyTintBackgroundLayer
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.formatters.SceytChatUIKitFormatters
import com.sceyt.chatuikit.formatters.attributes.ChannelEventTitleFormatterAttributes
import com.sceyt.chatuikit.formatters.attributes.ChannelItemSubtitleFormatterAttributes
import com.sceyt.chatuikit.formatters.attributes.DraftMessageBodyFormatterAttributes
import com.sceyt.chatuikit.formatters.attributes.MessageBodyFormatterAttributes
import com.sceyt.chatuikit.providers.SceytChatUIKitProviders
import com.sceyt.chatuikit.providers.VisualProvider
import com.sceyt.chatuikit.renderers.AvatarRenderer
import com.sceyt.chatuikit.renderers.SceytChatUIKitRenderers
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.AvatarStyle
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.MessageDeliveryStatusIcons
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.extensions.channel_list.buildAvatarStyle
import com.sceyt.chatuikit.styles.extensions.channel_list.buildChannelEventTextStyle
import com.sceyt.chatuikit.styles.extensions.channel_list.buildDateTextStyle
import com.sceyt.chatuikit.styles.extensions.channel_list.buildDeletedTextStyle
import com.sceyt.chatuikit.styles.extensions.channel_list.buildDraftPrefixTextStyle
import com.sceyt.chatuikit.styles.extensions.channel_list.buildLastMessageSenderNameStyle
import com.sceyt.chatuikit.styles.extensions.channel_list.buildLastMessageTextStyle
import com.sceyt.chatuikit.styles.extensions.channel_list.buildMentionTextStyle
import com.sceyt.chatuikit.styles.extensions.channel_list.buildSubjectTextStyle
import com.sceyt.chatuikit.styles.extensions.channel_list.buildUnreadCountMutedTextStyle
import com.sceyt.chatuikit.styles.extensions.channel_list.buildUnreadCountTextStyle
import com.sceyt.chatuikit.styles.extensions.channel_list.buildUnreadMentionBackgroundStyle
import com.sceyt.chatuikit.styles.extensions.channel_list.buildUnreadMentionMutedBackgroundStyle
import com.sceyt.chatuikit.theme.Colors
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme
import java.util.Date

/**
 * Style for [ChannelItemStyle] component.
 * @property backgroundColor - Background color of the channel item, default is [Color.TRANSPARENT].
 * @property pinnedChannelBackgroundColor - Background color of the pinned channel, default is [Colors.surface1Color].
 * @property dividerColor - Color of the divider, default is [Color.TRANSPARENT].
 * @property linkTextColor - Color of the link text, default is [R.color.sceyt_auto_link_color].
 * @property mutedIcon - Icon for muted channel, default is [R.drawable.sceyt_ic_muted].
 * @property pinIcon - Icon for pinned channel, default is [R.drawable.sceyt_ic_pin_filled].
 * @property autoDeletedChannelIcon - Icon for auto deleted channel, default is [R.drawable.sceyt_ic_auto_deleted_channel].
 * @property unreadMentionIcon - Icon for unread mention, default is [R.drawable.sceyt_ic_mention].
 * @property messageDeliveryStatusIcons - Icons for message delivery status, default is [MessageDeliveryStatusIcons].
 * @property deliveryStatusIndicatorSize - Size of the status icon, default is 16dp.
 * @property messageDeletedStateText - Text for deleted message, default is [R.string.sceyt_message_was_deleted].
 * @property subjectTextStyle - Style for channel subject, default is [buildSubjectTextStyle].
 * @property lastMessageTextStyle - Style for last message, default is [buildLastMessageTextStyle].
 * @property dateTextStyle - Style for date, default is [buildDateTextStyle].
 * @property lastMessageSenderNameTextStyle - Style for sender name, default is [buildLastMessageSenderNameStyle].
 * @property deletedTextStyle - Style for deleted message, default is [buildDeletedTextStyle].
 * @property draftPrefixTextStyle - Style for draft message, default is [buildDraftPrefixTextStyle].
 * @property channelEventTextStyle - Style for activity state message, default is [buildChannelEventTextStyle].
 * @property unreadCountTextStyle - Style for unread count, default is [buildUnreadCountTextStyle].
 * @property unreadCountMutedStateTextStyle - Style for unread count in muted channel, default is [buildUnreadCountMutedTextStyle].
 * @property mentionTextStyle - Style for mention message, default is [buildMentionTextStyle].
 * @property unreadMentionBackgroundStyle - Style for unread mention message, default is [buildUnreadMentionBackgroundStyle].
 * @property unreadMentionMutedStateBackgroundStyle - Style for mention message in muted channel, default is [buildUnreadMentionMutedBackgroundStyle].
 * @property avatarStyle - Style for avatar, default is [buildAvatarStyle].
 * @property channelTitleFormatter - Formatter for channel name, default is [SceytChatUIKitFormatters.channelNameFormatter].
 * @property channelDateFormatter - Date format for channel, default is [SceytChatUIKitFormatters.channelDateFormatter].
 * @property lastMessageSenderNameFormatter - Formatter for user name, default is [SceytChatUIKitFormatters.channelLastMessageSenderNameFormatter].
 * @property mentionUserNameFormatter - Formatter for user name, default is [SceytChatUIKitFormatters.mentionUserNameFormatter].
 * @property reactedUserNameFormatter - Formatter for user name, default is [SceytChatUIKitFormatters.reactedUserNameFormatter].
 * @property channelEventTitleFormatter - Formatter for activity title, default is [SceytChatUIKitFormatters.channelListChannelEventTitleFormatter].
 * @property unreadCountFormatter - Formatter for unread count, default is [SceytChatUIKitFormatters.unreadCountFormatter].
 * @property lastMessageBodyFormatter - Formatter for last message body, default is [SceytChatUIKitFormatters.channelLastMessageBodyFormatter].
 * @property unsupportedMessageBodyFormatter - Formatter for unsupported message body, default is [SceytChatUIKitFormatters.unsupportedMessageShortBodyFormatter].
 * @property draftMessageBodyFormatter - Formatter for draft message body, default is [SceytChatUIKitFormatters.channelDraftLastMessageBodyFormatter].
 * @property channelSubtitleFormatter - Formatter for channel subtitle, default is [SceytChatUIKitFormatters.channelListItemSubtitleFormatter].
 * @property attachmentIconProvider - Provider for attachment icon, default is [SceytChatUIKitProviders.attachmentIconProvider].
 * @property presenceStateColorProvider - Provider for presence state color, default is [SceytChatUIKitProviders.presenceStateColorProvider].
 * @property channelAvatarRenderer - Renderer for channel avatar, default is [SceytChatUIKitRenderers.channelAvatarRenderer].
 * */
data class ChannelItemStyle(
    @param:ColorInt val backgroundColor: Int,
    @param:ColorInt val pinnedChannelBackgroundColor: Int,
    @param:ColorInt val dividerColor: Int,
    @param:ColorInt val linkTextColor: Int,
    val mutedIcon: Drawable?,
    val pinIcon: Drawable?,
    val autoDeletedChannelIcon: Drawable?,
    val unreadMentionIcon: Drawable?,
    val messageDeliveryStatusIcons: MessageDeliveryStatusIcons,
    val deliveryStatusIndicatorSize: Int,
    val messageDeletedStateText: String,
    val subjectTextStyle: TextStyle,
    val lastMessageTextStyle: TextStyle,
    val dateTextStyle: TextStyle,
    val lastMessageSenderNameTextStyle: TextStyle,
    val deletedTextStyle: TextStyle,
    val draftPrefixTextStyle: TextStyle,
    val channelEventTextStyle: TextStyle,
    val unreadCountTextStyle: TextStyle,
    val unreadCountMutedStateTextStyle: TextStyle,
    val mentionTextStyle: TextStyle,
    val unreadMentionBackgroundStyle: BackgroundStyle,
    val unreadMentionMutedStateBackgroundStyle: BackgroundStyle,
    val avatarStyle: AvatarStyle,
    val channelTitleFormatter: Formatter<SceytChannel>,
    val channelSubtitleFormatter: Formatter<ChannelItemSubtitleFormatterAttributes>,
    val channelDateFormatter: Formatter<Date>,
    val lastMessageSenderNameFormatter: Formatter<SceytChannel>,
    val mentionUserNameFormatter: Formatter<SceytUser>,
    val reactedUserNameFormatter: Formatter<SceytUser>,
    val channelEventTitleFormatter: Formatter<ChannelEventTitleFormatterAttributes>,
    val unreadCountFormatter: Formatter<Long>,
    val lastMessageBodyFormatter: Formatter<MessageBodyFormatterAttributes>,
    val unsupportedMessageBodyFormatter: Formatter<SceytMessage>,
    val draftMessageBodyFormatter: Formatter<DraftMessageBodyFormatterAttributes>,
    val attachmentIconProvider: VisualProvider<SceytAttachment, Drawable?>,
    val presenceStateColorProvider: VisualProvider<PresenceState, Int>,
    val channelAvatarRenderer: AvatarRenderer<SceytChannel>,
) {

    companion object {
        @JvmField
        var styleCustomizer = StyleCustomizer<ChannelItemStyle> { _, style -> style }
    }

    internal class Builder(
            internal val context: Context,
            private val attrs: AttributeSet?,
    ) {
        fun build(): ChannelItemStyle {
            context.obtainStyledAttributes(attrs, R.styleable.ChannelListView).use { array ->
                val backgroundColor = array.getColor(R.styleable.ChannelListView_sceytUiChannelListItemBackgroundColor,
                    Color.TRANSPARENT)
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
                        ?: context.getCompatDrawable(R.drawable.sceyt_ic_auto_deleted_channel).applyTintBackgroundLayer(
                            context.getCompatColor(SceytChatUIKit.theme.colors.accentColor), R.id.backgroundLayer
                        )

                val unreadMentionIcon = array.getDrawable(R.styleable.ChannelListView_sceytUiChannelListUnreadMentionIcon)
                        ?: context.getCompatDrawable(R.drawable.sceyt_ic_mention)
                            .applyTint(context, SceytChatUIKitTheme.colors.onPrimaryColor)

                val messageDeliveryStatusIcons = MessageDeliveryStatusIcons.Builder(context, array)
                    .setPendingIconFromStyle(R.styleable.ChannelListView_sceytUiChannelListStatusPendingIndicator)
                    .setSentIconFromStyle(R.styleable.ChannelListView_sceytUiChannelListStatusSentIndicator)
                    .setReceivedIconIconFromStyle(R.styleable.ChannelListView_sceytUiChannelListStatusReceivedIndicator)
                    .setDisplayedIconFromStyle(R.styleable.ChannelListView_sceytUiChannelListStatusDisplayedIndicator)
                    .build()

                val statusIconSize = array.getDimensionPixelSize(R.styleable.ChannelListView_sceytUiChannelListStatusIndicatorSize,
                    dpToPx(16f))

                val deletedStateText = array.getString(R.styleable.ChannelListView_sceytUiChannelListMessageDeletedStateText)
                        ?: context.getString(R.string.sceyt_message_was_deleted)

                return ChannelItemStyle(
                    backgroundColor = backgroundColor,
                    pinnedChannelBackgroundColor = pinnedChannelBackgroundColor,
                    dividerColor = dividerColor,
                    linkTextColor = linkTextColor,
                    mutedIcon = mutedIcon,
                    pinIcon = pinIcon,
                    messageDeliveryStatusIcons = messageDeliveryStatusIcons,
                    autoDeletedChannelIcon = autoDeletedChannelIcon,
                    unreadMentionIcon = unreadMentionIcon,
                    deliveryStatusIndicatorSize = statusIconSize,
                    messageDeletedStateText = deletedStateText,
                    subjectTextStyle = buildSubjectTextStyle(array),
                    lastMessageTextStyle = buildLastMessageTextStyle(array),
                    dateTextStyle = buildDateTextStyle(array),
                    lastMessageSenderNameTextStyle = buildLastMessageSenderNameStyle(array),
                    deletedTextStyle = buildDeletedTextStyle(array),
                    draftPrefixTextStyle = buildDraftPrefixTextStyle(array),
                    channelEventTextStyle = buildChannelEventTextStyle(array),
                    unreadCountTextStyle = buildUnreadCountTextStyle(array),
                    unreadCountMutedStateTextStyle = buildUnreadCountMutedTextStyle(array),
                    mentionTextStyle = buildMentionTextStyle(array),
                    unreadMentionBackgroundStyle = buildUnreadMentionBackgroundStyle(array),
                    unreadMentionMutedStateBackgroundStyle = buildUnreadMentionMutedBackgroundStyle(array),
                    avatarStyle = buildAvatarStyle(array),
                    channelTitleFormatter = SceytChatUIKit.formatters.channelNameFormatter,
                    channelDateFormatter = SceytChatUIKit.formatters.channelDateFormatter,
                    lastMessageSenderNameFormatter = SceytChatUIKit.formatters.channelLastMessageSenderNameFormatter,
                    mentionUserNameFormatter = SceytChatUIKit.formatters.mentionUserNameFormatter,
                    reactedUserNameFormatter = SceytChatUIKit.formatters.reactedUserNameFormatter,
                    channelEventTitleFormatter = SceytChatUIKit.formatters.channelListChannelEventTitleFormatter,
                    unreadCountFormatter = SceytChatUIKit.formatters.unreadCountFormatter,
                    lastMessageBodyFormatter = SceytChatUIKit.formatters.channelLastMessageBodyFormatter,
                    unsupportedMessageBodyFormatter = SceytChatUIKit.formatters.unsupportedMessageShortBodyFormatter,
                    draftMessageBodyFormatter = SceytChatUIKit.formatters.channelDraftLastMessageBodyFormatter,
                    channelSubtitleFormatter = SceytChatUIKit.formatters.channelListItemSubtitleFormatter,
                    attachmentIconProvider = SceytChatUIKit.providers.channelListAttachmentIconProvider,
                    presenceStateColorProvider = SceytChatUIKit.providers.presenceStateColorProvider,
                    channelAvatarRenderer = SceytChatUIKit.renderers.channelAvatarRenderer,
                ).let { styleCustomizer.apply(context, it) }
            }
        }
    }
}