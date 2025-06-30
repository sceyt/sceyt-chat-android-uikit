package com.sceyt.chatuikit.presentation.components.channel.input.data

import android.text.Editable

sealed interface UserActivityState {
    data class Typing(
            val typing: Boolean,
            val text: Editable?,
    ) : UserActivityState

    data class Recording(val recording: Boolean) : UserActivityState
}