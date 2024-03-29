package com.sceyt.sceytchatuikit.sceytstyles

import android.content.res.TypedArray
import android.graphics.Typeface
import androidx.annotation.*
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.extensions.dpToPx
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.sceytconfigs.dateformaters.MessageDateSeparatorFormatter

object MessagesStyle {

    @JvmField
    @ColorRes
    var incBubbleColor: Int = R.color.sceyt_color_bg_inc_message

    @JvmField
    @ColorRes
    var outBubbleColor: Int = R.color.sceyt_color_bg_out_message

    @JvmField
    @ColorRes
    var incLinkPreviewBackgroundColor: Int = R.color.sceyt_color_bg_inc_link_preview

    @JvmField
    @ColorRes
    var outLinkPreviewBackgroundColor: Int = R.color.sceyt_color_bg_out_link_preview

    @JvmField
    @DrawableRes
    var messageStatusPendingIcon: Int = R.drawable.sceyt_ic_status_not_sent

    @JvmField
    @DrawableRes
    var messageStatusSentIcon: Int = R.drawable.sceyt_ic_status_on_server

    @JvmField
    @DrawableRes
    var messageStatusDeliveredIcon: Int = R.drawable.sceyt_ic_status_delivered

    @JvmField
    @DrawableRes
    var messageStatusReadIcon: Int = R.drawable.sceyt_ic_status_read

    @JvmField
    @ColorRes
    var senderNameTextColor: Int = SceytKitConfig.sceytColorAccent

    @JvmField
    @ColorRes
    var replyMessageLineColor: Int = SceytKitConfig.sceytColorAccent

    @JvmField
    @DrawableRes
    var dateSeparatorItemBackground = R.drawable.sceyt_bg_date_separator

    @JvmField
    @ColorRes
    var dateSeparatorItemTextColor = R.color.sceyt_color_white

    @JvmField
    @ColorRes
    var autoLinkTextColor = R.color.sceyt_auto_link_color

    @JvmField
    @LayoutRes
    var emptyState: Int = R.layout.sceyt_messages_empty_state

    @JvmField
    @LayoutRes
    var emptyStateSelfChannel: Int = R.layout.sceyt_messages_empty_state_self_channel

    @JvmField
    @LayoutRes
    var loadingState: Int = R.layout.sceyt_loading_state

    @JvmField
    var dateSeparatorDateFormat = MessageDateSeparatorFormatter()

    @JvmField
    @FontRes
    var dateSeparatorTextFont: Int = -1

    @JvmField
    @StringRes
    var messageEditedText: Int = R.string.sceyt_edited

    @JvmField
    var messageEditedTextStyle: Int = Typeface.ITALIC

    @JvmField
    var dateSeparatorTextStyle: Int = Typeface.NORMAL

    @JvmField
    @Dimension
    var sameSenderMsgDistance = dpToPx(4f)

    @JvmField
    @Dimension
    var differentSenderMsgDistance = dpToPx(8f)

    @JvmField
    @ColorRes
    var downScrollerUnreadCountColor: Int = SceytKitConfig.sceytColorAccent

    @JvmField
    @ColorRes
    var mediaLoaderColor: Int = R.color.sceyt_color_white

    @JvmField
    @DrawableRes
    var videoDurationIcon: Int = R.drawable.sceyt_ic_video

    @JvmField
    @DrawableRes
    var fileAttachmentIcon: Int = R.drawable.sceyt_ic_file_with_bg

    @JvmField
    @DrawableRes
    var linkAttachmentIcon: Int = R.drawable.sceyt_ic_link_attachment

    @JvmField
    @ColorRes
    var selfReactionBackgroundColor = R.color.sceyt_self_reaction_color

    @JvmField
    @ColorRes
    var selfReactionBorderColor = R.color.sceyt_color_divider

    @JvmField
    @DrawableRes
    var swipeReplyIcon: Int = R.drawable.sceyt_is_reply_swipe

    internal fun updateWithAttributes(typedArray: TypedArray): MessagesStyle {
        incBubbleColor = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiMessageIncBubbleColor, incBubbleColor)
        outBubbleColor = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiMessageOutBubbleColor, outBubbleColor)
        incLinkPreviewBackgroundColor = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiMessageIncLinkPreviewBackgroundColor, incLinkPreviewBackgroundColor)
        outLinkPreviewBackgroundColor = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiMessageOutLinkPreviewBackgroundColor, outLinkPreviewBackgroundColor)
        messageStatusPendingIcon = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiMessagePendingIcon, messageStatusPendingIcon)
        messageStatusSentIcon = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiMessageSentIcon, messageStatusSentIcon)
        messageStatusDeliveredIcon = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiMessageDeliveredIcon, messageStatusDeliveredIcon)
        messageStatusReadIcon = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiMessageReadIcon, messageStatusReadIcon)
        senderNameTextColor = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiMessageSenderNameTextColor, senderNameTextColor)
        replyMessageLineColor = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiReplyMessageLineColor, replyMessageLineColor)
        dateSeparatorItemBackground = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiDateSeparatorItemBackground, dateSeparatorItemBackground)
        dateSeparatorItemTextColor = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiDateSeparatorItemTextColor, dateSeparatorItemTextColor)
        emptyState = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiEmptyStateLayout, emptyState)
        emptyStateSelfChannel = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiEmptyStateSelfChannelLayout, emptyStateSelfChannel)
        autoLinkTextColor = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiAutoLinkTextColor, autoLinkTextColor)
        sameSenderMsgDistance = typedArray.getDimensionPixelSize(R.styleable.MessagesListView_sceytUiSameSenderMessageDistance, sameSenderMsgDistance)
        differentSenderMsgDistance = typedArray.getDimensionPixelSize(R.styleable.MessagesListView_sceytUiDifferentSenderMessageDistance, differentSenderMsgDistance)
        messageEditedText = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiMessageEditedText, messageEditedText)
        messageEditedTextStyle = typedArray.getInt(R.styleable.MessagesListView_sceytUiMessageEditedTextStyle, messageEditedTextStyle)
        dateSeparatorTextFont = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiDateSeparatorItemTextFont, dateSeparatorTextFont)
        dateSeparatorTextStyle = typedArray.getInt(R.styleable.MessagesListView_sceytUiDateSeparatorItemTextStyle, dateSeparatorTextStyle)
        downScrollerUnreadCountColor = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiDownScrollerUnreadCountColor, downScrollerUnreadCountColor)
        mediaLoaderColor = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiMediaLoaderColor, mediaLoaderColor)
        videoDurationIcon = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiVideoDurationIcon, videoDurationIcon)
        fileAttachmentIcon = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiFileAttachmentIcon, fileAttachmentIcon)
        linkAttachmentIcon = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiLinkAttachmentIcon, linkAttachmentIcon)
        selfReactionBackgroundColor = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiSelfReactionBackgroundColor, selfReactionBackgroundColor)
        selfReactionBorderColor = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiSelfReactionBorderColor, selfReactionBorderColor)
        swipeReplyIcon = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiSwipeReplyIcon, swipeReplyIcon)
        return this
    }
}