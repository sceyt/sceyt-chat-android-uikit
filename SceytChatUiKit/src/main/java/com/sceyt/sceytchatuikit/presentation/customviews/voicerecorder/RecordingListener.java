package com.sceyt.sceytchatuikit.presentation.customviews.voicerecorder;

public interface RecordingListener {

    void onRecordingStarted();

    void onRecordingLocked();

    void onRecordingCompleted(boolean shouldShowPreview);

    void onRecordingCanceled();
}
