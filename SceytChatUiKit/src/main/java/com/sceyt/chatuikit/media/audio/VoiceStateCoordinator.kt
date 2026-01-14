package com.sceyt.chatuikit.media.audio

object VoiceStateCoordinator {
    private var recordingStopCallback: (() -> Unit)? = null
    private var isRecordingActive: () -> Boolean = { false }

    /**
     * Register the recording state provider and stop callback
     * Called by MessageInputView when attached to window
     */
    fun registerRecordingController(
        isRecordingProvider: () -> Boolean,
        stopRecordingCallback: () -> Unit
    ) {
        this.isRecordingActive = isRecordingProvider
        this.recordingStopCallback = stopRecordingCallback
    }

    /**
     * Unregister when MessageInputView is detached from window
     */
    fun unregisterRecordingController() {
        this.isRecordingActive = { false }
        this.recordingStopCallback = null
    }

    /**
     * Check if recording is currently active
     * Called by ViewHolders before starting playback
     */
    fun isRecording(): Boolean {
        return isRecordingActive()
    }

    /**
     * Request to stop any active recording
     * Called when playback is about to start
     */
    fun stopRecordingIfActive(): Boolean {
        if (isRecording()) {
            recordingStopCallback?.invoke()
            return true
        }
        return false
    }
}