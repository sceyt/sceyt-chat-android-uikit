package com.sceyt.chatuikit.sceytstyles

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.core.graphics.ColorUtils
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable

/**
 * Style for the message item view.
 * @param incBubbleColor Color for the incoming message bubble, default is [R.color.sceyt_color_bg_inc_message]
 * @param outBubbleColor Color for the outgoing message bubble, default is [R.color.sceyt_color_bg_out_message]
 * @param incLinkPreviewBackgroundColor Color for the incoming link preview background, default is [R.color.sceyt_color_bg_inc_link_preview]
 * @param outLinkPreviewBackgroundColor Color for the outgoing link preview background, default is [R.color.sceyt_color_bg_out_link_preview]
 * @param messageStatusPendingIcon Icon for the pending message status, default is [R.drawable.sceyt_ic_status_not_sent]
 * @param messageStatusSentIcon Icon for the sent message status, default is [R.drawable.sceyt_ic_status_on_server]
 * @param messageStatusDeliveredIcon Icon for the delivered message status, default is [R.drawable.sceyt_ic_status_delivered]
 * @param messageStatusReadIcon Icon for the read message status, default is [R.drawable.sceyt_ic_status_read]
 * @param messageDateTextColor Color for the message date text, default is textColorSecondary from the theme [SceytChatUIKit.theme]
 * @param senderNameTextColor Color for the sender name text, default is accentColor from the theme [SceytChatUIKit.theme]
 * @param replyMessageLineColor Color for the reply message line, default is accentColor from the theme [SceytChatUIKit.theme]
 * @param autoLinkTextColor Color for the auto link text, default is [R.color.sceyt_auto_link_color]
 * @param editedMessageStateText Text for the edited message state, default is [R.string.sceyt_edited]
 * @param messageEditedTextStyle Style for the edited message state text, default is [Typeface.ITALIC]
 * @param sameSenderMsgDistance Distance between the same sender messages, default is 4dp
 * @param differentSenderMsgDistance Distance between the different sender messages, default is 8dp
 * @param mediaLoaderColor Color for the media loader, default is [R.color.sceyt_color_on_primary]
 * @param videoDurationIcon Icon for the video duration, default is [R.drawable.sceyt_ic_video]
 * @param fileAttachmentIcon Icon for the file attachment, default is [R.drawable.sceyt_ic_file_filled]
 * @param linkAttachmentIcon Icon for the link attachment, default is [R.drawable.sceyt_ic_link_attachment]
 * @param swipeReplyIcon Icon for the swipe reply, default is [R.drawable.sceyt_is_reply_swipe]
 * */
data class MessageItemStyle(
        @ColorInt val incBubbleColor: Int,
        @ColorInt val outBubbleColor: Int,
        @ColorInt val incLinkPreviewBackgroundColor: Int,
        @ColorInt val outLinkPreviewBackgroundColor: Int,
        val messageStatusPendingIcon: Drawable?,
        val messageStatusSentIcon: Drawable?,
        val messageStatusDeliveredIcon: Drawable?,
        val messageStatusReadIcon: Drawable?,
        @ColorInt val bodyTextColor: Int,
        @ColorInt val messageDateTextColor: Int,
        @ColorInt val senderNameTextColor: Int,
        @ColorInt val replyMessageLineColor: Int,
        @ColorInt val autoLinkTextColor: Int,
        val editedMessageStateText: String,
        val messageEditedTextStyle: Int = Typeface.ITALIC,
        @Dimension val sameSenderMsgDistance: Int = dpToPx(4f),
        @Dimension val differentSenderMsgDistance: Int = dpToPx(8f),
        @ColorInt val mediaLoaderColor: Int,
        val videoDurationIcon: Drawable?,
        val fileAttachmentIcon: Drawable?,
        val linkAttachmentIcon: Drawable?,
        val swipeReplyIcon: Drawable?
) {

    companion object {
        @JvmField
        var messageItemStyleCustomizer = StyleCustomizer<MessageItemStyle> { it }
    }

    internal class Builder(
            private val context: Context,
            private val attrs: AttributeSet?
    ) {
        fun build(): MessageItemStyle {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.MessagesListView, 0, 0)
            val accentColor = context.getCompatColor(SceytChatUIKit.theme.accentColor)

            val incBubbleColor: Int = typedArray.getColor(R.styleable.MessagesListView_sceytUiMessageIncBubbleColor,
                context.getCompatColor(R.color.sceyt_color_bg_inc_message))

            val defaultOutBubbleColor = ColorUtils.setAlphaComponent(accentColor, (0.2 * 255).toInt())
            val outBubbleColor: Int = typedArray.getColor(R.styleable.MessagesListView_sceytUiMessageOutBubbleColor, defaultOutBubbleColor)

            val incLinkPreviewBackgroundColor: Int = typedArray.getColor(R.styleable.MessagesListView_sceytUiMessageIncLinkPreviewBackgroundColor,
                context.getCompatColor(R.color.sceyt_color_bg_inc_link_preview))

            val defaultOutLinkPreviewBackgroundColor = ColorUtils.setAlphaComponent(accentColor, (0.3 * 255).toInt())
            val outLinkPreviewBackgroundColor: Int = typedArray.getColor(R.styleable.MessagesListView_sceytUiMessageOutLinkPreviewBackgroundColor,
                defaultOutLinkPreviewBackgroundColor)

            val messageStatusPendingIcon: Drawable? = typedArray.getDrawable(R.styleable.MessagesListView_sceytUiMessagePendingIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_status_not_sent)?.apply {
                        setTint(context.getCompatColor(SceytChatUIKit.theme.iconSecondaryColor))
                    }

            val messageStatusSentIcon: Drawable? = typedArray.getDrawable(R.styleable.MessagesListView_sceytUiMessageSentIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_status_on_server)?.apply {
                        setTint(context.getCompatColor(SceytChatUIKit.theme.iconSecondaryColor))
                    }

            val messageStatusDeliveredIcon: Drawable? = typedArray.getDrawable(R.styleable.MessagesListView_sceytUiMessageDeliveredIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_status_delivered)?.apply {
                        setTint(context.getCompatColor(SceytChatUIKit.theme.iconSecondaryColor))
                    }

            val messageStatusReadIcon: Drawable? = (typedArray.getDrawable(R.styleable.MessagesListView_sceytUiMessageReadIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_status_read))?.apply {
                setTint(accentColor)
            }

            val bodyTextColor: Int = typedArray.getColor(R.styleable.MessagesListView_sceytUiMessageBodyTextColor,
                context.getCompatColor(SceytChatUIKit.theme.textPrimaryColor))

            val messageDateTextColor: Int = typedArray.getColor(R.styleable.MessagesListView_sceytUiMessageDateTextColor,
                context.getCompatColor(SceytChatUIKit.theme.textSecondaryColor))

            val senderNameTextColor: Int = typedArray.getColor(R.styleable.MessagesListView_sceytUiMessageSenderNameTextColor,
                context.getCompatColor(SceytChatUIKit.theme.accentColor))

            val replyMessageLineColor: Int = typedArray.getColor(R.styleable.MessagesListView_sceytUiReplyMessageLineColor,
                context.getCompatColor(SceytChatUIKit.theme.accentColor))

            val autoLinkTextColor: Int = typedArray.getColor(R.styleable.MessagesListView_sceytUiAutoLinkTextColor,
                context.getCompatColor(R.color.sceyt_auto_link_color))

            val sameSenderMsgDistance: Int = typedArray.getDimensionPixelSize(R.styleable.MessagesListView_sceytUiSameSenderMessageDistance, dpToPx(4f))

            val differentSenderMsgDistance: Int = typedArray.getDimensionPixelSize(R.styleable.MessagesListView_sceytUiDifferentSenderMessageDistance, dpToPx(8f))

            val editedMessageStateText: String = typedArray.getString(R.styleable.MessagesListView_sceytUiMessageEditedText)
                    ?: context.getString(R.string.sceyt_edited)

            val messageEditedTextStyle: Int = typedArray.getInt(R.styleable.MessagesListView_sceytUiMessageEditedTextStyle, Typeface.ITALIC)

            val mediaLoaderColor: Int = typedArray.getColor(R.styleable.MessagesListView_sceytUiMediaLoaderColor,
                context.getCompatColor(SceytChatUIKit.theme.textOnPrimaryColor))

            val videoDurationIcon: Drawable? = typedArray.getDrawable(R.styleable.MessagesListView_sceytUiVideoDurationIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_video)

            val fileAttachmentIcon: Drawable? = typedArray.getDrawable(R.styleable.MessagesListView_sceytUiFileAttachmentIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_file_filled)

            val linkAttachmentIcon: Drawable? = typedArray.getDrawable(R.styleable.MessagesListView_sceytUiLinkAttachmentIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_link_attachment)

            val swipeReplyIcon: Drawable? = typedArray.getDrawable(R.styleable.MessagesListView_sceytUiSwipeReplyIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_is_reply_swipe)
            typedArray.recycle()

            return MessageItemStyle(
                incBubbleColor = incBubbleColor,
                outBubbleColor = outBubbleColor,
                incLinkPreviewBackgroundColor = incLinkPreviewBackgroundColor,
                outLinkPreviewBackgroundColor = outLinkPreviewBackgroundColor,
                messageStatusPendingIcon = messageStatusPendingIcon,
                messageStatusSentIcon = messageStatusSentIcon,
                messageStatusDeliveredIcon = messageStatusDeliveredIcon,
                bodyTextColor = bodyTextColor,
                messageDateTextColor = messageDateTextColor,
                messageStatusReadIcon = messageStatusReadIcon,
                senderNameTextColor = senderNameTextColor,
                replyMessageLineColor = replyMessageLineColor,
                autoLinkTextColor = autoLinkTextColor,
                sameSenderMsgDistance = sameSenderMsgDistance,
                differentSenderMsgDistance = differentSenderMsgDistance,
                editedMessageStateText = editedMessageStateText,
                messageEditedTextStyle = messageEditedTextStyle,
                mediaLoaderColor = mediaLoaderColor,
                videoDurationIcon = videoDurationIcon,
                fileAttachmentIcon = fileAttachmentIcon,
                linkAttachmentIcon = linkAttachmentIcon,
                swipeReplyIcon = swipeReplyIcon
            ).let(messageItemStyleCustomizer::apply)
        }
    }
}