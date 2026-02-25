package com.sceyt.chatuikit.presentation.custom_views.voice_recorder

enum class RecordingState {
    Recording,
    Preview,
    Idle;

    val isActive get() = this != Idle

    val isRecording get() = this == Recording
}