package com.sceyt.chatuikit.styles.messages_list.item

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.IntRange
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
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.formatters.SceytChatUIKitFormatters
import com.sceyt.chatuikit.formatters.attributes.MessageBodyFormatterAttributes
import com.sceyt.chatuikit.styles.SceytComponentStyle
import com.sceyt.chatuikit.providers.SceytChatUIKitProviders
import com.sceyt.chatuikit.providers.VisualProvider
import com.sceyt.chatuikit.renderers.AvatarRenderer
import com.sceyt.chatuikit.renderers.SceytChatUIKitRenderers
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.AvatarStyle
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.CheckboxStyle
import com.sceyt.chatuikit.styles.common.MediaLoaderStyle
import com.sceyt.chatuikit.styles.common.MessageDeliveryStatusIcons
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildAttachmentFileNameTextStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildAttachmentFileSizeTextStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildAudioWaveformStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildAvatarStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildBodyTextStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildDeletedMessageTextStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildDeliveryStatusIconStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildForwardTitleTextStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildIncomingBubbleBackgroundStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildIncomingLinkPreviewBackgroundStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildIncomingReplyBackgroundStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildLinkPreviewStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildMediaLoaderStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildMentionTextStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildMessageDateTextStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildMessageStateTextStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildOutgoingBubbleBackgroundStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildOutgoingLinkPreviewBackgroundStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildOutgoingReplyBackgroundStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildOverlayMediaLoaderStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildPollStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildReactionCountTextStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildReactionsContainerBackgroundStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildReadMoreStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildReplyMessageStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildSelectionCheckboxStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildSenderNameTextStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildSystemMessageItemStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildThreadReplyCountTextStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildVideoDurationTextStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildVoiceDurationTextStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildVoiceSpeedTextStyle
import com.sceyt.chatuikit.theme.Colors
import java.util.Date

/**
 * Style for the message item view.
 * @property incomingBubbleBackgroundStyle Color for the incoming message bubble, default is [buildIncomingBubbleBackgroundStyle]
 * @property outgoingBubbleBackgroundStyle Color for the outgoing message bubble, default is [buildOutgoingBubbleBackgroundStyle]
 * @property incomingReplyBackgroundStyle Color for the incoming reply background, default is [incomingReplyBackgroundStyle]
 * @property outgoingReplyBackgroundStyle Color for the outgoing reply background, default is [outgoingReplyBackgroundStyle]
 * @property incomingLinkPreviewBackgroundStyle Color for the incoming link preview background, default is [incomingLinkPreviewBackgroundStyle]
 * @property outgoingLinkPreviewBackgroundStyle Color for the outgoing link preview background, default is [outgoingLinkPreviewBackgroundStyle]
 * @property onOverlayColor Color for the overlay, default is [Colors.overlayBackground2Color]
 * @property threadReplyArrowStrokeColor Color for the thread reply arrow stroke, default is [Colors.accentColor]
 * @property reactionsContainerBackgroundStyle Color for the reactions container background, default is [buildReactionsContainerBackgroundStyle]
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
 * @property readMoreStyle Style for the "Read More" button that appears when message body exceeds character limit, default is [buildReadMoreStyle].
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
 * @property avatarStyle Style for the avatar, default is [buildAvatarStyle].
 * @property linkPreviewStyle Style for the link preview, default is [buildLinkPreviewStyle].
 * @property replyMessageStyle Style for the reply message, default is [buildReplyMessageStyle].
 * @property pollStyle Style for the poll, default is [buildPollStyle].
 * @property mediaLoaderStyle Style for the media loader, default is [buildMediaLoaderStyle].
 * @property overlayMediaLoaderStyle Style for the overlay media loader, default is [buildOverlayMediaLoaderStyle].
 * @property voiceWaveformStyle Style for the voice waveform, default is [buildAudioWaveformStyle].
 * @property selectionCheckboxStyle Style for the selection checkbox, default is [buildSelectionCheckboxStyle].
 * @property senderNameFormatter Formatter for the sender name, default is [SceytChatUIKitFormatters.userNameFormatter].
 * @property messageBodyFormatter Formatter for the message body. Use it to format the message body, default is [SceytChatUIKitFormatters.messageBodyFormatter].
 * @property unsupportedMessageBodyFormatter Formatter for unsupported message body. Use it to format unsupported messages, default is [SceytChatUIKitFormatters.unsupportedMessageBodyFormatter].
 * @property messageViewCountFormatter Formatter for the message view count, default is [SceytChatUIKitFormatters.messageViewCountFormatter].
 * @property messageDateFormatter Formatter for the message date, default is [SceytChatUIKitFormatters.messageDateFormatter].
 * @property voiceDurationFormatter Formatter for the voice duration, default is [SceytChatUIKitFormatters.mediaDurationFormatter].
 * @property videoDurationFormatter Formatter for the video duration, default is [SceytChatUIKitFormatters.mediaDurationFormatter].
 * @property attachmentFileSizeFormatter Formatter for the attachment file size, default is [SceytChatUIKitFormatters.attachmentSizeFormatter].
 * @property attachmentIconProvider Visual provider for the attachment icon, default is [SceytChatUIKitProviders.attachmentIconProvider].
 * @property senderNameColorProvider Visual provider for the sender name color, default is [SceytChatUIKitProviders.senderNameColorProvider].
 * @property userAvatarRenderer User avatar renderer, default is [SceytChatUIKitRenderers.userAvatarRenderer].
 * @property systemMessageItemStyle Style for the system message item, default is [SystemMessageItemStyle].
 * @property collapsedCharacterLimit Maximum number of characters to display before truncating message body and showing "Read More", default is [Int.MAX_VALUE].
 * */
data class MessageItemStyle(
    val incomingBubbleBackgroundStyle: BackgroundStyle,
    val outgoingBubbleBackgroundStyle: BackgroundStyle,
    val incomingReplyBackgroundStyle: BackgroundStyle,
    val outgoingReplyBackgroundStyle: BackgroundStyle,
    val incomingLinkPreviewBackgroundStyle: BackgroundStyle,
    val outgoingLinkPreviewBackgroundStyle: BackgroundStyle,
    val reactionsContainerBackgroundStyle: BackgroundStyle,
    @param:ColorInt val onOverlayColor: Int,
    @param:ColorInt val threadReplyArrowStrokeColor: Int,
    @param:ColorInt val highlightedMessageColor: Int,
    @param:ColorInt val linkTextColor: Int,
    val videoIcon: Drawable?,
    val swipeToReplyIcon: Drawable?,
    val forwardedIcon: Drawable?,
    val videoPlayIcon: Drawable?,
    val voicePlayIcon: Drawable?,
    val voicePauseIcon: Drawable?,
    val viewCountIcon: Drawable?,
    val editedStateText: String,
    val deletedStateText: String,
    val forwardedText: String,
    val messageDeliveryStatusIcons: MessageDeliveryStatusIcons,
    val readMoreStyle: ReadMoreStyle,
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
    val avatarStyle: AvatarStyle,
    val linkPreviewStyle: LinkPreviewStyle,
    val replyMessageStyle: ReplyMessageStyle,
    val pollStyle: PollStyle,
    val mediaLoaderStyle: MediaLoaderStyle,
    val overlayMediaLoaderStyle: MediaLoaderStyle,
    val voiceWaveformStyle: AudioWaveformStyle,
    val selectionCheckboxStyle: CheckboxStyle,
    val systemMessageItemStyle: SystemMessageItemStyle,
    val senderNameFormatter: Formatter<SceytUser>,
    val messageBodyFormatter: Formatter<MessageBodyFormatterAttributes>,
    val unsupportedMessageBodyFormatter: Formatter<SceytMessage>,
    val messageViewCountFormatter: Formatter<Long>,
    val messageDateFormatter: Formatter<Date>,
    val voiceDurationFormatter: Formatter<Long>,
    val videoDurationFormatter: Formatter<Long>,
    val attachmentFileSizeFormatter: Formatter<SceytAttachment>,
    val attachmentIconProvider: VisualProvider<SceytAttachment, Drawable?>,
    val senderNameColorProvider: VisualProvider<SceytUser, Int>,
    val userAvatarRenderer: AvatarRenderer<SceytUser>,
    @param:IntRange(from = 1, to = Long.MAX_VALUE) val collapsedCharacterLimit: Int,
) : SceytComponentStyle() {

    companion object {
        @JvmField
        var styleCustomizer = StyleCustomizer<MessageItemStyle> { _, style -> style }
    }

    internal class Builder(
            internal val context: Context,
            private val attrs: AttributeSet?,
    ) {
        private val accentColor = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
        private val bgColor = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColor)
        val defaultOutDarkColor = ColorUtils.blendARGB(accentColor, bgColor, 0.76f)
        val defaultOutBubbleColor = ColorUtils.blendARGB(accentColor, bgColor, 0.86f)

        fun build(): MessageItemStyle {
            context.obtainStyledAttributes(attrs, R.styleable.MessagesListView).use { array ->
                val onOverlayColor: Int = array.getColor(R.styleable.MessagesListView_sceytUiMessagesListOnOverlayColor,
                    context.getCompatColor(SceytChatUIKit.theme.colors.overlayBackground2Color))

                val threadReplyArrowStrokeColor: Int = array.getColor(R.styleable.MessagesListView_sceytUiMessagesListThreadReplyArrowStrokeColor,
                    context.getCompatColor(SceytChatUIKit.theme.colors.accentColor))

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

                val editedStateText = array.getString(R.styleable.MessagesListView_sceytUiMessagesListEditedStateText)
                        ?: context.getString(R.string.sceyt_edited)

                val deletedStateText = array.getString(R.styleable.MessagesListView_sceytUiMessagesListDeletedStateText)
                        ?: context.getString(R.string.sceyt_message_was_deleted)

                val forwardedText = array.getString(R.styleable.MessagesListView_sceytUiMessagesListForwardedText)
                        ?: context.getString(R.string.sceyt_forwarded_message)

                return MessageItemStyle(
                    incomingBubbleBackgroundStyle = buildIncomingBubbleBackgroundStyle(array),
                    outgoingBubbleBackgroundStyle = buildOutgoingBubbleBackgroundStyle(array),
                    incomingReplyBackgroundStyle = buildIncomingReplyBackgroundStyle(array),
                    outgoingReplyBackgroundStyle = buildOutgoingReplyBackgroundStyle(array),
                    incomingLinkPreviewBackgroundStyle = buildIncomingLinkPreviewBackgroundStyle(array),
                    outgoingLinkPreviewBackgroundStyle = buildOutgoingLinkPreviewBackgroundStyle(array),
                    reactionsContainerBackgroundStyle = buildReactionsContainerBackgroundStyle(array),
                    onOverlayColor = onOverlayColor,
                    threadReplyArrowStrokeColor = threadReplyArrowStrokeColor,
                    highlightedMessageColor = highlightedMessageColor,
                    linkTextColor = linkTextColor,
                    videoIcon = videoIcon,
                    swipeToReplyIcon = swipeToReplyIcon,
                    forwardedIcon = forwardedIcon,
                    videoPlayIcon = videoPlayIcon,
                    voicePlayIcon = voicePlayIcon,
                    voicePauseIcon = voicePauseIcon,
                    viewCountIcon = viewCountIcon,
                    editedStateText = editedStateText,
                    deletedStateText = deletedStateText,
                    forwardedText = forwardedText,
                    messageDeliveryStatusIcons = buildDeliveryStatusIconStyle(array),
                    readMoreStyle = buildReadMoreStyle(array),
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
                    avatarStyle = buildAvatarStyle(array),
                    linkPreviewStyle = buildLinkPreviewStyle(array),
                    replyMessageStyle = buildReplyMessageStyle(array),
                    pollStyle = buildPollStyle(array),
                    mediaLoaderStyle = buildMediaLoaderStyle(array),
                    voiceWaveformStyle = buildAudioWaveformStyle(array),
                    overlayMediaLoaderStyle = buildOverlayMediaLoaderStyle(array),
                    selectionCheckboxStyle = buildSelectionCheckboxStyle(array),
                    systemMessageItemStyle = buildSystemMessageItemStyle(array),
                    senderNameFormatter = SceytChatUIKit.formatters.userNameFormatter,
                    messageBodyFormatter = SceytChatUIKit.formatters.messageBodyFormatter,
                    unsupportedMessageBodyFormatter = SceytChatUIKit.formatters.unsupportedMessageBodyFormatter,
                    messageViewCountFormatter = SceytChatUIKit.formatters.messageViewCountFormatter,
                    messageDateFormatter = SceytChatUIKit.formatters.messageDateFormatter,
                    voiceDurationFormatter = SceytChatUIKit.formatters.voiceDurationFormatter,
                    videoDurationFormatter = SceytChatUIKit.formatters.mediaDurationFormatter,
                    attachmentFileSizeFormatter = SceytChatUIKit.formatters.attachmentSizeFormatter,
                    attachmentIconProvider = SceytChatUIKit.providers.attachmentIconProvider,
                    senderNameColorProvider = SceytChatUIKit.providers.senderNameColorProvider,
                    userAvatarRenderer = SceytChatUIKit.renderers.userAvatarRenderer,
                    collapsedCharacterLimit = Int.MAX_VALUE,
                ).let { styleCustomizer.apply(context, it) }
            }
        }
    }
}