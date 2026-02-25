package com.sceyt.chatuikit.presentation.components.channel.input.data

sealed class InputState {
    data object Voice : InputState()
    data class Recording(
        val isPreviewState: Boolean
    ) : InputState()
    data object Text : InputState()
    data class TextWithAttachments(
        val attachmentsCount: Int
    ) : InputState()

    data class Attachments(
        val count: Int
    ) : InputState()
}
