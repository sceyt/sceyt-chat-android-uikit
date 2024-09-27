package com.sceyt.chatuikit.styles.input

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.core.content.res.use
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.presentation.components.channel.input.MessageInputView
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.ButtonStyle
import com.sceyt.chatuikit.styles.common.LinkPreviewStyle
import com.sceyt.chatuikit.styles.common.TextInputStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.extensions.buildClearChatTextStyle
import com.sceyt.chatuikit.styles.extensions.buildEditMessageStyle
import com.sceyt.chatuikit.styles.extensions.buildInputCoverStyle
import com.sceyt.chatuikit.styles.extensions.buildInputTextInputStyle
import com.sceyt.chatuikit.styles.extensions.buildJoinButtonStyle
import com.sceyt.chatuikit.styles.extensions.buildLinkPreviewStyle
import com.sceyt.chatuikit.styles.extensions.buildMentionUsersListStyle
import com.sceyt.chatuikit.styles.extensions.buildMessageSearchControlStyle
import com.sceyt.chatuikit.styles.extensions.buildReplyMessageStyle
import com.sceyt.chatuikit.styles.extensions.buildSelectedMediaStyle
import com.sceyt.chatuikit.styles.extensions.buildVoiceRecordPlaybackViewStyle
import com.sceyt.chatuikit.styles.extensions.buildVoiceRecorderViewStyle
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme

/**
 * Style for [MessageInputView] component.
 * @param backgroundColor Background color for the input root view, default is [SceytChatUIKitTheme.backgroundColor]
 * @param dividerColor Color for the divider, default is [SceytChatUIKitTheme.borderColor]
 * @param sendIconBackgroundColor Background color for the send icon, default is [SceytChatUIKitTheme.accentColor]
 * @param attachmentIcon Icon for attachment button, default is [R.drawable.sceyt_ic_upload_file]
 * @param sendMessageIcon Icon for send message button, default is [R.drawable.sceyt_ic_send_message]
 * @param voiceRecordIcon Icon for voice record button, default is [R.drawable.sceyt_ic_voice_white]
 * @param sendVoiceMessageIcon Icon for send voice message button, default is [R.drawable.sceyt_ic_arrow_up]
 * @param closeIcon Icon for close button, default is [R.drawable.sceyt_ic_cancel]
 * @param enableVoiceRecord Enable voice recording, default is true
 * @param enableSendAttachment Enable send attachment, default is true
 * @param enableMention Enable mention, default is true
 * @param inputStyle Style for the input text, default is [buildInputTextInputStyle]
 * @param joinButtonStyle Style for the join button, default is [buildJoinButtonStyle]
 * @param clearChatTextStyle Style for the clear chat text, default is [buildClearChatTextStyle]
 * @param linkPreviewStyle Style for the link preview, default is [buildLinkPreviewStyle]
 * @param replyMessageStyle Style for the reply message, default is [buildReplyMessageStyle]
 * @param editMessageStyle Style for the edit message, default is [buildEditMessageStyle]
 * @param selectedMediaStyle Style for the selected media, default is [buildSelectedMediaStyle]
 * @param voiceRecorderViewStyle Style for the voice recorder view, default is [buildVoiceRecorderViewStyle]
 * @param voiceRecordPlaybackViewStyle Style for the voice record playback view, default is [buildVoiceRecordPlaybackViewStyle]
 * @param messageSearchControlsStyle Style for the message search controls, default is [buildMessageSearchControlStyle]
 * @param inputCoverStyle Style for the input cover, default is [buildInputCoverStyle]
 * @param mentionUsersListStyle Style for the mention users list, default is [buildMentionUsersListStyle]
 * */
data class MessageInputStyle(
        @ColorInt var backgroundColor: Int,
        @ColorInt var dividerColor: Int,
        @ColorInt var sendIconBackgroundColor: Int,
        var attachmentIcon: Drawable?,
        var sendMessageIcon: Drawable?,
        var voiceRecordIcon: Drawable?,
        var sendVoiceMessageIcon: Drawable?,
        var closeIcon: Drawable?,
        var enableVoiceRecord: Boolean,
        var enableSendAttachment: Boolean,
        var enableMention: Boolean,
        var inputStyle: TextInputStyle,
        var joinButtonStyle: ButtonStyle,
        var clearChatTextStyle: TextStyle,
        var linkPreviewStyle: LinkPreviewStyle,
        var replyMessageStyle: InputReplyMessageStyle,
        var editMessageStyle: InputEditMessageStyle,
        var selectedMediaStyle: InputSelectedMediaStyle,
        var voiceRecorderViewStyle: VoiceRecorderViewStyle,
        var voiceRecordPlaybackViewStyle: VoiceRecordPlaybackViewStyle,
        var messageSearchControlsStyle: MessageSearchControlsStyle,
        var inputCoverStyle: InputCoverStyle,
        var mentionUsersListStyle: MentionUsersListStyle
) {

    companion object {
        @JvmField
        var styleCustomizer = StyleCustomizer<MessageInputStyle> { _, style -> style }
    }

    internal class Builder(
            internal val context: Context,
            private val attrs: AttributeSet?
    ) {
        fun build(): MessageInputStyle {
            context.obtainStyledAttributes(attrs, R.styleable.MessageInputView).use { array ->
                val inputBackgroundColor = array.getColor(R.styleable.MessageInputView_sceytUiMessageInputBackgroundColor,
                    context.getCompatColor(SceytChatUIKit.theme.backgroundColor))

                val dividerColor = array.getColor(R.styleable.MessageInputView_sceytUiMessageInputDividerColor,
                    context.getCompatColor(SceytChatUIKit.theme.borderColor))

                val sendIconBackgroundColor = array.getColor(R.styleable.MessageInputView_sceytUiMessageInputSendIconBackgroundColor,
                    context.getCompatColor(SceytChatUIKit.theme.accentColor))

                val attachmentIcon = array.getDrawable(R.styleable.MessageInputView_sceytUiMessageInputAttachmentIcon)
                        ?: context.getCompatDrawable(R.drawable.sceyt_ic_upload_file)?.apply {
                            mutate().setTint(context.getCompatColor(SceytChatUIKit.theme.iconSecondaryColor))
                        }

                val sendMessageIcon = array.getDrawable(R.styleable.MessageInputView_sceytUiMessageInputSendIcon)
                        ?: context.getCompatDrawable(R.drawable.sceyt_ic_send_message)

                val voiceRecordIcon = array.getDrawable(R.styleable.MessageInputView_sceytUiMessageInputVoiceRecordIcon)
                        ?: context.getCompatDrawable(R.drawable.sceyt_ic_voice_white)

                val sendVoiceMessageIcon = array.getDrawable(R.styleable.MessageInputView_sceytUiMessageInputSendVoiceRecordIcon)
                        ?: context.getCompatDrawable(R.drawable.sceyt_ic_arrow_up)?.apply {
                            mutate().setTint(context.getCompatColor(SceytChatUIKit.theme.onPrimaryColor))
                        }

                val closeIcon = array.getDrawable(R.styleable.MessageInputView_sceytUiMessageInputCloseIcon)
                        ?: context.getCompatDrawable(R.drawable.sceyt_ic_cancel)?.apply {
                            mutate().setTint(context.getCompatColor(SceytChatUIKit.theme.iconSecondaryColor))
                        }

                val enableVoiceRecord = array.getBoolean(R.styleable.MessageInputView_sceytUiMessageInputEnableVoiceRecord,
                    true)

                val enableSendAttachment = array.getBoolean(R.styleable.MessageInputView_sceytUiMessageInputEnableSendAttachment,
                    true)

                val enableMention = array.getBoolean(R.styleable.MessageInputView_sceytUiMessageInputEnableMention,
                    true)

                return MessageInputStyle(
                    backgroundColor = inputBackgroundColor,
                    dividerColor = dividerColor,
                    sendIconBackgroundColor = sendIconBackgroundColor,
                    attachmentIcon = attachmentIcon,
                    sendMessageIcon = sendMessageIcon,
                    voiceRecordIcon = voiceRecordIcon,
                    sendVoiceMessageIcon = sendVoiceMessageIcon,
                    closeIcon = closeIcon,
                    enableVoiceRecord = enableVoiceRecord,
                    enableSendAttachment = enableSendAttachment,
                    enableMention = enableMention,
                    inputStyle = buildInputTextInputStyle(array),
                    joinButtonStyle = buildJoinButtonStyle(array),
                    clearChatTextStyle = buildClearChatTextStyle(array),
                    linkPreviewStyle = buildLinkPreviewStyle(array),
                    replyMessageStyle = buildReplyMessageStyle(array),
                    editMessageStyle = buildEditMessageStyle(array),
                    selectedMediaStyle = buildSelectedMediaStyle(array),
                    voiceRecorderViewStyle = buildVoiceRecorderViewStyle(array),
                    voiceRecordPlaybackViewStyle = buildVoiceRecordPlaybackViewStyle(array),
                    messageSearchControlsStyle = buildMessageSearchControlStyle(array),
                    inputCoverStyle = buildInputCoverStyle(array),
                    mentionUsersListStyle = buildMentionUsersListStyle(array)
                ).let { styleCustomizer.apply(context, it) }
            }
        }
    }
}
