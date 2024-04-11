package com.sceyt.sceytchatuikit.sceytstyles

import android.content.Context
import android.content.res.TypedArray
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

object MessageInputViewStyle {
    @JvmField
    @DrawableRes
    var attachmentIcon: Int = R.drawable.sceyt_ic_upload_file

    @JvmField
    @DrawableRes
    var sendMessageIcon: Int = R.drawable.sceyt_ic_send_message

    @JvmField
    @DrawableRes
    var voiceRecordIcon: Int = R.drawable.sceyt_ic_voice

    @JvmField
    @DrawableRes
    var sendVoiceMessageIcon: Int = R.drawable.sceyt_ic_arrow_up

    @JvmField
    @ColorRes
    var inputTextColor: Int = R.color.sceyt_color_black_themed

    @JvmField
    @ColorRes
    var inputHintTextColor: Int = R.color.sceyt_color_hint

    @JvmField
    @ColorRes
    var userNameTextColor: Int = SceytKitConfig.sceytColorAccent

    lateinit var inputHintText: String


    internal fun updateWithAttributes(context: Context, typedArray: TypedArray): MessageInputViewStyle {
        attachmentIcon = typedArray.getResourceId(R.styleable.MessageInputView_sceytMessageInputAttachmentIcon, attachmentIcon)
        sendMessageIcon = typedArray.getResourceId(R.styleable.MessageInputView_sceytMessageInputSendIcon, sendMessageIcon)
        voiceRecordIcon = typedArray.getResourceId(R.styleable.MessageInputView_sceytMessageInputVoiceRecordIcon, voiceRecordIcon)
        sendVoiceMessageIcon = typedArray.getResourceId(R.styleable.MessageInputView_sceytMessageInputSendVoiceRecordIcon, sendVoiceMessageIcon)
        inputTextColor = typedArray.getResourceId(R.styleable.MessageInputView_sceytMessageInputTextColor, inputTextColor)
        inputHintTextColor = typedArray.getResourceId(R.styleable.MessageInputView_sceytMessageInputHintTextColor, inputHintTextColor)
        inputHintText = typedArray.getString(R.styleable.MessageInputView_sceytMessageInputHintText)
                ?: context.getString(R.string.sceyt_message)
        userNameTextColor = typedArray.getResourceId(R.styleable.MessageInputView_sceytMessageInputUserNameTextColor, userNameTextColor)
        return this
    }
}