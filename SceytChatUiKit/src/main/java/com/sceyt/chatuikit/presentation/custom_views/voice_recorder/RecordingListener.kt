package com.sceyt.chatuikit.presentation.custom_views.voice_recorder

interface RecordingListener {
    fun onRecordingStarted() {}
    fun onRecordingLocked() {}
    fun onRecordingCompleted(shouldShowPreview: Boolean) {}
    fun onRecordingCanceled() {}
}