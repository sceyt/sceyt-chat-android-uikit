package com.sceyt.sceytchatuikit.sceytstyles

import android.content.res.TypedArray
import android.graphics.Typeface
import androidx.annotation.*
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.extensions.dpToPx
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.sceytconfigs.dateformaters.MessageDateSeparatorFormatter

object MessagesStyle {

    @ColorRes
    var incBubbleColor: Int = R.color.sceyt_color_bg_inc_message

    @ColorRes
    var outBubbleColor: Int = R.color.sceyt_color_bg_out_message

    @DrawableRes
    var messageStatusPendingIcon: Int = R.drawable.sceyt_ic_status_not_sent

    @DrawableRes
    var messageStatusSentIcon: Int = R.drawable.sceyt_ic_status_on_server

    @DrawableRes
    var messageStatusDeliveredIcon: Int = R.drawable.sceyt_ic_status_delivered

    @DrawableRes
    var messageStatusReadIcon: Int = R.drawable.sceyt_ic_status_read

    @ColorRes
    var senderNameTextColor: Int = SceytKitConfig.sceytColorAccent

    @ColorRes
    var replyMessageLineColor: Int = SceytKitConfig.sceytColorAccent

    @DrawableRes
    var dateSeparatorItemBackground = R.drawable.sceyt_bg_message_day

    @ColorRes
    var dateSeparatorItemTextColor = R.color.sceyt_color_gray_400

    @ColorRes
    var autoLinkTextColor = R.color.sceyt_auto_link_color

    @LayoutRes
    var emptyState: Int = R.layout.sceyt_messages_empty_state

    @LayoutRes
    var loadingState: Int = R.layout.sceyt_loading_state

    var dateSeparatorDateFormat = MessageDateSeparatorFormatter()

    @FontRes
    var dateSeparatorTextFont: Int = -1

    @StringRes
    var messageEditedText: Int = R.string.sceyt_edited

    var messageEditedTextStyle: Int = Typeface.ITALIC

    var dateSeparatorTextStyle: Int = Typeface.NORMAL

    @Dimension
    var sameSenderMsgDistance = dpToPx(4f)

    @Dimension
    var differentSenderMsgDistance = dpToPx(8f)

    @ColorRes
    var downScrollerUnreadCountColor: Int = SceytKitConfig.sceytColorAccent

    @ColorRes
    var mediaLoaderColor: Int = R.color.sceyt_color_white

    @DrawableRes
    var fileAttachmentIcon: Int = R.drawable.sceyt_ic_file_with_bg

    @ColorRes
    var selfReactionBackgroundColor = R.color.sceyt_self_reaction_color

    @ColorRes
    var selfReactionBorderColor = R.color.sceyt_color_divider

    @DrawableRes
    var swipeReplyIcon: Int = R.drawable.sceyt_is_reply_swipe

    internal fun updateWithAttributes(typedArray: TypedArray): MessagesStyle {
        incBubbleColor = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiMessageIncBubbleColor, incBubbleColor)
        outBubbleColor = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiMessageOutBubbleColor, outBubbleColor)
        messageStatusPendingIcon = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiMessagePendingIcon, messageStatusPendingIcon)
        messageStatusSentIcon = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiMessageSentIcon, messageStatusSentIcon)
        messageStatusDeliveredIcon = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiMessageDeliveredIcon, messageStatusDeliveredIcon)
        messageStatusReadIcon = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiMessageReadIcon, messageStatusReadIcon)
        senderNameTextColor = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiMessageSenderNameTextColor, senderNameTextColor)
        replyMessageLineColor = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiReplyMessageLineColor, replyMessageLineColor)
        dateSeparatorItemBackground = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiDateSeparatorItemBackground, dateSeparatorItemBackground)
        dateSeparatorItemTextColor = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiDateSeparatorItemTextColor, dateSeparatorItemTextColor)
        autoLinkTextColor = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiAutoLinkTextColor, autoLinkTextColor)
        sameSenderMsgDistance = typedArray.getDimensionPixelSize(R.styleable.MessagesListView_sceytUiSameSenderMessageDistance, sameSenderMsgDistance)
        differentSenderMsgDistance = typedArray.getDimensionPixelSize(R.styleable.MessagesListView_sceytUiDifferentSenderMessageDistance, differentSenderMsgDistance)
        messageEditedText = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiMessageEditedText, messageEditedText)
        messageEditedTextStyle = typedArray.getInt(R.styleable.MessagesListView_sceytUiMessageEditedTextStyle, messageEditedTextStyle)
        dateSeparatorTextFont = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiDateSeparatorItemTextFont, dateSeparatorTextFont)
        dateSeparatorTextStyle = typedArray.getInt(R.styleable.MessagesListView_sceytUiDateSeparatorItemTextStyle, dateSeparatorTextStyle)
        downScrollerUnreadCountColor = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiDownScrollerUnreadCountColor, downScrollerUnreadCountColor)
        mediaLoaderColor = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiMediaLoaderColor, mediaLoaderColor)
        fileAttachmentIcon = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiFileAttachmentIcon, fileAttachmentIcon)
        selfReactionBackgroundColor = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiSelfReactionBackgroundColor, selfReactionBackgroundColor)
        selfReactionBorderColor = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiSelfReactionBorderColor, selfReactionBorderColor)
        swipeReplyIcon = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiSwipeReplyIcon, swipeReplyIcon)
        return this
    }
}