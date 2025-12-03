package com.sceyt.chatuikit.styles.extensions.messages_list

import android.content.res.TypedArray
import android.graphics.Typeface
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.extensions.spToPx
import com.sceyt.chatuikit.styles.common.AvatarStyle
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.CheckboxStyle
import com.sceyt.chatuikit.styles.common.MediaLoaderStyle
import com.sceyt.chatuikit.styles.common.MessageDeliveryStatusIcons
import com.sceyt.chatuikit.styles.common.Shape
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.messages_list.DateSeparatorStyle
import com.sceyt.chatuikit.styles.messages_list.MessagesListViewStyle
import com.sceyt.chatuikit.styles.messages_list.ReactionPickerStyle
import com.sceyt.chatuikit.styles.messages_list.ScrollButtonStyle
import com.sceyt.chatuikit.styles.messages_list.UnreadMessagesSeparatorStyle
import com.sceyt.chatuikit.styles.messages_list.item.LinkPreviewStyle
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle
import com.sceyt.chatuikit.styles.messages_list.item.PollStyle
import com.sceyt.chatuikit.styles.messages_list.item.ReplyMessageStyle

internal fun MessagesListViewStyle.Builder.buildScrollDownTextStyle(
    typedArray: TypedArray,
) = TextStyle.Builder(typedArray)
    .setBackgroundColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListScrollDownButtonUnreadCountTextBackgroundColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
    )
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListScrollDownButtonUnreadCountTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor)
    )
    .setSize(
        index = R.styleable.MessagesListView_sceytUiMessagesListScrollDownButtonUnreadCountTextSize
    )
    .setFont(
        index = R.styleable.MessagesListView_sceytUiMessagesListScrollDownButtonUnreadCountTextFont
    )
    .build()


internal fun MessagesListViewStyle.Builder.buildScrollUnreadMentionTextStyle(
    typedArray: TypedArray,
) = TextStyle.Builder(typedArray)
    .setBackgroundColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListScrollUnreadMentionButtonUnreadCountTextBackgroundColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
    )
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListScrollUnreadMentionButtonUnreadCountTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor)
    )
    .setSize(
        index = R.styleable.MessagesListView_sceytUiMessagesListScrollUnreadMentionButtonUnreadCountTextSize
    )
    .setFont(
        index = R.styleable.MessagesListView_sceytUiMessagesListScrollUnreadMentionButtonUnreadCountTextFont
    )
    .build()


internal fun MessagesListViewStyle.Builder.buildScrollDownButtonStyle(
    typedArray: TypedArray,
) = ScrollButtonStyle.Builder(context, typedArray)
    .backgroundColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListScrollDownButtonBackgroundColor
    )
    .icon(
        index = R.styleable.MessagesListView_sceytUiMessagesListScrollDownButtonIcon,
        defValue = context.getCompatDrawable(R.drawable.sceyt_ic_arrow_down).applyTint(
            context.getCompatColor(SceytChatUIKit.theme.colors.iconSecondaryColor)
        )
    )

    .unreadCountTextStyle(buildScrollDownTextStyle(typedArray))
    .build()

internal fun MessagesListViewStyle.Builder.buildScrollUnreadMentionButtonStyle(
    typedArray: TypedArray,
) = ScrollButtonStyle.Builder(context, typedArray)
    .backgroundColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListScrollUnreadMentionButtonBackgroundColor
    )
    .icon(
        index = R.styleable.MessagesListView_sceytUiMessagesListScrollUnreadMentionButtonIcon,
        defValue = context.getCompatDrawable(R.drawable.sceyt_ic_mention).applyTint(
            context.getCompatColor(SceytChatUIKit.theme.colors.iconSecondaryColor)
        )
    )

    .unreadCountTextStyle(buildScrollUnreadMentionTextStyle(typedArray))
    .build()

// DateSeparatorStyle
internal fun MessagesListViewStyle.Builder.buildDateSeparatorTextStyle(
    typedArray: TypedArray,
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListDateSeparatorTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor)
    )
    .setSize(
        index = R.styleable.MessagesListView_sceytUiMessagesListDateSeparatorTextSize
    )
    .setStyle(
        index = R.styleable.MessagesListView_sceytUiMessagesListDateSeparatorTextStyle
    )
    .setFont(
        index = R.styleable.MessagesListView_sceytUiMessagesListDateSeparatorTextFont
    )
    .build()

internal fun MessagesListViewStyle.Builder.buildDateSeparatorStyle(
    typedArray: TypedArray,
) = DateSeparatorStyle.Builder(context, typedArray)
    .backgroundColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListDateSeparatorBackgroundColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.overlayBackgroundColor)
    )
    .cornerRadius(
        index = R.styleable.MessagesListView_sceytUiMessagesListDateSeparatorCornersRadius,
        defValue = dpToPx(20f).toFloat()
    )
    .borderColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListDateSeparatorBorderColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.borderColor)
    )
    .borderWidth(
        index = R.styleable.MessagesListView_sceytUiMessagesListDateSeparatorBorderWidth
    )
    .textStyle(buildDateSeparatorTextStyle(typedArray))
    .build()

//UnreadMessagesSeparatorStyle
internal fun MessagesListViewStyle.Builder.buildUnreadMessagesSeparatorTextStyle(
    typedArray: TypedArray,
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListUnreadMessagesSeparatorTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
    )
    .setSize(
        index = R.styleable.MessagesListView_sceytUiMessagesListUnreadMessagesSeparatorTextSize
    )
    .setFont(
        index = R.styleable.MessagesListView_sceytUiMessagesListUnreadMessagesSeparatorTextFont,
        defValue = R.font.roboto_medium
    )
    .build()

internal fun MessagesListViewStyle.Builder.buildUnreadMessagesSeparatorStyle(
    typedArray: TypedArray,
) = UnreadMessagesSeparatorStyle.Builder(context, typedArray)
    .backgroundColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListUnreadMessagesSeparatorBackgroundColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.surface1Color)
    )
    .unreadText(
        index = R.styleable.MessagesListView_sceytUiMessagesListUnreadMessagesSeparatorUnreadText,
        defValue = context.getString(R.string.sceyt_new_messages)
    )
    .textStyle(buildUnreadMessagesSeparatorTextStyle(typedArray))
    .build()

// ReactionPickerStyle
internal fun MessagesListViewStyle.Builder.buildReactionPickerStyle(
    typedArray: TypedArray,
) = ReactionPickerStyle.Builder(context, typedArray)
    .backgroundColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListReactionPickerBackgroundColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColorSections)
    )
    .moreBackgroundColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListReactionPickerMoreBackgroundColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.surface2Color)
    )
    .selectedBackgroundColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListReactionPickerSelectedBackgroundColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.surface2Color)
    )
    .moreIcon(
        index = R.styleable.MessagesListView_sceytUiMessagesListReactionPickerMoreIcon,
        defValue = context.getCompatDrawable(R.drawable.sceyt_ic_plus).applyTint(
            context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
        )
    )
    .build()


internal fun MessageItemStyle.Builder.buildIncomingBubbleBackgroundStyle(
    typedArray: TypedArray,
) = BackgroundStyle.Builder(typedArray)
    .setBackgroundColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListIncomingBubbleColor,
        defValue = context.getCompatColor(R.color.sceyt_color_bg_inc_message)
    )
    .setShape(
        Shape.RoundedCornerShape(radius = dpToPx(18f).toFloat())
    )
    .build()


internal fun MessageItemStyle.Builder.buildOutgoingBubbleBackgroundStyle(
    typedArray: TypedArray,
) = BackgroundStyle.Builder(typedArray)
    .setBackgroundColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListOutgoingBubbleColor,
        defValue = defaultOutBubbleColor
    )
    .setShape(
        Shape.RoundedCornerShape(radius = dpToPx(18f).toFloat())
    )
    .build()


//Incoming reply background
internal fun MessageItemStyle.Builder.buildIncomingReplyBackgroundStyle(
    typedArray: TypedArray,
) = BackgroundStyle.Builder(typedArray)
    .setBackgroundColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListIncomingReplyBackgroundColor,
        defValue = context.getCompatColor(R.color.sceyt_color_surface_2)
    )
    .setShape(
        Shape.RoundedCornerShape(radius = 5f.dpToPx())
    )
    .build()

//Outgoing reply background
internal fun MessageItemStyle.Builder.buildOutgoingReplyBackgroundStyle(
    typedArray: TypedArray,
) = BackgroundStyle.Builder(typedArray)
    .setBackgroundColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListOutgoingReplyBackgroundColor,
        defValue = defaultOutDarkColor
    )
    .setShape(
        Shape.RoundedCornerShape(radius = 5f.dpToPx())
    )
    .build()

// OutLinkPreviewBackgroundStyle
internal fun MessageItemStyle.Builder.buildOutgoingLinkPreviewBackgroundStyle(
    typedArray: TypedArray,
) = BackgroundStyle.Builder(typedArray)
    .setBackgroundColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListOutgoingLinkPreviewBackgroundColor,
        defValue = defaultOutDarkColor
    )
    .setShape(
        Shape.RoundedCornerShape(radius = 8f.dpToPx())
    )
    .build()

//ReactionsContainerBackgroundStyle
internal fun MessageItemStyle.Builder.buildReactionsContainerBackgroundStyle(
    typedArray: TypedArray,
) = BackgroundStyle.Builder(typedArray)
    .setBackgroundColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListReactionsContainerBackgroundColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColorSections)
    )
    .setShape(
        Shape.RoundedCornerShape(radius = 20f.dpToPx())
    )
    .build()


// Incoming Link Preview Background
internal fun MessageItemStyle.Builder.buildIncomingLinkPreviewBackgroundStyle(
    typedArray: TypedArray,
) = BackgroundStyle.Builder(typedArray)
    .setBackgroundColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListIncomingLinkPreviewBackgroundColor,
        defValue = context.getCompatColor(R.color.sceyt_color_bg_inc_link_preview)
    )
    .setShape(
        Shape.RoundedCornerShape(radius = 5f.dpToPx())
    )
    .build()

// Delivery status icons
internal fun MessageItemStyle.Builder.buildDeliveryStatusIconStyle(
    typedArray: TypedArray,
) = MessageDeliveryStatusIcons.Builder(context, typedArray)
    .setPendingIconFromStyle(R.styleable.MessagesListView_sceytUiMessagesListMessageDeliveryStatusPendingIcon)
    .setSentIconFromStyle(R.styleable.MessagesListView_sceytUiMessagesListMessageDeliveryStatusSentIcon)
    .setReceivedIconIconFromStyle(R.styleable.MessagesListView_sceytUiMessagesListMessageDeliveryStatusReceivedIcon)
    .setDisplayedIconFromStyle(R.styleable.MessagesListView_sceytUiMessagesListMessageDeliveryStatusDisplayedIcon)
    .setFailedIconFromStyle(R.styleable.MessagesListView_sceytUiMessagesListMessageDeliveryStatusFailedIcon)
    .build()

/* Item Body text style */
internal fun MessageItemStyle.Builder.buildBodyTextStyle(
    typedArray: TypedArray,
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListBodyTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor)
    )
    .setSize(
        index = R.styleable.MessagesListView_sceytUiMessagesListBodyTextSize
    )
    .setStyle(
        index = R.styleable.MessagesListView_sceytUiMessagesListBodyTextStyle
    )
    .setFont(
        index = R.styleable.MessagesListView_sceytUiMessagesListBodyTextFont
    )
    .build()

/*DeletedMessageTextStyle*/
internal fun MessageItemStyle.Builder.buildDeletedMessageTextStyle(
    typedArray: TypedArray,
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListDeletedMessageTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
    )
    .setSize(
        index = R.styleable.MessagesListView_sceytUiMessagesListDeletedMessageTextSize
    )
    .setStyle(
        index = R.styleable.MessagesListView_sceytUiMessagesListDeletedMessageTextStyle,
        defValue = Typeface.ITALIC
    )
    .setFont(
        index = R.styleable.MessagesListView_sceytUiMessagesListDeletedMessageTextFont
    )
    .build()

/*SenderNameTextStyle*/
internal fun MessageItemStyle.Builder.buildSenderNameTextStyle(
    typedArray: TypedArray,
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListSenderNameTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
    )
    .setSize(
        index = R.styleable.MessagesListView_sceytUiMessagesListSenderNameTextSize
    )
    .setStyle(
        index = R.styleable.MessagesListView_sceytUiMessagesListSenderNameTextStyle
    )
    .setFont(
        index = R.styleable.MessagesListView_sceytUiMessagesListSenderNameTextFont,
        defValue = R.font.roboto_medium
    )
    .build()

/*MessageDateTextStyle*/
internal fun MessageItemStyle.Builder.buildMessageDateTextStyle(
    typedArray: TypedArray,
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListMessageDateTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
    )
    .setSize(
        index = R.styleable.MessagesListView_sceytUiMessagesListMessageDateTextSize,
        defValue = 12f.spToPx().toInt()
    )
    .setStyle(
        index = R.styleable.MessagesListView_sceytUiMessagesListMessageDateTextStyle
    )
    .setFont(
        index = R.styleable.MessagesListView_sceytUiMessagesListMessageDateTextFont
    )
    .build()

/*MessageStateTextStyle*/
internal fun MessageItemStyle.Builder.buildMessageStateTextStyle(
    typedArray: TypedArray,
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListMessageStateTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
    )
    .setSize(
        index = R.styleable.MessagesListView_sceytUiMessagesListMessageStateTextSize,
        defValue = 12f.spToPx().toInt()
    )
    .setStyle(
        index = R.styleable.MessagesListView_sceytUiMessagesListMessageStateTextStyle,
        defValue = Typeface.ITALIC
    )
    .setFont(
        index = R.styleable.MessagesListView_sceytUiMessagesListMessageStateTextFont
    )
    .build()

/*MentionTextStyle*/
internal fun MessageItemStyle.Builder.buildMentionTextStyle(
    typedArray: TypedArray,
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListMentionTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
    )
    .setSize(
        index = R.styleable.MessagesListView_sceytUiMessagesListMentionTextSize
    )
    .setStyle(
        index = R.styleable.MessagesListView_sceytUiMessagesListMentionTextStyle
    )
    .setFont(
        index = R.styleable.MessagesListView_sceytUiMessagesListMentionTextFont
    )
    .build()

/*VideoDurationTextStyle*/
internal fun MessageItemStyle.Builder.buildVideoDurationTextStyle(
    typedArray: TypedArray,
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListVideoDurationTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor)
    )
    .setSize(
        index = R.styleable.MessagesListView_sceytUiMessagesListVideoDurationTextSize
    )
    .setStyle(
        index = R.styleable.MessagesListView_sceytUiMessagesListVideoDurationTextStyle
    )
    .setFont(
        index = R.styleable.MessagesListView_sceytUiMessagesListVideoDurationTextFont
    )
    .build()

/*ThreadReplyCountTextStyle*/
internal fun MessageItemStyle.Builder.buildThreadReplyCountTextStyle(
    typedArray: TypedArray,
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListThreadReplyCountTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
    )
    .setSize(
        index = R.styleable.MessagesListView_sceytUiMessagesListThreadReplyCountTextSize
    )
    .setStyle(
        index = R.styleable.MessagesListView_sceytUiMessagesListThreadReplyCountTextStyle
    )
    .setFont(
        index = R.styleable.MessagesListView_sceytUiMessagesListThreadReplyCountTextFont
    )
    .build()

/*ForwardTitleTextStyle*/
internal fun MessageItemStyle.Builder.buildForwardTitleTextStyle(
    typedArray: TypedArray,
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListForwardTitleTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
    )
    .setSize(
        index = R.styleable.MessagesListView_sceytUiMessagesListForwardTitleTextSize
    )
    .setStyle(
        index = R.styleable.MessagesListView_sceytUiMessagesListForwardTitleTextStyle
    )
    .setFont(
        index = R.styleable.MessagesListView_sceytUiMessagesListForwardTitleTextFont,
        defValue = R.font.roboto_medium
    )
    .build()

/*ReactionCountTextStyle*/
internal fun MessageItemStyle.Builder.buildReactionCountTextStyle(
    typedArray: TypedArray,
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListReactionCountTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor)
    )
    .setSize(
        index = R.styleable.MessagesListView_sceytUiMessagesListReactionCountTextSize
    )
    .setStyle(
        index = R.styleable.MessagesListView_sceytUiMessagesListReactionCountTextStyle
    )
    .setFont(
        index = R.styleable.MessagesListView_sceytUiMessagesListReactionCountTextFont
    )
    .build()

/*VoiceSpeedTextStyle*/
internal fun MessageItemStyle.Builder.buildVoiceSpeedTextStyle(
    typedArray: TypedArray,
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListVoiceSpeedTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
    )
    .setSize(
        index = R.styleable.MessagesListView_sceytUiMessagesListVoiceSpeedTextSize
    )
    .setStyle(
        index = R.styleable.MessagesListView_sceytUiMessagesListVoiceSpeedTextStyle,
        defValue = Typeface.BOLD
    )
    .setFont(
        index = R.styleable.MessagesListView_sceytUiMessagesListVoiceSpeedTextFont
    )
    .build()

/*VoiceDurationTextStyle*/
internal fun MessageItemStyle.Builder.buildVoiceDurationTextStyle(
    typedArray: TypedArray,
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListVoiceDurationTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
    )
    .setSize(
        index = R.styleable.MessagesListView_sceytUiMessagesListVoiceDurationTextSize
    )
    .setStyle(
        index = R.styleable.MessagesListView_sceytUiMessagesListVoiceDurationTextStyle
    )
    .setFont(
        index = R.styleable.MessagesListView_sceytUiMessagesListVoiceDurationTextFont
    )
    .build()

/*AttachmentFileNameTextStyle*/
internal fun MessageItemStyle.Builder.buildAttachmentFileNameTextStyle(
    typedArray: TypedArray,
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListAttachmentFileNameTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor)
    )
    .setSize(
        index = R.styleable.MessagesListView_sceytUiMessagesListAttachmentFileNameTextSize
    )
    .setStyle(
        index = R.styleable.MessagesListView_sceytUiMessagesListAttachmentFileNameTextStyle
    )
    .setFont(
        index = R.styleable.MessagesListView_sceytUiMessagesListAttachmentFileNameTextFont,
        defValue = R.font.roboto_medium
    )
    .build()

/*AttachmentFileSizeTextStyle*/
internal fun MessageItemStyle.Builder.buildAttachmentFileSizeTextStyle(
    typedArray: TypedArray,
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListAttachmentFileSizeTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
    )
    .setSize(
        index = R.styleable.MessagesListView_sceytUiMessagesListAttachmentFileSizeTextSize
    )
    .setStyle(
        index = R.styleable.MessagesListView_sceytUiMessagesListAttachmentFileSizeTextStyle
    )
    .setFont(
        index = R.styleable.MessagesListView_sceytUiMessagesListAttachmentFileSizeTextFont
    )
    .build()

/*LinkPreviewStyle*/
internal fun MessageItemStyle.Builder.buildLinkPreviewTitleTextStyle(
    typedArray: TypedArray,
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListLinkPreviewTitleTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor)
    )
    .setSize(
        index = R.styleable.MessagesListView_sceytUiMessagesListLinkPreviewTitleTextSize
    )
    .setStyle(
        index = R.styleable.MessagesListView_sceytUiMessagesListLinkPreviewTitleTextStyle
    )
    .setFont(
        index = R.styleable.MessagesListView_sceytUiMessagesListLinkPreviewTitleTextFont
    )
    .build()

internal fun MessageItemStyle.Builder.buildLinkPreviewDescriptionTextStyle(
    typedArray: TypedArray,
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListLinkPreviewDescriptionTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
    )
    .setSize(
        index = R.styleable.MessagesListView_sceytUiMessagesListLinkPreviewDescriptionTextSize
    )
    .setStyle(
        index = R.styleable.MessagesListView_sceytUiMessagesListLinkPreviewDescriptionTextStyle
    )
    .setFont(
        index = R.styleable.MessagesListView_sceytUiMessagesListLinkPreviewDescriptionTextFont
    )
    .build()

/* Avatar style */
internal fun MessageItemStyle.Builder.buildAvatarTextStyle(
    typedArray: TypedArray,
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListAvatarTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor)
    )
    .setSize(
        index = R.styleable.MessagesListView_sceytUiMessagesListAvatarTextSize
    )
    .setStyle(
        index = R.styleable.MessagesListView_sceytUiMessagesListAvatarTextStyle
    )
    .setFont(
        index = R.styleable.MessagesListView_sceytUiMessagesListAvatarTextFont
    )
    .build()

@Suppress("UnusedReceiverParameter")
internal fun MessageItemStyle.Builder.buildAvatarShape(
    array: TypedArray,
): Shape {
    val value = array.getInt(R.styleable.MessagesListView_sceytUiMessagesListAvatarShape, 0)
    val cornerRadius =
        array.getDimension(R.styleable.MessagesListView_sceytUiMessagesListAvatarCornerRadius, 0f)
    return if (value == 1) {
        Shape.RoundedCornerShape(cornerRadius)
    } else Shape.Circle
}

internal fun MessageItemStyle.Builder.buildAvatarStyle(
    typedArray: TypedArray,
) = AvatarStyle.Builder(typedArray)
    .avatarBackgroundColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListAvatarBackgroundColor
    )
    .textStyle(buildAvatarTextStyle(typedArray))
    .shape(buildAvatarShape(typedArray))
    .build()

internal fun MessageItemStyle.Builder.buildLinkPreviewStyle(
    typedArray: TypedArray,
) = LinkPreviewStyle.Builder(context, typedArray)
    .placeHolder(
        index = R.styleable.MessagesListView_sceytUiMessagesListLinkPreviewPlaceHolder
    )
    .titleStyle(buildLinkPreviewTitleTextStyle(typedArray))
    .descriptionStyle(buildLinkPreviewDescriptionTextStyle(typedArray))
    .build()

/* ReplyMessageStyle */
internal fun MessageItemStyle.Builder.buildReplyMessageTitleTextStyle(
    typedArray: TypedArray,
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListReplyMessageTitleTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
    )
    .setSize(
        index = R.styleable.MessagesListView_sceytUiMessagesListReplyMessageTitleTextSize
    )
    .setStyle(
        index = R.styleable.MessagesListView_sceytUiMessagesListReplyMessageTitleTextStyle
    )
    .setFont(
        index = R.styleable.MessagesListView_sceytUiMessagesListReplyMessageTitleTextFont,
        defValue = R.font.roboto_medium
    )
    .build()

internal fun MessageItemStyle.Builder.buildReplyMessageSubtitleTextStyle(
    typedArray: TypedArray,
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListReplyMessageSubtitleTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor)
    )
    .setSize(
        index = R.styleable.MessagesListView_sceytUiMessagesListReplyMessageSubtitleTextSize
    )
    .setStyle(
        index = R.styleable.MessagesListView_sceytUiMessagesListReplyMessageSubtitleTextStyle
    )
    .setFont(
        index = R.styleable.MessagesListView_sceytUiMessagesListReplyMessageSubtitleTextFont
    )
    .build()

//Reply message mention text style
internal fun MessageItemStyle.Builder.buildReplyMessageMentionTextStyle(
    typedArray: TypedArray,
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListReplyMessageMentionTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
    )
    .setSize(
        index = R.styleable.MessagesListView_sceytUiMessagesListReplyMessageMentionTextSize
    )
    .setStyle(
        index = R.styleable.MessagesListView_sceytUiMessagesListReplyMessageMentionTextStyle,
    )
    .setFont(
        index = R.styleable.MessagesListView_sceytUiMessagesListReplyMessageMentionTextFont
    )
    .build()

//Reply message deleted text style
internal fun MessageItemStyle.Builder.buildReplyMessageDeletedTextStyle(
    typedArray: TypedArray,
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListReplyMessageDeletedTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
    )
    .setSize(
        index = R.styleable.MessagesListView_sceytUiMessagesListReplyMessageDeletedTextSize
    )
    .setStyle(
        index = R.styleable.MessagesListView_sceytUiMessagesListReplyMessageDeletedTextStyle,
        defValue = Typeface.ITALIC
    )
    .setFont(
        index = R.styleable.MessagesListView_sceytUiMessagesListReplyMessageDeletedTextFont
    )
    .build()

//Reply message attachment duration text style
internal fun MessageItemStyle.Builder.buildReplyMessageAttachmentDurationTextStyle(
    typedArray: TypedArray,
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListReplyMessageAttachmentDurationTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
    )
    .setSize(
        index = R.styleable.MessagesListView_sceytUiMessagesListReplyMessageAttachmentDurationTextSize
    )
    .setStyle(
        index = R.styleable.MessagesListView_sceytUiMessagesListReplyMessageAttachmentDurationTextStyle
    )
    .setFont(
        index = R.styleable.MessagesListView_sceytUiMessagesListReplyMessageAttachmentDurationTextFont
    )
    .build()


internal fun MessageItemStyle.Builder.buildReplyMessageStyle(
    typedArray: TypedArray,
) = ReplyMessageStyle.Builder(context, typedArray)
    .borderColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListReplyMessageBorderColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
    )
    .voiceDurationColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListReplyMessageVoiceDurationColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
    )
    .titleTextStyle(buildReplyMessageTitleTextStyle(typedArray))
    .subtitleTextStyle(buildReplyMessageSubtitleTextStyle(typedArray))
    .mentionTextStyle(buildReplyMessageMentionTextStyle(typedArray))
    .deletedMessageTextStyle(buildReplyMessageDeletedTextStyle(typedArray))
    .attachmentDurationTextStyle(buildReplyMessageAttachmentDurationTextStyle(typedArray))
    .build()


/* Media Loader Style */
internal fun MessageItemStyle.Builder.buildMediaLoaderStyle(
    typedArray: TypedArray,
) = MediaLoaderStyle.Builder(typedArray)
    .backgroundColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListMediaLoaderBackgroundColor
    )
    .progressColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListMediaLoaderProgressColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor)
    )
    .trackColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListMediaLoaderTrackColor
    )
    .cancelIcon(
        index = R.styleable.MessagesListView_sceytUiMessagesListMediaLoaderCancelIcon,
        defValue = context.getCompatDrawable(R.drawable.sceyt_ic_cancel_transfer).applyTint(
            context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor)
        )
    )
    .uploadIcon(
        index = R.styleable.MessagesListView_sceytUiMessagesListMediaLoaderUploadIcon,
        defValue = context.getCompatDrawable(R.drawable.sceyt_ic_upload).applyTint(
            context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor)
        )
    )
    .downloadIcon(
        index = R.styleable.MessagesListView_sceytUiMessagesListMediaLoaderDownloadIcon,
        defValue = context.getCompatDrawable(R.drawable.sceyt_ic_download).applyTint(
            context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor)
        )
    )
    .build()

/* Audio waveform style */
internal fun MessageItemStyle.Builder.buildAudioWaveformStyle(
    typedArray: TypedArray,
) = com.sceyt.chatuikit.styles.messages_list.item.AudioWaveformStyle.Builder(context, typedArray)
    .trackColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListAudioWaveformTrackColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.iconSecondaryColor)
    )
    .progressColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListAudioWaveformProgressColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
    )
    .build()

/* Selection checkbox style */
internal fun MessageItemStyle.Builder.buildSelectionCheckboxStyle(
    typedArray: TypedArray,
) = CheckboxStyle.Builder(typedArray)
    .checkedIcon(
        index = R.styleable.MessagesListView_sceytUiMessagesListSelectionCheckboxCheckedIcon,
        defValue = context.getCompatDrawable(R.drawable.sceyt_ic_checked_state).applyTint(
            context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
        )
    )
    .uncheckedIcon(
        index = R.styleable.MessagesListView_sceytUiMessagesListSelectionCheckboxUncheckedIcon,
        defValue = context.getCompatDrawable(R.drawable.sceyt_ic_unchecked_state).applyTint(
            context.getCompatColor(SceytChatUIKit.theme.colors.iconSecondaryColor)
        )
    )
    .pressedIcon(
        index = R.styleable.MessagesListView_sceytUiMessagesListSelectionCheckboxPressedIcon,
        defValue = context.getCompatDrawable(R.drawable.sceyt_ic_pressed_state).applyTint(
            context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
        )
    )
    .buttonTint(
        index = R.styleable.MessagesListView_sceytUiMessagesListSelectionCheckboxButtonTint
    )
    .build()

/* Overlay Media Style*/
internal fun MessageItemStyle.Builder.buildOverlayMediaLoaderStyle(
    typedArray: TypedArray,
) = MediaLoaderStyle.Builder(typedArray)
    .backgroundColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListOverlayMediaLoaderBackgroundColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.overlayBackground2Color)
    )
    .progressColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListOverlayMediaLoaderProgressColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor)
    )
    .trackColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListOverlayMediaLoaderTrackColor
    )
    .cancelIcon(
        index = R.styleable.MessagesListView_sceytUiMessagesListOverlayMediaLoaderCancelIcon,
        defValue = context.getCompatDrawable(R.drawable.sceyt_ic_cancel_transfer).applyTint(
            context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor)
        )
    )
    .uploadIcon(
        index = R.styleable.MessagesListView_sceytUiMessagesListOverlayMediaLoaderUploadIcon,
        defValue = context.getCompatDrawable(R.drawable.sceyt_ic_upload).applyTint(
            context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor)
        )
    )
    .downloadIcon(
        index = R.styleable.MessagesListView_sceytUiMessagesListOverlayMediaLoaderDownloadIcon,
        defValue = context.getCompatDrawable(R.drawable.sceyt_ic_download).applyTint(
            context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor)
        )
    )
    .build()

/* Poll voter avatar text style */
internal fun MessageItemStyle.Builder.buildPollVoterAvatarTextStyle(
    typedArray: TypedArray,
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListPollVoterAvatarTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor)
    )
    .setSize(
        index = R.styleable.MessagesListView_sceytUiMessagesListPollVoterAvatarTextSize
    )
    .setStyle(
        index = R.styleable.MessagesListView_sceytUiMessagesListPollVoterAvatarTextStyle
    )
    .setFont(
        index = R.styleable.MessagesListView_sceytUiMessagesListPollVoterAvatarTextFont
    )
    .build()

@Suppress("UnusedReceiverParameter")
internal fun MessageItemStyle.Builder.buildPollVoterAvatarShape(
    array: TypedArray,
): Shape {
    val value =
        array.getInt(R.styleable.MessagesListView_sceytUiMessagesListPollVoterAvatarShape, 0)
    val cornerRadius = array.getDimension(
        R.styleable.MessagesListView_sceytUiMessagesListPollVoterAvatarCornerRadius,
        0f
    )
    return if (value == 1) {
        Shape.RoundedCornerShape(cornerRadius)
    } else Shape.Circle
}

internal fun MessageItemStyle.Builder.buildPollVoterAvatarStyle(
    typedArray: TypedArray,
) = AvatarStyle.Builder(typedArray)
    .avatarBackgroundColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListPollVoterAvatarBackgroundColor
    )
    .borderWidth(
        index = R.styleable.MessagesListView_sceytUiMessagesListPollVoterAvatarBorderWidth
    )
    .borderColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListPollVoterAvatarBorderColor
    )
    .textStyle(buildPollVoterAvatarTextStyle(typedArray))
    .shape(buildPollVoterAvatarShape(typedArray))
    .build()

/* Poll Style */
internal fun MessageItemStyle.Builder.buildPollStyle(
    typedArray: TypedArray,
) = PollStyle.Builder(context, typedArray)
    .dividerColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListPollDividerColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColor)
    )
    .progressTrackColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListPollProgressBarTrackColor,
        defValue = context.getCompatColor(R.color.sceyt_progress_track_color)
    )
    .progressColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListPollProgressBarColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
    )
    .questionTextStyle(
        TextStyle.Builder(typedArray)
            .setColor(
                index = R.styleable.MessagesListView_sceytUiMessagesListPollQuestionTextColor,
                defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor)
            )
            .setSize(
                index = R.styleable.MessagesListView_sceytUiMessagesListPollQuestionTextSize
            )
            .setFont(
                index = R.styleable.MessagesListView_sceytUiMessagesListPollQuestionTextFont,
                defValue = R.font.roboto_medium
            )
            .build()
    )
    .pollTypeTextStyle(
        TextStyle.Builder(typedArray)
            .setColor(
                index = R.styleable.MessagesListView_sceytUiMessagesListPollTypeTextColor,
                defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
            )
            .setSize(
                index = R.styleable.MessagesListView_sceytUiMessagesListPollTypeTextSize
            )
            .setFont(
                index = R.styleable.MessagesListView_sceytUiMessagesListPollTypeTextFont
            )
            .build()
    )
    .viewResultsTextStyle(
        TextStyle.Builder(typedArray)
            .setColor(
                index = R.styleable.MessagesListView_sceytUiMessagesListPollViewResultsTextColor,
                defValue = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
            )
            .setSize(
                index = R.styleable.MessagesListView_sceytUiMessagesListPollViewResultsTextSize
            )
            .setFont(
                index = R.styleable.MessagesListView_sceytUiMessagesListPollViewResultsTextFont,
                defValue = R.font.roboto_medium
            )
            .build()
    )
    .viewResultsDisabledTextStyle(
        TextStyle.Builder(typedArray)
            .setColor(
                index = R.styleable.MessagesListView_sceytUiMessagesListPollViewResultsDisabledTextColor,
                defValue = context.getCompatColor(R.color.sceyt_color_disabled)
            )
            .setSize(
                index = R.styleable.MessagesListView_sceytUiMessagesListPollViewResultsDisabledTextSize
            )
            .setFont(
                index = R.styleable.MessagesListView_sceytUiMessagesListPollViewResultsDisabledTextFont,
                defValue = R.font.roboto_medium
            )
            .build()
    )
    .optionTextStyle(
        TextStyle.Builder(typedArray)
            .setColor(
                index = R.styleable.MessagesListView_sceytUiMessagesListPollOptionTextColor,
                defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor)
            )
            .setSize(
                index = R.styleable.MessagesListView_sceytUiMessagesListPollOptionTextSize
            )
            .setFont(
                index = R.styleable.MessagesListView_sceytUiMessagesListPollOptionTextFont
            )
            .build()
    )
    .voteCountTextStyle(
        TextStyle.Builder(typedArray)
            .setColor(
                index = R.styleable.MessagesListView_sceytUiMessagesListPollVoteCountTextColor,
                defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor)
            )
            .setSize(
                index = R.styleable.MessagesListView_sceytUiMessagesListPollVoteCountTextSize
            )
            .setFont(
                index = R.styleable.MessagesListView_sceytUiMessagesListPollVoteCountTextFont
            )
            .build()
    )
    .voterAvatarStyle(buildPollVoterAvatarStyle(typedArray))
    .checkboxStyle(
        CheckboxStyle.Builder(typedArray)
            .checkedIcon(
                index = R.styleable.MessagesListView_sceytUiMessagesListPollCheckboxCheckedIcon,
                defValue = context.getCompatDrawable(R.drawable.sceyt_ic_checked_state_20)
                    .applyTint(
                        context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
                    )
            )
            .uncheckedIcon(
                index = R.styleable.MessagesListView_sceytUiMessagesListPollCheckboxUncheckedIcon,
                defValue = context.getCompatDrawable(R.drawable.sceyt_ic_unchecked_state_20)
                    .applyTint(
                        context.getCompatColor(SceytChatUIKit.theme.colors.iconSecondaryColor)
                    )
            )
            .pressedIcon(
                index = R.styleable.MessagesListView_sceytUiMessagesListPollCheckboxPressedIcon,
            )
            .build()
    )
    .build()