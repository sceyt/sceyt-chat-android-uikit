package com.sceyt.chatuikit.styles.messages_list.item

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.core.content.res.use
import androidx.core.graphics.ColorUtils
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.extensions.isAppInDarkMode
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.formatters.SceytChatUIKitFormatters
import com.sceyt.chatuikit.presentation.custom_views.AvatarView
import com.sceyt.chatuikit.providers.SceytChatUIKitProviders
import com.sceyt.chatuikit.providers.VisualProvider
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.CheckboxStyle
import com.sceyt.chatuikit.styles.common.MediaLoaderStyle
import com.sceyt.chatuikit.styles.common.MessageDeliveryStatusIcons
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildAttachmentFileNameTextStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildAttachmentFileSizeTextStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildAudioWaveformStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildBodyTextStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildDeletedMessageTextStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildForwardTitleTextStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildLinkPreviewStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildMediaLoaderStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildMentionTextStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildMessageDateTextStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildMessageStateTextStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildOverlayMediaLoaderStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildReactionCountTextStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildReplyMessageStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildSelectionCheckboxStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildSenderNameTextStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildThreadReplyCountTextStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildVideoDurationTextStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildVoiceDurationTextStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildVoiceSpeedTextStyle
import com.sceyt.chatuikit.theme.Colors
import java.util.Date

/**
 * Style for the message item view.
 * @property incomingBubbleColor Color for the incoming message bubble, default is [R.color.sceyt_color_bg_inc_message]
 * @property outgoingBubbleColor Color for the outgoing message bubble, default is 20% blend of [Colors.accentColor]
 * @property incomingReplyBackgroundColor Color for the incoming reply background, default is [R.color.sceyt_color_surface_2]
 * @property outgoingReplyBackgroundColor Color for the outgoing reply background, default is 25% blend of [Colors.accentColor]
 * @property incomingLinkPreviewBackgroundColor Color for the incoming link preview background, default is [R.color.sceyt_color_bg_inc_link_preview]
 * @property outgoingLinkPreviewBackgroundColor Color for the outgoing link preview background, default is 30% blend of [Colors.accentColor]
 * @property onOverlayColor Color for the overlay, default is [Colors.overlayBackground2Color]
 * @property threadReplyArrowStrokeColor Color for the thread reply arrow stroke, default is [Colors.accentColor]
 * @property reactionsContainerBackgroundColor Color for the reactions container background, default is [Colors.backgroundColorSections]
 * @property highlightedMessageColor Color for the highlighted message, default is 30% blend of [Colors.accentColor]
 * @property linkTextColor Color for the link text, default is [R.color.sceyt_auto_link_color]
 * @property videoIcon Icon for the video duration, default is [R.drawable.sceyt_ic_video]
 * @property swipeToReplyIcon Icon for the swipe reply, default is [R.drawable.sceyt_is_reply_swipe]
 * @property forwardedIcon Icon for the forwarded message, default is [R.drawable.sceyt_ic_forward_14]
 * @property videoPlayIcon Icon for the video play, default is [R.drawable.sceyt_ic_play]
 * @property voicePlayIcon Icon for the voice play, default is [R.drawable.sceyt_ic_play]
 * @property voicePauseIcon Icon for the voice pause, default is [R.drawable.sceyt_ic_pause]
 * @property viewCountIcon Icon for the view count, default is [R.drawable.sceyt_ic_display_count]
 * @property messageDeliveryStatusIcons Icons for the message delivery status.
 * @property editedStateText Title for the edited state, default is [R.string.sceyt_edited].
 * @property deletedStateText Title for the deleted state, default is [R.string.sceyt_message_was_deleted].
 * @property forwardedText Title for the forwarded message, default is [R.string.sceyt_forwarded_message].
 * @property bodyTextStyle Style for the message body, default is [buildBodyTextStyle].
 * @property deletedMessageTextStyle Style for the deleted message, default is [buildDeletedMessageTextStyle].
 * @property senderNameTextStyle Style for the sender name, default is [buildSenderNameTextStyle].
 * @property messageDateTextStyle Style for the message date, default is [buildMessageDateTextStyle].
 * @property messageStateTextStyle Style for the message state, default is [buildMessageStateTextStyle].
 * @property mentionTextStyle Style for the mention, default is [buildMentionTextStyle].
 * @property videoDurationTextStyle Style for the video duration, default is [buildVideoDurationTextStyle].
 * @property threadReplyCountTextStyle Style for the thread reply count, default is [buildThreadReplyCountTextStyle].
 * @property forwardTitleTextStyle Style for the forward title, default is [buildForwardTitleTextStyle].
 * @property reactionCountTextStyle Style for the reaction count, default is [buildReactionCountTextStyle].
 * @property voiceSpeedTextStyle Style for the voice speed, default is [buildVoiceSpeedTextStyle].
 * @property voiceDurationTextStyle Style for the voice duration, default is [buildVoiceDurationTextStyle].
 * @property attachmentFileNameTextStyle Style for the attachment file name, default is [buildAttachmentFileNameTextStyle].
 * @property attachmentFileSizeTextStyle Style for the attachment file size, default is [buildAttachmentFileSizeTextStyle].
 * @property linkPreviewStyle Style for the link preview, default is [buildLinkPreviewStyle].
 * @property replyMessageStyle Style for the reply message, default is [buildReplyMessageStyle].
 * @property mediaLoaderStyle Style for the media loader, default is [buildMediaLoaderStyle].
 * @property overlayMediaLoaderStyle Style for the overlay media loader, default is [buildOverlayMediaLoaderStyle].
 * @property voiceWaveformStyle Style for the voice waveform, default is [buildAudioWaveformStyle].
 * @property selectionCheckboxStyle Style for the selection checkbox, default is [buildSelectionCheckboxStyle].
 * @property senderNameFormatter Formatter for the sender name, default is [SceytChatUIKitFormatters.userNameFormatterNew].
 * @property messageBodyFormatter Formatter for the message body. Use it to format the message body before displaying it, default is null.
 * @property messageViewCountFormatter Formatter for the message view count, default is [SceytChatUIKitFormatters.messageViewCountFormatter].
 * @property messageDateFormatter Formatter for the message date, default is [SceytChatUIKitFormatters.messageDateFormatter].
 * @property mentionUserNameFormatter Formatter for the mention user name, default is [SceytChatUIKitFormatters.mentionUserNameFormatter].
 * @property voiceDurationFormatter Formatter for the voice duration, default is [SceytChatUIKitFormatters.mediaDurationFormatter].
 * @property videoDurationFormatter Formatter for the video duration, default is [SceytChatUIKitFormatters.mediaDurationFormatter].
 * @property attachmentFileSizeFormatter Formatter for the attachment file size, default is [SceytChatUIKitFormatters.attachmentSizeFormatter].
 * @property attachmentIconProvider Visual provider for the attachment icon, default is [SceytChatUIKitProviders.attachmentIconProvider].
 * @property userDefaultAvatarProvider Visual provider for the user default avatar, default is [SceytChatUIKitProviders.userDefaultAvatarProvider].
 * @property senderNameColorProvider Visual provider for the sender name color, default is [SceytChatUIKitProviders.senderNameColorProvider].
 * */
data class MessageItemStyle(
        @ColorInt val incomingBubbleColor: Int,
        @ColorInt val outgoingBubbleColor: Int,
        @ColorInt val incomingReplyBackgroundColor: Int,
        @ColorInt val outgoingReplyBackgroundColor: Int,
        @ColorInt val incomingLinkPreviewBackgroundColor: Int,
        @ColorInt val outgoingLinkPreviewBackgroundColor: Int,
        @ColorInt val onOverlayColor: Int,
        @ColorInt val threadReplyArrowStrokeColor: Int,
        @ColorInt val reactionsContainerBackgroundColor: Int,
        @ColorInt val highlightedMessageColor: Int,
        @ColorInt val linkTextColor: Int,
        val videoIcon: Drawable?,
        val swipeToReplyIcon: Drawable?,
        val forwardedIcon: Drawable?,
        val videoPlayIcon: Drawable?,
        val voicePlayIcon: Drawable?,
        val voicePauseIcon: Drawable?,
        val viewCountIcon: Drawable?,
        val messageDeliveryStatusIcons: MessageDeliveryStatusIcons,
        val editedStateText: String,
        val deletedStateText: String,
        val forwardedText: String,
        val bodyTextStyle: TextStyle,
        val deletedMessageTextStyle: TextStyle,
        val senderNameTextStyle: TextStyle,
        val messageDateTextStyle: TextStyle,
        val messageStateTextStyle: TextStyle,
        val mentionTextStyle: TextStyle,
        val videoDurationTextStyle: TextStyle,
        val threadReplyCountTextStyle: TextStyle,
        val forwardTitleTextStyle: TextStyle,
        val reactionCountTextStyle: TextStyle,
        val voiceSpeedTextStyle: TextStyle,
        val voiceDurationTextStyle: TextStyle,
        val attachmentFileNameTextStyle: TextStyle,
        val attachmentFileSizeTextStyle: TextStyle,
        val linkPreviewStyle: LinkPreviewStyle,
        val replyMessageStyle: ReplyMessageStyle,
        val mediaLoaderStyle: MediaLoaderStyle,
        val overlayMediaLoaderStyle: MediaLoaderStyle,
        val voiceWaveformStyle: AudioWaveformStyle,
        val selectionCheckboxStyle: CheckboxStyle,
        val senderNameFormatter: Formatter<SceytUser>,
        val messageBodyFormatter: Formatter<SceytMessage>?,
        val messageViewCountFormatter: Formatter<Long>,
        val messageDateFormatter: Formatter<Date>,
        val mentionUserNameFormatter: Formatter<SceytUser>,
        val voiceDurationFormatter: Formatter<Long>,
        val videoDurationFormatter: Formatter<Long>,
        val attachmentFileSizeFormatter: Formatter<SceytAttachment>,
        val attachmentIconProvider: VisualProvider<SceytAttachment, Drawable?>,
        val userDefaultAvatarProvider: VisualProvider<SceytUser, AvatarView.DefaultAvatar>,
        val senderNameColorProvider: VisualProvider<SceytUser, Int>,
) {

    companion object {
        @JvmField
        var styleCustomizer = StyleCustomizer<MessageItemStyle> { _, style -> style }
    }

    internal class Builder(
            internal val context: Context,
            private val attrs: AttributeSet?
    ) {
        fun build(): MessageItemStyle {
            context.obtainStyledAttributes(attrs, R.styleable.MessagesListView).use { array ->
                val accentColor = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)

                val incBubbleColor: Int = array.getColor(R.styleable.MessagesListView_sceytUiMessagesListIncomingBubbleColor,
                    context.getCompatColor(R.color.sceyt_color_bg_inc_message))

                val color2 = if (context.isAppInDarkMode()) Color.BLACK else Color.WHITE
                val defaultOutBubbleColor = ColorUtils.blendARGB(accentColor, color2, 0.8f)
                val outBubbleColor: Int = array.getColor(R.styleable.MessagesListView_sceytUiMessagesListOutgoingBubbleColor,
                    defaultOutBubbleColor)

                val defaultOutDarkColor = ColorUtils.blendARGB(accentColor, color2, 0.75f)
                val incReplyBackgroundColor: Int = array.getColor(R.styleable.MessagesListView_sceytUiMessagesListIncomingReplyBackgroundColor,
                    context.getCompatColor(R.color.sceyt_color_surface_2))

                val outReplyBackgroundColor: Int = array.getColor(R.styleable.MessagesListView_sceytUiMessagesListOutgoingReplyBackgroundColor,
                    defaultOutDarkColor)

                val incLinkPreviewBackgroundColor: Int = array.getColor(R.styleable.MessagesListView_sceytUiMessagesListIncomingLinkPreviewBackgroundColor,
                    context.getCompatColor(R.color.sceyt_color_bg_inc_link_preview))

                val outLinkPreviewBackgroundColor: Int = array.getColor(R.styleable.MessagesListView_sceytUiMessagesListOutgoingLinkPreviewBackgroundColor,
                    defaultOutDarkColor)

                val onOverlayColor: Int = array.getColor(R.styleable.MessagesListView_sceytUiMessagesListOnOverlayColor,
                    context.getCompatColor(SceytChatUIKit.theme.colors.overlayBackground2Color))

                val threadReplyArrowStrokeColor: Int = array.getColor(R.styleable.MessagesListView_sceytUiMessagesListThreadReplyArrowStrokeColor,
                    context.getCompatColor(SceytChatUIKit.theme.colors.accentColor))

                val reactionsContainerBackgroundColor: Int = array.getColor(R.styleable.MessagesListView_sceytUiMessagesListReactionsContainerBackgroundColor,
                    context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColorSections))

                val defaultHighlightedMessageColor = ColorUtils.setAlphaComponent(accentColor, (0.3 * 255).toInt())
                val highlightedMessageColor: Int = array.getColor(R.styleable.MessagesListView_sceytUiMessagesListHighlightedMessageColor,
                    defaultHighlightedMessageColor)

                val linkTextColor: Int = array.getColor(R.styleable.MessagesListView_sceytUiMessagesListLinkTextColor,
                    context.getCompatColor(R.color.sceyt_auto_link_color))

                val videoIcon = array.getDrawable(R.styleable.MessagesListView_sceytUiMessagesListVideoIcon)
                        ?: context.getCompatDrawable(R.drawable.sceyt_ic_video).applyTint(
                            context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor)
                        )

                val voicePlayIcon = array.getDrawable(R.styleable.MessagesListView_sceytUiMessagesListVoicePlayIcon)
                        ?: context.getCompatDrawable(R.drawable.sceyt_ic_play).applyTint(
                            context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor)
                        )

                val voicePauseIcon = array.getDrawable(R.styleable.MessagesListView_sceytUiMessagesListVoicePauseIcon)
                        ?: context.getCompatDrawable(R.drawable.sceyt_ic_pause).applyTint(
                            context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor)
                        )

                val viewCountIcon = array.getDrawable(R.styleable.MessagesListView_sceytUiMessagesListViewCountIcon)
                        ?: context.getCompatDrawable(R.drawable.sceyt_ic_display_count).applyTint(
                            context.getCompatColor(SceytChatUIKit.theme.colors.iconSecondaryColor)
                        )

                val swipeToReplyIcon = array.getDrawable(R.styleable.MessagesListView_sceytUiMessagesListSwipeToReplyIcon)
                        ?: context.getCompatDrawable(R.drawable.sceyt_is_reply_swipe).applyTint(
                            context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor)
                        )

                val forwardedIcon = array.getDrawable(R.styleable.MessagesListView_sceytUiMessagesListForwardedIcon)
                        ?: context.getCompatDrawable(R.drawable.sceyt_ic_forward_14).applyTint(
                            context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
                        )

                val videoPlayIcon = array.getDrawable(R.styleable.MessagesListView_sceytUiMessagesListVideoPlayIcon)
                        ?: context.getCompatDrawable(R.drawable.sceyt_ic_play).applyTint(
                            context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor)
                        )

                val deliveryStatusIcons = MessageDeliveryStatusIcons.Builder(context, array)
                    .setPendingIconFromStyle(R.styleable.MessagesListView_sceytUiMessagesListMessageDeliveryStatusPendingIcon)
                    .setSentIconFromStyle(R.styleable.MessagesListView_sceytUiMessagesListMessageDeliveryStatusSentIcon)
                    .setReceivedIconIconFromStyle(R.styleable.MessagesListView_sceytUiMessagesListMessageDeliveryStatusReceivedIcon)
                    .setDisplayedIconFromStyle(R.styleable.MessagesListView_sceytUiMessagesListMessageDeliveryStatusDisplayedIcon)
                    .build()

                val editedStateText = array.getString(R.styleable.MessagesListView_sceytUiMessagesListEditedStateText)
                        ?: context.getString(R.string.sceyt_edited)

                val deletedStateText = array.getString(R.styleable.MessagesListView_sceytUiMessagesListDeletedStateText)
                        ?: context.getString(R.string.sceyt_message_was_deleted)

                val forwardedText = array.getString(R.styleable.MessagesListView_sceytUiMessagesListForwardedText)
                        ?: context.getString(R.string.sceyt_forwarded_message)

                return MessageItemStyle(
                    incomingBubbleColor = incBubbleColor,
                    outgoingBubbleColor = outBubbleColor,
                    incomingReplyBackgroundColor = incReplyBackgroundColor,
                    outgoingReplyBackgroundColor = outReplyBackgroundColor,
                    incomingLinkPreviewBackgroundColor = incLinkPreviewBackgroundColor,
                    outgoingLinkPreviewBackgroundColor = outLinkPreviewBackgroundColor,
                    onOverlayColor = onOverlayColor,
                    threadReplyArrowStrokeColor = threadReplyArrowStrokeColor,
                    reactionsContainerBackgroundColor = reactionsContainerBackgroundColor,
                    highlightedMessageColor = highlightedMessageColor,
                    linkTextColor = linkTextColor,
                    videoIcon = videoIcon,
                    swipeToReplyIcon = swipeToReplyIcon,
                    forwardedIcon = forwardedIcon,
                    videoPlayIcon = videoPlayIcon,
                    voicePlayIcon = voicePlayIcon,
                    voicePauseIcon = voicePauseIcon,
                    viewCountIcon = viewCountIcon,
                    messageDeliveryStatusIcons = deliveryStatusIcons,
                    editedStateText = editedStateText,
                    deletedStateText = deletedStateText,
                    forwardedText = forwardedText,
                    bodyTextStyle = buildBodyTextStyle(array),
                    deletedMessageTextStyle = buildDeletedMessageTextStyle(array),
                    senderNameTextStyle = buildSenderNameTextStyle(array),
                    messageDateTextStyle = buildMessageDateTextStyle(array),
                    messageStateTextStyle = buildMessageStateTextStyle(array),
                    mentionTextStyle = buildMentionTextStyle(array),
                    videoDurationTextStyle = buildVideoDurationTextStyle(array),
                    threadReplyCountTextStyle = buildThreadReplyCountTextStyle(array),
                    forwardTitleTextStyle = buildForwardTitleTextStyle(array),
                    reactionCountTextStyle = buildReactionCountTextStyle(array),
                    voiceSpeedTextStyle = buildVoiceSpeedTextStyle(array),
                    voiceDurationTextStyle = buildVoiceDurationTextStyle(array),
                    attachmentFileNameTextStyle = buildAttachmentFileNameTextStyle(array),
                    attachmentFileSizeTextStyle = buildAttachmentFileSizeTextStyle(array),
                    linkPreviewStyle = buildLinkPreviewStyle(array),
                    replyMessageStyle = buildReplyMessageStyle(array),
                    mediaLoaderStyle = buildMediaLoaderStyle(array),
                    voiceWaveformStyle = buildAudioWaveformStyle(array),
                    overlayMediaLoaderStyle = buildOverlayMediaLoaderStyle(array),
                    selectionCheckboxStyle = buildSelectionCheckboxStyle(array),
                    senderNameFormatter = SceytChatUIKit.formatters.userNameFormatterNew,
                    messageBodyFormatter = null,
                    messageViewCountFormatter = SceytChatUIKit.formatters.messageViewCountFormatter,
                    messageDateFormatter = SceytChatUIKit.formatters.messageDateFormatter,
                    mentionUserNameFormatter = SceytChatUIKit.formatters.mentionUserNameFormatter,
                    voiceDurationFormatter = SceytChatUIKit.formatters.mediaDurationFormatter,
                    videoDurationFormatter = SceytChatUIKit.formatters.mediaDurationFormatter,
                    attachmentFileSizeFormatter = SceytChatUIKit.formatters.attachmentSizeFormatter,
                    attachmentIconProvider = SceytChatUIKit.providers.attachmentIconProvider,
                    userDefaultAvatarProvider = SceytChatUIKit.providers.userDefaultAvatarProvider,
                    senderNameColorProvider = SceytChatUIKit.providers.senderNameColorProvider,
                ).let { styleCustomizer.apply(context, it) }
            }
        }
    }
}