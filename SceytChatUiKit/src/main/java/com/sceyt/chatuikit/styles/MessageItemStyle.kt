package com.sceyt.chatuikit.styles

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.core.content.res.use
import androidx.core.graphics.ColorUtils
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.extensions.isAppInDarkMode
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.formatters.defaults.DefaultMessageBodyFormatter
import com.sceyt.chatuikit.providers.SceytChatUIKitProviders
import com.sceyt.chatuikit.providers.VisualProvider
import com.sceyt.chatuikit.styles.common.MessageDeliveryStatusIcons
import com.sceyt.chatuikit.theme.Colors

/**
 * Style for the message item view.
 * @property incomingBubbleColor Color for the incoming message bubble, default is [R.color.sceyt_color_bg_inc_message]
 * @property outgoingBubbleColor Color for the outgoing message bubble, default is 20% blend of [Colors.accentColor]
 * @property incomingLinkPreviewBackgroundColor Color for the incoming link preview background, default is [R.color.sceyt_color_bg_inc_link_preview]
 * @property outgoingLinkPreviewBackgroundColor Color for the outgoing link preview background, default is 30% blend of [Colors.accentColor]
 * @property messageDateTextColor Color for the message date text, default is [Colors.textSecondaryColor]
 * @property senderNameTextColor Color for the sender name text, default is [Colors.accentColor]
 * @property repliedMessageBorderColor Color for the reply message line, default is [Colors.accentColor]
 * @property autoLinkTextColor Color for the auto link text, default is [R.color.sceyt_auto_link_color]
 * @property mediaLoaderColor Color for the media loader, default is [R.color.sceyt_color_on_primary]
 * @property editedMessageStateText Text for the edited message state, default is [R.string.sceyt_edited]
 * @property messageEditedTextStyle Style for the edited message state text, default is [Typeface.ITALIC]
 * @property videoDurationIcon Icon for the video duration, default is [R.drawable.sceyt_ic_video]
 * @property swipeReplyIcon Icon for the swipe reply, default is [R.drawable.sceyt_is_reply_swipe]
 * @property replyMessageBodyFormatter Formatter for the reply message body, default is [DefaultMessageBodyFormatter] that returns the formatted body of the message
 * @property attachmentIconProvider - Provider for attachment icon, default is [SceytChatUIKitProviders.attachmentIconProvider].
 * @property replyMessageAttachmentIconProvider - Provider for attachment icon, default is [SceytChatUIKitProviders.attachmentIconProvider].
 * */
data class MessageItemStyle(
        @ColorInt var incomingBubbleColor: Int,
        @ColorInt var outgoingBubbleColor: Int,
        @ColorInt var incomingReplyBackgroundColor: Int,
        @ColorInt var outgoingReplyBackgroundColor: Int,
        @ColorInt var incomingLinkPreviewBackgroundColor: Int,
        @ColorInt var outgoingLinkPreviewBackgroundColor: Int,
        @ColorInt var bodyTextColor: Int,
        @ColorInt var messageDateTextColor: Int,
        @ColorInt var senderNameTextColor: Int,
        @ColorInt var repliedMessageBorderColor: Int,
        @ColorInt var autoLinkTextColor: Int,
        @ColorInt var mediaLoaderColor: Int,
        var messageDeliveryStatusIcons: MessageDeliveryStatusIcons,
        var editedMessageStateText: String,
        var messageEditedTextStyle: Int = Typeface.ITALIC,
        var videoDurationIcon: Drawable?,
        var swipeReplyIcon: Drawable?,
        var replyMessageBodyFormatter: Formatter<SceytMessage>,
        var attachmentIconProvider: VisualProvider<SceytAttachment, Drawable?>,
        var replyMessageAttachmentIconProvider: VisualProvider<SceytAttachment, Drawable?>
) {

    companion object {
        @JvmField
        var styleCustomizer = StyleCustomizer<MessageItemStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
            private val attrs: AttributeSet?
    ) {
        fun build(): MessageItemStyle {
            context.obtainStyledAttributes(attrs, R.styleable.MessagesListView).use { array ->
                val accentColor = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)

                val incBubbleColor: Int = array.getColor(R.styleable.MessagesListView_sceytUiMessageIncBubbleColor,
                    context.getCompatColor(R.color.sceyt_color_bg_inc_message))

                val color2 = if (context.isAppInDarkMode()) Color.BLACK else Color.WHITE
                val defaultOutBubbleColor = ColorUtils.blendARGB(accentColor, color2, 0.8f)
                val outBubbleColor: Int = array.getColor(R.styleable.MessagesListView_sceytUiMessageOutBubbleColor,
                    defaultOutBubbleColor)

                val defaultOutDarkColor = ColorUtils.blendARGB(accentColor, color2, 0.75f)
                val incReplyBackgroundColor: Int = array.getColor(R.styleable.MessagesListView_sceytUiMessageIncReplyBackgroundColor,
                    context.getCompatColor(R.color.sceyt_color_surface_2))

                val outReplyBackgroundColor: Int = array.getColor(R.styleable.MessagesListView_sceytUiMessageOutReplBackgroundColor,
                    defaultOutDarkColor)

                val incLinkPreviewBackgroundColor: Int = array.getColor(R.styleable.MessagesListView_sceytUiMessageIncLinkPreviewBackgroundColor,
                    context.getCompatColor(R.color.sceyt_color_bg_inc_link_preview))

                val outLinkPreviewBackgroundColor: Int = array.getColor(R.styleable.MessagesListView_sceytUiMessageOutLinkPreviewBackgroundColor,
                    defaultOutDarkColor)

                val deliveryStatusIcons = MessageDeliveryStatusIcons.Builder(context, array)
                    .setPendingIconFromStyle(R.styleable.MessagesListView_sceytUiMessagePendingIcon)
                    .setSentIconFromStyle(R.styleable.MessagesListView_sceytUiMessageSentIcon)
                    .setReceivedIconIconFromStyle(R.styleable.MessagesListView_sceytUiMessageReceivedIcon)
                    .setDisplayedIconFromStyle(R.styleable.MessagesListView_sceytUiMessageDisplayedIcon)
                    .build()

                val bodyTextColor: Int = array.getColor(R.styleable.MessagesListView_sceytUiMessageBodyTextColor,
                    context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor))

                val messageDateTextColor: Int = array.getColor(R.styleable.MessagesListView_sceytUiMessageDateTextColor,
                    context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor))

                val senderNameTextColor: Int = array.getColor(R.styleable.MessagesListView_sceytUiMessageSenderNameTextColor,
                    context.getCompatColor(SceytChatUIKit.theme.colors.accentColor))

                val replyMessageLineColor: Int = array.getColor(R.styleable.MessagesListView_sceytUiReplyMessageLineColor,
                    context.getCompatColor(SceytChatUIKit.theme.colors.accentColor))

                val autoLinkTextColor: Int = array.getColor(R.styleable.MessagesListView_sceytUiAutoLinkTextColor,
                    context.getCompatColor(R.color.sceyt_auto_link_color))

                val editedMessageStateText: String = array.getString(R.styleable.MessagesListView_sceytUiMessageEditedText)
                        ?: context.getString(R.string.sceyt_edited)

                val messageEditedTextStyle: Int = array.getInt(R.styleable.MessagesListView_sceytUiMessageEditedTextStyle, Typeface.ITALIC)

                val mediaLoaderColor: Int = array.getColor(R.styleable.MessagesListView_sceytUiMediaLoaderColor,
                    context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor))

                val videoDurationIcon: Drawable? = array.getDrawable(R.styleable.MessagesListView_sceytUiVideoDurationIcon)
                        ?: context.getCompatDrawable(R.drawable.sceyt_ic_video)

                val swipeReplyIcon: Drawable? = array.getDrawable(R.styleable.MessagesListView_sceytUiSwipeReplyIcon)
                        ?: context.getCompatDrawable(R.drawable.sceyt_is_reply_swipe)

                return MessageItemStyle(
                    incomingBubbleColor = incBubbleColor,
                    outgoingBubbleColor = outBubbleColor,
                    incomingReplyBackgroundColor = incReplyBackgroundColor,
                    outgoingReplyBackgroundColor = outReplyBackgroundColor,
                    incomingLinkPreviewBackgroundColor = incLinkPreviewBackgroundColor,
                    outgoingLinkPreviewBackgroundColor = outLinkPreviewBackgroundColor,
                    bodyTextColor = bodyTextColor,
                    messageDateTextColor = messageDateTextColor,
                    senderNameTextColor = senderNameTextColor,
                    repliedMessageBorderColor = replyMessageLineColor,
                    autoLinkTextColor = autoLinkTextColor,
                    messageDeliveryStatusIcons = deliveryStatusIcons,
                    editedMessageStateText = editedMessageStateText,
                    messageEditedTextStyle = messageEditedTextStyle,
                    mediaLoaderColor = mediaLoaderColor,
                    videoDurationIcon = videoDurationIcon,
                    swipeReplyIcon = swipeReplyIcon,
                    replyMessageBodyFormatter = SceytChatUIKit.formatters.messageBodyFormatter,
                    attachmentIconProvider = SceytChatUIKit.providers.attachmentIconProvider,
                    replyMessageAttachmentIconProvider = SceytChatUIKit.providers.attachmentIconProvider,
                ).let { styleCustomizer.apply(context, it) }
            }
        }
    }
}