package com.sceyt.chatuikit.presentation.customviews.voicerecorder

interface RecordingListener {
    fun onRecordingStarted() {}
    fun onRecordingLocked() {}
    fun onRecordingCompleted(shouldShowPreview: Boolean) {}
    fun onRecordingCanceled() {}
}