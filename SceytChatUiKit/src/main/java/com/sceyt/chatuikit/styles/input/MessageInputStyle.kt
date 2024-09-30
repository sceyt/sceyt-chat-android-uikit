package com.sceyt.chatuikit.styles.input

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.core.content.res.use
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.formatters.SceytChatUIKitFormatters
import com.sceyt.chatuikit.presentation.components.channel.input.MessageInputView
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.ButtonStyle
import com.sceyt.chatuikit.styles.common.TextInputStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.extensions.message_input.buildClearChatTextStyle
import com.sceyt.chatuikit.styles.extensions.message_input.buildEditMessageStyle
import com.sceyt.chatuikit.styles.extensions.message_input.buildInputCoverStyle
import com.sceyt.chatuikit.styles.extensions.message_input.buildInputTextInputStyle
import com.sceyt.chatuikit.styles.extensions.message_input.buildJoinButtonStyle
import com.sceyt.chatuikit.styles.extensions.message_input.buildLinkPreviewStyle
import com.sceyt.chatuikit.styles.extensions.message_input.buildMentionUserTextStyle
import com.sceyt.chatuikit.styles.extensions.message_input.buildMentionUsersListStyle
import com.sceyt.chatuikit.styles.extensions.message_input.buildMessageSearchControlStyle
import com.sceyt.chatuikit.styles.extensions.message_input.buildReplyMessageStyle
import com.sceyt.chatuikit.styles.extensions.message_input.buildSelectedMediaStyle
import com.sceyt.chatuikit.styles.extensions.message_input.buildVoiceRecordPlaybackViewStyle
import com.sceyt.chatuikit.styles.extensions.message_input.buildVoiceRecorderViewStyle
import com.sceyt.chatuikit.theme.Colors

/**
 * Style for [MessageInputView] component.
 * @property backgroundColor Background color for the input root view, default is [Colors.backgroundColor]
 * @property dividerColor Color for the divider, default is [Colors.borderColor]
 * @property sendIconBackgroundColor Background color for the send icon, default is [Colors.accentColor]
 * @property attachmentIcon Icon for attachment button, default is [R.drawable.sceyt_ic_upload_file]
 * @property sendMessageIcon Icon for send message button, default is [R.drawable.sceyt_ic_send_message]
 * @property voiceRecordIcon Icon for voice record button, default is [R.drawable.sceyt_ic_voice_white]
 * @property sendVoiceMessageIcon Icon for send voice message button, default is [R.drawable.sceyt_ic_arrow_up]
 * @property closeIcon Icon for close button, default is [R.drawable.sceyt_ic_cancel]
 * @property enableVoiceRecord Enable voice recording, default is true
 * @property enableSendAttachment Enable send attachment, default is true
 * @property enableMention Enable mention, default is true
 * @property inputStyle Style for the input text, default is [buildInputTextInputStyle]
 * @property joinButtonStyle Style for the join button, default is [buildJoinButtonStyle]
 * @property clearChatTextStyle Style for the clear chat text, default is [buildClearChatTextStyle]
 * @property linkPreviewStyle Style for the link preview, default is [buildLinkPreviewStyle]
 * @property replyMessageStyle Style for the reply message, default is [buildReplyMessageStyle]
 * @property editMessageStyle Style for the edit message, default is [buildEditMessageStyle]
 * @property selectedMediaStyle Style for the selected media, default is [buildSelectedMediaStyle]
 * @property voiceRecorderViewStyle Style for the voice recorder view, default is [buildVoiceRecorderViewStyle]
 * @property voiceRecordPlaybackViewStyle Style for the voice record playback view, default is [buildVoiceRecordPlaybackViewStyle]
 * @property messageSearchControlsStyle Style for the message search controls, default is [buildMessageSearchControlStyle]
 * @property inputCoverStyle Style for the input cover, default is [buildInputCoverStyle]
 * @property mentionUsersListStyle Style for the mention users list, default is [buildMentionUsersListStyle]
 * @property mentionTextStyle Style for the mention user name while typing, default is [buildMentionUserTextStyle]
 * @property mentionUserNameFormatter Formatter for the mention user name, default is [SceytChatUIKitFormatters.userNameFormatterNew]
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
        val mentionTextStyle: TextStyle,
        var linkPreviewStyle: InputLinkPreviewStyle,
        var replyMessageStyle: InputReplyMessageStyle,
        var editMessageStyle: InputEditMessageStyle,
        var selectedMediaStyle: InputSelectedMediaStyle,
        var voiceRecorderViewStyle: VoiceRecorderViewStyle,
        var voiceRecordPlaybackViewStyle: VoiceRecordPlaybackViewStyle,
        var messageSearchControlsStyle: MessageSearchControlsStyle,
        var inputCoverStyle: InputCoverStyle,
        var mentionUsersListStyle: MentionUsersListStyle,
        var mentionUserNameFormatter: Formatter<SceytUser>,
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
                    context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColor))

                val dividerColor = array.getColor(R.styleable.MessageInputView_sceytUiMessageInputDividerColor,
                    context.getCompatColor(SceytChatUIKit.theme.colors.borderColor))

                val sendIconBackgroundColor = array.getColor(R.styleable.MessageInputView_sceytUiMessageInputSendIconBackgroundColor,
                    context.getCompatColor(SceytChatUIKit.theme.colors.accentColor))

                val attachmentIcon = array.getDrawable(R.styleable.MessageInputView_sceytUiMessageInputAttachmentIcon)
                        ?: context.getCompatDrawable(R.drawable.sceyt_ic_upload_file)?.apply {
                            mutate().setTint(context.getCompatColor(SceytChatUIKit.theme.colors.iconSecondaryColor))
                        }

                val sendMessageIcon = array.getDrawable(R.styleable.MessageInputView_sceytUiMessageInputSendIcon)
                        ?: context.getCompatDrawable(R.drawable.sceyt_ic_send_message)

                val voiceRecordIcon = array.getDrawable(R.styleable.MessageInputView_sceytUiMessageInputVoiceRecordIcon)
                        ?: context.getCompatDrawable(R.drawable.sceyt_ic_voice_white)

                val sendVoiceMessageIcon = array.getDrawable(R.styleable.MessageInputView_sceytUiMessageInputSendVoiceRecordIcon)
                        ?: context.getCompatDrawable(R.drawable.sceyt_ic_arrow_up)?.apply {
                            mutate().setTint(context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor))
                        }

                val closeIcon = array.getDrawable(R.styleable.MessageInputView_sceytUiMessageInputCloseIcon)
                        ?: context.getCompatDrawable(R.drawable.sceyt_ic_cancel)?.apply {
                            mutate().setTint(context.getCompatColor(SceytChatUIKit.theme.colors.iconSecondaryColor))
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
                    mentionUsersListStyle = buildMentionUsersListStyle(array),
                    mentionTextStyle = buildMentionUserTextStyle(array),
                    mentionUserNameFormatter = SceytChatUIKit.formatters.userNameFormatterNew,
                ).let { styleCustomizer.apply(context, it) }
            }
        }
    }
}
