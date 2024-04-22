package com.sceyt.chatuikit.sceytstyles

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.FontRes
import androidx.annotation.LayoutRes
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.sceytconfigs.dateformaters.MessageDateSeparatorFormatter

data class MessagesListViewStyle(
        val context: Context,
        @ColorInt
        val incBubbleColor: Int = context.getCompatColor(R.color.sceyt_color_bg_inc_message),

        @ColorInt
        val outBubbleColor: Int = context.getCompatColor(R.color.sceyt_color_bg_out_message),

        @ColorInt
        val incLinkPreviewBackgroundColor: Int = context.getCompatColor(R.color.sceyt_color_bg_inc_link_preview),

        @ColorInt
        val outLinkPreviewBackgroundColor: Int = context.getCompatColor(R.color.sceyt_color_bg_out_link_preview),

        val messageStatusPendingIcon: Drawable? = context.getCompatDrawable(R.drawable.sceyt_ic_status_not_sent),

        val messageStatusSentIcon: Drawable? = context.getCompatDrawable(R.drawable.sceyt_ic_status_on_server),

        val messageStatusDeliveredIcon: Drawable? = context.getCompatDrawable(R.drawable.sceyt_ic_status_delivered),

        val messageStatusReadIcon: Drawable? = context.getCompatDrawable(R.drawable.sceyt_ic_status_read),

        @ColorInt
        val senderNameTextColor: Int = context.getCompatColor(SceytChatUIKit.theme.accentColor),

        @ColorInt
        val replyMessageLineColor: Int = context.getCompatColor(SceytChatUIKit.theme.accentColor),

        @ColorInt
        val dateSeparatorItemBackgroundColor: Int = context.getCompatColor(R.color.sceyt_color_dark_blue),

        @ColorInt
        val dateSeparatorItemTextColor: Int = context.getCompatColor(R.color.sceyt_color_white),

        @ColorInt
        val autoLinkTextColor: Int = context.getCompatColor(R.color.sceyt_auto_link_color),

        @LayoutRes
        val emptyState: Int = R.layout.sceyt_messages_empty_state,

        @LayoutRes
        val emptyStateSelfChannel: Int = R.layout.sceyt_messages_empty_state_self_channel,

        @LayoutRes
        val loadingState: Int = R.layout.sceyt_loading_state,

        val dateSeparatorDateFormat: MessageDateSeparatorFormatter = MessageDateSeparatorFormatter(),

        @FontRes
        val dateSeparatorTextFont: Int = -1,

        val dateSeparatorTextStyle: Int = Typeface.NORMAL,

        val unreadMessagesSeparatorTextStyle: Int = Typeface.NORMAL,

        @ColorInt
        val unreadMessagesTextColor: Int = context.getCompatColor(R.color.sceyt_color_gray_400),

        @ColorInt
        val unreadMessagesBackendColor: Int = context.getCompatColor(R.color.sceyt_color_bg_unread_messages_separator),

        val editedMessageStateText: String = context.getString(R.string.sceyt_edited),

        val messageEditedTextStyle: Int = Typeface.ITALIC,

        @Dimension
        val sameSenderMsgDistance: Int = dpToPx(4f),

        @Dimension
        val differentSenderMsgDistance: Int = dpToPx(8f),

        @ColorInt
        val downScrollerUnreadCountColor: Int = context.getCompatColor(SceytChatUIKit.theme.accentColor),

        @ColorInt
        val mediaLoaderColor: Int = context.getCompatColor(R.color.sceyt_color_white),

        val videoDurationIcon: Drawable? = context.getCompatDrawable(R.drawable.sceyt_ic_video),

        val fileAttachmentIcon: Drawable? = context.getCompatDrawable(R.drawable.sceyt_ic_file_filled),

        val linkAttachmentIcon: Drawable? = context.getCompatDrawable(R.drawable.sceyt_ic_link_attachment),

        @ColorInt
        val selfReactionBackgroundColor: Int = context.getCompatColor(R.color.sceyt_self_reaction_color),

        @ColorInt
        val selfReactionBorderColor: Int = context.getCompatColor(R.color.sceyt_color_border),

        val swipeReplyIcon: Drawable? = context.getCompatDrawable(R.drawable.sceyt_is_reply_swipe)
) {
    companion object {
        @JvmField
        var messagesStyleCustomizer = StyleCustomizer<MessagesListViewStyle> { it }
    }

    internal class Builder(
            private val context: Context,
            private val typedArray: TypedArray
    ) {

        fun build() = MessagesListViewStyle(context).run {
            copy(
                incBubbleColor = typedArray.getColor(R.styleable.MessagesListView_sceytUiMessageIncBubbleColor, incBubbleColor),
                outBubbleColor = typedArray.getColor(R.styleable.MessagesListView_sceytUiMessageOutBubbleColor, outBubbleColor),
                incLinkPreviewBackgroundColor = typedArray.getColor(R.styleable.MessagesListView_sceytUiMessageIncLinkPreviewBackgroundColor, incLinkPreviewBackgroundColor),
                outLinkPreviewBackgroundColor = typedArray.getColor(R.styleable.MessagesListView_sceytUiMessageOutLinkPreviewBackgroundColor, outLinkPreviewBackgroundColor),
                messageStatusPendingIcon = typedArray.getDrawable(R.styleable.MessagesListView_sceytUiMessagePendingIcon)
                        ?: messageStatusPendingIcon,
                messageStatusSentIcon = typedArray.getDrawable(R.styleable.MessagesListView_sceytUiMessageSentIcon)
                        ?: messageStatusSentIcon,
                messageStatusDeliveredIcon = typedArray.getDrawable(R.styleable.MessagesListView_sceytUiMessageDeliveredIcon)
                        ?: messageStatusDeliveredIcon,
                messageStatusReadIcon = typedArray.getDrawable(R.styleable.MessagesListView_sceytUiMessageReadIcon)
                        ?: messageStatusReadIcon,
                senderNameTextColor = typedArray.getColor(R.styleable.MessagesListView_sceytUiMessageSenderNameTextColor, senderNameTextColor),
                replyMessageLineColor = typedArray.getColor(R.styleable.MessagesListView_sceytUiReplyMessageLineColor, replyMessageLineColor),
                dateSeparatorItemBackgroundColor = typedArray.getColor(R.styleable.MessagesListView_sceytUiDateSeparatorItemBackgroundColor, dateSeparatorItemBackgroundColor),
                dateSeparatorItemTextColor = typedArray.getColor(R.styleable.MessagesListView_sceytUiDateSeparatorItemTextColor, dateSeparatorItemTextColor),
                emptyState = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiEmptyStateLayout, emptyState),
                emptyStateSelfChannel = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiEmptyStateSelfChannelLayout, emptyStateSelfChannel),
                autoLinkTextColor = typedArray.getColor(R.styleable.MessagesListView_sceytUiAutoLinkTextColor, autoLinkTextColor),
                sameSenderMsgDistance = typedArray.getDimensionPixelSize(R.styleable.MessagesListView_sceytUiSameSenderMessageDistance, sameSenderMsgDistance),
                differentSenderMsgDistance = typedArray.getDimensionPixelSize(R.styleable.MessagesListView_sceytUiDifferentSenderMessageDistance, differentSenderMsgDistance),
                editedMessageStateText = typedArray.getString(R.styleable.MessagesListView_sceytUiMessageEditedText)
                        ?: editedMessageStateText,
                messageEditedTextStyle = typedArray.getInt(R.styleable.MessagesListView_sceytUiMessageEditedTextStyle, messageEditedTextStyle),
                dateSeparatorTextFont = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiDateSeparatorItemTextFont, dateSeparatorTextFont),
                dateSeparatorTextStyle = typedArray.getInt(R.styleable.MessagesListView_sceytUiDateSeparatorItemTextStyle, dateSeparatorTextStyle),
                unreadMessagesSeparatorTextStyle = typedArray.getInt(R.styleable.MessagesListView_sceytUiUnreadMessagesSeparatorTextStyle, unreadMessagesSeparatorTextStyle),
                unreadMessagesTextColor = typedArray.getColor(R.styleable.MessagesListView_sceytUiUnreadMessagesSeparatorTextColor, unreadMessagesTextColor),
                unreadMessagesBackendColor = typedArray.getColor(R.styleable.MessagesListView_sceytUiUnreadMessagesSeparatorBackgroundColor, unreadMessagesBackendColor),
                downScrollerUnreadCountColor = typedArray.getColor(R.styleable.MessagesListView_sceytUiDownScrollerUnreadCountColor, downScrollerUnreadCountColor),
                mediaLoaderColor = typedArray.getColor(R.styleable.MessagesListView_sceytUiMediaLoaderColor, mediaLoaderColor),
                videoDurationIcon = typedArray.getDrawable(R.styleable.MessagesListView_sceytUiVideoDurationIcon)
                        ?: videoDurationIcon,
                fileAttachmentIcon = typedArray.getDrawable(R.styleable.MessagesListView_sceytUiFileAttachmentIcon)
                        ?: fileAttachmentIcon,
                linkAttachmentIcon = typedArray.getDrawable(R.styleable.MessagesListView_sceytUiLinkAttachmentIcon)
                        ?: linkAttachmentIcon,
                selfReactionBackgroundColor = typedArray.getColor(R.styleable.MessagesListView_sceytUiSelfReactionBackgroundColor, selfReactionBackgroundColor),
                selfReactionBorderColor = typedArray.getColor(R.styleable.MessagesListView_sceytUiSelfReactionBorderColor, selfReactionBorderColor),
                swipeReplyIcon = typedArray.getDrawable(R.styleable.MessagesListView_sceytUiSwipeReplyIcon)
                        ?: swipeReplyIcon,
            )
        }.let(messagesStyleCustomizer::apply)
    }
}