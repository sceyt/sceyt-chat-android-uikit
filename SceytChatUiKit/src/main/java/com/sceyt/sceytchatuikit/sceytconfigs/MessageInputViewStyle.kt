package com.sceyt.sceytchatuikit.sceytconfigs

import android.content.Context
import android.content.res.TypedArray
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.sceyt.sceytchatuikit.R

object MessageInputViewStyle {
    @DrawableRes
    var attachmentIcon: Int = R.drawable.sceyt_ic_upload_file

    @DrawableRes
    var sendMessageIcon: Int = R.drawable.sceyt_ic_send_message

    @ColorRes
    var inputTextColor: Int = R.color.sceyt_color_black_themed

    @ColorRes
    var inputHintTextColor: Int = R.color.sceyt_color_hint

    lateinit var inputHintText: String


    internal fun updateWithAttributes(context: Context, typedArray: TypedArray): MessageInputViewStyle {
        attachmentIcon = typedArray.getResourceId(R.styleable.MessageInputView_sceytMessageInputAttachmentIcon, attachmentIcon)
        sendMessageIcon = typedArray.getResourceId(R.styleable.MessageInputView_sceytMessageInputSendIcon, sendMessageIcon)
        inputTextColor = typedArray.getResourceId(R.styleable.MessageInputView_sceytMessageInputTextColor, inputTextColor)
        inputHintTextColor = typedArray.getResourceId(R.styleable.MessageInputView_sceytMessageInputHintTextColor, inputHintTextColor)
        inputHintText = typedArray.getString(R.styleable.MessageInputView_sceytMessageInputHintText)
                ?: context.getString(R.string.sceyt_message)
        return this
    }
}