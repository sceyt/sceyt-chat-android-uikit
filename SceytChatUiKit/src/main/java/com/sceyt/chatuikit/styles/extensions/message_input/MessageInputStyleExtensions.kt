package com.sceyt.chatuikit.styles.extensions.message_input

import android.content.res.TypedArray
import android.graphics.Typeface
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.styles.common.ButtonStyle
import com.sceyt.chatuikit.styles.common.HintStyle
import com.sceyt.chatuikit.styles.common.TextInputStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.input.InputCoverStyle
import com.sceyt.chatuikit.styles.input.InputEditMessageStyle
import com.sceyt.chatuikit.styles.input.InputLinkPreviewStyle
import com.sceyt.chatuikit.styles.input.InputReplyMessageStyle
import com.sceyt.chatuikit.styles.input.InputSelectedMediaStyle
import com.sceyt.chatuikit.styles.input.MentionUsersListStyle
import com.sceyt.chatuikit.styles.input.MessageInputStyle
import com.sceyt.chatuikit.styles.input.MessageSearchControlsStyle
import com.sceyt.chatuikit.styles.input.VoiceRecordPlaybackViewStyle
import com.sceyt.chatuikit.styles.input.VoiceRecorderViewStyle
import com.sceyt.chatuikit.styles.messages_list.item.AudioWaveformStyle

internal fun MessageInputStyle.Builder.buildInputTextStyle(
        array: TypedArray
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor)
    )
    .setSize(
        index = R.styleable.MessageInputView_sceytUiMessageInputTextSize
    )
    .setStyle(
        index = R.styleable.MessageInputView_sceytUiMessageInputTextStyle
    )
    .setFont(
        index = R.styleable.MessageInputView_sceytUiMessageInputTextFont,
        defValue = R.font.roboto_regular
    )
    .build()

internal fun MessageInputStyle.Builder.buildInputHintStyle(
        array: TypedArray
) = HintStyle.Builder(array)
    .textColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputHintTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textFootnoteColor)
    )
    .hint(
        index = R.styleable.MessageInputView_sceytUiMessageInputHintText,
        defValue = context.getString(R.string.sceyt_write_a_message)
    )
    .build()

internal fun MessageInputStyle.Builder.buildInputTextInputStyle(
        array: TypedArray
) = TextInputStyle.Builder(array)
    .setBackgroundColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputTextInputBackgroundColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.surface1Color)
    )
    .setBorderColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputBorderColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.borderColor)
    )
    .setBorderWidth(
        index = R.styleable.MessageInputView_sceytUiMessageInputBorderWidth
    )
    .setCornerRadius(
        index = R.styleable.MessageInputView_sceytUiMessageInputCornersRadius,
        defValue = dpToPx(20f).toFloat()
    )
    .setTextStyle(
        textStyle = buildInputTextStyle(array)
    )
    .setHintStyle(
        hintStyle = buildInputHintStyle(array)
    )
    .build()

internal fun MessageInputStyle.Builder.buildJoinButtonTextStyle(
        array: TypedArray
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputJoinButtonTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
    )
    .setSize(
        index = R.styleable.MessageInputView_sceytUiMessageInputJoinButtonTextSize
    )
    .setStyle(
        index = R.styleable.MessageInputView_sceytUiMessageInputJoinButtonTextStyle
    )
    .setFont(
        index = R.styleable.MessageInputView_sceytUiMessageInputJoinButtonTextFont,
        defValue = R.font.roboto_medium
    )
    .build()

internal fun MessageInputStyle.Builder.buildJoinButtonStyle(
        array: TypedArray
) = ButtonStyle.Builder(array)
    .setBackgroundColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputJoinButtonBackgroundColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.surface1Color)
    )
    .setCornerRadius(
        index = R.styleable.MessageInputView_sceytUiMessageInputJoinButtonCornersRadius
    )
    .setBorderWidth(
        index = R.styleable.MessageInputView_sceytUiMessageInputJoinButtonBorderWidth
    )
    .setBorderColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputJoinButtonBorderColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.borderColor)
    )
    .setTextStyle(
        textStyle = buildJoinButtonTextStyle(array)
    )
    .build()

internal fun MessageInputStyle.Builder.buildClearChatTextStyle(
        array: TypedArray
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputClearChatTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
    )
    .setSize(
        index = R.styleable.MessageInputView_sceytUiMessageInputClearChatTextSize
    )
    .setStyle(
        index = R.styleable.MessageInputView_sceytUiMessageInputClearChatTextStyle
    )
    .setFont(
        index = R.styleable.MessageInputView_sceytUiMessageInputClearChatTextFont,
        defValue = R.font.roboto_medium
    )
    .build()

internal fun MessageInputStyle.Builder.buildLinkPreviewTitleTextStyle(
        array: TypedArray
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputLinkPreviewTitleTextColor,
        defValue = context.getCompatColor(R.color.sceyt_auto_link_color)
    )
    .setSize(
        index = R.styleable.MessageInputView_sceytUiMessageInputLinkPreviewTitleTextSize
    )
    .setStyle(
        index = R.styleable.MessageInputView_sceytUiMessageInputLinkPreviewTitleTextStyle
    )
    .setFont(
        index = R.styleable.MessageInputView_sceytUiMessageInputLinkPreviewTitleTextFont,
        defValue = R.font.roboto_medium
    )
    .build()

internal fun MessageInputStyle.Builder.buildLinkPreviewDescriptionTextStyle(
        array: TypedArray
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputLinkPreviewDescriptionTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
    )
    .setSize(
        index = R.styleable.MessageInputView_sceytUiMessageInputLinkPreviewDescriptionTextSize
    )
    .setStyle(
        index = R.styleable.MessageInputView_sceytUiMessageInputLinkPreviewDescriptionTextStyle
    )
    .setFont(
        index = R.styleable.MessageInputView_sceytUiMessageInputLinkPreviewDescriptionTextFont,
        defValue = R.font.roboto_regular
    )
    .build()

internal fun MessageInputStyle.Builder.buildLinkPreviewStyle(
        array: TypedArray
) = InputLinkPreviewStyle.Builder(context, array)
    .backgroundColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputLinkPreviewBackgroundColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.surface1Color)
    )
    .dividerColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputLinkPreviewDividerColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.borderColor)
    )
    .titleStyle(
        titleStyle = buildLinkPreviewTitleTextStyle(array)
    )
    .descriptionStyle(
        descriptionStyle = buildLinkPreviewDescriptionTextStyle(array)
    )
    .placeHolder(
        index = R.styleable.MessageInputView_sceytUiMessageInputLinkPreviewPlaceHolder,
        defValue = context.getCompatDrawable(R.drawable.sceyt_ic_link).applyTint(
            context, SceytChatUIKit.theme.colors.accentColor
        )
    )
    .build()

internal fun MessageInputStyle.Builder.buildReplyMessageTitleTextStyle(
        array: TypedArray
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputReplyMessageTitleTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
    )
    .setSize(
        index = R.styleable.MessageInputView_sceytUiMessageInputReplyMessageTitleTextSize
    )
    .setStyle(
        index = R.styleable.MessageInputView_sceytUiMessageInputReplyMessageTitleTextStyle
    )
    .setFont(
        index = R.styleable.MessageInputView_sceytUiMessageInputReplyMessageTitleTextFont,
        defValue = R.font.roboto_regular
    )
    .build()

internal fun MessageInputStyle.Builder.buildReplyMessageSenderNameTextStyle(
        array: TypedArray
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputReplyMessageSenderNameTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
    )
    .setSize(
        index = R.styleable.MessageInputView_sceytUiMessageInputReplyMessageSenderNameTextSize
    )
    .setStyle(
        index = R.styleable.MessageInputView_sceytUiMessageInputReplyMessageSenderNameTextStyle
    )
    .setFont(
        index = R.styleable.MessageInputView_sceytUiMessageInputReplyMessageSenderNameTextFont,
        defValue = R.font.roboto_medium
    )
    .build()

internal fun MessageInputStyle.Builder.buildReplyMessageBodyTextStyle(
        array: TypedArray
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputReplyMessageBodyTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
    )
    .setSize(
        index = R.styleable.MessageInputView_sceytUiMessageInputReplyMessageBodyTextSize
    )
    .setStyle(
        index = R.styleable.MessageInputView_sceytUiMessageInputReplyMessageBodyTextStyle
    )
    .setFont(
        index = R.styleable.MessageInputView_sceytUiMessageInputReplyMessageBodyTextFont,
        defValue = R.font.roboto_regular
    )
    .build()

internal fun MessageInputStyle.Builder.buildReplyMessageMentionTextStyle(
        array: TypedArray
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputReplyMessageMentionTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
    )
    .setSize(
        index = R.styleable.MessageInputView_sceytUiMessageInputReplyMessageMentionTextSize
    )
    .setStyle(
        index = R.styleable.MessageInputView_sceytUiMessageInputReplyMessageMentionTextStyle,
        defValue = Typeface.BOLD
    )
    .setFont(
        index = R.styleable.MessageInputView_sceytUiMessageInputReplyMessageMentionTextFont,
        defValue = R.font.roboto_regular
    )
    .build()

internal fun MessageInputStyle.Builder.buildReplyMessageAttachmentDurationTextStyle(
        array: TypedArray
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputReplyMessageAttachmentDurationTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
    )
    .setSize(
        index = R.styleable.MessageInputView_sceytUiMessageInputReplyMessageAttachmentDurationTextSize
    )
    .setStyle(
        index = R.styleable.MessageInputView_sceytUiMessageInputReplyMessageAttachmentDurationTextStyle
    )
    .setFont(
        index = R.styleable.MessageInputView_sceytUiMessageInputReplyMessageAttachmentDurationTextFont,
        defValue = R.font.roboto_regular
    )
    .build()

internal fun MessageInputStyle.Builder.buildReplyMessageStyle(
        array: TypedArray
) = InputReplyMessageStyle.Builder(context, array)
    .backgroundColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputReplyMessageBackgroundColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.surface1Color)
    )
    .replyIcon(
        index = R.styleable.MessageInputView_sceytUiMessageInputReplyMessageIcon,
        defValue = context.getCompatDrawable(R.drawable.sceyt_ic_input_reply).applyTint(
            context, SceytChatUIKit.theme.colors.accentColor
        )
    )
    .titleTextStyle(
        titleTextStyle = buildReplyMessageTitleTextStyle(array)
    )
    .senderNameTextStyle(
        senderNameTextStyle = buildReplyMessageSenderNameTextStyle(array)
    )
    .bodyTextStyle(
        bodyTextStyle = buildReplyMessageBodyTextStyle(array)
    )
    .mentionTextStyle(
        mentionTextStyle = buildReplyMessageMentionTextStyle(array)
    )
    .attachmentDurationTextStyle(
        attachmentDurationTextStyle = buildReplyMessageAttachmentDurationTextStyle(array)
    )
    .build()

internal fun MessageInputStyle.Builder.buildEditMessageTitleTextStyle(
        array: TypedArray
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputEditMessageTitleTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
    )
    .setSize(
        index = R.styleable.MessageInputView_sceytUiMessageInputEditMessageTitleTextSize
    )
    .setStyle(
        index = R.styleable.MessageInputView_sceytUiMessageInputEditMessageTitleTextStyle
    )
    .setFont(
        index = R.styleable.MessageInputView_sceytUiMessageInputEditMessageTitleTextFont,
        defValue = R.font.roboto_medium
    )
    .build()

internal fun MessageInputStyle.Builder.buildEditMessageBodyTextStyle(
        array: TypedArray
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputEditMessageBodyTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
    )
    .setSize(
        index = R.styleable.MessageInputView_sceytUiMessageInputEditMessageBodyTextSize
    )
    .setStyle(
        index = R.styleable.MessageInputView_sceytUiMessageInputEditMessageBodyTextStyle
    )
    .setFont(
        index = R.styleable.MessageInputView_sceytUiMessageInputEditMessageBodyTextFont,
        defValue = R.font.roboto_regular
    )
    .build()

//Edit mention text style
internal fun MessageInputStyle.Builder.buildEditMessageMentionTextStyle(
        array: TypedArray
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputEditMessageMentionTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
    )
    .setSize(
        index = R.styleable.MessageInputView_sceytUiMessageInputEditMessageMentionTextSize
    )
    .setStyle(
        index = R.styleable.MessageInputView_sceytUiMessageInputEditMessageMentionTextStyle,
        defValue = Typeface.BOLD
    )
    .setFont(
        index = R.styleable.MessageInputView_sceytUiMessageInputEditMessageMentionTextFont,
        defValue = R.font.roboto_regular
    )
    .build()


internal fun MessageInputStyle.Builder.buildEditMessageAttachmentDurationTextStyle(
        array: TypedArray
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputEditMessageAttachmentDurationTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
    )
    .setSize(
        index = R.styleable.MessageInputView_sceytUiMessageInputEditMessageAttachmentDurationTextSize
    )
    .setStyle(
        index = R.styleable.MessageInputView_sceytUiMessageInputEditMessageAttachmentDurationTextStyle
    )
    .setFont(
        index = R.styleable.MessageInputView_sceytUiMessageInputEditMessageAttachmentDurationTextFont,
        defValue = R.font.roboto_regular
    )
    .build()

internal fun MessageInputStyle.Builder.buildEditMessageStyle(
        array: TypedArray
) = InputEditMessageStyle.Builder(context, array)
    .backgroundColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputEditMessageBackgroundColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.surface1Color)
    )
    .editIcon(
        index = R.styleable.MessageInputView_sceytUiMessageInputEditMessageEditIcon,
        defValue = context.getCompatDrawable(R.drawable.sceyt_ic_edit).applyTint(
            context, SceytChatUIKit.theme.colors.accentColor
        )
    )
    .titleTextStyle(
        titleTextStyle = buildEditMessageTitleTextStyle(array)
    )
    .bodyTextStyle(
        bodyTextStyle = buildEditMessageBodyTextStyle(array)
    )
    .mentionTextStyle(
        mentionTextStyle = buildEditMessageMentionTextStyle(array)
    )
    .attachmentDurationTextStyle(
        attachmentDurationTextStyle = buildEditMessageAttachmentDurationTextStyle(array)
    )
    .build()

internal fun MessageInputStyle.Builder.buildSelectedMediaAttachmentDurationTextStyle(
        array: TypedArray
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputSelectedMediaAttachmentDurationTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
    )
    .setSize(
        index = R.styleable.MessageInputView_sceytUiMessageInputSelectedMediaAttachmentDurationTextSize
    )
    .setStyle(
        index = R.styleable.MessageInputView_sceytUiMessageInputSelectedMediaAttachmentDurationTextStyle
    )
    .setFont(
        index = R.styleable.MessageInputView_sceytUiMessageInputSelectedMediaAttachmentDurationTextFont,
        defValue = R.font.roboto_regular
    )
    .build()

internal fun MessageInputStyle.Builder.buildSelectedMediaFileAttachmentNameTextStyle(
        array: TypedArray
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputSelectedMediaFileAttachmentNameTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor)
    )
    .setSize(
        index = R.styleable.MessageInputView_sceytUiMessageInputSelectedMediaFileAttachmentNameTextSize
    )
    .setStyle(
        index = R.styleable.MessageInputView_sceytUiMessageInputSelectedMediaFileAttachmentNameTextStyle
    )
    .setFont(
        index = R.styleable.MessageInputView_sceytUiMessageInputSelectedMediaFileAttachmentNameTextFont,
        defValue = R.font.roboto_regular
    )
    .build()

internal fun MessageInputStyle.Builder.buildSelectedMediaFileAttachmentSizeTextStyle(
        array: TypedArray
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputSelectedMediaFileAttachmentSizeTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
    )
    .setSize(
        index = R.styleable.MessageInputView_sceytUiMessageInputSelectedMediaFileAttachmentSizeTextSize
    )
    .setStyle(
        index = R.styleable.MessageInputView_sceytUiMessageInputSelectedMediaFileAttachmentSizeTextStyle
    )
    .setFont(
        index = R.styleable.MessageInputView_sceytUiMessageInputSelectedMediaFileAttachmentSizeTextFont,
        defValue = R.font.roboto_regular
    )
    .build()

internal fun MessageInputStyle.Builder.buildSelectedMediaStyle(
        array: TypedArray
) = InputSelectedMediaStyle.Builder(context, array)
    .backgroundColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputSelectedMediaBackgroundColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColor)
    )
    .fileAttachmentBackgroundColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputSelectedMediaFileAttachmentBackgroundColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.surface1Color)
    )
    .removeAttachmentIcon(
        index = R.styleable.MessageInputView_sceytUiMessageInputSelectedMediaRemoveAttachmentIcon,
        defValue = context.getCompatDrawable(R.drawable.sceyt_ic_close).applyTint(
            context, SceytChatUIKit.theme.colors.iconSecondaryColor
        )
    )
    .attachmentDurationTextStyle(
        attachmentDurationTextStyle = buildSelectedMediaAttachmentDurationTextStyle(array)
    )
    .fileAttachmentNameTextStyle(
        fileAttachmentNameTextStyle = buildSelectedMediaFileAttachmentNameTextStyle(array)
    )
    .fileAttachmentSizeTextStyle(
        fileAttachmentSizeTextStyle = buildSelectedMediaFileAttachmentSizeTextStyle(array)
    )
    .build()

internal fun MessageInputStyle.Builder.buildVoiceRecorderViewSlideToCancelTextStyle(
        array: TypedArray
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputVoiceRecorderSlideToCancelTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
    )
    .setSize(
        index = R.styleable.MessageInputView_sceytUiMessageInputVoiceRecorderSlideToCancelTextSize
    )
    .setStyle(
        index = R.styleable.MessageInputView_sceytUiMessageInputVoiceRecorderSlideToCancelTextStyle
    )
    .setFont(
        index = R.styleable.MessageInputView_sceytUiMessageInputVoiceRecorderSlideToCancelTextFont,
        defValue = R.font.roboto_regular
    )
    .build()

internal fun MessageInputStyle.Builder.buildVoiceRecorderViewDurationTextStyle(
        array: TypedArray
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputVoiceRecorderDurationTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor)
    )
    .setSize(
        index = R.styleable.MessageInputView_sceytUiMessageInputVoiceRecorderDurationTextSize
    )
    .setStyle(
        index = R.styleable.MessageInputView_sceytUiMessageInputVoiceRecorderDurationTextStyle
    )
    .setFont(
        index = R.styleable.MessageInputView_sceytUiMessageInputVoiceRecorderDurationTextFont,
        defValue = R.font.roboto_regular
    )
    .build()

internal fun MessageInputStyle.Builder.buildVoiceRecorderViewCancelTextStyle(
        array: TypedArray
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputVoiceRecorderCancelTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
    )
    .setSize(
        index = R.styleable.MessageInputView_sceytUiMessageInputVoiceRecorderCancelTextSize
    )
    .setStyle(
        index = R.styleable.MessageInputView_sceytUiMessageInputVoiceRecorderCancelTextStyle
    )
    .setFont(
        index = R.styleable.MessageInputView_sceytUiMessageInputVoiceRecorderCancelTextFont,
        defValue = R.font.roboto_medium
    )
    .build()

internal fun MessageInputStyle.Builder.buildVoiceRecorderViewStyle(
        array: TypedArray,
) = VoiceRecorderViewStyle.Builder(context, array)
    .backgroundColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputVoiceRecorderBackgroundColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColor)
    )
    .recordingIndicatorColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputVoiceRecorderRecordingIndicatorColor,
        defValue = context.getCompatColor(R.color.sceyt_color_red)
    )
    .slideToCancelText(
        index = R.styleable.MessageInputView_sceytUiMessageInputVoiceRecorderSlideToCancelText,
        defValue = context.getString(R.string.sceyt_slide_to_cancel)
    )
    .cancelText(
        index = R.styleable.MessageInputView_sceytUiMessageInputVoiceRecorderCancelText,
        defValue = context.getString(R.string.sceyt_cancel)
    )
    .recordingIcon(
        index = R.styleable.MessageInputView_sceytUiMessageInputVoiceRecorderRecordingIcon,
        defValue = context.getCompatDrawable(R.drawable.sceyt_ic_voice_white).applyTint(
            context, SceytChatUIKit.theme.colors.onPrimaryColor
        )
    )
    .deleteRecordIcon(
        index = R.styleable.MessageInputView_sceytUiMessageInputVoiceRecorderDeleteRecordIcon,
        defValue = context.getCompatDrawable(R.drawable.sceyt_ic_delete_record).applyTint(
            context, SceytChatUIKit.theme.colors.onPrimaryColor
        )
    )
    .lockRecordingIcon(
        index = R.styleable.MessageInputView_sceytUiMessageInputVoiceRecorderLockRecordingIcon,
        defValue = context.getCompatDrawable(R.drawable.sceyt_ic_lock).applyTint(
            context, SceytChatUIKit.theme.colors.iconSecondaryColor
        )
    )
    .arrowToLockIcon(
        index = R.styleable.MessageInputView_sceytUiMessageInputVoiceRecorderArrowToLockIcon,
        defValue = context.getCompatDrawable(R.drawable.sceyt_ic_arrow_lock_recording).applyTint(
            context, SceytChatUIKit.theme.colors.accentColor
        )
    )
    .stopRecordingIcon(
        index = R.styleable.MessageInputView_sceytUiMessageInputVoiceRecorderStopRecordingIcon,
        defValue = context.getCompatDrawable(R.drawable.sceyt_ic_stop_voice).applyTint(
            context, SceytChatUIKit.theme.colors.accentColor
        )
    )
    .sendVoiceIcon(
        index = R.styleable.MessageInputView_sceytUiMessageInputVoiceRecorderSendVoiceIcon,
        defValue = context.getCompatDrawable(R.drawable.sceyt_ic_arrow_up)?.applyTint(
            context, SceytChatUIKit.theme.colors.onPrimaryColor
        )
    )
    .slideToCancelTextStyle(
        slideToCancelTextStyle = buildVoiceRecorderViewSlideToCancelTextStyle(array)
    )
    .durationTextStyle(
        durationTextStyle = buildVoiceRecorderViewDurationTextStyle(array)
    )
    .cancelTextStyle(
        cancelTextStyle = buildVoiceRecorderViewCancelTextStyle(array)
    )
    .build()

/*VoiceRecordPlaybackViewStyle*/
internal fun MessageInputStyle.Builder.buildVoiceRecordPlaybackViewDurationTextStyle(
        array: TypedArray
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputVoiceRecordPlaybackDurationTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
    )
    .setSize(
        index = R.styleable.MessageInputView_sceytUiMessageInputVoiceRecordPlaybackDurationTextSize
    )
    .setStyle(
        index = R.styleable.MessageInputView_sceytUiMessageInputVoiceRecordPlaybackDurationTextStyle
    )
    .setFont(
        index = R.styleable.MessageInputView_sceytUiMessageInputVoiceRecordPlaybackDurationTextFont,
        defValue = R.font.roboto_regular
    )
    .build()

/*AudioWaveForm style*/
internal fun MessageInputStyle.Builder.buildVoiceRecordPlaybackViewAudioWaveFormStyle(
        array: TypedArray
) = AudioWaveformStyle.Builder(context, array)
    .trackColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputVoiceRecordPlaybackTrackColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.iconSecondaryColor)
    )
    .progressColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputVoiceRecordPlaybackProgressColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
    )
    .build()

internal fun MessageInputStyle.Builder.buildVoiceRecordPlaybackViewStyle(
        array: TypedArray,
) = VoiceRecordPlaybackViewStyle.Builder(context, array)
    .backgroundColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputVoiceRecordPlaybackBackgroundColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColor)
    )
    .playerBackgroundColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputVoiceRecordPlaybackPlayerBackgroundColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.surface1Color)
    )
    .trackColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputVoiceRecordPlaybackTrackColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.iconSecondaryColor)
    )
    .progressColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputVoiceRecordPlaybackProgressColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
    )
    .closeIcon(
        index = R.styleable.MessageInputView_sceytUiMessageInputVoiceRecordPlaybackCloseIcon,
        defValue = context.getCompatDrawable(R.drawable.sceyt_ic_close)?.applyTint(
            context, SceytChatUIKit.theme.colors.iconSecondaryColor
        )
    )
    .playIcon(
        index = R.styleable.MessageInputView_sceytUiMessageInputVoiceRecordPlaybackPlayIcon,
        defValue = context.getCompatDrawable(R.drawable.sceyt_ic_play)?.applyTint(
            context, SceytChatUIKit.theme.colors.iconSecondaryColor
        )
    )
    .pauseIcon(
        index = R.styleable.MessageInputView_sceytUiMessageInputVoiceRecordPlaybackPauseIcon,
        defValue = context.getCompatDrawable(R.drawable.sceyt_ic_pause)?.applyTint(
            context, SceytChatUIKit.theme.colors.iconSecondaryColor
        )
    )
    .sendVoiceIcon(
        index = R.styleable.MessageInputView_sceytUiMessageInputVoiceRecordPlaybackSendVoiceIcon,
        defValue = context.getCompatDrawable(R.drawable.sceyt_ic_send_message)?.applyTint(
            context, SceytChatUIKit.theme.colors.onPrimaryColor
        )
    )
    .durationTextStyle(
        durationTextStyle = buildVoiceRecordPlaybackViewDurationTextStyle(array)
    )
    .audioWaveformStyle(
        audioWaveformStyle = buildVoiceRecordPlaybackViewAudioWaveFormStyle(array)
    )
    .build()

/* Message search control style */
internal fun MessageInputStyle.Builder.buildMessageSearchControlTextStyle(
        array: TypedArray
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputSearchControlResultTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor)
    )
    .setSize(
        index = R.styleable.MessageInputView_sceytUiMessageInputSearchControlResultTextSize
    )
    .setStyle(
        index = R.styleable.MessageInputView_sceytUiMessageInputSearchControlResultTextStyle
    )
    .setFont(
        index = R.styleable.MessageInputView_sceytUiMessageInputSearchControlResultTextFont,
        defValue = R.font.roboto_regular
    )
    .build()

internal fun MessageInputStyle.Builder.buildMessageSearchControlStyle(
        array: TypedArray
) = MessageSearchControlsStyle.Builder(context, array)
    .backgroundColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputSearchControlBackgroundColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColor)
    )
    .previousIcon(
        index = R.styleable.MessageInputView_sceytUiMessageInputSearchControlPreviousIcon,
        defValue = context.getCompatDrawable(R.drawable.sceyt_scroll_prev_button).applyTint(
            context, SceytChatUIKit.theme.colors.accentColor
        )
    )
    .nextIcon(
        index = R.styleable.MessageInputView_sceytUiMessageInputSearchControlNextIcon,
        defValue = context.getCompatDrawable(R.drawable.sceyt_scroll_next_button).applyTint(
            context, SceytChatUIKit.theme.colors.accentColor
        )
    )
    .resultTextStyle(
        resultTextStyle = buildMessageSearchControlTextStyle(array)
    )
    .build()


/* InputCoverStyle */
internal fun MessageInputStyle.Builder.buildInputCoverTextStyle(
        array: TypedArray
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputCoverTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor)
    )
    .setSize(
        index = R.styleable.MessageInputView_sceytUiMessageInputCoverTextSize
    )
    .setStyle(
        index = R.styleable.MessageInputView_sceytUiMessageInputCoverTextStyle
    )
    .setFont(
        index = R.styleable.MessageInputView_sceytUiMessageInputCoverTextFont
    )
    .build()

internal fun MessageInputStyle.Builder.buildInputCoverStyle(
        array: TypedArray
) = InputCoverStyle.Builder(context, array)
    .backgroundColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputCoverBackgroundColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColor)
    )
    .dividerColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputCoverDividerColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.borderColor)
    )
    .textStyle(
        textStyle = buildInputCoverTextStyle(array)
    )
    .build()

/* Mention users list */
internal fun MessageInputStyle.Builder.buildMentionUsersListTextStyle(
        array: TypedArray
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputMentionUsersListTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor)
    )
    .setSize(
        index = R.styleable.MessageInputView_sceytUiMessageInputMentionUsersListTextSize
    )
    .setStyle(
        index = R.styleable.MessageInputView_sceytUiMessageInputMentionUsersListTextStyle
    )
    .setFont(
        index = R.styleable.MessageInputView_sceytUiMessageInputMentionUsersListTextFont,
        defValue = R.font.roboto_regular
    )
    .build()

internal fun MessageInputStyle.Builder.buildMentionUsersListStyle(
        array: TypedArray
) = MentionUsersListStyle.Builder(context, array)
    .backgroundColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputMentionUsersListBackgroundColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColorSections)
    )
    .titleTextStyle(
        titleTextStyle = buildMentionUsersListTextStyle(array)
    )
    .build()


/*Mention text style */
internal fun MessageInputStyle.Builder.buildMentionTextStyle(
        array: TypedArray
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.MessageInputView_sceytUiMessageInputMentionTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
    )
    .setSize(
        index = R.styleable.MessageInputView_sceytUiMessageInputMentionTextSize
    )
    .setStyle(
        index = R.styleable.MessageInputView_sceytUiMessageInputMentionTextStyle
    )
    .setFont(
        index = R.styleable.MessageInputView_sceytUiMessageInputMentionTextFont,
        defValue = R.font.roboto_regular
    )
    .build()