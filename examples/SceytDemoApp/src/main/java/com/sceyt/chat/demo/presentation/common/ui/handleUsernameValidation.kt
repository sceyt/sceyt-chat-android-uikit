package com.sceyt.chat.demo.presentation.common.ui

import android.content.Context
import androidx.core.content.ContextCompat
import com.sceyt.chat.demo.R

fun handleUsernameValidation(
    context: Context,
    validationState: UsernameValidationEnum,
    setAlert: (color: Int, message: String) -> Unit,
    isUsernameCorrect: (Boolean) -> Unit
) {
    when (validationState) {
        UsernameValidationEnum.Valid -> {
            isUsernameCorrect(true)
            setAlert(
                ContextCompat.getColor(context, com.sceyt.chatuikit.R.color.sceyt_color_accent_5),
                context.getString(R.string.username_is_available)
            )
        }

        UsernameValidationEnum.AlreadyExists -> {
            isUsernameCorrect(false)
            setAlert(
                ContextCompat.getColor(context, com.sceyt.chatuikit.R.color.sceyt_color_warning),
                context.getString(R.string.error_username_taken)
            )
        }

        UsernameValidationEnum.IncorrectSize -> {
            isUsernameCorrect(false)
            setAlert(
                ContextCompat.getColor(context, com.sceyt.chatuikit.R.color.sceyt_color_warning),
                context.getString(R.string.username_length_error)
            )
        }

        UsernameValidationEnum.InvalidCharacters -> {
            isUsernameCorrect(false)
            setAlert(
                ContextCompat.getColor(context, com.sceyt.chatuikit.R.color.sceyt_color_warning),
                context.getString(R.string.username_invalid_characters)
            )
        }
    }
}