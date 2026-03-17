package com.sceyt.chat.demo.call.manager

import androidx.compose.runtime.Immutable
import org.webrtc.VideoTrack

/**
 * Represents the current media state of an active call.
 * This is exposed as a StateFlow for UI observation.
 */
@Immutable
data class MediaState(
    /** Whether local microphone is muted */
    val isMuted: Boolean = false,

    /** Whether local camera is enabled */
    val isCameraEnabled: Boolean = false,

    /** Whether front camera is active (vs back camera) */
    val isFrontCamera: Boolean = true,

    /** Whether audio is routed to speaker (vs earpiece/headset) */
    val isSpeakerOn: Boolean = false,

    /** Local video track for rendering preview */
    val localVideoTrack: VideoTrack? = null,

    /** Whether local participant is on hold */
    val isOnHold: Boolean = false,

    /** Whether screen sharing is active */
    val isScreenSharing: Boolean = false
) {
    /**
     * Whether to show local video preview overlay.
     */
    val shouldShowLocalPreview: Boolean
        get() = isCameraEnabled && localVideoTrack != null

    companion object {
        /** Default state for a new audio call */
        val DEFAULT_AUDIO = MediaState(
            isMuted = false,
            isCameraEnabled = false,
            isSpeakerOn = false
        )

        /** Default state for a new video call */
        val DEFAULT_VIDEO = MediaState(
            isMuted = false,
            isCameraEnabled = true,
            isSpeakerOn = true,
            isFrontCamera = true
        )
    }
}
