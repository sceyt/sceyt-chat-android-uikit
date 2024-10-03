package com.sceyt.chatuikit.styles.extensions.messages_list

import android.content.res.TypedArray
import android.graphics.Typeface
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.applyTintBackgroundLayer
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.extensions.spToPx
import com.sceyt.chatuikit.styles.common.CheckboxStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.messages_list.DateSeparatorStyle
import com.sceyt.chatuikit.styles.messages_list.MessagesListViewStyle
import com.sceyt.chatuikit.styles.messages_list.ReactionPickerStyle
import com.sceyt.chatuikit.styles.messages_list.ScrollDownButtonStyle
import com.sceyt.chatuikit.styles.messages_list.UnreadMessagesSeparatorStyle
import com.sceyt.chatuikit.styles.messages_list.item.LinkPreviewStyle
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle
import com.sceyt.chatuikit.styles.messages_list.item.ReplyMessageStyle

internal fun MessagesListViewStyle.Builder.buildScrollDownTextStyle(
        typedArray: TypedArray
) = TextStyle.Builder(typedArray)
    .setBackgroundColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListScrollDownButtonUnreadCountTextBackgroundColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
    )
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListScrollDownButtonUnreadCountTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor))
    .setSize(
        index = R.styleable.MessagesListView_sceytUiMessagesListScrollDownButtonUnreadCountTextSize
    )
    .setFont(
        index = R.styleable.MessagesListView_sceytUiMessagesListScrollDownButtonUnreadCountTextFont
    )
    .build()


internal fun MessagesListViewStyle.Builder.buildScrollDownButtonStyle(
        typedArray: TypedArray
) = ScrollDownButtonStyle.Builder(context, typedArray)
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

// DateSeparatorStyle
internal fun MessagesListViewStyle.Builder.buildDateSeparatorTextStyle(
        typedArray: TypedArray
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListDateSeparatorTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor))
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
        typedArray: TypedArray
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
        typedArray: TypedArray
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
        typedArray: TypedArray
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
        typedArray: TypedArray
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


/* Item Body text style */
internal fun MessageItemStyle.Builder.buildBodyTextStyle(
        typedArray: TypedArray
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListBodyTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor))
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
        typedArray: TypedArray
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListDeletedMessageTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor))
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
        typedArray: TypedArray
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListSenderNameTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor))
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
        typedArray: TypedArray
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListMessageDateTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor))
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
        typedArray: TypedArray
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListMessageStateTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor))
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
        typedArray: TypedArray
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListMentionTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor))
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
        typedArray: TypedArray
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListVideoDurationTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor))
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
        typedArray: TypedArray
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListThreadReplyCountTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor))
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
        typedArray: TypedArray
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListForwardTitleTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor))
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
        typedArray: TypedArray
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListReactionCountTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor))
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
        typedArray: TypedArray
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListVoiceSpeedTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor))
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
        typedArray: TypedArray
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListVoiceDurationTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor))
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
        typedArray: TypedArray
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListAttachmentFileNameTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor))
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
        typedArray: TypedArray
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListAttachmentFileSizeTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor))
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
        typedArray: TypedArray
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListLinkPreviewTitleTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor))
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
        typedArray: TypedArray
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListLinkPreviewDescriptionTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor))
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

internal fun MessageItemStyle.Builder.buildLinkPreviewStyle(
        typedArray: TypedArray
) = LinkPreviewStyle.Builder(context, typedArray)
    .placeHolder(
        index = R.styleable.MessagesListView_sceytUiMessagesListLinkPreviewPlaceHolder
    )
    .titleStyle(buildLinkPreviewTitleTextStyle(typedArray))
    .descriptionStyle(buildLinkPreviewDescriptionTextStyle(typedArray))
    .build()

/* ReplyMessageStyle */
internal fun MessageItemStyle.Builder.buildReplyMessageTitleTextStyle(
        typedArray: TypedArray
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListReplyMessageTitleTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor))
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
        typedArray: TypedArray
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListReplyMessageSubtitleTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor))
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
        typedArray: TypedArray
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
        typedArray: TypedArray
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
        typedArray: TypedArray
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
        typedArray: TypedArray
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
        typedArray: TypedArray
) = com.sceyt.chatuikit.styles.common.MediaLoaderStyle.Builder(typedArray)
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
        typedArray: TypedArray
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
        typedArray: TypedArray
) = CheckboxStyle.Builder(typedArray)
    .checkedIcon(
        index = R.styleable.MessagesListView_sceytUiMessagesListSelectionCheckboxCheckedIcon,
        defValue = context.getCompatDrawable(R.drawable.sceyt_ic_checked_state_with_layers).applyTintBackgroundLayer(
            context.getCompatColor(SceytChatUIKit.theme.colors.accentColor), R.id.backgroundLayer
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
    .build()

/* Overlay Media Style*/
internal fun MessageItemStyle.Builder.buildOverlayMediaLoaderStyle(
        typedArray: TypedArray
) = com.sceyt.chatuikit.styles.common.MediaLoaderStyle.Builder(typedArray)
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