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
import com.sceyt.chatuikit.extensions.applyTintBackgroundLayer
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.formatters.SceytChatUIKitFormatters
import com.sceyt.chatuikit.formatters.attributes.DraftMessageBodyFormatterAttributes
import com.sceyt.chatuikit.formatters.attributes.MessageBodyFormatterAttributes
import com.sceyt.chatuikit.providers.SceytChatUIKitProviders
import com.sceyt.chatuikit.providers.VisualProvider
import com.sceyt.chatuikit.renderers.AvatarRenderer
import com.sceyt.chatuikit.renderers.SceytChatUIKitRenderers
import com.sceyt.chatuikit.styles.common.AvatarStyle
import com.sceyt.chatuikit.styles.common.MessageDeliveryStatusIcons
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.extensions.channel.buildAvatarStyle
import com.sceyt.chatuikit.styles.extensions.channel.buildDateTextStyle
import com.sceyt.chatuikit.styles.extensions.channel.buildDeletedTextStyle
import com.sceyt.chatuikit.styles.extensions.channel.buildDraftPrefixTextStyle
import com.sceyt.chatuikit.styles.extensions.channel.buildLastMessageSenderNameStyle
import com.sceyt.chatuikit.styles.extensions.channel.buildLastMessageTextStyle
import com.sceyt.chatuikit.styles.extensions.channel.buildMentionTextStyle
import com.sceyt.chatuikit.styles.extensions.channel.buildSubjectTextStyle
import com.sceyt.chatuikit.styles.extensions.channel.buildTypingTextStyle
import com.sceyt.chatuikit.styles.extensions.channel.buildUnreadCountMutedTextStyle
import com.sceyt.chatuikit.styles.extensions.channel.buildUnreadCountTextStyle
import com.sceyt.chatuikit.styles.extensions.channel.buildUnreadMentionMutedTextStyle
import com.sceyt.chatuikit.styles.extensions.channel.buildUnreadMentionTextStyle
import com.sceyt.chatuikit.theme.Colors
import java.util.Date

/**
 * Style for [ChannelItemStyle] component.
 * @property backgroundColor - Background color of the channel item, default is [Color.TRANSPARENT].
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
 * @property unreadMentionTextStyle - Style for unread mention message, default is [buildUnreadMentionTextStyle].
 * @property unreadMentionMutedStateTextStyle - Style for mention message in muted channel, default is [buildUnreadMentionMutedTextStyle].
 * @property avatarStyle - Style for avatar, default is [buildAvatarStyle].
 * @property channelNameFormatter - Formatter for channel name, default is [SceytChatUIKitFormatters.channelNameFormatter].
 * @property channelDateFormatter - Date format for channel, default is [SceytChatUIKitFormatters.channelDateFormatter].
 * @property lastMessageSenderNameFormatter - Formatter for user name, default is [SceytChatUIKitFormatters.channelLastMessageSenderNameFormatter].
 * @property mentionUserNameFormatter - Formatter for user name, default is [SceytChatUIKitFormatters.mentionUserNameFormatter].
 * @property reactedUserNameFormatter - Formatter for user name, default is [SceytChatUIKitFormatters.reactedUserNameFormatter].
 * @property typingUserNameFormatter - Formatter for user name, default is [SceytChatUIKitFormatters.typingUserNameFormatter].
 * @property unreadCountFormatter - Formatter for unread count, default is [SceytChatUIKitFormatters.unreadCountFormatter].
 * @property lastMessageBodyFormatter - Formatter for last message body, default is [SceytChatUIKitFormatters.channelLastMessageBodyFormatter].
 * @property draftMessageBodyFormatter - Formatter for draft message body, default is [SceytChatUIKitFormatters.draftMessageBodyFormatter].
 * @property attachmentIconProvider - Provider for attachment icon, default is [SceytChatUIKitProviders.attachmentIconProvider].
 * @property presenceStateColorProvider - Provider for presence state color, default is [SceytChatUIKitProviders.presenceStateColorProvider].
 * @property channelAvatarRenderer - Renderer for channel avatar, default is [SceytChatUIKitRenderers.channelAvatarRenderer].
 * */
data class ChannelItemStyle(
        @ColorInt val backgroundColor: Int,
        @ColorInt val pinnedChannelBackgroundColor: Int,
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
        val lastMessageSenderNameTextStyle: TextStyle,
        val deletedTextStyle: TextStyle,
        val draftPrefixTextStyle: TextStyle,
        val typingTextStyle: TextStyle,
        val unreadCountTextStyle: TextStyle,
        val unreadCountMutedStateTextStyle: TextStyle,
        val mentionTextStyle: TextStyle,
        val unreadMentionTextStyle: TextStyle,
        val unreadMentionMutedStateTextStyle: TextStyle,
        val avatarStyle: AvatarStyle,
        val channelNameFormatter: Formatter<SceytChannel>,
        val channelDateFormatter: Formatter<Date>,
        val lastMessageSenderNameFormatter: Formatter<SceytChannel>,
        val mentionUserNameFormatter: Formatter<SceytUser>,
        val reactedUserNameFormatter: Formatter<SceytUser>,
        val typingUserNameFormatter: Formatter<SceytUser>,
        val unreadCountFormatter: Formatter<Long>,
        val lastMessageBodyFormatter: Formatter<MessageBodyFormatterAttributes>,
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

                val messageDeliveryStatusIcons = MessageDeliveryStatusIcons.Builder(context, array)
                    .setPendingIconFromStyle(R.styleable.ChannelListView_sceytUiChannelListStatusPendingIndicator)
                    .setSentIconFromStyle(R.styleable.ChannelListView_sceytUiChannelListStatusSentIndicator)
                    .setReceivedIconIconFromStyle(R.styleable.ChannelListView_sceytUiChannelListStatusReceivedIndicator)
                    .setDisplayedIconFromStyle(R.styleable.ChannelListView_sceytUiChannelListStatusDisplayedIndicator)
                    .build()

                val statusIconSize = array.getDimensionPixelSize(R.styleable.ChannelListView_sceytUiChannelListStatusIndicatorSize,
                    dpToPx(16f))

                return ChannelItemStyle(
                    backgroundColor = backgroundColor,
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
                    avatarStyle = buildAvatarStyle(array),
                    channelNameFormatter = SceytChatUIKit.formatters.channelNameFormatter,
                    channelDateFormatter = SceytChatUIKit.formatters.channelDateFormatter,
                    lastMessageSenderNameFormatter = SceytChatUIKit.formatters.channelLastMessageSenderNameFormatter,
                    mentionUserNameFormatter = SceytChatUIKit.formatters.mentionUserNameFormatter,
                    reactedUserNameFormatter = SceytChatUIKit.formatters.reactedUserNameFormatter,
                    typingUserNameFormatter = SceytChatUIKit.formatters.typingUserNameFormatter,
                    unreadCountFormatter = SceytChatUIKit.formatters.unreadCountFormatter,
                    lastMessageBodyFormatter = SceytChatUIKit.formatters.channelLastMessageBodyFormatter,
                    draftMessageBodyFormatter = SceytChatUIKit.formatters.draftMessageBodyFormatter,
                    attachmentIconProvider = SceytChatUIKit.providers.channelListAttachmentIconProvider,
                    presenceStateColorProvider = SceytChatUIKit.providers.presenceStateColorProvider,
                    channelAvatarRenderer = SceytChatUIKit.renderers.channelAvatarRenderer
                ).let { styleCustomizer.apply(context, it) }
            }
        }
    }
}