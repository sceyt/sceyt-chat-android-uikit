package com.sceyt.chatuikit.sceytstyles

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.MessageInputView
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme

/**
 * Style for [MessageInputView] component.
 * @param attachmentIcon Icon for attachment button, default is [R.drawable.sceyt_ic_upload_file]
 * @param sendMessageIcon Icon for send message button, default is [R.drawable.sceyt_ic_send_message]
 * @param voiceRecordIcon Icon for voice record button, default is [R.drawable.sceyt_ic_voice]
 * @param sendVoiceMessageIcon Icon for send voice message button, default is [R.drawable.sceyt_ic_arrow_up]
 * @param linkIcon Icon for link button, default is [R.drawable.sceyt_ic_link]
 * @param inputTextColor Color for the input text, default is [SceytChatUIKitTheme.textPrimaryColor]
 * @param inputHintTextColor Color for the input hint text, default is [SceytChatUIKitTheme.textFootnoteColor]
 * @param inputHintText Hint text for the input, default is [R.string.sceyt_write_a_message]
 * */
data class MessageInputStyle(
        var attachmentIcon: Drawable?,
        var sendMessageIcon: Drawable?,
        var voiceRecordIcon: Drawable?,
        var sendVoiceMessageIcon: Drawable?,
        var linkIcon: Drawable?,
        @ColorInt var backgroundColor: Int,
        @ColorInt var inputTextColor: Int,
        @ColorInt var inputHintTextColor: Int,
        @ColorInt var inputBackgroundColor: Int,
        var inputHintText: String
) {

    companion object {
        @JvmField
        var messageInputStyleCustomizer = StyleCustomizer<MessageInputStyle> { it }
    }

    internal class Builder(
            private val context: Context,
            private val attrs: AttributeSet?
    ) {
        fun build(): MessageInputStyle {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.MessageInputView, 0, 0)
            val attachmentIcon = typedArray.getDrawable(R.styleable.MessageInputView_sceytMessageInputAttachmentIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_upload_file)?.apply {
                        mutate().setTint(context.getCompatColor(SceytChatUIKit.theme.iconSecondaryColor))
                    }

            val sendMessageIcon = typedArray.getDrawable(R.styleable.MessageInputView_sceytMessageInputSendIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_send_message)?.apply {
                        mutate().setTint(context.getCompatColor(SceytChatUIKit.theme.textOnPrimaryColor))
                    }

            val voiceRecordIcon = typedArray.getDrawable(R.styleable.MessageInputView_sceytMessageInputVoiceRecordIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_voice)?.apply {
                        mutate().setTint(context.getCompatColor(SceytChatUIKit.theme.textOnPrimaryColor))
                    }

            val sendVoiceMessageIcon = typedArray.getDrawable(R.styleable.MessageInputView_sceytMessageInputSendVoiceRecordIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_arrow_up)?.apply {
                        mutate().setTint(context.getCompatColor(SceytChatUIKit.theme.textOnPrimaryColor))
                    }


            val linkIcon = typedArray.getDrawable(R.styleable.MessageInputView_sceytMessageInputLinkIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_link)?.apply {
                        mutate().setTint(context.getCompatColor(SceytChatUIKit.theme.accentColor))
                    }

            val inputTextColor = typedArray.getColor(R.styleable.MessageInputView_sceytMessageInputTextColor,
                context.getCompatColor(SceytChatUIKit.theme.textPrimaryColor))

            val inputHintTextColor = typedArray.getColor(R.styleable.MessageInputView_sceytMessageInputHintTextColor,
                context.getCompatColor(SceytChatUIKit.theme.textFootnoteColor))

            val inputHintText = typedArray.getString(R.styleable.MessageInputView_sceytMessageInputHintText)
                    ?: context.getString(R.string.sceyt_write_a_message)

            val backgroundColor = typedArray.getColor(R.styleable.MessageInputView_sceytMessageInputRootBackgroundColor,
                context.getCompatColor(SceytChatUIKit.theme.backgroundColor))

            val inputBackgroundColor = typedArray.getColor(R.styleable.MessageInputView_sceytMessageInputBackgroundColor,
                context.getCompatColor(SceytChatUIKit.theme.surface1Color))

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
                backgroundColor = backgroundColor,
                inputBackgroundColor = inputBackgroundColor
            ).let(messageInputStyleCustomizer::apply)
        }
    }
}
