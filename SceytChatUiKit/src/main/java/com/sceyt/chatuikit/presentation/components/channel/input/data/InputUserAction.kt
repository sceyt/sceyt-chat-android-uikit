package com.sceyt.chatuikit.presentation.components.channel.input.data

import android.text.Editable

sealed interface InputUserAction {

    data class Typing(
            val typing: Boolean,
            val text: Editable?,
    ) : InputUserAction

    data class Recording(
            val recording: Boolean
    ) : InputUserAction
}