package com.sceyt.chatuikit.presentation.components.channel.input.data

import android.text.Editable

sealed interface InputUserActivity {

    data class Typing(
            val typing: Boolean,
            val text: Editable?,
    ) : InputUserActivity

    data class Recording(
            val recording: Boolean
    ) : InputUserActivity
}