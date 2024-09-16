package com.sceyt.chatuikit.presentation.customviews.voice_recorder

interface RecordingListener {
    fun onRecordingStarted() {}
    fun onRecordingLocked() {}
    fun onRecordingCompleted(shouldShowPreview: Boolean) {}
    fun onRecordingCanceled() {}
}