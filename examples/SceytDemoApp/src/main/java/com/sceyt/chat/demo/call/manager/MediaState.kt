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

    /** Remote participant's video track */
    val remoteVideoTrack: VideoTrack? = null,

    /** Whether remote participant has muted their audio */
    val isRemoteMuted: Boolean = false,

    /** Whether remote participant has video enabled */
    val isRemoteVideoEnabled: Boolean = false,

    /** Whether local participant is on hold */
    val isOnHold: Boolean = false,

    /** Whether screen sharing is active */
    val isScreenSharing: Boolean = false
) {
    /**
     * Whether this is a video call (either local or remote has video enabled).
     */
    val isVideoCall: Boolean
        get() = isCameraEnabled || isRemoteVideoEnabled || localVideoTrack != null || remoteVideoTrack != null

    /**
     * Whether any video should be rendered (for UI layout decisions).
     */
    val hasActiveVideo: Boolean
        get() = (isCameraEnabled && localVideoTrack != null) ||
                (isRemoteVideoEnabled && remoteVideoTrack != null)

    /**
     * Whether to show full-screen remote video.
     */
    val shouldShowRemoteVideo: Boolean
        get() = isRemoteVideoEnabled && remoteVideoTrack != null

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

