package com.sceyt.chatuikit.styles

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.presentation.extensions.getFormattedBody
import com.sceyt.chatuikit.presentation.extensions.isTextMessage
import com.sceyt.chatuikit.presentation.components.channel.input.MessageInputView
import com.sceyt.chatuikit.presentation.components.channel.input.mention.MessageBodyStyleHelper
import com.sceyt.chatuikit.formatters.MessageBodyFormatter
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme

/**
 * Style for [MessageInputView] component.
 * @param attachmentIcon Icon for attachment button, default is [R.drawable.sceyt_ic_upload_file]
 * @param sendMessageIcon Icon for send message button, default is [R.drawable.sceyt_ic_send_message]
 * @param voiceRecordIcon Icon for voice record button, default is [R.drawable.sceyt_ic_voice_white]
 * @param sendVoiceMessageIcon Icon for send voice message button, default is [R.drawable.sceyt_ic_arrow_up]
 * @param linkIcon Icon for link button, default is [R.drawable.sceyt_ic_link]
 * @param enableVoiceRecord Enable voice recording, default is true
 * @param enableSendAttachment Enable send attachment, default is true
 * @param enableMention Enable mention, default is true
 * @param sendIconBackgroundColor Background color for the send icon, default is [SceytChatUIKitTheme.accentColor]
 * @param inputTextColor Color for the input text, default is [SceytChatUIKitTheme.textPrimaryColor]
 * @param inputHintTextColor Color for the input hint text, default is [SceytChatUIKitTheme.textFootnoteColor]
 * @param inputBackgroundColor Background color for the input view, default is [SceytChatUIKitTheme.surface1Color]
 * @param inputHintText Hint text for the input, default is [R.string.sceyt_write_a_message]
 * @param replyMessageBodyFormatter Formatter for the reply message body, default is [MessageBodyFormatter] that returns the formatted body of the message
 * */
data class MessageInputStyle(
        var attachmentIcon: Drawable?,
        var sendMessageIcon: Drawable?,
        var voiceRecordIcon: Drawable?,
        var sendVoiceMessageIcon: Drawable?,
        var linkIcon: Drawable?,
        var enableVoiceRecord: Boolean,
        var enableSendAttachment: Boolean,
        var enableMention: Boolean,
        @ColorInt var sendIconBackgroundColor: Int,
        @ColorInt var inputTextColor: Int,
        @ColorInt var inputHintTextColor: Int,
        @ColorInt var inputBackgroundColor: Int,
        var inputHintText: String,
        val replyMessageBodyFormatter: MessageBodyFormatter
) {

    companion object {
        @JvmField
        var styleCustomizer = StyleCustomizer<MessageInputStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
            private val attrs: AttributeSet?
    ) {
        fun build(): MessageInputStyle {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.MessageInputView)
            val attachmentIcon = typedArray.getDrawable(R.styleable.MessageInputView_sceytUiMessageInputAttachmentIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_upload_file)?.apply {
                        mutate().setTint(context.getCompatColor(SceytChatUIKit.theme.iconSecondaryColor))
                    }

            val sendMessageIcon = typedArray.getDrawable(R.styleable.MessageInputView_sceytUiMessageInputSendIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_send_message)

            val voiceRecordIcon = typedArray.getDrawable(R.styleable.MessageInputView_sceytUiMessageInputVoiceRecordIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_voice_white)

            val sendVoiceMessageIcon = typedArray.getDrawable(R.styleable.MessageInputView_sceytUiMessageInputSendVoiceRecordIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_arrow_up)?.apply {
                        mutate().setTint(context.getCompatColor(SceytChatUIKit.theme.onPrimaryColor))
                    }


            val linkIcon = typedArray.getDrawable(R.styleable.MessageInputView_sceytUiMessageInputLinkIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_link)?.apply {
                        mutate().setTint(context.getCompatColor(SceytChatUIKit.theme.accentColor))
                    }

            val inputTextColor = typedArray.getColor(R.styleable.MessageInputView_sceytUiMessageInputTextColor,
                context.getCompatColor(SceytChatUIKit.theme.textPrimaryColor))

            val inputHintTextColor = typedArray.getColor(R.styleable.MessageInputView_sceytUiMessageInputHintTextColor,
                context.getCompatColor(SceytChatUIKit.theme.textFootnoteColor))

            val inputHintText = typedArray.getString(R.styleable.MessageInputView_sceytUiMessageInputHintText)
                    ?: context.getString(R.string.sceyt_write_a_message)

            val inputBackgroundColor = typedArray.getColor(R.styleable.MessageInputView_sceytUiMessageInputBackgroundColor,
                context.getCompatColor(SceytChatUIKit.theme.surface1Color))

            val sendIconBackgroundColor = typedArray.getColor(R.styleable.MessageInputView_sceytUiMessageInputSendIconBackgroundColor,
                context.getCompatColor(SceytChatUIKit.theme.accentColor))

            val enableVoiceRecord = typedArray.getBoolean(R.styleable.MessageInputView_sceytUiMessageInputEnableVoiceRecord,
                true)

            val enableSendAttachment = typedArray.getBoolean(R.styleable.MessageInputView_sceytUiMessageInputEnableSendAttachment,
                true)

            val enableMention = typedArray.getBoolean(R.styleable.MessageInputView_sceytUiMessageInputEnableMention,
                true)

            val replyMessageBodyFormatter = MessageBodyFormatter { context, message ->
                if (message.isTextMessage())
                    MessageBodyStyleHelper.buildOnlyBoldMentionsAndStylesWithAttributes(message)
                else message.getFormattedBody(context)
            }

            typedArray.recycle()

            return MessageInputStyle(
                attachmentIcon = attachmentIcon,
                sendMessageIcon = sendMessageIcon,
                voiceRecordIcon = voiceRecordIcon,
                sendVoiceMessageIcon = sendVoiceMessageIcon,
                linkIcon = linkIcon,
                inputTextColor = inputTextColor,
                inputHintTextColor = inputHintTextColor,
                inputHintText = inputHintText,
                inputBackgroundColor = inputBackgroundColor,
                sendIconBackgroundColor = sendIconBackgroundColor,
                enableVoiceRecord = enableVoiceRecord,
                enableSendAttachment = enableSendAttachment,
                enableMention = enableMention,
                replyMessageBodyFormatter = replyMessageBodyFormatter
            ).let { styleCustomizer.apply(context, it) }
        }
    }
}
